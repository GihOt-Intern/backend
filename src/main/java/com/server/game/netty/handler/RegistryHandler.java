package com.server.game.netty.handler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelRegistry;
import com.server.game.netty.messageObject.receiveObject.AuthenticationReceive;
import com.server.game.netty.messageObject.sendObject.AuthenticationSend;
import com.server.game.netty.pipelineComponent.AuthenticationHandler;
import com.server.game.service.AuthenticationService;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegistryHandler {

    AuthenticationService authenticationService;

    @MessageMapping(AuthenticationReceive.class)
    public AuthenticationSend authenticate(AuthenticationReceive receiveObject, ChannelHandlerContext ctx) {
        String token = receiveObject.getToken();
        String gameId = receiveObject.getGameId();

        String userId = authenticationService.getJWTSubject(token);
        if (userId == null) {
            System.out.println(">>> Authentication failed for token: " + token);
            return new AuthenticationSend(AuthenticationSend.Status.FAILURE, "Invalid token, playerId does not exist");
        }

        Channel channel = ctx.channel();
        ChannelRegistry.register(userId, gameId, channel);

        // Make sure removing is invoked in the correct thread
        ctx.channel().eventLoop().execute(() -> {
            // Remove the AuthenticationHandler from the pipeline after successful authentication
            ctx.pipeline().remove(AuthenticationHandler.class);
        });

        return new AuthenticationSend(AuthenticationSend.Status.SUCCESS, "Authenticated successfully!");
    }
}
