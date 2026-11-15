package com.chess2.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_move_logs", uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "sequence_no"}))
public class GameMoveLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "game_id", nullable = false) private UUID gameId;
    @Column(name = "sequence_no", nullable = false) private int sequenceNo;
    @Column(name = "actor_user_id") private UUID actorUserId;
    @Column(name = "actor_side", nullable = false, length = 5) private String actorSide;
    @Column(name = "event_type", nullable = false, length = 12) private String eventType;
    @Column(name = "from_square", length = 2) private String fromSquare;
    @Column(name = "to_square", length = 8) private String toSquare;
    @Column(name = "captured_piece", length = 2) private String capturedPiece;
    @Column(name = "effect_code", length = 32) private String effectCode;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt = Instant.now();

    protected GameMoveLog() { }
    public GameMoveLog(UUID gameId, int sequenceNo, UUID actorUserId, String actorSide, String eventType, String fromSquare, String toSquare, String capturedPiece, String effectCode) {
        this.gameId=gameId; this.sequenceNo=sequenceNo; this.actorUserId=actorUserId; this.actorSide=actorSide; this.eventType=eventType;
        this.fromSquare=fromSquare; this.toSquare=toSquare; this.capturedPiece=capturedPiece; this.effectCode=effectCode;
    }
    public int getSequenceNo() { return sequenceNo; } public UUID getActorUserId() { return actorUserId; }
    public String getActorSide() { return actorSide; } public String getEventType() { return eventType; }
    public String getFromSquare() { return fromSquare; } public String getToSquare() { return toSquare; }
    public String getCapturedPiece() { return capturedPiece; } public String getEffectCode() { return effectCode; }
    public Instant getOccurredAt() { return occurredAt; }
}
