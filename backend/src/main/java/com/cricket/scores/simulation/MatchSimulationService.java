package com.cricket.scores.simulation;

import com.cricket.scores.dto.DtoMapper;
import com.cricket.scores.dto.Dtos;
import com.cricket.scores.exception.ResourceNotFoundException;
import com.cricket.scores.model.BallEvent;
import com.cricket.scores.model.ExtraType;
import com.cricket.scores.model.Innings;
import com.cricket.scores.model.Match;
import com.cricket.scores.model.MatchStatus;
import com.cricket.scores.model.Player;
import com.cricket.scores.model.PlayerMatchStats;
import com.cricket.scores.model.PlayerRole;
import com.cricket.scores.model.Team;
import com.cricket.scores.model.WicketType;
import com.cricket.scores.repository.BallEventRepository;
import com.cricket.scores.repository.InningsRepository;
import com.cricket.scores.repository.MatchRepository;
import com.cricket.scores.repository.PlayerMatchStatsRepository;
import com.cricket.scores.repository.PlayerRepository;
import com.cricket.scores.sse.ScoreBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulates T20 matches ball by ball on a fixed scheduler tick. Every delivery is
 * persisted (Innings totals, PlayerMatchStats, BallEvent) and then pushed to all
 * connected SSE clients, so the REST layer and the live stream stay in sync.
 *
 * The in-memory SimState (strike, bowler, next batsman) is derivable from the
 * database, so a restart mid-match resumes the simulation where it left off.
 */
@Service
public class MatchSimulationService {

    private static final Logger log = LoggerFactory.getLogger(MatchSimulationService.class);

    private final MatchRepository matchRepository;
    private final InningsRepository inningsRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMatchStatsRepository statsRepository;
    private final BallEventRepository ballEventRepository;
    private final LiveStateRegistry liveStateRegistry;
    private final ScoreBroadcastService broadcaster;
    private final DtoMapper mapper;
    private final Random random = new Random();

    @Value("${cricket.simulation.innings-break-ticks:3}")
    private int inningsBreakTicks;

    /** While seeding, the scheduler must not race with the historical data generation. */
    private final AtomicBoolean seeding = new AtomicBoolean(false);
    private final Map<Long, SimState> states = new ConcurrentHashMap<>();

    private enum BallOutcomeType { RUNS, WICKET, WIDE, NO_BALL }

    private record BallOutcome(BallOutcomeType type, int runs) {
    }

    private static final class SimState {
        Long strikerId;
        Long nonStrikerId;
        int nextBattingPosition;
        Long bowlerId;
        Long lastBowlerId;
        long sequence;
        int breakTicks;
    }

    public MatchSimulationService(MatchRepository matchRepository,
                                  InningsRepository inningsRepository,
                                  PlayerRepository playerRepository,
                                  PlayerMatchStatsRepository statsRepository,
                                  BallEventRepository ballEventRepository,
                                  LiveStateRegistry liveStateRegistry,
                                  ScoreBroadcastService broadcaster,
                                  DtoMapper mapper) {
        this.matchRepository = matchRepository;
        this.inningsRepository = inningsRepository;
        this.playerRepository = playerRepository;
        this.statsRepository = statsRepository;
        this.ballEventRepository = ballEventRepository;
        this.liveStateRegistry = liveStateRegistry;
        this.broadcaster = broadcaster;
        this.mapper = mapper;
    }

    public void setSeeding(boolean seeding) {
        this.seeding.set(seeding);
    }

    public boolean isSeeding() {
        return seeding.get();
    }

    // ------------------------------------------------------------- lifecycle

