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
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final GameRepository games; private final CatalogService catalog; private final UserRepository users; private final ObjectMapper mapper; private final SimpMessagingTemplate messages;
    public GameService(GameRepository games, CatalogService catalog, UserRepository users, ObjectMapper mapper, SimpMessagingTemplate messages) { this.games=games; this.catalog=catalog; this.users=users; this.mapper=mapper; this.messages=messages; }
    @Transactional public GameDtos.GameResponse create(UserAccount player, GameType type) {
        UUID black = type == GameType.LOCAL ? player.getId() : null;
        Game game=games.save(new Game(type, player.getId(), black, ChessBoard.initial(mapper))); return publish(game);
    }
    @Transactional public GameDtos.GameResponse join(UserAccount player, UUID gameId) { Game game=game(gameId); if (game.getType()!=GameType.PVP || game.getStatus()!=GameStatus.WAITING) throw new ApiException(HttpStatus.CONFLICT,"Game cannot be joined"); game.join(player.getId()); return publish(game); }
    @Transactional public GameDtos.GameResponse move(UserAccount player, UUID gameId, GameDtos.MoveRequest request) {
        Game game=participant(player, gameId); if (game.getStatus()!=GameStatus.PLAYING) throw new ApiException(HttpStatus.CONFLICT,"Game is not active");
        boolean white=game.getCurrentTurn().equals("WHITE"); UUID expected=white?game.getWhitePlayerId():game.getBlackPlayerId();
        if (game.getType()!=GameType.PVE && !player.getId().equals(expected)) throw new ApiException(HttpStatus.FORBIDDEN,"It is not your turn");
        if (game.getType()==GameType.PVE && !white) throw new ApiException(HttpStatus.CONFLICT,"Wait for the bot");
        game.updateBoard(ChessBoard.move(mapper,game.getBoard(),request.from(),request.to(),game.getCurrentTurn()));
        if (game.getType()==GameType.PVE && game.getStatus()==GameStatus.PLAYING && game.getCurrentTurn().equals("BLACK")) { try { game.updateBoard(ChessBoard.botMove(mapper,game.getBoard())); } catch (ApiException ignored) { game.finish(GameStatus.DRAW); } }
        return publish(game);
    }
    @Transactional public GameDtos.GameResponse useCard(UserAccount player, UUID gameId, GameDtos.UseCardRequest request) {
        Game game=participant(player,gameId); FunctionCard card=catalog.useCard(player,request.cardId());
        String ownSide=player.getId().equals(game.getWhitePlayerId())?"WHITE":"BLACK"; String other=ownSide.equals("WHITE")?"BLACK":"WHITE";
        if (card.getEffectType().equals("HEAL")) game.adjustHp(ownSide, Math.max(1,card.getEffectValue()));
        if (card.getEffectType().equals("DOUBLE_DAMAGE")) game.adjustHp(other, -Math.max(1,card.getEffectValue()));
        return publish(game);
    }
    public GameDtos.GameResponse get(UserAccount player, UUID id) { return view(participant(player,id)); }
    public List<GameDtos.GameResponse> history(UserAccount player) { return games.findTop20ByWhitePlayerIdOrBlackPlayerIdOrderByUpdatedAtDesc(player.getId(),player.getId()).stream().map(this::view).toList(); }
    public List<GameDtos.GameResponse> waiting() { return games.findTop20ByStatusOrderByCreatedAtAsc(GameStatus.WAITING).stream().map(this::view).toList(); }
    private Game participant(UserAccount player, UUID id) { Game game=game(id); if(!player.getId().equals(game.getWhitePlayerId())&&!player.getId().equals(game.getBlackPlayerId())) throw new ApiException(HttpStatus.FORBIDDEN,"You are not a match participant"); return game; }
    private Game game(UUID id) { return games.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Game not found")); }
    private GameDtos.GameResponse publish(Game game) { GameDtos.GameResponse response=view(game); messages.convertAndSend("/topic/games/"+game.getId(),response); return response; }
    private GameDtos.GameResponse view(Game game) { return new GameDtos.GameResponse(game.getId(),game.getType().name(),game.getStatus().name(),game.getWhitePlayerId(),game.getBlackPlayerId(),game.getCurrentTurn(),ChessBoard.view(mapper,game.getBoard()),game.getWhiteHp(),game.getBlackHp()); }
}
