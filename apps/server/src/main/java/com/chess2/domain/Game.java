package com.chess2.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "games")
public class Game {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private GameType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private GameStatus status;
    @Column(name = "white_player_id", nullable = false) private UUID whitePlayerId;
    @Column(name = "black_player_id") private UUID blackPlayerId;
    @Column(name = "room_code", unique = true, length = 6) private String roomCode;
    @Column(name = "pve_player_side", nullable = false, length = 5) private String pvePlayerSide = "WHITE";
    @Column(name = "current_turn", nullable = false, length = 5) private String currentTurn = "WHITE";
    @Column(nullable = false, columnDefinition = "text") private String board;
    @Column(name = "previous_board", columnDefinition = "text") private String previousBoard;
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
    @Column(name = "trap_square", length = 2) private String trapSquare;
    @Column(name = "trap_side", length = 5) private String trapSide;
    @Column(name = "trap_turns", nullable = false) private int trapTurns;
    @Column(name = "mosquito_square", length = 2) private String mosquitoSquare;
    @Column(name = "mosquito_side", length = 5) private String mosquitoSide;
    @Column(name = "backward_pawns_white", nullable = false) private boolean backwardPawnsWhite;
    @Column(name = "backward_pawns_black", nullable = false) private boolean backwardPawnsBlack;
    @Column(name = "captured_white", nullable = false, length = 32) private String capturedWhite = "";
    @Column(name = "captured_black", nullable = false, length = 32) private String capturedBlack = "";
    @Column(name = "white_deck", nullable = false, columnDefinition = "text") private String whiteDeck = "";
    @Column(name = "black_deck", nullable = false, columnDefinition = "text") private String blackDeck = "";
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    protected Game() { }
    public Game(GameType type, UUID whitePlayerId, UUID blackPlayerId, String board, BotDifficulty botDifficulty, String pvePlayerSide, List<UUID> playerDeck) {
        this.type=type; this.whitePlayerId=whitePlayerId; this.blackPlayerId=blackPlayerId; this.board=board; this.botDifficulty=botDifficulty;
        this.pvePlayerSide=pvePlayerSide == null ? "WHITE" : pvePlayerSide;
        String deck=encodeDeck(playerDeck);
        if(type==GameType.LOCAL){whiteDeck=deck;blackDeck=deck;} else if(type==GameType.PVE&&"BLACK".equals(this.pvePlayerSide))blackDeck=deck; else whiteDeck=deck;
        this.status=blackPlayerId == null && type == GameType.PVP ? GameStatus.WAITING : GameStatus.PLAYING;
    }
    public UUID getId() { return id; } public GameType getType() { return type; } public GameStatus getStatus() { return status; }
    public UUID getWhitePlayerId() { return whitePlayerId; } public UUID getBlackPlayerId() { return blackPlayerId; }
    public String getPvePlayerSide() { return pvePlayerSide; } public String getCurrentTurn() { return currentTurn; }
    public String getBoard() { return board; } public int getWhiteHp() { return whiteHp; } public int getBlackHp() { return blackHp; }
    public BotDifficulty getBotDifficulty() { return botDifficulty; } public String getWinnerColor() { return winnerColor; }
    public Instant getCompletedAt() { return completedAt; } public String getTrapSquare() { return trapTurns > 0 ? trapSquare : null; }
    public Instant getCreatedAt() { return createdAt; } public String getRoomCode() { return roomCode; }
    public int getTrapTurns() { return trapTurns; } public String getMosquitoSquare() { return mosquitoSquare; }
    public String getFrozenSquare() { return frozenTurns > 0 ? frozenSquare : null; }
    public String getInvulnerableSquare() { return invulnerableTurns > 0 ? invulnerableSquare : null; }

