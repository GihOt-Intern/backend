package com.server.game.netty.receiveMessageHandler;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.ChampionInitialPositionsSend.ChampionInitialPositionData;
import com.server.game.netty.messageObject.sendObject.ChampionInitialPositionsSend;
import com.server.game.resource.service.GameMapService;
import com.server.game.util.ChampionEnum;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MapHandler {

    GameMapService gameMapService;

    // This method is called by LobbyHandler when all players are ready
    public void handleInitialGameStateLoading(Channel channel) {
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        Short numPlayers = (short) playersInRoom.size();

        // GameMap id is determined by the number of players
        Short gameMapId = numPlayers;

        Map<Short, ChampionEnum> slot2ChampionId = new HashMap<>();
        playersInRoom.forEach(
            playerChannel -> slot2ChampionId.putAll(
                ChannelManager.getSlot2ChampionIdByChannel(playerChannel))
        );

        List<ChampionInitialPositionData> championPositionsData = 
            gameMapService.getChampionPositionsData(gameMapId, slot2ChampionId);

        ChampionInitialPositionsSend championPositionsSend = 
            new ChampionInitialPositionsSend(gameMapId, championPositionsData);
        System.out.println(">>> Send loading map messsage");
        channel.writeAndFlush(championPositionsSend);        
    }

}
