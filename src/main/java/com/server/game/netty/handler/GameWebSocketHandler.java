package com.server.game.netty.handler;

import com.server.game.tlv.serializationable.TLVDecodable;
import com.server.game.tlv.serializationable.TLVEncodable;
import com.server.game.messageMapping.MessageDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class GameWebSocketHandler extends SimpleChannelInboundHandler<Class<? extends TLVDecodable>> {
    private final MessageDispatcher dispatcher = new MessageDispatcher();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Class<? extends TLVDecodable> receiveObj) throws Exception {
        TLVEncodable response = (TLVEncodable) dispatcher.dispatch(receiveObj);
        ctx.writeAndFlush(response); // TLVEncodable sẽ được encode bởi TLVMessageEncoder
    }
}
