package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.*;
import com.chess2.dto.GameDtos;
import com.chess2.realtime.GameSocketHub;
import com.chess2.repository.GameMoveLogRepository;
import com.chess2.repository.GameRepository;
import com.chess2.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class GameService {
    private static final Duration ROOM_TTL=Duration.ofMinutes(5);
    private final GameRepository games; private final GameMoveLogRepository moveLogs; private final CatalogService catalog; private final UserRepository users; private final ObjectMapper mapper;private final GameSocketHub realtime;
    public GameService(GameRepository games, GameMoveLogRepository moveLogs, CatalogService catalog, UserRepository users, ObjectMapper mapper,GameSocketHub realtime) { this.games=games; this.moveLogs=moveLogs; this.catalog=catalog; this.users=users; this.mapper=mapper;this.realtime=realtime; }

    @Transactional public GameDtos.GameResponse create(UserAccount player, GameType type, String difficulty, String requestedSide) {
        UUID black = type == GameType.LOCAL ? player.getId() : null;
        BotDifficulty botDifficulty = difficulty == null ? BotDifficulty.MEDIUM : BotDifficulty.valueOf(difficulty);
        String side = type == GameType.PVE ? resolveSide(requestedSide) : "WHITE";
        Game draft=new Game(type, player.getId(), black, ChessBoard.initial(mapper), botDifficulty, side, catalog.deck(player));
        if(type==GameType.PVP)draft.openRoom(newRoomCode());
        Game game=games.save(draft);
        if(type==GameType.PVE && side.equals("BLACK")) botTurn(game);
        return publish(game,player);
    }

    @Transactional public GameDtos.GameResponse quickMatch(UserAccount player) {
        expireWaitingRooms();
        for (Game candidate : games.findTop20ByStatusOrderByCreatedAtAsc(GameStatus.WAITING)) {
            if (candidate.getWhitePlayerId().equals(player.getId())) continue;
            Game locked = locked(candidate.getId());
            if (locked.getStatus() == GameStatus.WAITING && locked.getType() == GameType.PVP && locked.getBlackPlayerId() == null) { locked.join(player.getId(),catalog.deck(player)); return publish(locked,player); }
        }
        return create(player, GameType.PVP, BotDifficulty.MEDIUM.name(), "WHITE");
    }

    @Transactional public GameDtos.GameResponse join(UserAccount player, UUID gameId) {
        expireWaitingRooms();
        Game game = locked(gameId);
        return joinLocked(player,game);
    }

    @Transactional public GameDtos.GameResponse joinByRoomCode(UserAccount player,String roomCode) {
        expireWaitingRooms();
        if(roomCode==null||!roomCode.matches("\\d{6}"))throw new ApiException(HttpStatus.BAD_REQUEST,"Mã phòng phải gồm 6 chữ số");
        Game game=games.findWithLockByRoomCode(roomCode).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Không tìm thấy phòng PvP"));
        if(player.getId().equals(game.getWhitePlayerId())||player.getId().equals(game.getBlackPlayerId()))return publish(game,player);
        return joinLocked(player,game);
    }

    private GameDtos.GameResponse joinLocked(UserAccount player,Game game) {
        if (game.getWhitePlayerId().equals(player.getId())) throw new ApiException(HttpStatus.CONFLICT, "You cannot join your own game");
        if (game.getType()!=GameType.PVP || game.getStatus()!=GameStatus.WAITING) throw new ApiException(HttpStatus.CONFLICT,"Game cannot be joined");
        game.join(player.getId(),catalog.deck(player)); return publish(game,player);
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
        record(game,player.getId(),movingSide,"MOVE",request.from(),request.to(),captured,null);
        recordEffect(game,movingSide,finishTurn(game,movingSide));
        if(game.getStatus()==GameStatus.PLAYING && game.getType()==GameType.PVE) botTurn(game);
        return publish(game,player);
    }

    @Transactional public GameDtos.GameResponse useCard(UserAccount player, UUID gameId, GameDtos.UseCardRequest request) {
        Game game = participant(player, gameId, true);
        requireActiveTurn(game,player);
        FunctionCard card = catalog.card(request.cardId());
        String ownSide = ownSide(game, player), other = opposite(ownSide), target=request.target();
        if(!game.hasCard(ownSide,request.cardId()))throw new ApiException(HttpStatus.CONFLICT,"Thẻ này không nằm trong Deck của ván đấu");
        boolean endsTurn=false;
        switch (card.getEffectType()) {
            case "HEAL" -> {
                int hp=ownSide.equals("WHITE")?game.getWhiteHp():game.getBlackHp();
                if(hp>=100)throw new ApiException(HttpStatus.CONFLICT,"HP đã đầy. Thẻ Hồi phục chưa được sử dụng.");
                game.adjustHp(ownSide,Math.max(1,card.getEffectValue()));
            }
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
        record(game,player.getId(),ownSide,"CARD",null,target,null,card.getEffectType());
        if(game.getStatus()==GameStatus.FINISHED){ ensureHpWinner(game); settleWinner(game); }
        else if(game.getStatus()==GameStatus.RESIGNED) settleWinner(game);
        else if(endsTurn){ game.updateBoard(game.getBoard()); recordEffect(game,ownSide,finishTurn(game,ownSide)); if(game.getStatus()==GameStatus.PLAYING&&game.getType()==GameType.PVE)botTurn(game); }
        return publish(game,player);
    }

    @Transactional public GameDtos.GameResponse resign(UserAccount player, UUID gameId) {
        Game game = participant(player, gameId, true);
        if (game.getStatus()!=GameStatus.PLAYING) throw new ApiException(HttpStatus.CONFLICT,"Game is not active");
        String side=ownSide(game,player);game.resignWithWinner(opposite(side));record(game,player.getId(),side,"RESIGN",null,null,null,"RESIGN");settleWinner(game);return publish(game,player);
    }

    @Transactional public GameDtos.GameResponse cancelWaiting(UserAccount player, UUID gameId) {
        Game game = participant(player, gameId, true);
        if (!player.getId().equals(game.getWhitePlayerId())) throw new ApiException(HttpStatus.FORBIDDEN, "Only the room creator can cancel it");
        if (game.getStatus() != GameStatus.WAITING) throw new ApiException(HttpStatus.CONFLICT, "Only a waiting game can be cancelled");
        game.cancelWaiting(); return publish(game,player);
    }

    @Transactional public GameDtos.GameResponse get(UserAccount player, UUID id) { Game game=participant(player,id,false);if(game.getType()==GameType.PVP&&game.getStatus()==GameStatus.WAITING&&!game.getCreatedAt().plus(ROOM_TTL).isAfter(Instant.now()))game.cancelWaiting();return view(game,player); }
    @Transactional public List<GameDtos.GameResponse> history(UserAccount player) { expireWaitingRooms();return games.findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByUpdatedAtDesc(player.getId(),player.getId()).stream().map(game->view(game,player)).toList(); }
    @Transactional public List<GameDtos.GameResponse> waiting() { expireWaitingRooms();return games.findTop20ByStatusOrderByCreatedAtAsc(GameStatus.WAITING).stream().map(game->view(game,null)).toList(); }
    @Scheduled(fixedDelayString="${app.rooms.cleanup-ms:60000}")
    @Transactional public void expireWaitingRooms(){Instant now=Instant.now();games.expireWaitingRooms(GameType.PVP,GameStatus.WAITING,GameStatus.ABANDONED,now.minus(ROOM_TTL),now);}
    public List<GameDtos.MoveLogResponse> logs(UserAccount player,UUID id){participant(player,id,false);return moveLogs.findByGameIdOrderBySequenceNoAsc(id).stream().map(log->new GameDtos.MoveLogResponse(log.getSequenceNo(),log.getActorUserId(),log.getActorSide(),log.getEventType(),log.getFromSquare(),log.getToSquare(),log.getCapturedPiece(),log.getEffectCode(),log.getOccurredAt())).toList();}

    private void botTurn(Game game){
        if(game.getStatus()!=GameStatus.PLAYING||game.getType()!=GameType.PVE||game.getCurrentTurn().equals(game.getPvePlayerSide()))return;
        String side=game.getCurrentTurn(),other=opposite(side);
        try{
            ChessBoard.BotMove move=ChessBoard.botMove(mapper,game.getBoard(),game.getBotDifficulty(),game.getInvulnerableSquare(),game.getFrozenSquare(),side,game.hasBackwardPawns(side));
            game.updateBoard(move.board());game.moveEffects(move.from(),move.to(),side,move.captured()!=null);applyCapture(game,move.captured(),other);record(game,null,side,"MOVE",move.from(),move.to(),move.captured(),null);recordEffect(game,side,finishTurn(game,side));
        }catch(ApiException ignored){game.finish(GameStatus.DRAW);settleDraw(game);}
    }
    private ResolvedEffect finishTurn(Game game,String movingSide){
        game.advanceEffects(movingSide);ResolvedEffect effect=resolveTrap(game);
        if(game.getStatus()==GameStatus.FINISHED){ensureHpWinner(game);settleWinner(game);return effect;}
        endIfKingCaptured(game);
        return effect;
    }
    private ResolvedEffect resolveTrap(Game game){
        String square=game.resolveTrap();if(square==null)return null;String piece=ChessBoard.pieceAt(mapper,game.getBoard(),square);if(piece==null)return new ResolvedEffect("TRAP",square,null);
        String side=piece.charAt(0)=='w'?"WHITE":"BLACK";if(game.isInvulnerable(square,side))return new ResolvedEffect("TRAP_BLOCKED",square,null);
        game.replaceBoard(ChessBoard.removePiece(mapper,game.getBoard(),square));game.recordCapture(piece);game.adjustHp(side,-captureDamage(piece));return new ResolvedEffect("TRAP",square,piece);
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
    private String newRoomCode(){for(int attempt=0;attempt<20;attempt++){String code=String.format("%06d",ThreadLocalRandom.current().nextInt(1_000_000));if(!games.existsByRoomCode(code))return code;}throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa thể cấp mã phòng. Hãy thử lại.");}
    private String opposite(String side){return side.equals("WHITE")?"BLACK":"WHITE";}
    private Game game(UUID id) { return games.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Game not found")); }
    private Game locked(UUID id) { return games.findWithLockById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Game not found")); }
    private void record(Game game,UUID actor,String side,String type,String from,String to,String captured,String effect){int sequence=moveLogs.findTopByGameIdOrderBySequenceNoDesc(game.getId()).map(log->log.getSequenceNo()+1).orElse(1);moveLogs.save(new GameMoveLog(game.getId(),sequence,actor,side,type,from,to,captured,effect));}
    private void recordEffect(Game game,String side,ResolvedEffect effect){if(effect!=null)record(game,null,side,"EFFECT",null,effect.square(),effect.capturedPiece(),effect.code());}
    private GameDtos.GameResponse publish(Game game,UserAccount viewer) { GameDtos.GameResponse response=view(game,viewer);signalAfterCommit(game.getId(),viewer.getId());return response; }
    private void signalAfterCommit(UUID gameId,UUID actorId){
        if(!TransactionSynchronizationManager.isSynchronizationActive()){realtime.signal(gameId,actorId);return;}
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){realtime.signal(gameId,actorId);}});
    }
    private String enemyPiece(Game game, String square, String side) { return targetPiece(game, square, side, "Hãy chọn quân đối phương trên bàn cờ"); }
    private String ownPiece(Game game, String square, String side) { return targetPiece(game, square, side, "Hãy chọn quân của bạn trên bàn cờ"); }
    private String targetPiece(Game game, String square, String side, String message) { square(square,message); String piece = ChessBoard.pieceAt(mapper, game.getBoard(), square); if (piece == null || piece.charAt(0) != Character.toLowerCase(side.charAt(0))) throw new ApiException(HttpStatus.BAD_REQUEST, message); return square; }
    private void square(String value,String message){if(value==null||!value.matches("[a-h][1-8]"))throw new ApiException(HttpStatus.BAD_REQUEST,message);}
    private String[] pair(String value){if(value==null||!value.matches("[a-h][1-8]:[a-h][1-8]"))throw new ApiException(HttpStatus.BAD_REQUEST,"Chọn lần lượt hai quân cho Portal");return value.split(":");}
    private GameDtos.GameResponse view(Game game,UserAccount viewer) { String side=game.getType()==GameType.PVE?game.getPvePlayerSide():null;List<UUID> deck=viewer==null?List.of():game.deckFor(ownSide(game,viewer));UserAccount white=users.findById(game.getWhitePlayerId()).orElseThrow();UserAccount black=game.getBlackPlayerId()==null?null:users.findById(game.getBlackPlayerId()).orElseThrow();Instant expires=game.getType()==GameType.PVP&&game.getStatus()==GameStatus.WAITING?game.getCreatedAt().plus(ROOM_TTL):null;return new GameDtos.GameResponse(game.getId(), game.getType().name(), game.getStatus().name(), game.getBotDifficulty().name(), game.getWhitePlayerId(), game.getBlackPlayerId(),white.getUsername(),white.getRating(),black==null?null:black.getUsername(),black==null?null:black.getRating(),game.getRoomCode(),expires,side, game.getCurrentTurn(), ChessBoard.view(mapper,game.getBoard()), game.getWhiteHp(), game.getBlackHp(), game.getWinnerColor(), game.getFrozenSquare(), game.getInvulnerableSquare(),game.getTrapSquare(),game.getTrapTurns(),game.getMosquitoSquare(),game.isBackwardPawnsWhite(),game.isBackwardPawnsBlack(),deck); }
    private record ResolvedEffect(String code,String square,String capturedPiece){}
}
