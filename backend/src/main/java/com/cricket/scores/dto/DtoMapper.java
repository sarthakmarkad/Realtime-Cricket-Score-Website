package com.cricket.scores.dto;

import com.cricket.scores.exception.ResourceNotFoundException;
import com.cricket.scores.model.BallEvent;
import com.cricket.scores.model.ExtraType;
import com.cricket.scores.model.Innings;
import com.cricket.scores.model.Match;
import com.cricket.scores.model.PlayerMatchStats;
import com.cricket.scores.model.Team;
import com.cricket.scores.repository.BallEventRepository;
import com.cricket.scores.repository.InningsRepository;
import com.cricket.scores.repository.PlayerMatchStatsRepository;
import com.cricket.scores.simulation.LiveStateRegistry;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Maps JPA entities to the API DTOs. All queries needed for rendering happen here,
 * so controllers stay thin and entities never leak to the wire.
 */
@Component
public class DtoMapper {

    private final InningsRepository inningsRepository;
    private final PlayerMatchStatsRepository statsRepository;
    private final BallEventRepository ballEventRepository;
    private final LiveStateRegistry liveStateRegistry;

    public DtoMapper(InningsRepository inningsRepository,
                     PlayerMatchStatsRepository statsRepository,
                     BallEventRepository ballEventRepository,
                     LiveStateRegistry liveStateRegistry) {
        this.inningsRepository = inningsRepository;
        this.statsRepository = statsRepository;
        this.ballEventRepository = ballEventRepository;
        this.liveStateRegistry = liveStateRegistry;
    }

