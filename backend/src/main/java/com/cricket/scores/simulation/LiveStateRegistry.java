package com.cricket.scores.simulation;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory snapshot of who is on strike / bowling for every live match.
 * Persisted data remains the source of truth; this registry is rebuilt from it
 * if the application restarts mid-match.
 */
@Component
public class LiveStateRegistry {

    public record LiveState(Long matchId, Long strikerId, Long nonStrikerId, Long bowlerId, int inningsNumber) {
    }

    private final Map<Long, LiveState> live = new ConcurrentHashMap<>();

    public void put(LiveState state) {
        live.put(state.matchId(), state);
    }

    public Optional<LiveState> get(Long matchId) {
        return Optional.ofNullable(live.get(matchId));
    }

    public void remove(Long matchId) {
        live.remove(matchId);
    }
}
