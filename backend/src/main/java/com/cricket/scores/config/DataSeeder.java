package com.cricket.scores.config;

import com.cricket.scores.model.Innings;
import com.cricket.scores.model.Match;
import com.cricket.scores.model.MatchStatus;
import com.cricket.scores.model.Player;
import com.cricket.scores.model.PlayerRole;
import com.cricket.scores.model.Team;
import com.cricket.scores.repository.InningsRepository;
import com.cricket.scores.repository.MatchRepository;
import com.cricket.scores.repository.PlayerRepository;
import com.cricket.scores.repository.TeamRepository;
import com.cricket.scores.simulation.MatchSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Seeds the tournament on first boot: 6 teams with full squads plus three matches —
 * one already completed (full scorecard), one live mid-innings, one upcoming.
 * Historical deliveries are produced by the same simulation engine so stats,
 * scorecards and leaderboards stay perfectly consistent.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[][] TEAM_SEEDS = {
            {"Mumbai Mavericks", "MUM", "Mumbai Cricket Ground",
                    "Aryan Kadam:BATSMAN", "Ishaan Verma:BATSMAN", "Devraj Patil:WICKETKEEPER",
                    "Samar Joshi:BATSMAN", "Nikhil Rao:BATSMAN", "Kabir Shaikh:ALL_ROUNDER",
                    "Yuvan Naik:ALL_ROUNDER", "Om Prabhu:BOWLER", "Rehan Qureshi:BOWLER",
                    "Tanmay Bhat:BOWLER", "Vivaan Shetty:BOWLER"},
            {"Chennai Chargers", "CHE", "Chennai Cricket Ground",
                    "Vikram Anand:BATSMAN", "Aditya Iyer:BATSMAN", "Harish Kumar:WICKETKEEPER",
                    "Sandeep Menon:BATSMAN", "Karthik Raja:BATSMAN", "Dinesh Bala:ALL_ROUNDER",
                    "Aravind Krishnan:ALL_ROUNDER", "Mohit Sinha:BOWLER", "Zaid Ahmed:BOWLER",
                    "Gopal Raman:BOWLER", "Surya Teja:BOWLER"},
            {"Delhi Dynamos", "DEL", "Delhi Cricket Ground",
                    "Kabir Malhotra:BATSMAN", "Arnav Gupta:BATSMAN", "Rohit Bansal:WICKETKEEPER",
                    "Shrey Sethi:BATSMAN", "Manav Chopra:BATSMAN", "Ayaan Khan:ALL_ROUNDER",
                    "Pranav Mehta:ALL_ROUNDER", "Deepak Rana:BOWLER", "Imran Sheikh:BOWLER",
                    "Kunal Yadav:BOWLER", "Ashish Thakur:BOWLER"},
            {"Bengaluru Blasters", "BLR", "Bengaluru Cricket Ground",
                    "Nishanth Reddy:BATSMAN", "Rohan Pillai:BATSMAN", "Sagar Gowda:WICKETKEEPER",
                    "Kiran Shetty:BATSMAN", "Abhinav Menon:BATSMAN", "Suhas Hegde:ALL_ROUNDER",
                    "Farhan Ali:ALL_ROUNDER", "Manoj Kumar:BOWLER", "Ritesh Singh:BOWLER",
                    "Pavan Raj:BOWLER", "Dhruv Nair:BOWLER"},
            {"Kolkata Cyclones", "KOL", "Kolkata Cricket Ground",
                    "Sourav Ghosh:BATSMAN", "Ankit Roy:BATSMAN", "Rishi Sen:WICKETKEEPER",
                    "Aakash Dutta:BATSMAN", "Sujan Das:BATSMAN", "Irfan Mondal:ALL_ROUNDER",
                    "Joydeep Sarkar:ALL_ROUNDER", "Amitava Pal:BOWLER", "Sanjay Prasad:BOWLER",
                    "Bilal Hussain:BOWLER", "Tapan Maity:BOWLER"},
            {"Jaipur Jaguars", "JAI", "Jaipur Cricket Ground",
                    "Kunal Rathore:BATSMAN", "Mahipal Chouhan:BATSMAN", "Bhuvan Singh:WICKETKEEPER",
                    "Rajat Bhandari:BATSMAN", "Nikhil Chaudhary:BATSMAN", "Hrithik Sharma:ALL_ROUNDER",
                    "Devansh Joshi:ALL_ROUNDER", "Chetan Saini:BOWLER", "Aman Lohar:BOWLER",
                    "Vikas Jat:BOWLER", "Harsh Vardhan:BOWLER"}
    };

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final InningsRepository inningsRepository;
    private final MatchSimulationService simulation;

    public DataSeeder(TeamRepository teamRepository,
                      PlayerRepository playerRepository,
                      MatchRepository matchRepository,
                      InningsRepository inningsRepository,
                      MatchSimulationService simulation) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.inningsRepository = inningsRepository;
        this.simulation = simulation;
    }

    @Override
    public void run(String... args) {
        if (teamRepository.count() > 0) {
            return;
        }
        simulation.setSeeding(true);
        try {
            seedTeamsAndSquads();

            Match completed = createMatch("KOL", "JAI", "Kolkata Cricket Ground",
                    Instant.now().minus(Duration.ofDays(1)));
            simulation.startMatch(completed.getId());
            simulateToEnd(completed.getId());
            Match finished = matchRepository.findById(completed.getId()).orElseThrow();
            log.info("Seeded completed match: {}", finished.getResultText());

            Match live = createMatch("MUM", "CHE", "Mumbai Cricket Ground", Instant.now());
            simulation.startMatch(live.getId());
            fastForwardToBall(live.getId(), 58);
            Innings liveInnings = inningsRepository
                    .findByMatchIdAndInningsNumber(live.getId(), 1).orElseThrow();
            log.info("Seeded live match MUM vs CHE: {}/{} after {} overs",
                    liveInnings.getRuns(), liveInnings.getWickets(), DtoOvers.format(liveInnings.getLegalBalls()));

            createMatch("DEL", "BLR", "Delhi Cricket Ground", Instant.now().plus(Duration.ofHours(2)));

            log.info("Seeded {} teams, {} players, {} matches",
                    teamRepository.count(), playerRepository.count(), matchRepository.count());
        } finally {
            simulation.setSeeding(false);
        }
    }

    private void seedTeamsAndSquads() {
        for (String[] seed : TEAM_SEEDS) {
            Team team = teamRepository.save(new Team(seed[0], seed[1]));
            for (int i = 3; i < seed.length; i++) {
                String[] parts = seed[i].split(":");
                PlayerRole role = PlayerRole.valueOf(parts[1]);
                playerRepository.save(new Player(parts[0], role, team, i - 2));
            }
        }
    }

    private Match createMatch(String shortOne, String shortTwo, String venue, Instant scheduledAt) {
        Team one = teamRepository.findByShortName(shortOne).orElseThrow();
        Team two = teamRepository.findByShortName(shortTwo).orElseThrow();
        Match match = new Match();
        match.setTeamOne(one);
        match.setTeamTwo(two);
        match.setVenue(venue);
        match.setStatus(MatchStatus.UPCOMING);
        match.setOversPerInnings(20);
        match.setScheduledAt(scheduledAt);
        match.setLastUpdatedAt(Instant.now());
        return matchRepository.save(match);
    }

    private void simulateToEnd(Long matchId) {
        for (int i = 0; i < 900; i++) {
            Match match = matchRepository.findById(matchId).orElseThrow();
            if (match.getStatus() == MatchStatus.COMPLETED) {
                return;
            }
            if (match.getStatus() == MatchStatus.INNINGS_BREAK) {
                simulation.handleInningsBreak(matchId);
            } else {
                simulation.playBall(matchId);
            }
        }
        throw new IllegalStateException("Match " + matchId + " did not complete during seeding");
    }

    private void fastForwardToBall(Long matchId, int legalBalls) {
        for (int i = 0; i < 300; i++) {
            Match match = matchRepository.findById(matchId).orElseThrow();
            if (match.getStatus() != MatchStatus.LIVE) {
                return;
            }
            Innings innings = inningsRepository
                    .findByMatchIdAndInningsNumber(matchId, match.getCurrentInningsNumber()).orElseThrow();
            if (innings.getLegalBalls() >= legalBalls) {
                return;
            }
            simulation.playBall(matchId);
        }
    }

    /** Tiny helper for over notation in logs. */
    private static final class DtoOvers {
        private static String format(int legalBalls) {
            return (legalBalls / 6) + "." + (legalBalls % 6);
        }
    }
}
