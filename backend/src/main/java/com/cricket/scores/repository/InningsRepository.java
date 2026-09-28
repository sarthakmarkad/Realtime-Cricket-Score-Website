package com.cricket.scores.repository;

import com.cricket.scores.model.Innings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InningsRepository extends JpaRepository<Innings, Long> {

    List<Innings> findByMatchIdOrderByInningsNumber(Long matchId);

    Optional<Innings> findByMatchIdAndInningsNumber(Long matchId, int inningsNumber);
}
