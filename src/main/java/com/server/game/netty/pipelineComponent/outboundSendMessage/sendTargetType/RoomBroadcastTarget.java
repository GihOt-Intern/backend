package com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType;

import java.util.Set;

import com.server.game.netty.ChannelRegistry;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTargetInterface;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class RoomBroadcastTarget implements SendTargetInterface {
    private final String gameId;

    public RoomBroadcastTarget(Channel channel) {
        this.gameId = ChannelRegistry.getGameIdByChannel(channel);
        if (this.gameId == null) {
            System.out.println(">>> Error: Channel does not belong to any game.");
        }
    }

    @Override
    public void send(ByteBuf payload) {
        System.out.println(">>> Sending Room Broadcast for gameId: " + gameId);


        Set<Channel> channels = ChannelRegistry.getChannelsByGameId(gameId);
        if (channels == null || channels.isEmpty()) {
            System.out.println(">>> No active channels found for gameId: " + gameId);
            return;
        }

        for (Channel channel : channels) {
            if (channel.isActive()) {
                channel.writeAndFlush(new BinaryWebSocketFrame(payload.retainedDuplicate()));
                System.out.println(">>> Server sent BinaryWebSocketFrame to user: " + ChannelRegistry.getUserIdByChannel(channel) +
                        " in game: " + gameId);
            }
        }
    }
}