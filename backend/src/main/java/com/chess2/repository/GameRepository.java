package com.chess2.repository;

import com.chess2.domain.Game;
import com.chess2.domain.GameStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {
    List<Game> findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByUpdatedAtDesc(UUID whitePlayerId, UUID blackPlayerId);
    List<Game> findTop20ByStatusOrderByCreatedAtAsc(GameStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Game> findWithLockById(UUID id);
}
