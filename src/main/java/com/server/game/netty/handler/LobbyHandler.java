package com.server.game.netty.handler;


import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.receiveObject.ChooseChampionReceive;
import com.server.game.netty.messageObject.receiveObject.LobbyLoadedReceive;
import com.server.game.netty.messageObject.receiveObject.PlayerReadyReceive;
import com.server.game.netty.messageObject.sendObject.ChooseChampionSend;
import com.server.game.netty.messageObject.sendObject.CurrentLobbyStateSend;
import com.server.game.netty.messageObject.sendObject.PlayerReadySend;
import com.server.game.netty.receiveObject.ChooseChampionReceive;
import com.server.game.netty.receiveObject.PlayerReadyReceive;
import com.server.game.netty.sendObject.PlayerReadySend;
import com.server.game.netty.sendObject.lobby.ChooseChampionSend;
import com.server.game.util.ChampionEnum;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;


@Component
@AllArgsConstructor
public class LobbyHandler {

    private final GameInititalLoadingHandler gameLoadingHandler;


    @MessageMapping(LobbyLoadedReceive.class)
    public CurrentLobbyStateSend handleLobbyLoaded(LobbyLoadedReceive receiveObject, Channel channel) {
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        List<CurrentLobbyStateSend.PlayerLobbyStateData> lobbyPlayerStates = 
            playersInRoom.stream().map(ch -> {
                short slot = ChannelManager.getSlotByChannel(ch);
                ChampionEnum championId = ChannelManager.getChampionIdByChannel(ch);
                boolean isReady = ChannelManager.isUserReady(ch);
                return new CurrentLobbyStateSend.PlayerLobbyStateData(
                    slot,
                    championId != null ? championId.getChampionId() : -1, // -1 if not chosen
                    isReady
                );
            })
            .toList();


        return new CurrentLobbyStateSend(lobbyPlayerStates);
    }

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
                    gameLoadingHandler.loadInitial(channel);
                } else {
                    System.out.println(">>> [Log in LobbyHandler.handlePlayerReady] Not all players are ready yet.");
                }
            }
        });
    }
}
