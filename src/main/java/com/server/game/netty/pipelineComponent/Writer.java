package com.server.game.netty.pipelineComponent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;


public class Writer extends  ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Only process ByteBuf messages, if not, pass it to the next handler
        if (!(msg instanceof ByteBuf)) {
            ctx.writeAndFlush(msg);
            return;
        }

        System.out.println(">>> Server received a send ByteBuf");
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(((ByteBuf) msg).retain());
        System.out.println(">>> Server converted ByteBuf to BinaryWebSocketFrame");
        ctx.writeAndFlush(frame);
        System.out.println(">>> ====== Server sent BinaryWebSocketFrame to the client ======\n\n");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
