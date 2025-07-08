package com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTargetInterface;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class UnicastTarget implements SendTargetInterface {
    private final Channel channel;

    public UnicastTarget(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void send(ByteBuf payload) {
        System.out.println(">>> Sending Unicast to channel: " + channel.id());

        if (channel.isActive()) {
            channel.writeAndFlush(new BinaryWebSocketFrame(payload.retainedDuplicate()));
        }
    }
}