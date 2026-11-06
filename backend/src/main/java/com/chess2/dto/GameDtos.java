package com.chess2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class GameDtos {
    private GameDtos() { }
    public record CreateGameRequest(@NotBlank @Pattern(regexp = "PVE|PVP|LOCAL") String type, @Pattern(regexp = "EASY|MEDIUM|HARD") String difficulty) { }
    public record MoveRequest(@NotBlank @Pattern(regexp = "[a-h][1-8]") String from, @NotBlank @Pattern(regexp = "[a-h][1-8]") String to) { }
    public record UseCardRequest(UUID cardId, @NotBlank @Size(max = 5) String target) { }
    public record GameResponse(UUID id, String type, String status, String difficulty, UUID whitePlayerId, UUID blackPlayerId, String currentTurn, List<List<String>> board, int whiteHp, int blackHp, String winnerColor) { }
}
