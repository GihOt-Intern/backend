package com.server.game.netty.receiveMessageHandler;


import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.receiveObject.ChooseChampionReceive;
import com.server.game.netty.messageObject.receiveObject.PlayerReadyReceive;
import com.server.game.netty.messageObject.sendObject.ChooseChampionSend;
import com.server.game.netty.messageObject.sendObject.PlayerReadySend;
import com.server.game.util.ChampionEnum;

import io.netty.channel.Channel;


@Component
public class LobbyHandler {


    @MessageMapping(ChooseChampionReceive.class)
    public ChooseChampionSend handleChooseChampion(ChooseChampionReceive receiveObject, Channel channel) {
        ChampionEnum championId = receiveObject.getChampionEnum();
        short slot = ChannelManager.getSlotByChannel(channel);
        ChannelManager.setChampionId2Channel(championId.getChampionId(), channel);
        System.out.println("Slot " + slot + " chose Champion ID: " + championId);
        return new ChooseChampionSend(slot, championId);
    }

    @MessageMapping(PlayerReadyReceive.class)
    public PlayerReadySend handlePlayerReady(PlayerReadyReceive receiveObject, Channel channel) {
        ChannelManager.setUserReady(channel);
        String gameId = ChannelManager.getGameIdByChannel(channel);
        Set<Channel> playersInRoom = ChannelManager.getChannelsByGameId(gameId);
        boolean isAllPlayersReady = playersInRoom.stream() // fun sân nồ prồ ram minh
            .allMatch(ChannelManager::isUserReady);
        PlayerReadySend playerReadySend = new PlayerReadySend(
            ChannelManager.getSlotByChannel(channel),
            isAllPlayersReady
        );
        return playerReadySend;
    }
}
