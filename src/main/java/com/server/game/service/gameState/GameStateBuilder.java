package com.server.game.service.gameState;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.service.GameMapGridService;
import com.server.game.resource.service.GameMapService;
import com.server.game.service.champion.ChampionService;
import com.server.game.util.ChampionEnum;

import io.netty.channel.Channel;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.netty.ChannelManager;

import lombok.AccessLevel;


@Data
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameStateBuilder {


    GameMapService gameMapService;
    GameMapGridService gameMapGridService;
    ChampionService championService;

    // From the channel, get all components needed to build the game state
    public GameState build(Channel channel) {
        Set<Channel> playersInRoom = ChannelManager.getGameChannelsByInnerChannel(channel);
        Short numPlayers = (short) playersInRoom.size();

        Short gameMapId = numPlayers; // GameMap id is determined by the number of players

        String gameId = ChannelManager.getGameIdByChannel(channel);
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);

        Map<Short, Champion> slot2Champion = this.getSlot2Champion(slot2ChampionId);

        GameMap gameMap = gameMapService.getGameMapById(gameMapId);
        GameMapGrid gameMapGrid = gameMapGridService.getGameMapGridById(gameMapId);

        GameState gameState = new GameState(gameId, gameMap, gameMapGrid, slot2Champion);


        return gameState;
    }


    public GameState build(String gameId, Map<Short, ChampionEnum> slot2ChampionId) {
        
        Short gameMapId = (short) slot2ChampionId.size(); // GameMap id is determined by the number of players
        
        Map<Short, Champion> slot2Champion = this.getSlot2Champion(slot2ChampionId);

        GameMap gameMap = gameMapService.getGameMapById(gameMapId);
        GameMapGrid gameMapGrid = gameMapGridService.getGameMapGridById(gameMapId);

        GameState gameState = new GameState(gameId, gameMap, gameMapGrid, slot2Champion);

        return gameState;
    }


    private Map<Short, Champion> getSlot2Champion(Map<Short, ChampionEnum> slot2ChampionId) {
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
        return slot2Champion;
    }
}
