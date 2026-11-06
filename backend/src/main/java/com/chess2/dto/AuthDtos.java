package com.chess2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }
    public record RegisterRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,32}") String username, @NotBlank @Email String email, @NotBlank @Size(min = 8, max = 72) String password) { }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record AuthResponse(String accessToken, String tokenType, PlayerResponse player) { }
    public record PlayerResponse(String username, String email, String role, int level, long experience, int rating, int wins, int losses, int draws, long gold, long gems) { }
    public record LeaderboardEntry(String username, int level, long experience, int rating, int wins, int losses, int draws) { }
}
