package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.*;
import com.chess2.dto.GameDtos;
import com.chess2.repository.GameRepository;
import com.chess2.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final GameRepository games; private final CatalogService catalog; private final UserRepository users; private final ObjectMapper mapper;
    public GameService(GameRepository games, CatalogService catalog, UserRepository users, ObjectMapper mapper) { this.games=games; this.catalog=catalog; this.users=users; this.mapper=mapper; }

    @Transactional public GameDtos.GameResponse create(UserAccount player, GameType type, String difficulty, String requestedSide) {
        UUID black = type == GameType.LOCAL ? player.getId() : null;
        BotDifficulty botDifficulty = difficulty == null ? BotDifficulty.MEDIUM : BotDifficulty.valueOf(difficulty);
        String side = type == GameType.PVE ? resolveSide(requestedSide) : "WHITE";
        Game game=games.save(new Game(type, player.getId(), black, ChessBoard.initial(mapper), botDifficulty, side));
        if(type==GameType.PVE && side.equals("BLACK")) botTurn(game);
        return publish(game);
    }

    @Transactional public GameDtos.GameResponse quickMatch(UserAccount player) {
        for (Game candidate : games.findTop20ByStatusOrderByCreatedAtAsc(GameStatus.WAITING)) {
            if (candidate.getWhitePlayerId().equals(player.getId())) continue;
            Game locked = locked(candidate.getId());
            if (locked.getStatus() == GameStatus.WAITING && locked.getType() == GameType.PVP && locked.getBlackPlayerId() == null) { locked.join(player.getId()); return publish(locked); }
        }
        return create(player, GameType.PVP, BotDifficulty.MEDIUM.name(), "WHITE");
    }

    @Transactional public GameDtos.GameResponse join(UserAccount player, UUID gameId) {
        Game game = locked(gameId);
        if (game.getWhitePlayerId().equals(player.getId())) throw new ApiException(HttpStatus.CONFLICT, "You cannot join your own game");
        if (game.getType()!=GameType.PVP || game.getStatus()!=GameStatus.WAITING) throw new ApiException(HttpStatus.CONFLICT,"Game cannot be joined");
        game.join(player.getId()); return publish(game);
    }

    @Transactional public GameDtos.GameResponse move(UserAccount player, UUID gameId, GameDtos.MoveRequest request) {
        Game game = participant(player, gameId, true);
        requireActiveTurn(game,player);
        String movingSide=game.getCurrentTurn(), opposingSide=opposite(movingSide);
        if (game.isFrozen(request.from(), movingSide)) throw new ApiException(HttpStatus.CONFLICT, "Quân cờ này đang bị khóa trong lượt hiện tại");
        if (game.isInvulnerable(request.to(), opposingSide)) throw new ApiException(HttpStatus.CONFLICT, "Quân cờ mục tiêu đang bất bại");
        String captured=ChessBoard.pieceAt(mapper,game.getBoard(),request.to());
        game.rememberBoard();
        game.updateBoard(ChessBoard.move(mapper, game.getBoard(), request.from(), request.to(), movingSide,game.hasBackwardPawns(movingSide)));
        game.moveEffects(request.from(), request.to(), movingSide,captured!=null);
        applyCapture(game,captured,opposingSide);
        finishTurn(game,movingSide);
        if(game.getStatus()==GameStatus.PLAYING && game.getType()==GameType.PVE) botTurn(game);
        return publish(game);
    }

    @Transactional public GameDtos.GameResponse useCard(UserAccount player, UUID gameId, GameDtos.UseCardRequest request) {
        Game game = participant(player, gameId, true);
        requireActiveTurn(game,player);
        FunctionCard card = catalog.card(request.cardId());
        String ownSide = ownSide(game, player), other = opposite(ownSide), target=request.target();
        boolean endsTurn=false;
        switch (card.getEffectType()) {
            case "HEAL" -> game.adjustHp(ownSide, Math.max(1,card.getEffectValue()));
            case "DOUBLE_DAMAGE" -> game.adjustHp(other, -Math.max(1,card.getEffectValue()));
            case "FREEZE" -> game.freeze(enemyPiece(game, target, other), other, Math.max(1, card.getEffectValue()));
            case "INVULNERABLE" -> game.protect(ownPiece(game, target, ownSide), ownSide, Math.max(1, card.getEffectValue()));
            case "PAWN_DASH" -> game.replaceBoard(ChessBoard.advancePawn(mapper,game.getBoard(),ownPiece(game,target,ownSide),ownSide));
            case "TRAP" -> { square(target,"Hãy chọn một ô trên bàn cờ"); game.placeTrap(target,ownSide,Math.max(1,card.getEffectValue())); }
            case "REWIND" -> { if(!game.rewind()) throw new ApiException(HttpStatus.CONFLICT,"Chưa có lượt trước để quay lại"); }
            case "PAWN_BACKWARD" -> game.enableBackwardPawns(ownSide);
            case "PORTAL" -> { String[] pair=pair(target); game.replaceBoard(ChessBoard.swap(mapper,game.getBoard(),pair[0],pair[1])); endsTurn=true; }
            case "EXTRA_QUEEN" -> { int health = ownSide.equals("WHITE") ? game.getWhiteHp() : game.getBlackHp(); game.adjustHp(ownSide, -Math.max(1, (int) Math.ceil(health * 0.8))); if (game.getStatus()!=GameStatus.FINISHED) game.replaceBoard(ChessBoard.addQueen(mapper, game.getBoard(), ownSide.charAt(0))); }
            case "REVIVE" -> { Character piece=game.revive(ownSide); if(piece==null) throw new ApiException(HttpStatus.CONFLICT,"Bạn chưa có quân bị bắt để hồi sinh"); game.replaceBoard(ChessBoard.revive(mapper,game.getBoard(),Character.toLowerCase(ownSide.charAt(0)),piece)); }
            case "HIDE_KING" -> game.replaceBoard(ChessBoard.hideKing(mapper,game.getBoard(),Character.toLowerCase(ownSide.charAt(0))));
            case "TELEPORT" -> { game.replaceBoard(ChessBoard.teleport(mapper,game.getBoard(),ownPiece(game,target,ownSide),Character.toLowerCase(ownSide.charAt(0)))); endsTurn=true; }
            case "MOSQUITO" -> game.mosquito(enemyPiece(game,target,other),other);
            case "SURRENDER" -> game.resignWithWinner(other);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Hiệu ứng thẻ này chưa khả dụng");
        }
        catalog.useCard(player,request.cardId());
        if(game.getStatus()==GameStatus.FINISHED){ ensureHpWinner(game); settleWinner(game); }
        else if(game.getStatus()==GameStatus.RESIGNED) settleWinner(game);
        else if(endsTurn){ game.updateBoard(game.getBoard()); finishTurn(game,ownSide); if(game.getStatus()==GameStatus.PLAYING&&game.getType()==GameType.PVE)botTurn(game); }
        return publish(game);
    }

    @Transactional public GameDtos.GameResponse resign(UserAccount player, UUID gameId) {
        Game game = participant(player, gameId, true);
        if (game.getStatus()!=GameStatus.PLAYING) throw new ApiException(HttpStatus.CONFLICT,"Game is not active");
        game.resignWithWinner(opposite(ownSide(game, player))); settleWinner(game); return publish(game);
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

    private void botTurn(Game game){
        if(game.getStatus()!=GameStatus.PLAYING||game.getType()!=GameType.PVE||game.getCurrentTurn().equals(game.getPvePlayerSide()))return;
        String side=game.getCurrentTurn(),other=opposite(side);
        try{
            ChessBoard.BotMove move=ChessBoard.botMove(mapper,game.getBoard(),game.getBotDifficulty(),game.getInvulnerableSquare(),game.getFrozenSquare(),side,game.hasBackwardPawns(side));
            game.updateBoard(move.board());game.moveEffects(move.from(),move.to(),side,move.captured()!=null);applyCapture(game,move.captured(),other);finishTurn(game,side);
        }catch(ApiException ignored){game.finish(GameStatus.DRAW);settleDraw(game);}
    }
    private void finishTurn(Game game,String movingSide){
        game.advanceEffects(movingSide);resolveTrap(game);
        if(game.getStatus()==GameStatus.FINISHED){ensureHpWinner(game);settleWinner(game);return;}
        endIfKingCaptured(game);
    }
    private void resolveTrap(Game game){
        String square=game.resolveTrap();if(square==null)return;String piece=ChessBoard.pieceAt(mapper,game.getBoard(),square);if(piece==null)return;
        String side=piece.charAt(0)=='w'?"WHITE":"BLACK";if(game.isInvulnerable(square,side))return;
        game.replaceBoard(ChessBoard.removePiece(mapper,game.getBoard(),square));game.recordCapture(piece);game.adjustHp(side,-captureDamage(piece));
    }
    private void applyCapture(Game game,String captured,String victim){if(captured==null)return;game.recordCapture(captured);game.adjustHp(victim,-captureDamage(captured));}
    private int captureDamage(String piece){return switch(piece.charAt(1)){case 'P'->5;case 'N','B'->10;case 'R'->15;case 'Q'->25;case 'K'->100;default->5;};}
    private boolean endIfKingCaptured(Game game) {
        if (!ChessBoard.hasKing(mapper, game.getBoard(), 'w')) { game.finishWithWinner("BLACK"); settleWinner(game); return true; }
        if (!ChessBoard.hasKing(mapper, game.getBoard(), 'b')) { game.finishWithWinner("WHITE"); settleWinner(game); return true; }
        return false;
    }
    private void ensureHpWinner(Game game){if(game.getWinnerColor()==null)game.finishWithWinner(game.getWhiteHp()==0?"BLACK":"WHITE");}
    private void settleWinner(Game game) {
        if (game.getType()==GameType.LOCAL) return;
        UserAccount owner = users.findById(game.getWhitePlayerId()).orElseThrow();
        if (game.getType()==GameType.PVE) { if (game.getPvePlayerSide().equals(game.getWinnerColor())) owner.recordWin(); else owner.recordLoss(); return; }
        UserAccount black = users.findById(game.getBlackPlayerId()).orElseThrow();
        if ("WHITE".equals(game.getWinnerColor())) { owner.recordWin(); black.recordLoss(); } else { black.recordWin(); owner.recordLoss(); }
    }
    private void settleDraw(Game game) { if (game.getType()==GameType.LOCAL) return; users.findById(game.getWhitePlayerId()).orElseThrow().recordDraw(); if (game.getType()==GameType.PVP && game.getBlackPlayerId()!=null) users.findById(game.getBlackPlayerId()).orElseThrow().recordDraw(); }
    private void requireActiveTurn(Game game,UserAccount player){
        if(game.getStatus()!=GameStatus.PLAYING)throw new ApiException(HttpStatus.CONFLICT,"Game is not active");
        if(game.getType()==GameType.LOCAL)return;
        if(game.getType()==GameType.PVE&&game.getCurrentTurn().equals(game.getPvePlayerSide()))return;
        if(game.getType()==GameType.PVP){UUID expected=game.getCurrentTurn().equals("WHITE")?game.getWhitePlayerId():game.getBlackPlayerId();if(player.getId().equals(expected))return;}
        throw new ApiException(HttpStatus.FORBIDDEN,"It is not your turn");
    }
    private Game participant(UserAccount player, UUID id, boolean lock) { Game game = lock ? locked(id) : game(id); if(!player.getId().equals(game.getWhitePlayerId())&&!player.getId().equals(game.getBlackPlayerId())) throw new ApiException(HttpStatus.FORBIDDEN,"You are not a match participant"); return game; }
    private String ownSide(Game game, UserAccount player) { if (game.getType()==GameType.LOCAL) return game.getCurrentTurn(); if(game.getType()==GameType.PVE)return game.getPvePlayerSide(); return player.getId().equals(game.getWhitePlayerId()) ? "WHITE" : "BLACK"; }
    private String resolveSide(String side){if(side==null)return "WHITE";if(side.equals("RANDOM"))return ThreadLocalRandom.current().nextBoolean()?"WHITE":"BLACK";return side;}
    private String opposite(String side){return side.equals("WHITE")?"BLACK":"WHITE";}
    private Game game(UUID id) { return games.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Game not found")); }
    private Game locked(UUID id) { return games.findWithLockById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Game not found")); }
    private GameDtos.GameResponse publish(Game game) { return view(game); }
    private String enemyPiece(Game game, String square, String side) { return targetPiece(game, square, side, "Hãy chọn quân đối phương trên bàn cờ"); }
    private String ownPiece(Game game, String square, String side) { return targetPiece(game, square, side, "Hãy chọn quân của bạn trên bàn cờ"); }
    private String targetPiece(Game game, String square, String side, String message) { square(square,message); String piece = ChessBoard.pieceAt(mapper, game.getBoard(), square); if (piece == null || piece.charAt(0) != Character.toLowerCase(side.charAt(0))) throw new ApiException(HttpStatus.BAD_REQUEST, message); return square; }
    private void square(String value,String message){if(value==null||!value.matches("[a-h][1-8]"))throw new ApiException(HttpStatus.BAD_REQUEST,message);}
    private String[] pair(String value){if(value==null||!value.matches("[a-h][1-8]:[a-h][1-8]"))throw new ApiException(HttpStatus.BAD_REQUEST,"Chọn lần lượt hai quân cho Portal");return value.split(":");}
    private GameDtos.GameResponse view(Game game) { String side=game.getType()==GameType.PVE?game.getPvePlayerSide():null; return new GameDtos.GameResponse(game.getId(), game.getType().name(), game.getStatus().name(), game.getBotDifficulty().name(), game.getWhitePlayerId(), game.getBlackPlayerId(),side, game.getCurrentTurn(), ChessBoard.view(mapper,game.getBoard()), game.getWhiteHp(), game.getBlackHp(), game.getWinnerColor(), game.getFrozenSquare(), game.getInvulnerableSquare(),game.getTrapSquare(),game.getTrapTurns(),game.getMosquitoSquare(),game.isBackwardPawnsWhite(),game.isBackwardPawnsBlack()); }
}
