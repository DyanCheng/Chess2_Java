package com.chess2.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_cards", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "card_id"}))
public class UserCard {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "card_id", nullable = false) private UUID cardId;
    @Column(nullable = false) private int quantity;
    protected UserCard() { }
    public UserCard(UUID userId, UUID cardId, int quantity) { this.userId=userId; this.cardId=cardId; this.quantity=quantity; }
    public UUID getCardId() { return cardId; } public int getQuantity() { return quantity; }
    public void add(int amount) { quantity += amount; }
    public void use() { if (quantity < 1) throw new IllegalStateException("Card is not owned"); quantity--; }
}
