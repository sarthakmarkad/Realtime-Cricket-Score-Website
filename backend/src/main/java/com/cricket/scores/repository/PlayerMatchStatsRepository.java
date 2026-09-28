package com.cricket.scores.repository;

import com.cricket.scores.model.PlayerMatchStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerMatchStatsRepository extends JpaRepository<PlayerMatchStats, Long> {

    List<PlayerMatchStats> findByMatchId(Long matchId);

    List<PlayerMatchStats> findByPlayerIdOrderByMatchIdDesc(Long playerId);

    Optional<PlayerMatchStats> findByMatchIdAndPlayerId(Long matchId, Long playerId);
}