    @Transactional
    public Dtos.MatchSummary startMatch(Long matchId) {
        Match match = findMatch(matchId);
        if (match.getStatus() != MatchStatus.UPCOMING) {
            throw new IllegalStateException("Only upcoming matches can be started (status=" + match.getStatus() + ")");
        }
        Team battingTeam = random.nextBoolean() ? match.getTeamOne() : match.getTeamTwo();
        Team bowlingTeam = battingTeam.getId().equals(match.getTeamOne().getId()) ? match.getTeamTwo() : match.getTeamOne();

        createInnings(match, 1, battingTeam, bowlingTeam, null);

        SimState state = new SimState();
        state.strikerId = playerAtPosition(battingTeam, 1).getId();
        state.nonStrikerId = playerAtPosition(battingTeam, 2).getId();
        state.nextBattingPosition = 3;
        state.bowlerId = pickBowler(match, bowlingTeam, state).getId();
        state.sequence = 0;
        states.put(matchId, state);
        publishLiveState(match, state, 1);

        match.setStatus(MatchStatus.LIVE);
        match.setCurrentInningsNumber(1);
        match.setStartedAt(Instant.now());
        match.setLastUpdatedAt(Instant.now());

        Dtos.MatchSummary summary = mapper.toSummary(match);
        broadcast("STARTED", match.getId(), summary);
        log.info("Match {} started: {} vs {}", matchId, match.getTeamOne().getShortName(), match.getTeamTwo().getShortName());
        return summary;
    }

    @Transactional
    public void handleInningsBreak(Long matchId) {
        Match match = findMatch(matchId);
        if (match.getStatus() != MatchStatus.INNINGS_BREAK) {
            return;
        }
        SimState state = states.computeIfAbsent(matchId, id -> new SimState());
        state.breakTicks++;
        if (state.breakTicks < inningsBreakTicks) {
            return;
        }

        Innings first = inningsRepository.findByMatchIdAndInningsNumber(matchId, 1)
                .orElseThrow(() -> new ResourceNotFoundException("First innings missing for match " + matchId));
        Team battingTeam = first.getBowlingTeam();
        Team bowlingTeam = first.getBattingTeam();
        createInnings(match, 2, battingTeam, bowlingTeam, first.getRuns() + 1);

        SimState fresh = new SimState();
        fresh.strikerId = playerAtPosition(battingTeam, 1).getId();
        fresh.nonStrikerId = playerAtPosition(battingTeam, 2).getId();
        fresh.nextBattingPosition = 3;
        fresh.bowlerId = pickBowler(match, bowlingTeam, fresh).getId();
        fresh.sequence = lastSequence(matchId);
        states.put(matchId, fresh);
        publishLiveState(match, fresh, 2);

        match.setCurrentInningsNumber(2);
        match.setStatus(MatchStatus.LIVE);
        match.setLastUpdatedAt(Instant.now());

        broadcast("INNINGS_BREAK", matchId, mapper.toSummary(match));
        log.info("Match {}: second innings started, target {}", matchId, first.getRuns() + 1);
    }

    /** Rebuilds the in-memory state for a live match (e.g. right after a restart). */
    @Transactional
    public void ensureLiveState(Long matchId) {
        Match match = findMatch(matchId);
        if (match.getStatus() != MatchStatus.LIVE) {
            return;
        }
        Innings innings = inningsRepository.findByMatchIdAndInningsNumber(matchId, match.getCurrentInningsNumber()).orElse(null);
        if (innings != null) {
            ensureState(match, innings);
        }
    }

    // ------------------------------------------------------------- ball play

