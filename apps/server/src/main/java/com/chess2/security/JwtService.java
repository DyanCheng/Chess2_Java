package com.chess2.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;
    public JwtService(@Value("${app.security.jwt-secret}") String encodedSecret, @Value("${app.security.jwt-expiration-minutes}") long expirationMinutes) {
        byte[] secret;
        try { secret = Base64.getDecoder().decode(encodedSecret); } catch (IllegalArgumentException ignored) { secret = encodedSecret.getBytes(StandardCharsets.UTF_8); }
        if (secret.length < 32) throw new IllegalStateException("JWT_SECRET must be at least 32 bytes after Base64 decoding");
        this.key = Keys.hmacShaKeyFor(secret); this.expirationMinutes = expirationMinutes;
    }
    public String issue(String subject, String role) {
        Instant now = Instant.now();
        return Jwts.builder().subject(subject).claim("role", role).issuedAt(Date.from(now)).expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES))).signWith(key).compact();
    }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
