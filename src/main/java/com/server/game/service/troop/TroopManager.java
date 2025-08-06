package com.server.game.service.troop;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.TroopDB;
import com.server.game.resource.service.TroopService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.service.move.MoveService2;
import com.server.game.util.TroopEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.troop.TroopDeathSend;

import io.netty.channel.Channel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TroopManager {
    private final GameStateService gameStateService;
    private final MoveService2 moveService;
    
    /**
     * Remove a troop instance (when it dies or is manually removed)
     */
    public boolean removeTroop(String gameId, String troopInstanceId) {
        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            log.warn("Game state not found for game ID: {}", gameId);
            return false;
        }
        Entity troopInstance = gameStateService.getEntityByStringId(gameId, troopInstanceId);
        gameStateService.removeEntity(gameState, troopInstance);
        return true;
    }
}
