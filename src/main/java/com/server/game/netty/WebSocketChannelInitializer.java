package com.server.game.netty;



import com.server.game.netty.handler.GameWebSocketHandler;
import com.server.game.netty.handler.TLVMessageDecoder;
import com.server.game.netty.handler.TLVMessageEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;


public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        SslContext sslCtx = SslContextProvider.createSslContext("src/main/resources/keystore.p12", "password");

        ch.pipeline()
                .addLast(sslCtx.newHandler(ch.alloc()))
                .addLast(new HttpServerCodec()) // 1. HTTP codec
                .addLast(new HttpObjectAggregator(65536)) // 2. Aggregate HTTP messages
                .addLast(new WebSocketServerProtocolHandler("/ws")) // 3. WebSocket protocol handler

                // Handle WebSocket frames
                .addLast(new TLVMessageDecoder()) // 4. Decode BinaryWebSocketFrame to TLVDecodable objects
                .addLast(new GameWebSocketHandler()) // 5. Business logic handler
                .addLast(new TLVMessageEncoder()) // 6. Encode TLVDecodable objects to BinaryWebSocketFrame
        ;
    }
}
