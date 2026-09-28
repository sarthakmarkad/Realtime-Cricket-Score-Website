package com.cricket.scores.repository;

import com.cricket.scores.model.Match;
import com.cricket.scores.model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByStatusIn(Collection<MatchStatus> statuses);

    List<Match> findAll();
}
