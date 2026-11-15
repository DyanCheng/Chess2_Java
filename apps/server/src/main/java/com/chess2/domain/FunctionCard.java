package com.chess2.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "function_cards")
public class FunctionCard {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 48) private String code;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false, length = 32) private String effectType;
    @Column(nullable = false) private int effectValue;
    @Column(nullable = false) private long price;
    protected FunctionCard() { }
    public FunctionCard(String code, String name, String effectType, int effectValue, long price) { this.code=code; this.name=name; this.effectType=effectType; this.effectValue=effectValue; this.price=price; }
    public UUID getId() { return id; } public String getCode() { return code; } public String getName() { return name; }
    public String getEffectType() { return effectType; } public int getEffectValue() { return effectValue; } public long getPrice() { return price; }
}
