package com.server.game.annotation.annotationScanner;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.codec.TLVDecoder;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ReceiveTypeScanner implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private ApplicationContext context;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        // System.out.println(">>> Scanning for @ReceiveType classes...");

        String[] beanNames = context.getBeanDefinitionNames();
        for (String name : beanNames) {
            Class<?> clazz = context.getType(name);
            if (clazz == null) continue;

            ReceiveType annotation = clazz.getAnnotation(ReceiveType.class);
            if (annotation != null && TLVDecodable.class.isAssignableFrom(clazz)) {
                short type = annotation.value().getType();
                
                @SuppressWarnings("unchecked")
                Class<? extends TLVDecodable> receiveClazz = (Class<? extends TLVDecodable>) clazz;
                TLVDecoder.register(type, receiveClazz);
            }
        }
    }
}
