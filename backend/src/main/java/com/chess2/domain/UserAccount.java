package com.chess2.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
public class UserAccount implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 32) private String username;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Role role = Role.PLAYER;
    @Enumerated(EnumType.STRING) @Column(name = "account_type", nullable = false, length = 16) private AccountType accountType = AccountType.REGISTERED;
    @Column(name = "guest_key", unique = true, length = 64) private String guestKey;
    @Column(nullable = false) private long experience = 0;
    @Column(nullable = false) private int level = 1;
    @Column(nullable = false) private int rating = 1000;
    @Column(nullable = false) private int wins = 0;
    @Column(nullable = false) private int losses = 0;
    @Column(nullable = false) private int draws = 0;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected UserAccount() { }
    public UserAccount(String username, String email, String passwordHash) { this.username = username; this.email = email; this.passwordHash = passwordHash; }
    public static UserAccount guest(String username, String email, String guestKey, String passwordHash) {
        UserAccount account = new UserAccount(username, email, passwordHash);
        account.accountType = AccountType.GUEST;
        account.guestKey = guestKey;
        return account;
    }
    public UUID getId() { return id; }
    @Override public String getUsername() { return username; }
    public String getEmail() { return email; }
    @Override public String getPassword() { return passwordHash; }
    public Role getRole() { return role; }
    public AccountType getAccountType() { return accountType; }
    public boolean isGuest() { return accountType == AccountType.GUEST; }
    public long getExperience() { return experience; }
    public int getLevel() { return level; }
    public int getRating() { return rating; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
    public void grantAdmin() { role = Role.ADMIN; }
    public void addExperience(long amount) { experience += amount; level = (int) Math.max(1, experience / 500 + 1); }
    public void recordWin() { wins++; rating += 24; addExperience(120); }
    public void recordLoss() { losses++; rating = Math.max(0, rating - 16); addExperience(25); }
    public void recordDraw() { draws++; rating += 4; addExperience(50); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); }
}
