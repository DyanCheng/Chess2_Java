package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.BotDifficulty;
import com.chess2.domain.FunctionCard;
import com.chess2.domain.Game;
import com.chess2.domain.GameStatus;
import com.chess2.domain.GameType;
import com.chess2.domain.UserAccount;
import com.chess2.dto.GameDtos;
import com.chess2.repository.GameRepository;
import com.chess2.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final GameRepository games; private final CatalogService catalog; private final UserRepository users; private final ObjectMapper mapper;
    public GameService(GameRepository games, CatalogService catalog, UserRepository users, ObjectMapper mapper) { this.games=games; this.catalog=catalog; this.users=users; this.mapper=mapper; }

    @Transactional public GameDtos.GameResponse create(UserAccount player, GameType type, String difficulty) {
        UUID black = type == GameType.LOCAL ? player.getId() : null;
        BotDifficulty botDifficulty = difficulty == null ? BotDifficulty.MEDIUM : BotDifficulty.valueOf(difficulty);
        return publish(games.save(new Game(type, player.getId(), black, ChessBoard.initial(mapper), botDifficulty)));
    }

    @Transactional public GameDtos.GameResponse quickMatch(UserAccount player) {
        for (Game candidate : games.findTop20ByStatusOrderByCreatedAtAsc(GameStatus.WAITING)) {
            if (candidate.getWhitePlayerId().equals(player.getId())) continue;
            Game locked = locked(candidate.getId());
            if (locked.getStatus() == GameStatus.WAITING && locked.getType() == GameType.PVP && locked.getBlackPlayerId() == null) { locked.join(player.getId()); return publish(locked); }
        }
        return create(player, GameType.PVP, BotDifficulty.MEDIUM.name());
    }

    @Transactional public GameDtos.GameResponse join(UserAccount player, UUID gameId) {
        Game game = locked(gameId);
        if (game.getWhitePlayerId().equals(player.getId())) throw new ApiException(HttpStatus.CONFLICT, "You cannot join your own game");
        if (game.getType()!=GameType.PVP || game.getStatus()!=GameStatus.WAITING) throw new ApiException(HttpStatus.CONFLICT,"Game cannot be joined");
        game.join(player.getId()); return publish(game);
    }

    @Transactional public GameDtos.GameResponse move(UserAccount player, UUID gameId, GameDtos.MoveRequest request) {
        Game game = participant(player, gameId, true);
        if (game.getStatus()!=GameStatus.PLAYING) throw new ApiException(HttpStatus.CONFLICT,"Game is not active");
        boolean white = game.getCurrentTurn().equals("WHITE"); UUID expected = white ? game.getWhitePlayerId() : game.getBlackPlayerId();
        if (game.getType()!=GameType.LOCAL && !player.getId().equals(expected)) throw new ApiException(HttpStatus.FORBIDDEN,"It is not your turn");
        if (game.getType()==GameType.PVE && !white) throw new ApiException(HttpStatus.CONFLICT,"Wait for the bot");
        String movingSide = game.getCurrentTurn(); String opposingSide = movingSide.equals("WHITE") ? "BLACK" : "WHITE";
        if (game.isFrozen(request.from(), movingSide)) throw new ApiException(HttpStatus.CONFLICT, "Quân cờ này đang bị Freeze trong lượt hiện tại");
        if (game.isInvulnerable(request.to(), opposingSide)) throw new ApiException(HttpStatus.CONFLICT, "Quân cờ mục tiêu đang bất bại");
        game.updateBoard(ChessBoard.move(mapper, game.getBoard(), request.from(), request.to(), movingSide));
        game.moveEffects(request.from(), request.to(), movingSide);
        game.advanceEffects(movingSide);
        if (endIfKingCaptured(game)) return publish(game);
        if (game.getType()==GameType.PVE && game.getCurrentTurn().equals("BLACK")) {
            try { game.updateBoard(ChessBoard.botMove(mapper, game.getBoard(), game.getBotDifficulty(), game.getInvulnerableSquare(), game.getFrozenSquare())); game.advanceEffects("BLACK"); }
            catch (ApiException ignored) { game.finish(GameStatus.DRAW); settleDraw(game); }
            endIfKingCaptured(game);
        }
        return publish(game);
    }

    @Transactional public GameDtos.GameResponse useCard(UserAccount player, UUID gameId, GameDtos.UseCardRequest request) {
        Game game = participant(player, gameId, true);
        if (game.getStatus()!=GameStatus.PLAYING) throw new ApiException(HttpStatus.CONFLICT,"Game is not active");
        FunctionCard card = catalog.card(request.cardId());
        String ownSide = ownSide(game, player); String other = ownSide.equals("WHITE") ? "BLACK" : "WHITE";
        if (!ownSide.equals(game.getCurrentTurn())) throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ được dùng thẻ trong lượt của bạn");
        switch (card.getEffectType()) {
            case "HEAL" -> game.adjustHp(ownSide, Math.max(1,card.getEffectValue()));
            case "DOUBLE_DAMAGE" -> game.adjustHp(other, -Math.max(1,card.getEffectValue()));
            case "FREEZE" -> game.freeze(enemyPiece(game, request.target(), other), other, Math.max(1, card.getEffectValue()));
            case "INVULNERABLE" -> game.protect(ownPiece(game, request.target(), ownSide), ownSide, Math.max(1, card.getEffectValue()));
            case "EXTRA_QUEEN" -> { int health = ownSide.equals("WHITE") ? game.getWhiteHp() : game.getBlackHp(); game.adjustHp(ownSide, -Math.max(1, (int) Math.ceil(health * 0.8))); if (game.getStatus()!=GameStatus.FINISHED) game.replaceBoard(ChessBoard.addQueen(mapper, game.getBoard(), ownSide.charAt(0))); }
            case "SURRENDER" -> game.resignWithWinner(other);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Hiệu ứng thẻ này chưa khả dụng trong bản demo");
        }
        catalog.useCard(player,request.cardId());
        if (game.getStatus()==GameStatus.FINISHED) { if (game.getWinnerColor()==null) game.finishWithWinner(game.getWhiteHp() == 0 ? "BLACK" : "WHITE"); settleWinner(game); }
        if (game.getStatus()==GameStatus.RESIGNED) settleWinner(game);
        return publish(game);
    }

    @Transactional public GameDtos.GameResponse resign(UserAccount player, UUID gameId) {
        Game game = participant(player, gameId, true);
        if (game.getStatus()!=GameStatus.PLAYING) throw new ApiException(HttpStatus.CONFLICT,"Game is not active");
        String winner = ownSide(game, player).equals("WHITE") ? "BLACK" : "WHITE";
        game.resignWithWinner(winner); settleWinner(game); return publish(game);
    }

    @Transactional public GameDtos.GameResponse cancelWaiting(UserAccount player, UUID gameId) {
        Game game = participant(player, gameId, true);
        if (!player.getId().equals(game.getWhitePlayerId())) throw new ApiException(HttpStatus.FORBIDDEN, "Only the room creator can cancel it");
        if (game.getStatus() != GameStatus.WAITING) throw new ApiException(HttpStatus.CONFLICT, "Only a waiting game can be cancelled");
        game.cancelWaiting(); return publish(game);
    }

    public GameDtos.GameResponse get(UserAccount player, UUID id) { return view(participant(player,id,false)); }
    public List<GameDtos.GameResponse> history(UserAccount player) { return games.findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByUpdatedAtDesc(player.getId(),player.getId()).stream().map(this::view).toList(); }
    public List<GameDtos.GameResponse> waiting() { return games.findTop20ByStatusOrderByCreatedAtAsc(GameStatus.WAITING).stream().map(this::view).toList(); }

    private boolean endIfKingCaptured(Game game) {
        if (!ChessBoard.hasKing(mapper, game.getBoard(), 'w')) { game.finishWithWinner("BLACK"); settleWinner(game); return true; }
        if (!ChessBoard.hasKing(mapper, game.getBoard(), 'b')) { game.finishWithWinner("WHITE"); settleWinner(game); return true; }
        return false;
    }
    private void settleWinner(Game game) {
        if (game.getType()==GameType.LOCAL) return;
        UserAccount white = users.findById(game.getWhitePlayerId()).orElseThrow();
        if (game.getType()==GameType.PVE) { if ("WHITE".equals(game.getWinnerColor())) white.recordWin(); else white.recordLoss(); return; }
        UserAccount black = users.findById(game.getBlackPlayerId()).orElseThrow();
        if ("WHITE".equals(game.getWinnerColor())) { white.recordWin(); black.recordLoss(); } else { black.recordWin(); white.recordLoss(); }
    }
    private void settleDraw(Game game) {
        if (game.getType()==GameType.LOCAL) return;
        users.findById(game.getWhitePlayerId()).orElseThrow().recordDraw();
        if (game.getType()==GameType.PVP && game.getBlackPlayerId()!=null) users.findById(game.getBlackPlayerId()).orElseThrow().recordDraw();
    }
    private Game participant(UserAccount player, UUID id, boolean lock) {
        Game game = lock ? locked(id) : game(id);
        if(!player.getId().equals(game.getWhitePlayerId())&&!player.getId().equals(game.getBlackPlayerId())) throw new ApiException(HttpStatus.FORBIDDEN,"You are not a match participant");
        return game;
    }
    private String ownSide(Game game, UserAccount player) { if (game.getType()==GameType.LOCAL) return game.getCurrentTurn(); return player.getId().equals(game.getWhitePlayerId()) ? "WHITE" : "BLACK"; }
    private Game game(UUID id) { return games.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Game not found")); }
    private Game locked(UUID id) { return games.findWithLockById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Game not found")); }
    private GameDtos.GameResponse publish(Game game) { return view(game); }
    private String enemyPiece(Game game, String square, String side) { return targetPiece(game, square, side, "Hãy chọn quân đối phương trên bàn cờ"); }
    private String ownPiece(Game game, String square, String side) { return targetPiece(game, square, side, "Hãy chọn quân của bạn trên bàn cờ"); }
    private String targetPiece(Game game, String square, String side, String message) { if (!square.matches("[a-h][1-8]")) throw new ApiException(HttpStatus.BAD_REQUEST, message); String piece = ChessBoard.pieceAt(mapper, game.getBoard(), square); if (piece == null || piece.charAt(0) != Character.toLowerCase(side.charAt(0))) throw new ApiException(HttpStatus.BAD_REQUEST, message); return square; }
    private GameDtos.GameResponse view(Game game) { return new GameDtos.GameResponse(game.getId(), game.getType().name(), game.getStatus().name(), game.getBotDifficulty().name(), game.getWhitePlayerId(), game.getBlackPlayerId(), game.getCurrentTurn(), ChessBoard.view(mapper,game.getBoard()), game.getWhiteHp(), game.getBlackHp(), game.getWinnerColor(), game.getFrozenSquare(), game.getInvulnerableSquare()); }
}
