package com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class UnicastTarget implements SendTarget {
    private final Channel channel;

    public UnicastTarget(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void send(ByteBuf message) {
        System.out.println(">>> Sending Unicast to channel: " + channel.id());

        if (channel.isActive()) {
            channel.writeAndFlush(message.retain());
        }
    }
}