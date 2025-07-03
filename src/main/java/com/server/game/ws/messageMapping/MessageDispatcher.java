package com.server.game.ws.messageMapping;


import java.util.HashMap;
import java.util.Map;

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