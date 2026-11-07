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
    @Column(name = "frozen_square", length = 2) private String frozenSquare;
    @Column(name = "frozen_side", length = 5) private String frozenSide;
    @Column(name = "frozen_turns", nullable = false) private int frozenTurns;
    @Column(name = "invulnerable_square", length = 2) private String invulnerableSquare;
    @Column(name = "invulnerable_side", length = 5) private String invulnerableSide;
    @Column(name = "invulnerable_turns", nullable = false) private int invulnerableTurns;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected Game() { }
    public Game(GameType type, UUID whitePlayerId, UUID blackPlayerId, String board, BotDifficulty botDifficulty) { this.type=type; this.whitePlayerId=whitePlayerId; this.blackPlayerId=blackPlayerId; this.board=board; this.botDifficulty=botDifficulty; this.status=blackPlayerId == null && type == GameType.PVP ? GameStatus.WAITING : GameStatus.PLAYING; }
    public UUID getId() { return id; } public GameType getType() { return type; } public GameStatus getStatus() { return status; } public UUID getWhitePlayerId() { return whitePlayerId; } public UUID getBlackPlayerId() { return blackPlayerId; } public String getCurrentTurn() { return currentTurn; } public String getBoard() { return board; } public int getWhiteHp() { return whiteHp; } public int getBlackHp() { return blackHp; } public BotDifficulty getBotDifficulty() { return botDifficulty; } public String getWinnerColor() { return winnerColor; } public Instant getCompletedAt() { return completedAt; }
    public String getFrozenSquare() { return frozenTurns > 0 ? frozenSquare : null; } public String getInvulnerableSquare() { return invulnerableTurns > 0 ? invulnerableSquare : null; }
    public void join(UUID userId) { if (blackPlayerId != null) throw new IllegalStateException("Game is full"); blackPlayerId=userId; status=GameStatus.PLAYING; touch(); }
    public void updateBoard(String board) { this.board=board; currentTurn = currentTurn.equals("WHITE") ? "BLACK" : "WHITE"; touch(); }
    public void replaceBoard(String board) { this.board=board; touch(); }
    public void moveEffects(String from, String to, String side) {
        if (from.equals(invulnerableSquare) && side.equals(invulnerableSide)) invulnerableSquare = to;
        if (to.equals(frozenSquare) && !side.equals(frozenSide)) { frozenSquare = null; frozenSide = null; frozenTurns = 0; }
    }
    public void freeze(String square, String side, int turns) { frozenSquare=square; frozenSide=side; frozenTurns=turns; touch(); }
    public void protect(String square, String side, int turns) { invulnerableSquare=square; invulnerableSide=side; invulnerableTurns=turns; touch(); }
    public boolean isFrozen(String square, String side) { return frozenTurns > 0 && square.equals(frozenSquare) && side.equals(frozenSide); }
    public boolean isInvulnerable(String square, String side) { return invulnerableTurns > 0 && square.equals(invulnerableSquare) && side.equals(invulnerableSide); }
    public void advanceEffects(String side) { if (side.equals(frozenSide) && frozenTurns > 0 && --frozenTurns == 0) { frozenSquare=null; frozenSide=null; } if (side.equals(invulnerableSide) && invulnerableTurns > 0 && --invulnerableTurns == 0) { invulnerableSquare=null; invulnerableSide=null; } touch(); }
    public void adjustHp(String target, int delta) { if (target.equals("WHITE")) whiteHp=Math.max(0, Math.min(100, whiteHp + delta)); else blackHp=Math.max(0, Math.min(100, blackHp + delta)); if (whiteHp==0 || blackHp==0) status=GameStatus.FINISHED; touch(); }
    public void finish(GameStatus finalStatus) { status=finalStatus; completedAt=Instant.now(); touch(); }
    public void finishWithWinner(String colour) { winnerColor=colour; finish(GameStatus.FINISHED); }
    public void resignWithWinner(String colour) { winnerColor=colour; finish(GameStatus.RESIGNED); }
    public void cancelWaiting() { if (status != GameStatus.WAITING) throw new IllegalStateException("Only a waiting game can be cancelled"); finish(GameStatus.ABANDONED); }
    private void touch() { updatedAt=Instant.now(); }
}
