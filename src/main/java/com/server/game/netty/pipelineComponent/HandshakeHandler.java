package com.server.game.netty.pipelineComponent;

import com.server.game.exception.DataNotFoundException;
import com.server.game.netty.UserChannelRegistry;
import com.server.game.util.Util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;

public class HandshakeHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // If it's not a handshake event, pass it to the next handler in the pipeline
        if (!(evt instanceof HandshakeComplete)) {
            super.userEventTriggered(ctx, evt);
            return;
        }

        HandshakeComplete handshake = (HandshakeComplete) evt;

        System.out.println(">>> WebSocket Handshake completed. Channel: " + ctx.channel().id());

        String token = null;
        try {
            String uri = handshake.requestUri();
            System.out.println("Request URI: " + uri);
            // Parse the request URI to get token in uri
            token = Util.getTokenFromUri(uri);
        } catch (DataNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
            ctx.close(); // Close the channel if token is not found
            return;
        }
        
        System.out.println(">>> Handshake token: " + token);

        UserChannelRegistry.register(token, ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