    public static String overs(int legalBalls) {
        return (legalBalls / 6) + "." + (legalBalls % 6);
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public Dtos.TeamRef ref(Team team) {
        return new Dtos.TeamRef(team.getId(), team.getName(), team.getShortName());
    }

    public Dtos.InningsScore toScore(Innings innings) {
        return new Dtos.InningsScore(innings.getInningsNumber(), innings.getBattingTeam().getName(),
                innings.getBowlingTeam().getName(), innings.getRuns(), innings.getWickets(),
                overs(innings.getLegalBalls()), innings.isCompleted(), innings.getTarget());
    }

    public Dtos.MatchSummary toSummary(Match match) {
        List<Innings> innings = inningsRepository.findByMatchIdOrderByInningsNumber(match.getId());
        List<Dtos.InningsScore> scores = innings.stream().map(this::toScore).toList();

        Innings current = innings.stream().filter(i -> !i.isCompleted()).reduce((a, b) -> b).orElse(null);

        Double crr = null;
        Double rrr = null;
        Integer ballsRemaining = null;
        Integer target = null;
        String battingShort = null;
        String bowlingShort = null;
        List<String> thisOver = null;

        if (current != null) {
            crr = current.getLegalBalls() == 0 ? 0.0 : round2(current.getRuns() * 6.0 / current.getLegalBalls());
            battingShort = current.getBattingTeam().getShortName();
            bowlingShort = current.getBowlingTeam().getShortName();
            if (current.getTarget() != null) {
                target = current.getTarget();
                ballsRemaining = match.getOversPerInnings() * 6 - current.getLegalBalls();
                rrr = ballsRemaining <= 0 ? 0.0
                        : round2((current.getTarget() - current.getRuns()) * 6.0 / ballsRemaining);
            }
            thisOver = currentOverChips(match.getId(), current);
        }

        String lastEvent = ballEventRepository.findFirstByMatchIdOrderBySequenceDesc(match.getId())
                .map(BallEvent::getCommentary).orElse(null);

        return new Dtos.MatchSummary(match.getId(), ref(match.getTeamOne()), ref(match.getTeamTwo()),
                match.getVenue(), match.getStatus().name(), match.getCurrentInningsNumber(), scores,
                match.getResultText(), match.getWinner() == null ? null : ref(match.getWinner()),
                target, crr, rrr, ballsRemaining, lastEvent, thisOver, battingShort, bowlingShort,
                match.getScheduledAt(), match.getLastUpdatedAt());
    }

    public Dtos.MatchDetail toDetail(Match match) {
        List<Innings> innings = inningsRepository.findByMatchIdOrderByInningsNumber(match.getId());
        List<Dtos.InningsDetail> details = innings.stream().map(this::toInningsDetail).toList();
        return new Dtos.MatchDetail(toSummary(match), details, buildLiveInfo(match).orElse(null));
    }

    public Dtos.InningsDetail toInningsDetail(Innings innings) {
        Long matchId = innings.getMatch().getId();
        List<PlayerMatchStats> allStats = statsRepository.findByMatchId(matchId);

        List<Dtos.BattingEntry> batting = allStats.stream()
                .filter(s -> s.getTeam().getId().equals(innings.getBattingTeam().getId()))
                .filter(s -> s.getBattingPosition() > 0)
                .sorted(Comparator.comparingInt(PlayerMatchStats::getBattingPosition))
                .map(this::toBattingEntry)
                .toList();

        List<Dtos.BowlingEntry> bowling = allStats.stream()
                .filter(s -> s.getTeam().getId().equals(innings.getBowlingTeam().getId()))
                .filter(s -> s.getBallsBowled() > 0)
                .sorted(Comparator.comparingInt(PlayerMatchStats::getWickets).reversed()
                        .thenComparingInt(PlayerMatchStats::getRunsConceded))
                .map(this::toBowlingEntry)
                .toList();

        double runRate = innings.getLegalBalls() == 0 ? 0.0
                : round2(innings.getRuns() * 6.0 / innings.getLegalBalls());

        return new Dtos.InningsDetail(innings.getInningsNumber(), innings.getBattingTeam().getName(),
                innings.getBowlingTeam().getName(), innings.getRuns(), innings.getWickets(),
                overs(innings.getLegalBalls()), innings.isCompleted(), innings.getTarget(), runRate,
                batting, bowling);
    }

    public Dtos.BattingEntry toBattingEntry(PlayerMatchStats stats) {
        boolean onStrike = liveStateRegistry.get(stats.getMatch().getId())
                .map(s -> s.strikerId().equals(stats.getPlayer().getId())).orElse(false);
        boolean nonStriker = liveStateRegistry.get(stats.getMatch().getId())
                .map(s -> s.nonStrikerId().equals(stats.getPlayer().getId())).orElse(false);
        double strikeRate = stats.getBallsFaced() == 0 ? 0.0
                : round2(stats.getRuns() * 100.0 / stats.getBallsFaced());
        return new Dtos.BattingEntry(stats.getPlayer().getId(), stats.getPlayer().getName(),
                stats.getBattingPosition(), stats.getDismissalText(), stats.isOut(), stats.getRuns(),
                stats.getBallsFaced(), stats.getFours(), stats.getSixes(), strikeRate, onStrike, nonStriker);
    }

    public Dtos.BowlingEntry toBowlingEntry(PlayerMatchStats stats) {
        boolean current = liveStateRegistry.get(stats.getMatch().getId())
                .map(s -> s.bowlerId().equals(stats.getPlayer().getId())).orElse(false);
        double economy = stats.getBallsBowled() == 0 ? 0.0
                : round2(stats.getRunsConceded() * 6.0 / stats.getBallsBowled());
        return new Dtos.BowlingEntry(stats.getPlayer().getId(), stats.getPlayer().getName(),
                overs(stats.getBallsBowled()), stats.getRunsConceded(), stats.getWickets(), economy, current);
    }

    public Optional<Dtos.LiveInfo> buildLiveInfo(Match match) {
        return liveStateRegistry.get(match.getId()).map(state -> {
            Innings innings = inningsRepository
                    .findByMatchIdAndInningsNumber(match.getId(), state.inningsNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Innings not found for live match " + match.getId()));

            PlayerMatchStats striker = statsRepository.findByMatchIdAndPlayerId(match.getId(), state.strikerId()).orElse(null);
            PlayerMatchStats nonStriker = statsRepository.findByMatchIdAndPlayerId(match.getId(), state.nonStrikerId()).orElse(null);
            PlayerMatchStats bowler = statsRepository.findByMatchIdAndPlayerId(match.getId(), state.bowlerId()).orElse(null);

            double crr = innings.getLegalBalls() == 0 ? 0.0
                    : round2(innings.getRuns() * 6.0 / innings.getLegalBalls());
            Double rrr = null;
            Integer ballsRemaining = null;
            if (innings.getTarget() != null) {
                ballsRemaining = match.getOversPerInnings() * 6 - innings.getLegalBalls();
                rrr = ballsRemaining <= 0 ? 0.0
                        : round2((innings.getTarget() - innings.getRuns()) * 6.0 / ballsRemaining);
            }

            return new Dtos.LiveInfo(
                    striker == null ? null : toBattingEntry(striker),
                    nonStriker == null ? null : toBattingEntry(nonStriker),
                    bowler == null ? null : toBowlingEntry(bowler),
                    crr, rrr, ballsRemaining);
        });
    }

    public Dtos.CommentaryBall toCommentary(BallEvent event) {
        String wicketType = event.getWicketType() == null ? null : event.getWicketType().name();
        String extraType = event.getExtraType() == null ? null : event.getExtraType().name();
        return new Dtos.CommentaryBall(event.getId(), event.getInningsNumber(),
                event.getOverNumber() + "." + event.getBallInOver(), event.getBowler().getName(),
                event.getBatsman().getName(), event.getRunsScored(), extraType, event.isWicket(),
                wicketType, event.getDismissedPlayer() == null ? null : event.getDismissedPlayer().getName(),
                event.getCommentary(), event.getCreatedAt());
    }

    private List<String> currentOverChips(Long matchId, Innings current) {
        int overIndex = current.getLegalBalls() / 6;
        return ballEventRepository.findByMatchIdAndInningsNumberOrderBySequenceAsc(matchId, current.getInningsNumber())
                .stream()
                .filter(event -> event.getOverNumber() == overIndex)
                .map(this::chipOf)
                .toList();
    }

    public String chipOf(BallEvent event) {
        if (event.isWicket()) {
            return "W";
        }
        if (event.getExtraType() == ExtraType.WIDE) {
            return "wd";
        }
        if (event.getExtraType() == ExtraType.NO_BALL) {
            return "nb";
        }
        return String.valueOf(event.getRunsScored());
    }
}
