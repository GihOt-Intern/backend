package com.server.game.netty.pipelineComponent;


import com.server.game.netty.ChannelRegistry;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;


import java.util.Set;


public class Writer extends  ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Only process ByteBuf messages, if not, pass it to the next handler
        if (!(msg instanceof ByteBuf)) {
            ctx.writeAndFlush(msg, promise);
            return;
        }

        ByteBuf byteBuf = (ByteBuf) msg;
        System.out.println(">>> Server received a send ByteBuf");

        String gameId = ChannelRegistry.getGameIdByChannel(ctx.channel());
        if (gameId == null) {
            System.out.println(">>> No gameId found, send message for only this channel.");

            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(byteBuf.retain());
            System.out.println(">>> Server converted ByteBuf to BinaryWebSocketFrame");
            ctx.writeAndFlush(frame);
            System.out.println(">>> ====== Server sent BinaryWebSocketFrame to one client ======\n\n");
            return;
        }


        System.out.println(">>> Broadcasting BinaryWebSocketFrame to all channels in game: " + gameId);
        Set<Channel> channels = ChannelRegistry.getChannelsByGameId(gameId);
        for (Channel channel : channels) {
            if (channel.isActive()) {
                // cannot send 1 frame for many channels, because it will be released after writeAndFlush
                // must create a new frame for each channel
                BinaryWebSocketFrame frame = new BinaryWebSocketFrame(byteBuf.retainedDuplicate());
                System.out.println(">>> Server sending BinaryWebSocketFrame to channel: " + channel.id());
                channel.writeAndFlush(frame);
            } else {
                System.out.println(">>> Channel is inactive or is the same as the current channel, skipping.");
            }
        }
        System.out.println(">>> ====== Server broadcasted BinaryWebSocketFrame to the clients ======\n\n");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage()); 
        cause.printStackTrace();
        ctx.close(); // Close the channel on exception
    }
}
