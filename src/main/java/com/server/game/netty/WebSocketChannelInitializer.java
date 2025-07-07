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

    @Autowired
    private MessageDispatcher dispatcher;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void initChannel(SocketChannel ch) throws Exception {

        ch.pipeline()
                .addLast(new HttpServerCodec()) // 1. HTTP codec
                .addLast(new HttpObjectAggregator(65536)) // 2. Aggregate HTTP messages

                .addLast(new HandshakeHandler(authenticationService)) // Parse the request parameters and extract token to identify user
                // Upgrade from HTTP to WebSocket
                .addLast(new WebSocketServerProtocolHandler("/ws", null, true))


                // Handle WebSocket frames
                .addLast(new Reader()) // Receive BinaryWebSocketFrame and convert to ByteBuf       I
                .addLast(new TLVMessageDecoder()) // Receive ByteBuf and convert to TLVDecodable objects  I

                .addLast(new Writer()) // Receive ByteBuf, convert it to BinaryWebSocketFrame and send it to the client  O
                .addLast(new TLVMessageEncoder()) // Receive TLVEncodable objects and convert them to ByteBuf   O
                .addLast(new BussinessHandler(dispatcher)) // Business logic handler, receives TLVDecodable objects and creates TLVEncodable objects  I

                .addLast(new DisconnectHandler()) // Handle channel disconnection and unregister user channel
                ;
    }
}
