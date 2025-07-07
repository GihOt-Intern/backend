package com.server.game.config;

// import java.lang.reflect.Method;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.server.game.netty.messageMapping.MessageDispatcher;

@Configuration
public class DispatcherConfig {

    @Bean
    public MessageDispatcher messageDispatcher() {
        return new MessageDispatcher();
    }
}
