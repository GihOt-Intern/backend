package com.server.game.ws.messageMapping;

import org.springframework.web.socket.WebSocketSession;

@FunctionalInterface
public interface MessageHandler {
    void handle(WebSocketSession session, Object object);
}

