package com.chess2.service;

import com.chess2.api.ApiException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public AuthResponse register(String username, String password) {
        String normalized = username.trim();
        String key = normalized.toLowerCase();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        Player player = new Player(normalized, salt, hash(password, salt));
        if (players.putIfAbsent(key, player) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already registered");
        }
        return createSession(player);
    }

    public AuthResponse login(String username, String password) {
        Player player = players.get(username.trim().toLowerCase());
        if (player == null || !MessageDigest.isEqual(player.passwordHash, hash(password, player.salt))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return createSession(player);
    }

    public Player requirePlayer(String authorization) {
        String token = getToken(authorization);
        String username = sessions.get(token);
        Player player = username == null ? null : players.get(username.toLowerCase());
        if (player == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in to continue");
        }
        return player;
    }

    public PlayerView profile(String authorization) {
        return view(requirePlayer(authorization));
    }

    public List<PlayerView> leaderboard() {
        return players.values().stream()
                .sorted(Comparator.comparingInt((Player player) -> player.elo).reversed()
                        .thenComparing(player -> player.username.toLowerCase()))
                .map(AccountService::view)
                .toList();
    }

    public void recordResult(Player player, boolean won) {
        synchronized (player) {
            player.games++;
            if (won) {
                player.wins++;
                player.elo += 18;
            } else {
                player.losses++;
                player.elo = Math.max(100, player.elo - 12);
            }
        }
    }

    private AuthResponse createSession(Player player) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, player.username);
        return new AuthResponse(token, view(player));
    }

    private static String getToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authorization header must use Bearer token");
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private byte[] hash(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private static PlayerView view(Player player) {
        synchronized (player) {
            return new PlayerView(player.username, player.elo, player.games, player.wins, player.losses);
        }
    }

    public record AuthResponse(String token, PlayerView player) {}

    public record PlayerView(String username, int elo, int games, int wins, int losses) {}

    public static final class Player {
        private final String username;
        private final byte[] salt;
        private final byte[] passwordHash;
        private int elo = 1000;
        private int games;
        private int wins;
        private int losses;

        private Player(String username, byte[] salt, byte[] passwordHash) {
            this.username = username;
            this.salt = salt;
            this.passwordHash = passwordHash;
        }

        public String username() {
            return username;
        }
    }
}
