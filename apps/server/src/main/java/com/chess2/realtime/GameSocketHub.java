package com.chess2.realtime;

import com.chess2.domain.Game;
import com.chess2.domain.UserAccount;
import com.chess2.repository.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameSocketHub extends TextWebSocketHandler implements SubProtocolCapable {
    private static final Duration STALE_AFTER=Duration.ofSeconds(60);
    private final GameRepository games;private final ObjectMapper mapper;
    // ponytail: this hub is instance-local; replace signal() with shared pub/sub before multi-instance load testing.
    private final ConcurrentMap<String,Client> clients=new ConcurrentHashMap<>();
    public GameSocketHub(GameRepository games,ObjectMapper mapper){this.games=games;this.mapper=mapper;}
    @Override public List<String> getSubProtocols(){return List.of("chess2");}

    @Override public void afterConnectionEstablished(WebSocketSession session)throws Exception{
        UserAccount user=user(session);
        if(user==null){session.close(CloseStatus.POLICY_VIOLATION);return;}
        clients.put(session.getId(),new Client(new ConcurrentWebSocketSessionDecorator(session,5000,65536),user.getId()));
    }
    @Override protected void handleTextMessage(WebSocketSession session,TextMessage text)throws Exception{
        Client client=clients.get(session.getId());if(client==null)return;
        JsonNode message;
        try{message=mapper.readTree(text.getPayload());}catch(Exception error){close(client,CloseStatus.BAD_DATA);return;}
        if(!"subscribe".equals(message.path("type").asText()))return;
        UUID gameId;
        try{gameId=UUID.fromString(message.path("gameId").asText());}catch(IllegalArgumentException error){close(client,CloseStatus.BAD_DATA);return;}
        Game game=games.findById(gameId).orElse(null);
        if(game==null||(!client.userId.equals(game.getWhitePlayerId())&&!client.userId.equals(game.getBlackPlayerId()))){close(client,CloseStatus.POLICY_VIOLATION);return;}
        client.gameId=gameId;
        send(client,event("ready",gameId));
    }
    @Override protected void handlePongMessage(WebSocketSession session,PongMessage message){Client client=clients.get(session.getId());if(client!=null)client.lastPong=System.currentTimeMillis();}
    @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){clients.remove(session.getId());}
    @Override public void handleTransportError(WebSocketSession session,Throwable error)throws Exception{Client client=clients.remove(session.getId());if(client!=null)close(client,CloseStatus.SERVER_ERROR);}

    public void signal(UUID gameId,UUID actorId){for(Client client:clients.values())if(gameId.equals(client.gameId)&&!actorId.equals(client.userId))send(client,event("game-updated",gameId));}
    @Scheduled(fixedDelayString="${app.websocket.heartbeat-ms:25000}")
    void heartbeat(){long now=System.currentTimeMillis();for(Client client:clients.values()){if(now-client.lastPong>STALE_AFTER.toMillis())close(client,CloseStatus.SESSION_NOT_RELIABLE);else send(client,new PingMessage(ByteBuffer.wrap(new byte[]{1})));}}

    private TextMessage event(String type,UUID gameId){return new TextMessage(mapper.createObjectNode().put("type",type).put("gameId",gameId.toString()).toString());}
    private void send(Client client,org.springframework.web.socket.WebSocketMessage<?> message){try{if(client.session.isOpen())client.session.sendMessage(message);}catch(IOException error){clients.remove(client.session.getId());close(client,CloseStatus.SERVER_ERROR);}}
    private void close(Client client,CloseStatus status){clients.remove(client.session.getId());try{if(client.session.isOpen())client.session.close(status);}catch(IOException ignored){}}
    private UserAccount user(WebSocketSession session){if(session.getPrincipal() instanceof Authentication auth&&auth.getPrincipal() instanceof UserAccount account)return account;return null;}
    private static final class Client{
        private final WebSocketSession session;private final UUID userId;private volatile UUID gameId;private volatile long lastPong=System.currentTimeMillis();
        private Client(WebSocketSession session,UUID userId){this.session=session;this.userId=userId;}
    }
}
