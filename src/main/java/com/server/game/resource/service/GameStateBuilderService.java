package com.server.game.resource.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.server.game.resource.model.Champion;
import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.util.ChampionEnum;

import io.netty.channel.Channel;

import com.server.game.model.GameState;
import com.server.game.netty.ChannelManager;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class GameStateBuilderService {


    GameMapService gameMapService;
    GameMapGridService gameMapGridService;
    ChampionService championService;

    public GameState getGameState(Channel channel) {
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        Short numPlayers = (short) playersInRoom.size();

        Short gameMapId = numPlayers; // GameMap id is determined by the number of players

        String gameId = ChannelManager.getGameIdByChannel(channel);
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);

        Map<Short, Champion> slot2Champion = new HashMap<>();
        for (Map.Entry<Short, ChampionEnum> entry : slot2ChampionId.entrySet()) {
            Short slot = entry.getKey();
            ChampionEnum championId = entry.getValue();
            Champion champion = championService.getChampionById(championId);
            if (champion != null) {
                slot2Champion.put(slot, champion);
            } else {
                System.out.println(">>> [Log in MapHandler.handleInitialGameStateLoading] Champion with ID " + championId + " not found.");
            }
        }


        GameMap gameMap = gameMapService.getGameMapById(gameMapId);
        GameMapGrid gameMapGrid = gameMapGridService.getGameMapGridById(gameMapId);

        GameState gameState = new GameState(gameMap, gameMapGrid, slot2Champion);

        return gameState;
    }

}
