package com.chess2.repository;

import com.chess2.domain.Puzzle;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuzzleRepository extends JpaRepository<Puzzle, UUID> { }
