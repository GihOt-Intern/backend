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
        Set<Channel> channels = ChannelManager.getChannelsByGameId(gameId);
        System.out.println(">>> [AMatchBroadcastTarget] Broadcasting to gameId: " + gameId);
        System.out.println(">>> [AMatchBroadcastTarget] Found " + (channels != null ? channels.size() : 0) + " channels");
        
        if (channels == null || channels.isEmpty()) {
            System.out.println(">>> [AMatchBroadcastTarget] No active channels found for gameId: " + gameId);
            return new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE).setSuccess();
        }

        ChannelFuture lastFuture = null;
        int sentCount = 0;
        for (Channel channel : channels) {
            if (channel.isActive()) {
                lastFuture = channel.writeAndFlush(message.retainedDuplicate());
                sentCount++;
                System.out.println(">>> [AMatchBroadcastTarget] Sent message to channel " + sentCount);
            } else {
                System.out.println(">>> [AMatchBroadcastTarget] Skipped inactive channel");
            }
        }
        
        System.out.println(">>> [AMatchBroadcastTarget] Total messages sent: " + sentCount);

        return lastFuture != null 
            ? lastFuture 
            : new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE).setSuccess();
    }
}