    @Transactional
    public void playBall(Long matchId) {
        Match match = findMatch(matchId);
        if (match.getStatus() != MatchStatus.LIVE) {
            return;
        }
        Innings innings = inningsRepository.findByMatchIdAndInningsNumber(matchId, match.getCurrentInningsNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Innings missing for match " + matchId));
        SimState state = ensureState(match, innings);

        if (isInningsOver(match, innings)) {
            closeInnings(match, innings, state);
            return;
        }
        playBallInternal(match, innings, state);
    }

    private void playBallInternal(Match match, Innings innings, SimState state) {
        int overIndex = innings.getLegalBalls() / 6;
        int ballSlot = innings.getLegalBalls() % 6 + 1;

        BallOutcome outcome = nextOutcome(overIndex, match.getOversPerInnings());
        boolean legal = outcome.type() != BallOutcomeType.WIDE && outcome.type() != BallOutcomeType.NO_BALL;

        Player striker = playerRepository.findById(state.strikerId).orElseThrow();
        Player bowler = playerRepository.findById(state.bowlerId).orElseThrow();
        PlayerMatchStats strikerStats = statsFor(match, striker);
        PlayerMatchStats bowlerStats = statsFor(match, bowler);

        int totalRuns = outcome.runs();
        String commentary = "";
        Player dismissed = null;
        WicketType wicketType = null;

        switch (outcome.type()) {
            case WIDE -> {
                commentary = commentary("wide", striker.getName(), bowler.getName());
                bowlerStats.setRunsConceded(bowlerStats.getRunsConceded() + 1);
            }
            case NO_BALL -> {
                commentary = commentary("noball", striker.getName(), bowler.getName());
                bowlerStats.setRunsConceded(bowlerStats.getRunsConceded() + 1);
            }
            case RUNS -> {
                strikerStats.setRuns(strikerStats.getRuns() + totalRuns);
                if (totalRuns == 4) {
                    strikerStats.setFours(strikerStats.getFours() + 1);
                } else if (totalRuns == 6) {
                    strikerStats.setSixes(strikerStats.getSixes() + 1);
                }
                commentary = commentary(totalRuns == 0 ? "dot" : String.valueOf(totalRuns),
                        striker.getName(), bowler.getName());
            }
            case WICKET -> {
                innings.setWickets(innings.getWickets() + 1);
                wicketType = pickWicketType();
                dismissed = (wicketType == WicketType.RUN_OUT && random.nextBoolean())
                        ? playerRepository.findById(state.nonStrikerId).orElseThrow()
                        : striker;
                PlayerMatchStats dismissedStats = statsFor(match, dismissed);
                dismissedStats.setOut(true);
                dismissedStats.setDismissalText(dismissalText(wicketType, bowler, innings.getBowlingTeam()));
                if (wicketType != WicketType.RUN_OUT) {
                    bowlerStats.setWickets(bowlerStats.getWickets() + 1);
                }
                commentary = commentary("wicket", dismissed.getName(), bowler.getName());
                if (state.nextBattingPosition <= 11) {
                    Player next = playerAtPosition(innings.getBattingTeam(), state.nextBattingPosition);
                    state.nextBattingPosition++;
                    if (dismissed.getId().equals(state.strikerId)) {
                        state.strikerId = next.getId();
                    } else {
                        state.nonStrikerId = next.getId();
                    }
                }
            }
        }

        innings.setRuns(innings.getRuns() + totalRuns);
        if (legal) {
            innings.setLegalBalls(innings.getLegalBalls() + 1);
            strikerStats.setBallsFaced(strikerStats.getBallsFaced() + 1);
            bowlerStats.setBallsBowled(bowlerStats.getBallsBowled() + 1);
            bowlerStats.setRunsConceded(bowlerStats.getRunsConceded() + totalRuns);
        }

        BallEvent event = new BallEvent();
        event.setMatch(match);
        event.setInningsNumber(innings.getInningsNumber());
        event.setSequence(++state.sequence);
        event.setOverNumber(overIndex);
        event.setBallInOver(ballSlot);
        event.setBatsman(striker);
        event.setBowler(bowler);
        event.setRunsScored(totalRuns);
        event.setExtraType(switch (outcome.type()) {
            case WIDE -> ExtraType.WIDE;
            case NO_BALL -> ExtraType.NO_BALL;
            default -> null;
        });
        event.setWicket(outcome.type() == BallOutcomeType.WICKET);
        event.setWicketType(wicketType);
        event.setDismissedPlayer(dismissed);
        event.setCommentary(commentary);
        ballEventRepository.save(event);

        if (outcome.type() == BallOutcomeType.RUNS && totalRuns % 2 == 1) {
            swapStrike(state);
        }
        boolean overCompleted = legal && innings.getLegalBalls() % 6 == 0;
        if (overCompleted) {
            swapStrike(state);
            state.lastBowlerId = state.bowlerId;
            state.bowlerId = pickBowler(match, innings.getBowlingTeam(), state).getId();
        }

        match.setLastUpdatedAt(Instant.now());

        if (innings.getTarget() != null && innings.getRuns() >= innings.getTarget()) {
            innings.setCompleted(true);
            completeMatch(match);
            return;
        }
        if (isInningsOver(match, innings)) {
            closeInnings(match, innings, state);
            return;
        }
        publishLiveState(match, state, innings.getInningsNumber());
        broadcast("BALL", match.getId(), mapper.toSummary(match));
    }

    // ------------------------------------------------------- innings / match

    private boolean isInningsOver(Match match, Innings innings) {
        return innings.getWickets() >= 10 || innings.getLegalBalls() >= match.getOversPerInnings() * 6;
    }

    private void closeInnings(Match match, Innings innings, SimState state) {
        innings.setCompleted(true);
        if (innings.getInningsNumber() == 1) {
            match.setStatus(MatchStatus.INNINGS_BREAK);
            match.setLastUpdatedAt(Instant.now());
            state.breakTicks = 0;
            broadcast("INNINGS_BREAK", match.getId(), mapper.toSummary(match));
            log.info("Match {}: first innings closed at {}/{} ({} overs), break starts",
                    match.getId(), innings.getRuns(), innings.getWickets(), DtoMapper.overs(innings.getLegalBalls()));
        } else {
            completeMatch(match);
        }
    }

    private void completeMatch(Match match) {
        List<Innings> all = inningsRepository.findByMatchIdOrderByInningsNumber(match.getId());
        Innings first = all.get(0);
        Innings second = all.size() > 1 ? all.get(1) : null;
        if (second != null) {
            second.setCompleted(true);
        }

        Team winner = null;
        String result;
        if (second != null && second.getRuns() > first.getRuns()) {
            winner = second.getBattingTeam();
            int ballsLeft = match.getOversPerInnings() * 6 - second.getLegalBalls();
            result = winner.getName() + " won by " + (10 - second.getWickets()) + " wicket"
                    + (10 - second.getWickets() == 1 ? "" : "s")
                    + " (" + ballsLeft + " balls remaining)";
        } else if (second != null && second.getRuns() == first.getRuns()) {
            result = "Match tied";
        } else {
            winner = first.getBattingTeam();
            int margin = second == null ? first.getRuns() : first.getRuns() - second.getRuns();
            result = winner.getName() + " won by " + margin + " run" + (margin == 1 ? "" : "s");
        }

        match.setWinner(winner);
        match.setResultText(result);
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(Instant.now());
        match.setLastUpdatedAt(Instant.now());
        states.remove(match.getId());
        liveStateRegistry.remove(match.getId());

        broadcast("COMPLETED", match.getId(), mapper.toSummary(match));
        log.info("Match {} completed: {}", match.getId(), result);
    }

    // ----------------------------------------------------------------- setup

    private void createInnings(Match match, int number, Team battingTeam, Team bowlingTeam, Integer target) {
        Innings innings = new Innings();
        innings.setMatch(match);
        innings.setInningsNumber(number);
        innings.setBattingTeam(battingTeam);
        innings.setBowlingTeam(bowlingTeam);
        innings.setTarget(target);
        inningsRepository.save(innings);

        for (Player player : playerRepository.findByTeamIdOrderByBattingPosition(battingTeam.getId())) {
            ensureStatsRow(match, player, battingTeam, player.getBattingPosition());
        }
        for (Player player : playerRepository.findByTeamIdOrderByBattingPosition(bowlingTeam.getId())) {
            ensureStatsRow(match, player, bowlingTeam, 0);
        }
    }

    /**
     * One PlayerMatchStats row per player per match (covers batting + bowling across both
     * innings). When a player's side bats for the first time, their batting position is
     * stamped onto the existing row.
     */
    private void ensureStatsRow(Match match, Player player, Team team, int battingPosition) {
        PlayerMatchStats stats = statsRepository.findByMatchIdAndPlayerId(match.getId(), player.getId()).orElse(null);
        if (stats == null) {
            stats = new PlayerMatchStats();
            stats.setMatch(match);
            stats.setPlayer(player);
            stats.setTeam(team);
            stats.setBattingPosition(battingPosition);
            statsRepository.save(stats);
            return;
        }
        if (battingPosition > 0 && stats.getBattingPosition() == 0) {
            stats.setBattingPosition(battingPosition);
            statsRepository.save(stats);
        }
    }

    private SimState ensureState(Match match, Innings innings) {
        SimState state = states.get(match.getId());
        if (state != null) {
            return state;
        }
        state = new SimState();
        state.sequence = lastSequence(match.getId());
        state.nextBattingPosition = innings.getWickets() + 3;

        List<PlayerMatchStats> notOut = statsRepository.findByMatchId(match.getId()).stream()
                .filter(s -> s.getTeam().getId().equals(innings.getBattingTeam().getId()))
                .filter(s -> s.getBattingPosition() > 0 && !s.isOut())
                .sorted(Comparator.comparingInt(PlayerMatchStats::getBattingPosition))
                .toList();
        if (notOut.size() >= 2) {
            state.strikerId = notOut.get(0).getPlayer().getId();
            state.nonStrikerId = notOut.get(1).getPlayer().getId();
        } else {
            List<Player> order = playerRepository.findByTeamIdOrderByBattingPosition(innings.getBattingTeam().getId());
            state.strikerId = order.get(Math.min(innings.getWickets(), 10)).getId();
            state.nonStrikerId = order.get(Math.min(innings.getWickets() + 1, 10)).getId();
        }

        BallEvent last = ballEventRepository.findFirstByMatchIdOrderBySequenceDesc(match.getId()).orElse(null);
        if (last != null) {
            state.bowlerId = last.getBowler().getId();
            state.lastBowlerId = state.bowlerId;
        } else {
            state.bowlerId = pickBowler(match, innings.getBowlingTeam(), state).getId();
        }
        states.put(match.getId(), state);
        publishLiveState(match, state, innings.getInningsNumber());
        log.info("Match {}: simulation state rebuilt from database", match.getId());
        return state;
    }

    // ------------------------------------------------------------- mechanics

    private BallOutcome nextOutcome(int overIndex, int totalOvers) {
        boolean death = overIndex >= totalOvers - 5;
        double roll = random.nextDouble();
        if (death) {
            if (roll < 0.26) return new BallOutcome(BallOutcomeType.RUNS, 0);
            if (roll < 0.53) return new BallOutcome(BallOutcomeType.RUNS, 1);
            if (roll < 0.65) return new BallOutcome(BallOutcomeType.RUNS, 2);
            if (roll < 0.67) return new BallOutcome(BallOutcomeType.RUNS, 3);
            if (roll < 0.82) return new BallOutcome(BallOutcomeType.RUNS, 4);
            if (roll < 0.90) return new BallOutcome(BallOutcomeType.RUNS, 6);
            if (roll < 0.96) return new BallOutcome(BallOutcomeType.WICKET, 0);
            if (roll < 0.98) return new BallOutcome(BallOutcomeType.WIDE, 1);
            return new BallOutcome(BallOutcomeType.NO_BALL, 1);
        }
        if (roll < 0.34) return new BallOutcome(BallOutcomeType.RUNS, 0);
        if (roll < 0.64) return new BallOutcome(BallOutcomeType.RUNS, 1);
        if (roll < 0.76) return new BallOutcome(BallOutcomeType.RUNS, 2);
        if (roll < 0.775) return new BallOutcome(BallOutcomeType.RUNS, 3);
        if (roll < 0.865) return new BallOutcome(BallOutcomeType.RUNS, 4);
        if (roll < 0.91) return new BallOutcome(BallOutcomeType.RUNS, 6);
        if (roll < 0.95) return new BallOutcome(BallOutcomeType.WICKET, 0);
        if (roll < 0.975) return new BallOutcome(BallOutcomeType.WIDE, 1);
        return new BallOutcome(BallOutcomeType.NO_BALL, 1);
    }

    private WicketType pickWicketType() {
        double roll = random.nextDouble();
        if (roll < 0.45) return WicketType.CAUGHT;
        if (roll < 0.70) return WicketType.BOWLED;
        if (roll < 0.82) return WicketType.LBW;
        if (roll < 0.87) return WicketType.STUMPED;
        return WicketType.RUN_OUT;
    }

    private Player pickBowler(Match match, Team bowlingTeam, SimState state) {
        int maxOvers = Math.max(1, match.getOversPerInnings() / 5);
        List<Player> candidates = playerRepository.findByTeamIdAndRoleIn(bowlingTeam.getId(),
                List.of(PlayerRole.BOWLER, PlayerRole.ALL_ROUNDER));
        List<Player> pool = candidates.stream()
                .filter(p -> !p.getId().equals(state.lastBowlerId))
                .filter(p -> oversBowled(match, p) < maxOvers)
                .toList();
        if (pool.isEmpty()) {
            pool = candidates.stream().filter(p -> !p.getId().equals(state.lastBowlerId)).toList();
        }
        if (pool.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private int oversBowled(Match match, Player player) {
        return statsRepository.findByMatchIdAndPlayerId(match.getId(), player.getId())
                .map(PlayerMatchStats::getBallsBowled).orElse(0) / 6;
    }

    private String dismissalText(WicketType wicketType, Player bowler, Team bowlingTeam) {
        List<Player> fielders = playerRepository.findByTeamIdOrderByBattingPosition(bowlingTeam.getId());
        Player keeper = fielders.stream()
                .filter(p -> p.getRole() == PlayerRole.WICKETKEEPER)
                .findFirst().orElse(fielders.get(0));
        Player fielder = fielders.get(random.nextInt(fielders.size()));
        return switch (wicketType) {
            case BOWLED -> "b " + bowler.getName();
            case LBW -> "lbw b " + bowler.getName();
            case CAUGHT -> "c " + fielder.getName() + " b " + bowler.getName();
            case STUMPED -> "st " + keeper.getName() + " b " + bowler.getName();
            case RUN_OUT -> "run out (" + fielder.getName() + ")";
        };
    }

    private String commentary(String kind, String batsman, String bowler) {
        return switch (kind) {
            case "dot" -> pick(
                    bowler + " lands it on a length, " + batsman + " defends",
                    "No run — " + batsman + " pushes it to cover",
                    "Tight from " + bowler + ", dot ball",
                    batsman + " leaves it alone outside off");
            case "1" -> pick(
                    batsman + " works it into the leg side for a single",
                    "One run, quick steal by " + batsman,
                    batsman + " nudges to mid-on and takes one");
            case "2" -> pick(
                    batsman + " drives into the deep and comes back for two",
                    "Two runs, placed wide of midwicket by " + batsman);
            case "3" -> pick(
                    "Three! " + batsman + " finds the gap in the deep",
                    "Hard running — three to " + batsman);
            case "4" -> pick(
                    "FOUR! " + batsman + " threads the field",
                    "FOUR! Crisply driven by " + batsman,
                    "FOUR! " + bowler + " overpitches and " + batsman + " punishes it",
                    "Boundary! " + batsman + " cuts it away behind point");
            case "6" -> pick(
                    "SIX! " + batsman + " launches it over long-on",
                    "SIX! Monster strike from " + batsman,
                    "SIX! " + batsman + " slog-sweeps it out of the ground");
            case "wide" -> pick(
                    "Wide down the leg side",
                    "Wasted delivery — wide called",
                    bowler + " strays onto the pads, wide signalled");
            case "noball" -> pick(
                    "No ball! " + bowler + " overstepped",
                    "Overstepped — no ball called");
            case "wicket" -> pick(
                    "OUT! " + batsman + " departs, " + bowler + " strikes",
                    "WICKET! Big blow — " + batsman + " has to go",
                    "Gone! " + bowler + " removes " + batsman,
                    "Timber! " + batsman + " is dismissed");
            default -> batsman + " plays it out";
        };
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    private void swapStrike(SimState state) {
        Long tmp = state.strikerId;
        state.strikerId = state.nonStrikerId;
        state.nonStrikerId = tmp;
    }

    private void publishLiveState(Match match, SimState state, int inningsNumber) {
        liveStateRegistry.put(new LiveStateRegistry.LiveState(
                match.getId(), state.strikerId, state.nonStrikerId, state.bowlerId, inningsNumber));
    }

    private void broadcast(String type, Long matchId, Dtos.MatchSummary summary) {
        broadcaster.broadcast("update", Map.of("type", type, "matchId", matchId, "summary", summary));
    }

    private long lastSequence(Long matchId) {
        return ballEventRepository.findFirstByMatchIdOrderBySequenceDesc(matchId)
                .map(BallEvent::getSequence).orElse(0L);
    }

    private Player playerAtPosition(Team team, int position) {
        List<Player> order = playerRepository.findByTeamIdOrderByBattingPosition(team.getId());
        return order.stream()
                .filter(p -> p.getBattingPosition() == position)
                .findFirst()
                .orElse(order.get(Math.min(position - 1, order.size() - 1)));
    }

    private PlayerMatchStats statsFor(Match match, Player player) {
        return statsRepository.findByMatchIdAndPlayerId(match.getId(), player.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stats missing for player " + player.getName() + " in match " + match.getId()));
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + matchId));
    }
}
