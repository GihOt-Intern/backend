package com.server.game.netty.pipelineComponent;


import com.server.game.exception.DataNotFoundException;
import com.server.game.netty.UserChannelRegistry;
import com.server.game.service.AuthenticationService;
import com.server.game.util.Util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;


public class HandshakeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // cannot use @Autowired here because this class is not a Spring component
    private AuthenticationService authenticationService;

    public HandshakeHandler(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        
        System.out.println(">>> HTTP Request: " + request);


        if (!request.uri().contains("token=")) {
            System.out.println(">>> No token found in request URI, closing channel.");
            ctx.close(); // Close the channel if no token is found
            return;
        }


        String token = null;        
        try {
            token = Util.getTokenFromUri(request.uri());
        } catch (DataNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
            ctx.close(); // Close the channel if token is not found
            return;
        }

        // System.out.println(">>> Token: " + token);

        String userId = authenticationService.getJWTSubject(token);

        UserChannelRegistry.register(userId, ctx.channel());


        FullHttpRequest newRequest = request.replace(request.content().retain());
        newRequest.setUri("/ws");

        System.out.println("Continue to next handler in pipeline...");
        System.out.println(">>> Request after: " + newRequest);
        ctx.fireChannelRead(newRequest);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
