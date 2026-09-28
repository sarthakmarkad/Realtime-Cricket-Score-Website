package com.cricket.scores.simulation;

import com.cricket.scores.model.Match;
import com.cricket.scores.model.MatchStatus;
import com.cricket.scores.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drives the live simulation: one scheduled tick plays a single delivery for every
 * live match and advances innings breaks. Calls go through the MatchSimulationService
 * proxy so each ball is persisted in its own transaction.
 */
@Component
public class SimulationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SimulationScheduler.class);

    private final MatchRepository matchRepository;
    private final MatchSimulationService simulation;

    public SimulationScheduler(MatchRepository matchRepository, MatchSimulationService simulation) {
        this.matchRepository = matchRepository;
        this.simulation = simulation;
    }

    @Scheduled(fixedDelayString = "${cricket.simulation.tick-ms:3500}")
    public void tick() {
        if (simulation.isSeeding()) {
            return;
        }
        try {
            List<Match> active = matchRepository.findByStatusIn(List.of(MatchStatus.LIVE, MatchStatus.INNINGS_BREAK));
            for (Match match : active) {
                if (match.getStatus() == MatchStatus.INNINGS_BREAK) {
                    simulation.handleInningsBreak(match.getId());
                } else if (match.getStatus() == MatchStatus.LIVE) {
                    simulation.playBall(match.getId());
                }
            }
        } catch (Exception e) {
            log.error("Simulation tick failed", e);
        }
    }
}
