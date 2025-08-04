package com.server.game.service.troop;

import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.TroopDB;
import com.server.game.resource.service.TroopService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.TroopEnum;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TroopManager {
    private GameStateService gameStateService;
    private TroopService troopService;

    // gameId -> Map<troopInstanceId, TroopInstance>
    private final Map<String, Map<String, TroopInstance2>> gameTroops = new ConcurrentHashMap<>();

    // gameId -> Map<ownerSlot, List<troopInstanceId>>
    private final Map<String, Map<Short, List<String>>> playerTroops = new ConcurrentHashMap<>();
    
    /**
     * Create a new troop instance for a player
     */
    public TroopInstance2 createTroop(String gameId, short ownerSlot, TroopEnum troopType, Vector2 spawnPosition) {
        GameState gameState = gameStateService.getGameStateById(gameId);
        if (gameState == null) {
            log.error("Game state not found for gameId: {}", gameId);
            return null;
        }

        SlotState slotState = gameState.getSlotState(ownerSlot);
        if (slotState == null) {
            log.warn("Slot state not found for ownerSlot: {}", ownerSlot);
            return null;
        }

        TroopDB troopDB = troopService.getTroopDBById(troopType);
        if (troopDB == null) {
            log.error("TroopDB not found for troopType: {}", troopType);
            return null;
        }

        if (gameState.peekGold(ownerSlot) < troopDB.getCost()) {
            log.warn("Player {} cannot afford troop of type {} in game {}", ownerSlot, troopType, gameId);
            return null;
        }

        TroopInstance2 troopInstance = new TroopInstance2(
            troopDB,
            gameState,
            slotState
        );

        gameState.spendGold(ownerSlot, troopDB.getCost());

        gameStateService.addEntityTo(gameState, troopInstance);

        gameTroops.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
            .put(troopInstance.getStringId(), troopInstance);

        playerTroops.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(ownerSlot, k -> new ArrayList<>())
            .add(troopInstance.getStringId());

        log.info("Troop {} created for player {} in game {}", troopType, ownerSlot, gameId);
        return troopInstance;
    }
}
