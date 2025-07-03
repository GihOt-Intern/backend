package com.server.game.netty.handler;

import com.server.game.tlv.serializationable.TLVDecodable;
import com.server.game.tlv.serializationable.TLVEncodable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.server.game.messageMapping.DispatcherHolder;
import com.server.game.messageMapping.MessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Component
public class GameWebSocketHandler extends SimpleChannelInboundHandler<TLVDecodable> {

    // @Autowired
    // private MessageDispatcher dispatcher;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TLVDecodable receiveObj) throws Exception {

        System.out.println("Received TLVDecodable object: " + receiveObj.getClass().getSimpleName());

        // TLVEncodable response = (TLVEncodable) dispatcher.dispatch(receiveObj);
        TLVEncodable response = (TLVEncodable) DispatcherHolder.INSTANCE.dispatch(receiveObj);
        // TODO: NO write here
        ctx.writeAndFlush(response); // TLVEncodable sẽ được encode bởi TLVMessageEncoder
    }
}
