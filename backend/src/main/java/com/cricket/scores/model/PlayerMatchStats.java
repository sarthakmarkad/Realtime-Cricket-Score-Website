package com.cricket.scores.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A player's performance in a single match (batting and bowling combined).
 */
@Entity
@Table(name = "player_match_stats")
public class PlayerMatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Match match;

    @ManyToOne(optional = false)
    private Player player;

    /** Team the player represented in this match. */
    @ManyToOne(optional = false)
    private Team team;

    /** 0 if the player did not bat. */
    private int battingPosition;

    // Batting
    private int runs;
    private int ballsFaced;
    private int fours;
    private int sixes;
    private boolean out;
    private String dismissalText;

    // Bowling
    private int ballsBowled;
    private int runsConceded;
    private int wickets;

    public Long getId() { return id; }
    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public int getBattingPosition() { return battingPosition; }
    public void setBattingPosition(int battingPosition) { this.battingPosition = battingPosition; }
    public int getRuns() { return runs; }
    public void setRuns(int runs) { this.runs = runs; }
    public int getBallsFaced() { return ballsFaced; }
    public void setBallsFaced(int ballsFaced) { this.ballsFaced = ballsFaced; }
    public int getFours() { return fours; }
    public void setFours(int fours) { this.fours = fours; }
    public int getSixes() { return sixes; }
    public void setSixes(int sixes) { this.sixes = sixes; }
    public boolean isOut() { return out; }
    public void setOut(boolean out) { this.out = out; }
    public String getDismissalText() { return dismissalText; }
    public void setDismissalText(String dismissalText) { this.dismissalText = dismissalText; }
    public int getBallsBowled() { return ballsBowled; }
    public void setBallsBowled(int ballsBowled) { this.ballsBowled = ballsBowled; }
    public int getRunsConceded() { return runsConceded; }
    public void setRunsConceded(int runsConceded) { this.runsConceded = runsConceded; }
    public int getWickets() { return wickets; }
    public void setWickets(int wickets) { this.wickets = wickets; }
}
