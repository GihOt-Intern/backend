package com.server.game.netty.pipelineComponent;


import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import com.server.game.netty.ChannelRegistry;
import com.server.game.service.AuthenticationService;
import com.server.game.util.Util;

import io.netty.channel.Channel;
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
        
  
        URI uri = new URI(request.uri());   // "ws://localhost:8386/ws?token=<jwt_token>"   or   "ws://localhost:8386/game/{gameId}?token=<jwt_token>"
        String path = uri.getPath();  

        // System.out.println(">>> PATH: " + path);
        String[] segments = Arrays.stream(path.split("/"))
                                .filter(s -> !s.isEmpty()) // Filter out empty segments
                                .toArray(String[]::new);
        
        String gameId = null;
        if (segments[0].equals("game")) {
            System.out.println(">>> Game request detected.");
            try {
                gameId = segments[1]; // Get the second segment as gameId
                System.out.println(">>> Game ID: " + gameId);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println(">>> Invalid request URI, closing channel.");
                ctx.close(); // Close the channel if the URI is invalid
                return;
            }
        } else if (segments[0].equals("ws")) {
            System.out.println(">>> WebSocket request detected.");
        } else {
            System.out.println(">>> Invalid request URI, closing channel.");
            ctx.close(); // Close the channel if the URI is invalid
            throw new IllegalArgumentException("Invalid request URI: " + request.uri());
        }

        String query = uri.getQuery();

        Map<String, String> queryParams = Util.handleQueryString(query);
        if (!queryParams.containsKey("token")) {
            System.out.println(">>> Token not found in query parameters, closing channel.");
            ctx.close(); // Close the channel if token is not found
            return;
        }
        String token = queryParams.get("token");

        String userId = authenticationService.getJWTSubject(token);

        Channel channel = ctx.channel();
        ChannelRegistry.userRegister(userId, channel);

        if (gameId != null) {
            ChannelRegistry.gameRegister(gameId, channel);
        }


        System.out.println(">>> Handshake successful for userId: " + userId);

        // Clean path and query parameters to send to next handler in pipeline
        FullHttpRequest newRequest = request.replace(request.content().retain());
        newRequest.setUri("");

        System.out.println("Continue to next handler in pipeline...");
        ctx.fireChannelRead(newRequest); 
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
