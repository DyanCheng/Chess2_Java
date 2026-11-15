package com.chess2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

public final class GameDtos {
    private GameDtos() { }
    public record CreateGameRequest(@NotBlank @Pattern(regexp = "PVE|PVP|LOCAL") String type, @Pattern(regexp = "EASY|MEDIUM|HARD") String difficulty, @Pattern(regexp = "WHITE|BLACK|RANDOM") String side) { }
    public record MoveRequest(@NotBlank @Pattern(regexp = "[a-h][1-8]") String from, @NotBlank @Pattern(regexp = "[a-h][1-8]") String to) { }
    public record UseCardRequest(@NotNull UUID cardId, @NotBlank @Size(max = 8) String target) { }
    public record GameResponse(UUID id, String type, String status, String difficulty, UUID whitePlayerId, UUID blackPlayerId, String whiteUsername, int whiteRating, String blackUsername, Integer blackRating, String roomCode, Instant roomExpiresAt, String playerSide, String currentTurn, List<List<String>> board, int whiteHp, int blackHp, String winnerColor, String frozenSquare, String invulnerableSquare, String trapSquare, int trapTurns, String mosquitoSquare, boolean backwardPawnsWhite, boolean backwardPawnsBlack, List<UUID> deckCardIds) { }
    public record MoveLogResponse(int sequence, UUID actorUserId, String actorSide, String eventType, String from, String to, String capturedPiece, String effectCode, Instant occurredAt) { }
}
