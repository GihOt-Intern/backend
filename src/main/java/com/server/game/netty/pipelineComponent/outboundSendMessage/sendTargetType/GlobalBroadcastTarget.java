package com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType;

import java.util.Set;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;

public class GlobalBroadcastTarget implements SendTarget {

    @Override
    public ChannelFuture send(ByteBuf message) {
        System.out.println(">>> Sending Global Broadcast...");


        Set<Channel> channels = ChannelManager.getAllChannels();
        if (channels == null || channels.isEmpty()) {
            System.out.println(">>> No active channels found.");
            return new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE).setSuccess();
        }

        ChannelFuture lastFuture = null;
        for (Channel channel : channels) {
            if (channel.isActive()) {
                lastFuture = channel.writeAndFlush(message.retainedDuplicate());
                System.out.println(">>> Server sent BinaryWebSocketFrame to user: " + ChannelManager.getUserIdByChannel(channel));
            }
        }

        return lastFuture != null
            ? lastFuture
            : new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE).setSuccess();
    }
}