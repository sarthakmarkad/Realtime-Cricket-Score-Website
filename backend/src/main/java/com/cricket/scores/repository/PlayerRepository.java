package com.cricket.scores.repository;

import com.cricket.scores.model.Player;
import com.cricket.scores.model.PlayerRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamIdOrderByBattingPosition(Long teamId);

    List<Player> findByTeamIdAndRoleIn(Long teamId, List<PlayerRole> roles);
}
