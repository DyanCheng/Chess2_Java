package com.chess2.repository;

import com.chess2.domain.Game;
import com.chess2.domain.GameStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {
    List<Game> findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByUpdatedAtDesc(UUID whitePlayerId, UUID blackPlayerId);
    List<Game> findTop20ByStatusOrderByCreatedAtAsc(GameStatus status);
}
