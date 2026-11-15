package com.chess2.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "actor_user_id") private UUID actorUserId;
    @Column(name = "actor_username", nullable = false, length = 32) private String actorUsername;
    @Column(nullable = false, length = 24) private String category;
    @Column(nullable = false, length = 8) private String method;
    @Column(nullable = false, length = 255) private String path;
    @Column(nullable = false) private int status;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt = Instant.now();

    protected AuditEvent() { }
    public AuditEvent(UUID actorUserId, String actorUsername, String category, String method, String path, int status) {
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.category = category;
        this.method = method;
        this.path = path;
        this.status = status;
    }
    public UUID getId() { return id; }
    public UUID getActorUserId() { return actorUserId; }
    public String getActorUsername() { return actorUsername; }
    public String getCategory() { return category; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public int getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
}
