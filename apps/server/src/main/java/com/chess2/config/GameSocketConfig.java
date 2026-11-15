package com.chess2.config;

import com.chess2.realtime.GameSocketHub;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class GameSocketConfig implements WebSocketConfigurer {
    private final GameSocketHub hub;
    public GameSocketConfig(GameSocketHub hub){this.hub=hub;}
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(hub,"/ws/games");}
}
