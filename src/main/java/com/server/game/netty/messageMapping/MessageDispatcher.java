package com.server.game.netty.messageMapping;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

import java.lang.reflect.Method;

@Component
public class MessageDispatcher {

    private final Map<Class<?>, HandlerMethod> handlerMap = new HashMap<>();

    public void register(Class<?> receiveClazz, Object classContains, Method method) {
        handlerMap.put(receiveClazz, new HandlerMethod(classContains, method));
        System.out.println(">>> Registered handler: <" + method.getName() + "> method in <" + classContains.getClass().getSimpleName() + "> class" +
            " to handle <" + receiveClazz.getSimpleName() + "> message type");
    }

    // Dispatches a message to the appropriate handler based on its type
    // public Object dispatch(Object message) throws Exception {
    //     HandlerMethod handler = handlerMap.get(message.getClass());
    //     if (handler == null) {
    //         throw new IllegalArgumentException("No handler for message: " + message);
    //     }
    //     return handler.invoke(message); // trả về DTO response
    // }



    public Object dispatch(Object message, Map<Class<?>, Object> contextParams) throws Exception {
    HandlerMethod handler = handlerMap.get(message.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler for message: " + message);
        }
        return handler.invoke(message, contextParams);
    }



    @AllArgsConstructor
    private static class HandlerMethod {

        private final Object classContains; // The class that contains the method to be invoked
        private final Method method; // The method to be invoked

        // public Object invoke(Object message) throws Exception {
        //     if (method.getReturnType() == void.class) {
        //         // If the method returns void, invoke it with the message
        //         method.invoke(classContains, message);
        //         return null; // No response to return
        //     }
        //     return method.invoke(classContains, message);
        // }

        public Object invoke(Object message, Map<Class<?>, Object> contextParams) throws Exception {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> paramType = parameterTypes[i];
                if (paramType.isInstance(message)) {
                    args[i] = message; 
                } else {
                    Object contextObject = contextParams.get(paramType);
                    if (contextObject == null) {
                        throw new IllegalArgumentException("Method " + method.getName() + " is missing context for parameter: " + paramType.getSimpleName());
                    }
                    args[i] = contextObject;
                }
            }
            
            if (method.getReturnType() == void.class) {
                // If the method returns void, invoke it with the args
                method.invoke(classContains, args);
                return null; // No response to return
            }

            return method.invoke(classContains, args);
        }
    }



}
