package com.server.game.netty.messageHandler;

import org.springframework.stereotype.Component;

import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.GoldAmountSend;
import com.server.game.netty.sendObject.GoldMineSpawnSend;
import com.server.game.netty.sendObject.playground.IsInPlaygroundSend;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PlaygroundMessageHandler {

    public void sendInPlaygroundUpdateMessage(String gameId, Short slot, boolean isInPlayground) {
        Channel channel = ChannelManager.getChannelByGameIdAndSlot(gameId, slot);
        if (channel != null) {
            IsInPlaygroundSend isInPlaygroundSend = new IsInPlaygroundSend(isInPlayground);
            channel.writeAndFlush(isInPlaygroundSend);
            log.info("Sending isInPlayground update message for gameId: {}, slot: {}, isInPlayground: {}", gameId, slot, isInPlayground);
        }
    }

    public void sendGoldChangeMessage(String gameId, Short slot, Integer currentGold) {
        Channel channel = ChannelManager.getChannelByGameIdAndSlot(gameId, slot);
        if (channel != null) {
            GoldAmountSend goldAmountSend = new GoldAmountSend(currentGold);
            channel.writeAndFlush(goldAmountSend);
            // log.info("Sending gold change message for gameId: {}, slot: {}, currentGold: {}", gameId, slot, currentGold);
        }
    }

    public void sendGoldMineSpawnMessage(String gameId, String goldMineId, boolean isSmallGoldMine, Vector2 position, int initHP) {
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null) {
            GoldMineSpawnSend goldMineSpawnSend = new GoldMineSpawnSend(goldMineId, position, isSmallGoldMine, initHP);
            channel.writeAndFlush(goldMineSpawnSend);
            log.info("Sending gold mine spawn message for gameId: {}, goldMine: {}", gameId, goldMineSpawnSend);
        }
    }
} 