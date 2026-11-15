package com.chess2.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_cards", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "card_id"}),
        @UniqueConstraint(columnNames = {"user_id", "deck_slot"})
})
public class UserCard {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "card_id", nullable = false) private UUID cardId;
    @Column(nullable = false) private int quantity;
    @Column(name = "deck_slot") private Integer deckSlot;
    protected UserCard() { }
    public UserCard(UUID userId, UUID cardId, int quantity) { this.userId=userId; this.cardId=cardId; this.quantity=quantity; }
    public UUID getUserId() { return userId; } public UUID getCardId() { return cardId; } public int getQuantity() { return quantity; } public Integer getDeckSlot() { return deckSlot; }
    public void add(int amount) { quantity += amount; }
    public void equip(Integer slot) { if (slot != null && (slot < 1 || slot > 3)) throw new IllegalArgumentException("Deck slot must be between 1 and 3"); deckSlot=slot; }
    public void use() { if (quantity < 1) throw new IllegalStateException("Card is not owned"); if (--quantity == 0) deckSlot=null; }
}
