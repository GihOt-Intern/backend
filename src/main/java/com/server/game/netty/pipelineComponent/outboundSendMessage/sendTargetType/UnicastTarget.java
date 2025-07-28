package com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;

public class UnicastTarget implements SendTarget {
    private final Channel channel;

    public UnicastTarget(Channel channel) {
        this.channel = channel;
    }

    @Override
    public ChannelFuture send(ByteBuf message) {

        if (channel.isActive()) {
            return channel.writeAndFlush(message.retain());
        }

        return new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE).setSuccess();
    }
}