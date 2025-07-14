package com.server.game.netty.receiveMessageHandler;


import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.receiveObject.ChooseChampionReceive;
import com.server.game.netty.messageObject.receiveObject.PlayerReadyReceive;
import com.server.game.netty.messageObject.sendObject.ChooseChampionSend;
import com.server.game.netty.messageObject.sendObject.PlayerReadySend;

import io.netty.channel.Channel;


@Component
public class LobbyHandler {


    @MessageMapping(ChooseChampionReceive.class)
    public ChooseChampionSend handleChooseChampion(ChooseChampionReceive receiveObject, Channel channel) {
        int championId = receiveObject.getChampionEnum().getChampionId();
        short slot = ChannelManager.getSlotByChannel(channel);
        System.out.println("Chosen Champion ID: " + championId);
        return new ChooseChampionSend(slot, championId);
    }

    @MessageMapping(PlayerReadyReceive.class)
    public PlayerReadySend handlePlayerReady(PlayerReadyReceive receiveObject, Channel channel) {
        ChannelManager.setUserReady(channel);
        String roomId = ChannelManager.getGameIdByChannel(channel);
        Set<Channel> playersInRoom = ChannelManager.getChannelsByRoomId(roomId);
        boolean isAllPlayersReady = playersInRoom.stream() // fun sân nồ prồ ram minh
            .allMatch(ChannelManager::isUserReady);
        PlayerReadySend playerReadySend = new PlayerReadySend(
            ChannelManager.getSlotByChannel(channel),
            isAllPlayersReady
        );
        return playerReadySend;
    }
}
