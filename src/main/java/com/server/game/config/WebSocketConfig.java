package com.server.game.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import com.server.game.ws.BinarySocketHandler;
import com.server.game.ws.TextSocketHandler;



@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private BinarySocketHandler socketHandler;

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(socketHandler, "/ws")
            .addHandler(new TextSocketHandler(), "/ws2")
                .setAllowedOriginPatterns("*");
    }
}



// @Configuration
// @EnableWebSocketMessageBroker
// public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//     @Autowired
//     private AuthChannelInterceptor authChannelInterceptor;

//     @Override
//     public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
//         registry.addEndpoint("/ws-chat")
//                 .setAllowedOriginPatterns("http://localhost:8080")
//                 // .setHandshakeHandler(new UserHandshakeHandler())
//                 .withSockJS();
//     }

//     @Override
//     public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
//         registry.setApplicationDestinationPrefixes("/app");
//         // registry.enableSimpleBroker("/topic", "/queue", "/user");
//         // registry.setUserDestinationPrefix("/user");
//         registry.enableSimpleBroker("/topic", "/queue");
//     }


//     @Override
//     public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
//         registration.interceptors(authChannelInterceptor);
//     }


//     @Override
//     public boolean configureMessageConverters(@NonNull List<MessageConverter> messageConverters) {
//         messageConverters.add(new ProtobufMessageConverter());
//         return false; // không override các converter mặc định (JSON, String,...)
//     }

// }



