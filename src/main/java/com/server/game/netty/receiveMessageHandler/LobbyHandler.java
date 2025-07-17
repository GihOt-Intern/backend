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
import lombok.AllArgsConstructor;


@Component
@AllArgsConstructor
public class LobbyHandler {

    private final MapHandler mapHandler;


    @MessageMapping(ChooseChampionReceive.class)
    public ChooseChampionSend handleChooseChampion(ChooseChampionReceive receiveObject, Channel channel) {
        ChampionEnum championId = receiveObject.getChampionEnum();
        short slot = ChannelManager.getSlotByChannel(channel);
        ChannelManager.setChampionId2Channel(championId, channel);
        System.out.println("Slot " + slot + " chose Champion ID: " + championId);
        return new ChooseChampionSend(slot, championId);
    }

    @MessageMapping(PlayerReadyReceive.class)
    public void handlePlayerReady(PlayerReadyReceive receiveObject, Channel channel) {
        System.out.println(">>> [Log in LobbyHandler.handlePlayerReady] Channel ID: " + channel.id());
        ChannelManager.setUserReady(channel);
        String gameId = ChannelManager.getGameIdByChannel(channel);
        Set<Channel> playersInRoom = ChannelManager.getChannelsByGameId(gameId);
        System.out.println(">>> [Log in LobbyHandler.handlePlayerReady] Room has " + playersInRoom.size() + " players.");
        boolean isAllPlayersReady = playersInRoom.stream() // fun sân nồ prồ ram minh
            .allMatch(ChannelManager::isUserReady);
        System.out.println(">>> [Log in LobbyHandler.handlePlayerReady] Is all players ready? " + isAllPlayersReady);
        PlayerReadySend playerReadySend = new PlayerReadySend(
            ChannelManager.getSlotByChannel(channel),
            isAllPlayersReady
        );

        // Send PlayerReadySend to all players in the room first,
        // then proceed to map loading if all players are ready
        channel.writeAndFlush(playerReadySend).addListener(future -> {
            if (future.isSuccess()) {
                if (isAllPlayersReady) {
                    System.out.println(">>> [Log in LobbyHandler.handlePlayerReady] All players are ready. Proceeding to map loading.");
                    mapHandler.handleInitialGameStateLoading(channel);
                } else {
                    System.out.println(">>> [Log in LobbyHandler.handlePlayerReady] Not all players are ready yet.");
                }
            }
        });
    }
}
