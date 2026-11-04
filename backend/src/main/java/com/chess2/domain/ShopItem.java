package com.chess2.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "shop_items")
public class ShopItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 80) private String name;
    @Column(name = "card_id") private UUID cardId;
    @Column(nullable = false) private long price;
    @Column(nullable = false) private boolean active = true;
    protected ShopItem() { }
    public ShopItem(String name, UUID cardId, long price) { this.name=name; this.cardId=cardId; this.price=price; }
    public UUID getId() { return id; } public String getName() { return name; } public UUID getCardId() { return cardId; } public long getPrice() { return price; } public boolean isActive() { return active; }
}
