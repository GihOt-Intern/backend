package com.server.game.ws.messageMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class MessageHandlerScanner implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private MessageDispatcher dispatcher;

    @Autowired
    private ApplicationContext context;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        System.out.println(">>> Scanning for message handler methods...");
        for (Object bean : context.getBeansWithAnnotation(Component.class).values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                MessageMapping mapping = method.getAnnotation(MessageMapping.class);
                if (mapping != null) {
                    dispatcher.register(mapping.value(), bean, method);
                }
            }
        }
    }
}
