package com.server.game.ws.messageMapping;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.socket.WebSocketSession;

import lombok.AllArgsConstructor;

import java.lang.reflect.Method;

public class MessageDispatcher {

    private final Map<Class<?>, HandlerMethod> handlerMap = new HashMap<>();

    public void register(Class<?> messageDTO, Object handler, Method method) {
        handlerMap.put(messageDTO, new HandlerMethod(handler, method));
        System.out.println(">>> Registered handler for " + messageDTO.getName() + " with method " + method.getName() 
                + " in " + handler.getClass().getSimpleName() + " class");
    }

    public Object dispatch(Object message) throws Exception {
        HandlerMethod handler = handlerMap.get(message.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for message: " + message);
        }
        return handler.invoke(message); // trả về DTO response
    }

    @AllArgsConstructor
    private static class HandlerMethod {

        private final Object handler;
        private final Method method;


        public Object invoke(Object message) throws Exception {
            return method.invoke(handler, message); // method phải trả về Object (DTO response)
        }
    }
}




class MethodHandler {
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