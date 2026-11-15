package com.chess2.repository;

import com.chess2.domain.PuzzleAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuzzleAttemptRepository extends JpaRepository<PuzzleAttempt, UUID> {
    Optional<PuzzleAttempt> findByUserIdAndPuzzleId(UUID userId, UUID puzzleId);
}
