package com.chess2.repository;

import com.chess2.domain.Game;
import com.chess2.domain.GameStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface GameRepository extends JpaRepository<Game, UUID> {
    List<Game> findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByUpdatedAtDesc(UUID whitePlayerId, UUID blackPlayerId);
    List<Game> findTop20ByStatusOrderByCreatedAtAsc(GameStatus status);
    long countByStatus(GameStatus status);
    boolean existsByRoomCode(String roomCode);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Game> findWithLockById(UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Game> findWithLockByRoomCode(String roomCode);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Game g set g.status=:abandoned, g.completedAt=:now, g.updatedAt=:now where g.type=:type and g.status=:waiting and g.createdAt<=:cutoff")
    int expireWaitingRooms(@Param("type") com.chess2.domain.GameType type, @Param("waiting") GameStatus waiting, @Param("abandoned") GameStatus abandoned, @Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
