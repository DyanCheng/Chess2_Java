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
    @Column(nullable = false) private long experience = 0;
    @Column(nullable = false) private int level = 1;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected UserAccount() { }
    public UserAccount(String username, String email, String passwordHash) { this.username = username; this.email = email; this.passwordHash = passwordHash; }
    public UUID getId() { return id; }
    @Override public String getUsername() { return username; }
    public String getEmail() { return email; }
    @Override public String getPassword() { return passwordHash; }
    public Role getRole() { return role; }
    public long getExperience() { return experience; }
    public int getLevel() { return level; }
    public void grantAdmin() { role = Role.ADMIN; }
    public void addExperience(long amount) { experience += amount; level = (int) Math.max(1, experience / 500 + 1); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); }
}
