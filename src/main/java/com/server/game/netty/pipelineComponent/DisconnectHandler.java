package com.server.game.netty.pipelineComponent;


import com.server.game.netty.ChannelManager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class DisconnectHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();

        // Clean up slot mapping before unregistering if they exist
        String gameId = ChannelManager.getGameIdByChannel(channel);
        Short slot = ChannelManager.getSlotByChannel(channel);
        
        if (gameId != null && slot != null) {
            ChannelManager.removeSlotMapping(gameId, slot);
        }

        ChannelManager.unregister(channel);
        // Gui so 4 o day
        super.channelInactive(ctx);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(">>> Server Exception: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
