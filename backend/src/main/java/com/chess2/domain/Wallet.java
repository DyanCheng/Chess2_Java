package com.chess2.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id private UUID userId;
    @Column(nullable = false) private long gold;
    @Column(nullable = false) private long gems;
    protected Wallet() { }
    public Wallet(UUID userId) { this.userId = userId; }
    public UUID getUserId() { return userId; }
    public long getGold() { return gold; }
    public long getGems() { return gems; }
    public void debitGold(long amount) { if (amount < 0 || gold < amount) throw new IllegalStateException("Insufficient gold"); gold -= amount; }
    public void creditGold(long amount) { gold += amount; }
    public void creditGems(long amount) { gems += amount; }
    public void setBalances(long nextGold, long nextGems) { if (nextGold < 0 || nextGems < 0) throw new IllegalArgumentException("Balances cannot be negative"); gold=nextGold; gems=nextGems; }
}
