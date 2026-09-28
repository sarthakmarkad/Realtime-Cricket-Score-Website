package com.cricket.scores.repository;

import com.cricket.scores.model.BallEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BallEventRepository extends JpaRepository<BallEvent, Long> {

    Optional<BallEvent> findFirstByMatchIdOrderBySequenceDesc(Long matchId);

    List<BallEvent> findByMatchIdAndInningsNumberOrderBySequenceAsc(Long matchId, int inningsNumber);

    List<BallEvent> findByMatchIdOrderBySequenceDesc(Long matchId, Pageable pageable);

    List<BallEvent> findByMatchIdOrderBySequenceAsc(Long matchId);
}
