package com.server.game.netty.messageHandler;


import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.receiveObject.ChooseChampionReceive;
import com.server.game.netty.receiveObject.LobbyLoadedReceive;
import com.server.game.netty.receiveObject.PlayerReadyReceive;
import com.server.game.netty.sendObject.lobby.ChooseChampionSend;
import com.server.game.netty.sendObject.lobby.CurrentLobbyStateSend;
import com.server.game.netty.sendObject.lobby.PlayerReadySend;
import com.server.game.util.ChampionEnum;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class LobbyMessageHandler {

    private final GameInititalLoadingMessageHandler gameLoadingHandler;


    @MessageMapping(LobbyLoadedReceive.class)
    public CurrentLobbyStateSend handleLobbyLoaded(LobbyLoadedReceive receiveObject, Channel channel) {
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        List<CurrentLobbyStateSend.PlayerLobbyStateData> lobbyPlayerStates = 
            playersInRoom.stream().map(ch -> {
                short slot = ChannelManager.getSlotByChannel(ch);
                ChampionEnum championEnum = ChannelManager.getChampionEnumByChannel(ch);
                boolean isReady = ChannelManager.isUserReady(ch);
                return new CurrentLobbyStateSend.PlayerLobbyStateData(
                    slot,
                    championEnum != null ? championEnum.getChampionId() : -1, // -1 if not chosen
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
        log.info("Slot " + slot + " chose Champion ID: " + championId);
        return new ChooseChampionSend(slot, championId);
    }

    @MessageMapping(PlayerReadyReceive.class)
    public void handlePlayerReady(PlayerReadyReceive receiveObject, Channel channel) {
        log.info("Channel ID: " + channel.id());
        ChannelManager.setUserReady(channel);
        String gameId = ChannelManager.getGameIdByChannel(channel);
        Set<Channel> playersInRoom = ChannelManager.getChannelsByGameId(gameId);
        log.info("Room has " + playersInRoom.size() + " players.");
        boolean isAllPlayersReady = playersInRoom.stream() // fun sân nồ prồ ram minh
            .allMatch(ChannelManager::isUserReady);
        log.info("Is all players ready? " + isAllPlayersReady);
        PlayerReadySend playerReadySend = new PlayerReadySend(
            ChannelManager.getSlotByChannel(channel),
            isAllPlayersReady
        );

        // Send PlayerReadySend to all players in the room first,
        // then proceed to map loading if all players are ready
        channel.writeAndFlush(playerReadySend).addListener(future -> {
            if (future.isSuccess()) {
                if (isAllPlayersReady) {
                    log.info("All players are ready. Proceeding to map loading.");
                    gameLoadingHandler.loadInitial(channel);
                } else {
                    log.info("Not all players are ready yet.");
                }
            }
        });
    }
}
