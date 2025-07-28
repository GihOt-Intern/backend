package com.server.game.netty.receiveMessageHandler;

import org.springframework.stereotype.Component;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.GoldAmountSend;
import com.server.game.netty.messageObject.sendObject.IsInPlayGroundSend;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PlayGroundHandler {

    public void sendInPlayGroundUpdateMessage(String gameId, short slot, boolean isInPlayGround) {
        Channel channel = ChannelManager.getChannelByGameIdAndSlot(gameId, slot);
        if (channel != null) {
            IsInPlayGroundSend isInPlayGroundSend = new IsInPlayGroundSend(isInPlayGround);
            channel.writeAndFlush(isInPlayGroundSend);        
        }
    }

    public void sendGoldChangeMessage(String gameId, Short slot, Integer currentGold) {
        Channel channel = ChannelManager.getChannelByGameIdAndSlot(gameId, slot);
        if (channel != null) {
            GoldAmountSend goldAmountSend = new GoldAmountSend(currentGold);
            channel.writeAndFlush(goldAmountSend);
        }
    }
} 