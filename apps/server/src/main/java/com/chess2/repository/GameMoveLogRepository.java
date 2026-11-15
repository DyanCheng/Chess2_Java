package com.chess2.repository;

import com.chess2.domain.GameMoveLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMoveLogRepository extends JpaRepository<GameMoveLog, UUID> {
    List<GameMoveLog> findByGameIdOrderBySequenceNoAsc(UUID gameId);
    Optional<GameMoveLog> findTopByGameIdOrderBySequenceNoDesc(UUID gameId);
}
