package com.server.game.netty.handler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelRegistry;
import com.server.game.netty.messageObject.receiveObject.AuthenticationReceive;
import com.server.game.service.AuthenticationService;

import io.netty.channel.Channel;


@Component
public class AuthenticationHandler {

    AuthenticationService authenticationService;

    @MessageMapping(AuthenticationReceive.class)
    public void authenticate(AuthenticationReceive receiveObject, Channel channel) {
        String token = receiveObject.getToken();
        String gameId = receiveObject.getGameId();

        String userId = authenticationService.getJWTSubject(token);
        if (userId == null) {
            System.out.println(">>> Authentication failed for token: " + token);
            return; // Handle authentication failure as needed
        }

        ChannelRegistry.register(userId, gameId, channel);
    }
}
