package com.server.game.ws.messageMapping;

import java.lang.reflect.Method;

import org.springframework.web.socket.WebSocketSession;

public class MethodHandler {
    private final Object controller;
    private final Method method;

    public MethodHandler(Object controller, Method method) {
        this.controller = controller;
        this.method = method;
    }

    public void invoke(WebSocketSession session, Object message) throws Exception {
        method.invoke(controller, session, message);
    }
}
