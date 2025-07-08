package com.server.game.netty.pipelineComponent;


import com.server.game.netty.ChannelRegistry;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class DisconnectHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();

        ChannelRegistry.unregister(channel);
        super.channelInactive(ctx);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
