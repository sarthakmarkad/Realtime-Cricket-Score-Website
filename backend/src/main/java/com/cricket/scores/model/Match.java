package com.cricket.scores.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Team teamOne;

    @ManyToOne(optional = false)
    private Team teamTwo;

    private String venue;

    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.UPCOMING;

    private int oversPerInnings = 20;

    private int currentInningsNumber;

    @ManyToOne
    private Team winner;

    private String resultText;

    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant lastUpdatedAt;

    public Long getId() { return id; }
    public Team getTeamOne() { return teamOne; }
    public void setTeamOne(Team teamOne) { this.teamOne = teamOne; }
    public Team getTeamTwo() { return teamTwo; }
    public void setTeamTwo(Team teamTwo) { this.teamTwo = teamTwo; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }
    public int getOversPerInnings() { return oversPerInnings; }
    public void setOversPerInnings(int oversPerInnings) { this.oversPerInnings = oversPerInnings; }
    public int getCurrentInningsNumber() { return currentInningsNumber; }
    public void setCurrentInningsNumber(int currentInningsNumber) { this.currentInningsNumber = currentInningsNumber; }
    public Team getWinner() { return winner; }
    public void setWinner(Team winner) { this.winner = winner; }
    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
