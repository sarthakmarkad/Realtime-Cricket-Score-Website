package com.cricket.scores.service;

import com.cricket.scores.dto.DtoMapper;
import com.cricket.scores.dto.Dtos;
import com.cricket.scores.exception.ResourceNotFoundException;
import com.cricket.scores.model.Innings;
import com.cricket.scores.model.Match;
import com.cricket.scores.model.MatchStatus;
import com.cricket.scores.model.Player;
import com.cricket.scores.model.PlayerMatchStats;
import com.cricket.scores.repository.BallEventRepository;
import com.cricket.scores.repository.InningsRepository;
import com.cricket.scores.repository.MatchRepository;
import com.cricket.scores.repository.PlayerMatchStatsRepository;
import com.cricket.scores.repository.PlayerRepository;
import com.cricket.scores.repository.TeamRepository;
import com.cricket.scores.simulation.MatchSimulationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Read-side queries and aggregations served over REST.
 */
@Service
public class CricketService {

    private final MatchRepository matchRepository;
    private final InningsRepository inningsRepository;
    private final PlayerMatchStatsRepository statsRepository;
    private final BallEventRepository ballEventRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final DtoMapper mapper;
    private final MatchSimulationService simulation;

    public CricketService(MatchRepository matchRepository,
                          InningsRepository inningsRepository,
                          PlayerMatchStatsRepository statsRepository,
                          BallEventRepository ballEventRepository,
                          PlayerRepository playerRepository,
                          TeamRepository teamRepository,
                          DtoMapper mapper,
                          MatchSimulationService simulation) {
        this.matchRepository = matchRepository;
        this.inningsRepository = inningsRepository;
        this.statsRepository = statsRepository;
        this.ballEventRepository = ballEventRepository;
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.mapper = mapper;
        this.simulation = simulation;
    }

