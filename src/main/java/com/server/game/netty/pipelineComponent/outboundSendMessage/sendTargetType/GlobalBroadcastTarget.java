package com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType;

import java.util.Set;

import com.server.game.netty.ChannelRegistry;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTargetInterface;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class GlobalBroadcastTarget implements SendTargetInterface {

    @Override
    public void send(ByteBuf payload) {
        System.out.println(">>> Sending Global Broadcast...");


        Set<Channel> channels = ChannelRegistry.getAllChannels();
        if (channels == null || channels.isEmpty()) {
            System.out.println(">>> No active channels found.");
            return;
        }

        for (Channel channel : channels) {
            if (channel.isActive()) {
                channel.writeAndFlush(new BinaryWebSocketFrame(payload.retainedDuplicate()));
                System.out.println(">>> Server sent BinaryWebSocketFrame to user: " + ChannelRegistry.getUserIdByChannel(channel));
            }
        }
    }
}