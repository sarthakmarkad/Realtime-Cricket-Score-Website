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

/**
 * A single delivery in a match (including extras such as wides and no-balls).
 */
@Entity
@Table(name = "ball_events")
public class BallEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Match match;

    private int inningsNumber;

    /** Monotonic ordering of deliveries within the match. */
    private long sequence;

    /** Zero-based index of the over this delivery belongs to. */
    private int overNumber;

    /** Ball number within the over as displayed (legal balls 1-6; extras keep the upcoming slot). */
    private int ballInOver;

    @ManyToOne(optional = false)
    private Player batsman;

    @ManyToOne(optional = false)
    private Player bowler;

    /** Total runs added to the team total by this delivery (including extras). */
    private int runsScored;

    @Enumerated(EnumType.STRING)
    private ExtraType extraType;

    private boolean wicket;

    @Enumerated(EnumType.STRING)
    private WicketType wicketType;

    @ManyToOne
    private Player dismissedPlayer;

    private String commentary;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }
    public int getInningsNumber() { return inningsNumber; }
    public void setInningsNumber(int inningsNumber) { this.inningsNumber = inningsNumber; }
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
    public int getOverNumber() { return overNumber; }
    public void setOverNumber(int overNumber) { this.overNumber = overNumber; }
    public int getBallInOver() { return ballInOver; }
    public void setBallInOver(int ballInOver) { this.ballInOver = ballInOver; }
    public Player getBatsman() { return batsman; }
    public void setBatsman(Player batsman) { this.batsman = batsman; }
    public Player getBowler() { return bowler; }
    public void setBowler(Player bowler) { this.bowler = bowler; }
    public int getRunsScored() { return runsScored; }
    public void setRunsScored(int runsScored) { this.runsScored = runsScored; }
    public ExtraType getExtraType() { return extraType; }
    public void setExtraType(ExtraType extraType) { this.extraType = extraType; }
    public boolean isWicket() { return wicket; }
    public void setWicket(boolean wicket) { this.wicket = wicket; }
    public WicketType getWicketType() { return wicketType; }
    public void setWicketType(WicketType wicketType) { this.wicketType = wicketType; }
    public Player getDismissedPlayer() { return dismissedPlayer; }
    public void setDismissedPlayer(Player dismissedPlayer) { this.dismissedPlayer = dismissedPlayer; }
    public String getCommentary() { return commentary; }
    public void setCommentary(String commentary) { this.commentary = commentary; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