    @Transactional(readOnly = true)
    public List<Dtos.MatchSummary> getMatches() {
        Comparator<Match> byStatus = Comparator.comparingInt((Match m) -> switch (m.getStatus()) {
            case LIVE, INNINGS_BREAK -> 0;
            case UPCOMING -> 1;
            case COMPLETED -> 2;
        });
        List<Match> all = new ArrayList<>(matchRepository.findAll());
        all.sort(byStatus.thenComparing(Match::getScheduledAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return all.stream().map(mapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Dtos.MatchDetail getMatchDetail(Long id) {
        Match match = findMatch(id);
        if (match.getStatus() == MatchStatus.LIVE) {
            simulation.ensureLiveState(id);
        }
        return mapper.toDetail(match);
    }

    @Transactional(readOnly = true)
    public List<Dtos.InningsDetail> getScorecard(Long id) {
        findMatch(id);
        return inningsRepository.findByMatchIdOrderByInningsNumber(id).stream()
                .map(mapper::toInningsDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.CommentaryBall> getCommentary(Long id, int limit) {
        findMatch(id);
        return ballEventRepository.findByMatchIdOrderBySequenceDesc(id, PageRequest.of(0, Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(mapper::toCommentary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Dtos.MatchSummaryInfo getMatchSummary(Long id) {
        Match match = findMatch(id);
        List<PlayerMatchStats> stats = statsRepository.findByMatchId(id);

        Dtos.BattingEntry topScorer = stats.stream()
                .filter(s -> s.getBattingPosition() > 0)
                .max(Comparator.comparingInt(PlayerMatchStats::getRuns)
                        .thenComparing(Comparator.comparingInt(PlayerMatchStats::getBallsFaced).reversed()))
                .map(mapper::toBattingEntry)
                .orElse(null);

        Dtos.BowlingEntry topBowler = stats.stream()
                .filter(s -> s.getBallsBowled() > 0)
                .min(Comparator.comparingInt(PlayerMatchStats::getWickets).reversed()
                        .thenComparingInt(PlayerMatchStats::getRunsConceded))
                .map(mapper::toBowlingEntry)
                .orElse(null);

        List<Dtos.InningsScore> innings = inningsRepository.findByMatchIdOrderByInningsNumber(id).stream()
                .map(mapper::toScore)
                .toList();

        return new Dtos.MatchSummaryInfo(match.getResultText(),
                match.getWinner() == null ? null : mapper.ref(match.getWinner()), innings, topScorer, topBowler);
    }

    @Transactional(readOnly = true)
    public Dtos.Leaderboard getLeaderboard() {
        List<PlayerMatchStats> all = statsRepository.findAll();
        Map<Long, List<PlayerMatchStats>> byPlayer = all.stream()
                .collect(Collectors.groupingBy(s -> s.getPlayer().getId()));

        List<Dtos.BattingLeader> topScorers = byPlayer.values().stream()
                .map(list -> list.stream().filter(s -> s.getBattingPosition() > 0).toList())
                .filter(batted -> !batted.isEmpty())
                .map(batted -> {
                    Player player = batted.get(0).getPlayer();
                    int runs = batted.stream().mapToInt(PlayerMatchStats::getRuns).sum();
                    int balls = batted.stream().mapToInt(PlayerMatchStats::getBallsFaced).sum();
                    int highest = batted.stream().mapToInt(PlayerMatchStats::getRuns).max().orElse(0);
                    int fifties = (int) batted.stream().filter(s -> s.getRuns() >= 50).count();
                    int matches = matchCount(batted.get(0).getPlayer().getId(), all);
                    double strikeRate = balls == 0 ? 0.0 : DtoMapper.round2(runs * 100.0 / balls);
                    return new Dtos.BattingLeader(player.getId(), player.getName(), player.getTeam().getShortName(),
                            matches, runs, balls, highest, strikeRate, fifties);
                })
                .filter(l -> l.ballsFaced() > 0)
                .sorted(Comparator.comparingInt(Dtos.BattingLeader::runs).reversed())
                .limit(10)
                .toList();

        List<Dtos.BowlingLeader> topBowlers = byPlayer.values().stream()
                .map(list -> {
                    Player player = list.get(0).getPlayer();
                    int balls = list.stream().mapToInt(PlayerMatchStats::getBallsBowled).sum();
                    if (balls == 0) {
                        return null;
                    }
                    int runsConceded = list.stream().mapToInt(PlayerMatchStats::getRunsConceded).sum();
                    int wickets = list.stream().mapToInt(PlayerMatchStats::getWickets).sum();
                    int matches = matchCount(player.getId(), all);
                    String bestFigures = list.stream()
                            .filter(s -> s.getBallsBowled() > 0)
                            .min(Comparator.comparingInt(PlayerMatchStats::getWickets).reversed()
                                    .thenComparingInt(PlayerMatchStats::getRunsConceded))
                            .map(s -> s.getWickets() + "/" + s.getRunsConceded())
                            .orElse("-");
                    double economy = DtoMapper.round2(runsConceded * 6.0 / balls);
                    return new Dtos.BowlingLeader(player.getId(), player.getName(), player.getTeam().getShortName(),
                            matches, wickets, DtoMapper.overs(balls), runsConceded, economy, bestFigures);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(Dtos.BowlingLeader::wickets).reversed()
                        .thenComparingDouble(Dtos.BowlingLeader::economy))
                .limit(10)
                .toList();

        return new Dtos.Leaderboard(topScorers, topBowlers);
    }

    @Transactional(readOnly = true)
    public Dtos.PlayerProfile getPlayer(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
        List<PlayerMatchStats> stats = statsRepository.findByPlayerIdOrderByMatchIdDesc(id);

        int matches = (int) stats.stream().map(s -> s.getMatch().getId()).distinct().count();
        int runs = stats.stream().mapToInt(PlayerMatchStats::getRuns).sum();
        long outs = stats.stream().filter(PlayerMatchStats::isOut).count();
        int wickets = stats.stream().mapToInt(PlayerMatchStats::getWickets).sum();
        double average = outs == 0 ? runs : DtoMapper.round2((double) runs / outs);
        String bestBowling = stats.stream()
                .filter(s -> s.getBallsBowled() > 0)
                .min(Comparator.comparingInt(PlayerMatchStats::getWickets).reversed()
                        .thenComparingInt(PlayerMatchStats::getRunsConceded))
                .map(s -> s.getWickets() + "/" + s.getRunsConceded())
                .orElse("-");

        List<Dtos.PlayerMatchLine> recent = stats.stream().map(s -> {
            Match match = s.getMatch();
            String opponent = match.getTeamOne().getId().equals(s.getTeam().getId())
                    ? match.getTeamTwo().getShortName()
                    : match.getTeamOne().getShortName();
            return new Dtos.PlayerMatchLine(match.getId(), opponent, match.getStatus().name(), s.getRuns(),
                    s.getBallsFaced(), s.isOut(), s.getDismissalText(), s.getWickets(),
                    DtoMapper.overs(s.getBallsBowled()), s.getRunsConceded());
        }).toList();

        return new Dtos.PlayerProfile(player.getId(), player.getName(), player.getRole().name(),
                player.getTeam().getName(), player.getTeam().getShortName(), matches, runs, average,
                wickets, bestBowling, recent);
    }

    @Transactional(readOnly = true)
    public List<Dtos.TeamRoster> getTeams() {
        return teamRepository.findAll().stream()
                .map(team -> new Dtos.TeamRoster(team.getId(), team.getName(), team.getShortName(),
                        playerRepository.findByTeamIdOrderByBattingPosition(team.getId()).stream()
                                .map(p -> new Dtos.PlayerRow(p.getId(), p.getName(), p.getRole().name()))
                                .toList()))
                .sorted(Comparator.comparing(Dtos.TeamRoster::name))
                .toList();
    }

    public Dtos.MatchSummary startMatch(Long id) {
        return simulation.startMatch(id);
    }

    private int matchCount(Long playerId, List<PlayerMatchStats> all) {
        return (int) all.stream()
                .filter(s -> s.getPlayer().getId().equals(playerId))
                .map(s -> s.getMatch().getId())
                .distinct()
                .count();
    }

    private Match findMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
    }
}
