package com.server.game.netty.sender;


import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.pipelineComponent.AuthenticationHandler;
import com.server.game.netty.receiveObject.AuthenticationReceive;
import com.server.game.netty.sendObject.AuthenticationSend;
import com.server.game.netty.sendObject.MessageSend;
import com.server.game.service.authentication.AuthenticationService;
import com.server.game.service.user.UserService;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegistrySender {

    AuthenticationService authenticationService;
    UserService userService;

    @MessageMapping(AuthenticationReceive.class)
    public void authenticate(AuthenticationReceive receiveObject, ChannelHandlerContext ctx) {
        String token = receiveObject.getToken();
        String gameId = receiveObject.getGameId();
        String userId = authenticationService.getJWTSubject(token);

        Channel existingChannel = ChannelManager.getChannelByUserId(userId);
        if (existingChannel != null && existingChannel != ctx.channel()) {
            System.out.println(">>> User already has an active session, disconnecting the old channel.");
            ChannelManager.unregister(existingChannel);
            existingChannel.close();
        }

        if (userId == null || !userService.isUserExist(userId)) {
            System.out.println(">>> Authentication failed for token: " + token);
            ctx.channel().writeAndFlush(new AuthenticationSend(AuthenticationSend.Status.FAILURE, "Invalid token, playerId does not exist or failed to register for room notifications"));
            return;
        }

        Channel channel = ctx.channel();
        ChannelManager.register(userId, gameId, channel);
        // ChannelRegistry.registerToRoom(userId, gameId, channel); // Register for room notifications as well

        // Make sure removing is invoked in the correct thread
        ctx.channel().eventLoop().execute(() -> {
            // Remove the AuthenticationHandler from the pipeline after successful authentication
            ctx.pipeline().remove(AuthenticationHandler.class);
        });

        ctx.channel().writeAndFlush(new AuthenticationSend(AuthenticationSend.Status.SUCCESS, "Authenticated and registered to room notifications successfully!"));
        // Send room ID to all the client in the room
        ctx.channel().writeAndFlush(new MessageSend(gameId));
    }
}
