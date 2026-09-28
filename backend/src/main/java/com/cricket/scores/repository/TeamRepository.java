package com.cricket.scores.repository;

import com.cricket.scores.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByShortName(String shortName);
}
