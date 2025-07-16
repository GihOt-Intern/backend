package com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType;

import java.util.Set;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;


public class AMatchBroadcastTarget implements SendTarget {
    private final String gameId;

    public AMatchBroadcastTarget(Channel channel) {
        this.gameId = ChannelManager.getGameIdByChannel(channel);
        if (this.gameId == null) {
            System.out.println(">>> Error: Channel does not belong to any game.");
        }
    }

    @Override
    public ChannelFuture send(ByteBuf message) {
        System.out.println(">>> Sending Room Broadcast for gameId: " + gameId);


        Set<Channel> channels = ChannelManager.getChannelsByGameId(gameId);
        if (channels == null || channels.isEmpty()) {
            System.out.println(">>> No active channels found for gameId: " + gameId);
            return new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE).setSuccess();
        }

        ChannelFuture lastFuture = null;
        for (Channel channel : channels) {
            if (channel.isActive()) {
                lastFuture = channel.writeAndFlush(message.retainedDuplicate());
                System.out.println(">>> Server sent message to user: " + ChannelManager.getUserIdByChannel(channel) +
                        " in game: " + gameId);
            }
        }

        return lastFuture != null 
            ? lastFuture 
            : new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE).setSuccess();
    }
}