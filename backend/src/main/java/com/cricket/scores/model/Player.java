package com.cricket.scores.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private PlayerRole role;

    @ManyToOne(optional = false)
    private Team team;

    /** Batting order position 1-11 for this player's team. */
    private int battingPosition;

    public Player() {
    }

    public Player(String name, PlayerRole role, Team team, int battingPosition) {
        this.name = name;
        this.role = role;
        this.team = team;
        this.battingPosition = battingPosition;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PlayerRole getRole() { return role; }
    public void setRole(PlayerRole role) { this.role = role; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public int getBattingPosition() { return battingPosition; }
    public void setBattingPosition(int battingPosition) { this.battingPosition = battingPosition; }
}
