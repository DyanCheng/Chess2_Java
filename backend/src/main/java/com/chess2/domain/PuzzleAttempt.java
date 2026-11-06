package com.chess2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "puzzle_attempts", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "puzzle_id"}))
public class PuzzleAttempt {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "puzzle_id", nullable = false) private UUID puzzleId;
    @Column(name = "solved_at") private Instant solvedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    protected PuzzleAttempt() { }
    public PuzzleAttempt(UUID userId, UUID puzzleId) { this.userId = userId; this.puzzleId = puzzleId; }
    public boolean rewardIfFirstSolve() { if (solvedAt != null) return false; solvedAt = Instant.now(); return true; }
    public boolean isSolved() { return solvedAt != null; }
}
