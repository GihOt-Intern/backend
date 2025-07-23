package com.server.game.netty.receiveMessageHandler;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.InitialPositionsSend.InitialPositionData;
import com.server.game.resource.service.ChampionService;
import com.server.game.service.GameCoordinator;
import com.server.game.service.GameStateService;
import com.server.game.service.gameState.GameStateManager;
import com.server.game.util.ChampionEnum;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;


@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameHandler {

    MapHandler mapHandler;
    GameCoordinator gameCoordinator;
    GameStateService gameStateService;
    GameStateManager gameStateManager;
    ChampionService championService;
    
    public void handleGameStart(Channel channel) {
        
        List<InitialPositionData> initialPositionDatas = mapHandler.handleInitialGameStateLoading(channel);

        String gameId = ChannelManager.getGameIdByChannel(channel);
        
        // Get slot to champion mapping
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);
        if (slot2ChampionId == null || slot2ChampionId.isEmpty()) {
            System.err.println(">>> No slot to champion mapping found for gameId: " + gameId);
            return;
        }
        
        // Build champion to initial HP mapping
        Map<ChampionEnum, Integer> championInitialHPMap = new HashMap<>();
        for (ChampionEnum championId : slot2ChampionId.values()) {
            Integer initialHP = championService.getInitialHP(championId);
            if (initialHP != null) {
                championInitialHPMap.put(championId, initialHP);
            } else {
                System.err.println(">>> Failed to get initial HP for champion: " + championId);
                return;
            }
        }
        
        // Initialize comprehensive game state using GameStateManager
        boolean initSuccess = gameStateManager.initializeGame(gameId, slot2ChampionId, championInitialHPMap);
        if (!initSuccess) {
            System.err.println(">>> Failed to initialize comprehensive game state for gameId: " + gameId);
            return;
        }
        
        // Log successful initialization
        System.out.println(">>> Successfully initialized comprehensive game state for gameId: " + gameId);
        for (Map.Entry<Short, ChampionEnum> entry : slot2ChampionId.entrySet()) {
            Short slot = entry.getKey();
            ChampionEnum championId = entry.getValue();
            Integer initialHP = championInitialHPMap.get(championId);
            System.out.println(">>> Player slot " + slot + " with champion " + championId + 
                " initialized with " + initialHP + " HP");
        }
        
        // Register game with coordinator
        gameCoordinator.registerGame(gameId);

        // Initialize positions
        for(InitialPositionData initialPositionsData : initialPositionDatas) {
            gameCoordinator.updatePosition(
                gameId, 
                initialPositionsData.getSlot(), 
                initialPositionsData.getPosition(),
                System.currentTimeMillis()
            );
        }
        
        // Print game state summary
        System.out.println(">>> Game state summary:\n" + gameStateService.getGameStatistics(gameId));
    }
}
