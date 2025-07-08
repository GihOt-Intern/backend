package com.server.game.netty;

import com.server.game.netty.pipelineComponent.*;
import com.server.game.service.AuthenticationService;
import com.server.game.netty.messageMapping.MessageDispatcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;


@Component
public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    // Will be talked later
    @Autowired
    private MessageDispatcher dispatcher;

    // This service will be talked later
    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void initChannel(SocketChannel ch) throws Exception {

        ch.pipeline()
            .addLast(new HttpServerCodec()) // HTTP codec
            .addLast(new HttpObjectAggregator(65536)) // Aggregate HTTP messages

            // Parse the request parameters and extract token to identify user (inbound)
            .addLast(new HandshakeHandler(authenticationService)) 
            // Upgrade from HTTP to WebSocket (inbound)
            .addLast(new WebSocketServerProtocolHandler(""))


            // Handle WebSocket frames: 

            // Receive BinaryWebSocketFrame and convert to ByteBuf  (inbound)
            .addLast(new Reader()) 
            // Receive ByteBuf and convert to TLVDecodable objects  (inbound)
            .addLast(new TLVMessageDecoder()) 
            // Receive ByteBuf, convert it to BinaryWebSocketFrame and send it to the client  (outbound)
            
            .addLast(new Writer()) 
            // Receive TLVEncodable objects and convert them to ByteBuf   (outbound)
            .addLast(new TLVMessageEncoder()) 
            // Business logic handler, receives TLVDecodable objects and creates TLVEncodable objects  (inbound)
            .addLast(new BussinessHandler(dispatcher)) 

            // Handle channel disconnection and unregister user channel (inbound)
            .addLast(new DisconnectHandler()) 
        ;
    }
}
