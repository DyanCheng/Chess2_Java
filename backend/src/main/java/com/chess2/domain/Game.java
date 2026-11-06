package com.chess2.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "games")
public class Game {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private GameType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private GameStatus status;
    @Column(name = "white_player_id", nullable = false) private UUID whitePlayerId;
    @Column(name = "black_player_id") private UUID blackPlayerId;
    @Column(name = "current_turn", nullable = false, length = 5) private String currentTurn = "WHITE";
    @Column(nullable = false, columnDefinition = "text") private String board;
    @Column(name = "white_hp", nullable = false) private int whiteHp = 100;
    @Column(name = "black_hp", nullable = false) private int blackHp = 100;
    @Enumerated(EnumType.STRING) @Column(name = "bot_difficulty", nullable = false, length = 12) private BotDifficulty botDifficulty = BotDifficulty.MEDIUM;
    @Column(name = "winner_color", length = 5) private String winnerColor;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected Game() { }
    public Game(GameType type, UUID whitePlayerId, UUID blackPlayerId, String board, BotDifficulty botDifficulty) { this.type=type; this.whitePlayerId=whitePlayerId; this.blackPlayerId=blackPlayerId; this.board=board; this.botDifficulty=botDifficulty; this.status=blackPlayerId == null && type == GameType.PVP ? GameStatus.WAITING : GameStatus.PLAYING; }
    public UUID getId() { return id; } public GameType getType() { return type; } public GameStatus getStatus() { return status; } public UUID getWhitePlayerId() { return whitePlayerId; } public UUID getBlackPlayerId() { return blackPlayerId; } public String getCurrentTurn() { return currentTurn; } public String getBoard() { return board; } public int getWhiteHp() { return whiteHp; } public int getBlackHp() { return blackHp; } public BotDifficulty getBotDifficulty() { return botDifficulty; } public String getWinnerColor() { return winnerColor; } public Instant getCompletedAt() { return completedAt; }
    public void join(UUID userId) { if (blackPlayerId != null) throw new IllegalStateException("Game is full"); blackPlayerId=userId; status=GameStatus.PLAYING; touch(); }
    public void updateBoard(String board) { this.board=board; currentTurn = currentTurn.equals("WHITE") ? "BLACK" : "WHITE"; touch(); }
    public void adjustHp(String target, int delta) { if (target.equals("WHITE")) whiteHp=Math.max(0, Math.min(100, whiteHp + delta)); else blackHp=Math.max(0, Math.min(100, blackHp + delta)); if (whiteHp==0 || blackHp==0) status=GameStatus.FINISHED; touch(); }
    public void finish(GameStatus finalStatus) { status=finalStatus; completedAt=Instant.now(); touch(); }
    public void finishWithWinner(String colour) { winnerColor=colour; finish(GameStatus.FINISHED); }
    public void resignWithWinner(String colour) { winnerColor=colour; finish(GameStatus.RESIGNED); }
    private void touch() { updatedAt=Instant.now(); }
}
