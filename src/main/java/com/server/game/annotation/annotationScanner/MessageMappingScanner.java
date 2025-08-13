package com.server.game.annotation.annotationScanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.messageMapping.MessageDispatcher;

import java.lang.reflect.Method;

@Component
public class MessageMappingScanner implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private MessageDispatcher dispatcher;

    @Autowired
    private ApplicationContext context;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        // System.out.println(">>> Scanning for @MessageMapping methods...");
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
