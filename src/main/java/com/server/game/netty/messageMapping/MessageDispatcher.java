package com.server.game.netty.messageMapping;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import lombok.AllArgsConstructor;

import java.lang.reflect.Method;

@Component
public class MessageDispatcher {

    private final Map<Class<?>, HandlerMethod> handlerMap = new HashMap<>();

    public void register(Class<?> messageDTO, Object handler, Method method) {
        handlerMap.put(messageDTO, new HandlerMethod(handler, method));
        System.out.println(">>> Registered handler: <" + method.getName() + "> method in <" + handler.getClass().getSimpleName() + "> class" +
            " to handle <" + messageDTO.getSimpleName() + "> message type");
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
            if (method.getReturnType() == void.class) {
                // If the method returns void, invoke it with the message
                method.invoke(handler, message);
                return null; // No response to return
            }
            return method.invoke(handler, message);
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