    public void join(UUID userId, List<UUID> deck) { if (blackPlayerId != null) throw new IllegalStateException("Game is full"); blackPlayerId=userId; blackDeck=encodeDeck(deck); status=GameStatus.PLAYING; touch(); }
    public void openRoom(String code) { if(type!=GameType.PVP)throw new IllegalStateException("Only PvP games have room codes");roomCode=code; }
    public List<UUID> deckFor(String side) { String deck="WHITE".equals(side)?whiteDeck:blackDeck; return deck==null||deck.isBlank()?List.of():List.of(deck.split(",")).stream().map(UUID::fromString).toList(); }
    public boolean hasCard(String side, UUID cardId) { return deckFor(side).contains(cardId); }
    public void rememberBoard() { previousBoard=board; }
    public boolean rewind() { if (previousBoard == null) return false; String current=board; board=previousBoard; previousBoard=current; touch(); return true; }
    public void updateBoard(String board) { this.board=board; currentTurn = currentTurn.equals("WHITE") ? "BLACK" : "WHITE"; touch(); }
    public void replaceBoard(String board) { this.board=board; touch(); }
    public void moveEffects(String from, String to, String side, boolean captured) {
        if (from.equals(invulnerableSquare) && side.equals(invulnerableSide)) invulnerableSquare = to;
        if (captured && to.equals(frozenSquare)) clearFreeze();
        if (captured && to.equals(mosquitoSquare)) { mosquitoSquare=null; mosquitoSide=null; }
    }
    public void recordCapture(String piece) { if (piece == null) return; if (piece.charAt(0)=='w') capturedWhite += piece.charAt(1); else capturedBlack += piece.charAt(1); }
    public Character revive(String side) {
        String pieces=side.equals("WHITE")?capturedWhite:capturedBlack; if (pieces.isEmpty()) return null;
        int best=0; for(int i=1;i<pieces.length();i++) if(value(pieces.charAt(i))>value(pieces.charAt(best))) best=i;
        char piece=pieces.charAt(best); String next=pieces.substring(0,best)+pieces.substring(best+1);
        if(side.equals("WHITE")) capturedWhite=next; else capturedBlack=next; touch(); return piece;
    }
    public void freeze(String square, String side, int turns) { frozenSquare=square; frozenSide=side; frozenTurns=turns; touch(); }
    public void protect(String square, String side, int turns) { invulnerableSquare=square; invulnerableSide=side; invulnerableTurns=turns; touch(); }
    public void placeTrap(String square, String ownerSide, int turns) { trapSquare=square; trapSide=ownerSide; trapTurns=turns; touch(); }
    public String resolveTrap() { if (trapTurns > 0 || trapSquare == null) return null; String square=trapSquare; trapSquare=null; trapSide=null; touch(); return square; }
    public void mosquito(String square, String victimSide) { mosquitoSquare=square; mosquitoSide=victimSide; touch(); }
    public boolean isFrozen(String square, String side) { return (frozenTurns > 0 && square.equals(frozenSquare) && side.equals(frozenSide)) || (square.equals(mosquitoSquare) && side.equals(mosquitoSide)); }
    public boolean isInvulnerable(String square, String side) { return invulnerableTurns > 0 && square.equals(invulnerableSquare) && side.equals(invulnerableSide); }
    public void enableBackwardPawns(String side) { if(side.equals("WHITE")) backwardPawnsWhite=true; else backwardPawnsBlack=true; touch(); }
    public boolean hasBackwardPawns(String side) { return side.equals("WHITE") ? backwardPawnsWhite : backwardPawnsBlack; }
    public boolean isBackwardPawnsWhite() { return backwardPawnsWhite; } public boolean isBackwardPawnsBlack() { return backwardPawnsBlack; }
    public void advanceEffects(String side) {
        if (side.equals(frozenSide) && frozenTurns > 0 && --frozenTurns == 0) clearFreeze();
        if (side.equals(invulnerableSide) && invulnerableTurns > 0 && --invulnerableTurns == 0) { invulnerableSquare=null; invulnerableSide=null; }
        if (trapTurns > 0) trapTurns--;
        if (side.equals(mosquitoSide) && mosquitoSquare != null) adjustHp(side,-1);
        touch();
    }
    public void adjustHp(String target, int delta) { if (target.equals("WHITE")) whiteHp=Math.max(0, Math.min(100, whiteHp + delta)); else blackHp=Math.max(0, Math.min(100, blackHp + delta)); if (whiteHp==0 || blackHp==0) status=GameStatus.FINISHED; touch(); }
    public void finish(GameStatus finalStatus) { status=finalStatus; completedAt=Instant.now(); touch(); }
    public void finishWithWinner(String colour) { winnerColor=colour; finish(GameStatus.FINISHED); }
    public void resignWithWinner(String colour) { winnerColor=colour; finish(GameStatus.RESIGNED); }
    public void cancelWaiting() { if (status != GameStatus.WAITING) throw new IllegalStateException("Only a waiting game can be cancelled"); finish(GameStatus.ABANDONED); }
    private void clearFreeze(){ frozenSquare=null; frozenSide=null; frozenTurns=0; }
    private int value(char piece){ return switch(piece){case 'Q'->5;case 'R'->4;case 'B','N'->3;default->1;}; }
    private static String encodeDeck(List<UUID> deck){return deck==null?"":deck.stream().map(UUID::toString).collect(Collectors.joining(","));}
    private void touch() { updatedAt=Instant.now(); }
}
