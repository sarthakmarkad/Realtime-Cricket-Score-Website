package com.cricket.scores.dto;

import java.time.Instant;
import java.util.List;

/**
 * All API response shapes. Kept as records so Jackson serializes exactly these fields.
 */
public final class Dtos {

    private Dtos() {
    }

    public record TeamRef(Long id, String name, String shortName) {
    }

    public record InningsScore(int inningsNumber, String battingTeam, String bowlingTeam,
                               int runs, int wickets, String overs, boolean completed, Integer target) {
    }

    public record MatchSummary(Long id, TeamRef teamOne, TeamRef teamTwo, String venue, String status,
                               int currentInningsNumber, List<InningsScore> innings, String result, TeamRef winner,
                               Integer target, Double currentRunRate, Double requiredRunRate, Integer ballsRemaining,
                               String lastEvent, List<String> thisOver, String battingTeamShort, String bowlingTeamShort,
                               Instant scheduledAt, Instant updatedAt) {
    }

    public record BattingEntry(Long playerId, String playerName, int battingPosition, String dismissal,
                               boolean out, int runs, int ballsFaced, int fours, int sixes,
                               double strikeRate, boolean onStrike, boolean nonStriker) {
    }

    public record BowlingEntry(Long playerId, String playerName, String overs, int runsConceded, int wickets,
                               double economy, boolean currentBowler) {
    }

    public record InningsDetail(int inningsNumber, String battingTeam, String bowlingTeam, int runs, int wickets,
                                String overs, boolean completed, Integer target, double runRate,
                                List<BattingEntry> batting, List<BowlingEntry> bowling) {
    }

    public record LiveInfo(BattingEntry striker, BattingEntry nonStriker, BowlingEntry bowler,
                           double currentRunRate, Double requiredRunRate, Integer ballsRemaining) {
    }

    public record MatchDetail(MatchSummary summary, List<InningsDetail> innings, LiveInfo live) {
    }

    public record CommentaryBall(Long id, int inningsNumber, String overDisplay, String bowlerName,
                                 String batsmanName, int runsScored, String extraType, boolean wicket,
                                 String wicketType, String dismissedPlayerName, String commentary, Instant createdAt) {
    }

    public record MatchSummaryInfo(String result, TeamRef winner, List<InningsScore> innings,
                                   BattingEntry topScorer, BowlingEntry topBowler) {
    }

    public record BattingLeader(Long playerId, String playerName, String teamShort, int matches, int runs,
                                int ballsFaced, int highest, double strikeRate, int fifties) {
    }

    public record BowlingLeader(Long playerId, String playerName, String teamShort, int matches, int wickets,
                                String overs, int runsConceded, double economy, String bestFigures) {
    }

    public record Leaderboard(List<BattingLeader> topScorers, List<BowlingLeader> topWicketTakers) {
    }

    public record PlayerMatchLine(Long matchId, String opponentShort, String matchStatus, int runs, int ballsFaced,
                                  boolean out, String dismissal, int wickets, String oversBowled, int runsConceded) {
    }

    public record PlayerProfile(Long id, String name, String role, String teamName, String teamShort,
                                int matches, int runs, double battingAverage, int wickets, String bestBowling,
                                List<PlayerMatchLine> recentMatches) {
    }

    public record TeamRoster(Long id, String name, String shortName, List<PlayerRow> players) {
    }

    public record PlayerRow(Long id, String name, String role) {
    }

    public record ApiError(String message, Instant timestamp) {
    }
}
