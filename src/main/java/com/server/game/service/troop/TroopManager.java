package com.server.game.service.troop;

import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.TroopDB;
import com.server.game.resource.service.TroopService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.gameState.SlotStateService;
import com.server.game.service.move.MoveService;
import com.server.game.util.TroopEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.attack.HealthUpdateSend;
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
    private final TroopService troopService;
    private final SlotStateService slotStateService;
    private final MoveService moveService;

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
            slotState,
            moveService
        );

        gameState.spendGold(ownerSlot, troopDB.getCost());

        gameStateService.addEntityTo(gameState, troopInstance);
        slotStateService.addTroop(slotState, troopInstance);

        gameTroops.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
            .put(troopInstance.getStringId(), troopInstance);

        playerTroops.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(ownerSlot, k -> new ArrayList<>())
                   .add(troopInstance.getStringId());
        
        log.info("Created troop {} of type {} for player {} at position ({}, {}) for {} gold", 
                troopInstance.getStringId(), troopType, ownerSlot, 
                troopInstance.getCurrentPosition().x(), troopInstance.getCurrentPosition().y(), troopDB.getCost());
        
        return troopInstance;
    }
    
    /**
     * Remove a troop instance (when it dies or is manually removed)
     */
    public boolean removeTroop(String gameId, String troopInstanceId) {
        Map<String, TroopInstance2> troops = gameTroops.get(gameId);
        if (troops == null) {
            return false;
        }
        
        TroopInstance2 troop = troops.remove(troopInstanceId);
        if (troop == null) {
            return false;
        }
        
        // Remove from player troops
        Map<Short, List<String>> playerTroopMap = playerTroops.get(gameId);
        if (playerTroopMap != null) {
            List<String> playerTroopList = playerTroopMap.get(troop.getOwnerSlot().getSlot());
            if (playerTroopList != null) {
                playerTroopList.remove(troopInstanceId);
            }
        }
        
        log.info("Removed troop {} from game {}", troopInstanceId, gameId);
        return true;
    }
    
    /**
     * Get all troops in a game
     */
    public Collection<TroopInstance2> getGameTroops(String gameId) {
        Map<String, TroopInstance2> troops = gameTroops.get(gameId);
        return troops != null ? troops.values() : Collections.emptyList();
    }
    
    /**
     * Get troops owned by a specific player
     */
    public List<TroopInstance2> getPlayerTroops(String gameId, short ownerSlot) {
        Map<Short, List<String>> playerTroopMap = playerTroops.get(gameId);
        if (playerTroopMap == null) {
            return Collections.emptyList();
        }
        
        List<String> troopIds = playerTroopMap.get(ownerSlot);
        if (troopIds == null) {
            return Collections.emptyList();
        }
        
        Map<String, TroopInstance2> troops = gameTroops.get(gameId);
        if (troops == null) {
            return Collections.emptyList();
        }
        
        return troopIds.stream()
                .map(troops::get)
                .filter(Objects::nonNull)
                .filter(TroopInstance2::isAlive)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a specific troop instance
     */
    public TroopInstance2 getTroop(String gameId, String troopInstanceId) {
        Map<String, TroopInstance2> troops = gameTroops.get(gameId);
        return troops != null ? troops.get(troopInstanceId) : null;
    }
    
    /**
     * Get troops within range of a position
     */
    public List<TroopInstance2> getTroopsInRange(String gameId, Vector2 position, float range) {
        return getGameTroops(gameId).stream()
                .filter(TroopInstance2::isAlive)
                .filter(troop -> {
                    Vector2 troopPos = troop.getCurrentPosition();
                    if (troopPos == null || position == null) return false;
                    float dx = troopPos.x() - position.x();
                    float dy = troopPos.y() - position.y();
                    return Math.sqrt(dx * dx + dy * dy) <= range;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get enemy troops for a specific player
     */
    public List<TroopInstance2> getEnemyTroops(String gameId, short playerSlot) {
        return getGameTroops(gameId).stream()
                .filter(TroopInstance2::isAlive)
                .filter(troop -> troop.getOwnerSlot().getSlot() != playerSlot)
                .collect(Collectors.toList());
    }
    
    /**
     * Get friendly troops for a specific player
     */
    public List<TroopInstance2> getFriendlyTroops(String gameId, short playerSlot) {
        return getGameTroops(gameId).stream()
                .filter(TroopInstance2::isAlive)
                .filter(troop -> troop.getOwnerSlot().getSlot() == playerSlot)
                .collect(Collectors.toList());
    }
    
    /**
     * Find nearest enemy troop to a position
     */
    public TroopInstance2 findNearestEnemyTroop(String gameId, short playerSlot, Vector2 position, float maxRange) {
        return getEnemyTroops(gameId, playerSlot).stream()
                .filter(troop -> {
                    Vector2 troopPos = troop.getCurrentPosition();
                    if (troopPos == null || position == null) return false;
                    float dx = troopPos.x() - position.x();
                    float dy = troopPos.y() - position.y();
                    return Math.sqrt(dx * dx + dy * dy) <= maxRange;
                })
                .min(Comparator.comparing(troop -> {
                    Vector2 troopPos = troop.getCurrentPosition();
                    if (troopPos == null || position == null) return Float.MAX_VALUE;
                    float dx = troopPos.x() - position.x();
                    float dy = troopPos.y() - position.y();
                    return (float) Math.sqrt(dx * dx + dy * dy);
                }))
                .orElse(null);
    }
    
    /**
     * Find friendly troops needing healing
     */
    public List<TroopInstance2> findTroopsNeedingHealing(String gameId, short playerSlot, 
            Vector2 healerPosition, float healRange, float healthThreshold) {
        return getFriendlyTroops(gameId, playerSlot).stream()
                .filter(troop -> {
                    Vector2 troopPos = troop.getCurrentPosition();
                    if (troopPos == null || healerPosition == null) return false;
                    float dx = troopPos.x() - healerPosition.x();
                    float dy = troopPos.y() - healerPosition.y();
                    return Math.sqrt(dx * dx + dy * dy) <= healRange;
                })
                .filter(troop -> {
                    float healthPercentage = (float) troop.getCurrentHP() / troop.getMaxHP();
                    return healthPercentage < healthThreshold;
                })
                .sorted(Comparator.comparing(troop -> (float) troop.getCurrentHP() / troop.getMaxHP()))
                .collect(Collectors.toList());
    }

    
    /**
     * Apply damage to a troop and handle death
     */
    public boolean applyDamageToTroop(String gameId, String troopInstanceId, int damage) {
        TroopInstance2 troop = getTroop(gameId, troopInstanceId);
        if (troop == null || !troop.isAlive()) {
            return false;
        }
        // TODO: Implement proper damage application logic
        log.info("Applying {} damage to troop {}", damage, troopInstanceId);
        return true;
    }

    /**
     * Broadcast a troop death event
     */
    private void broadcastTroopDeath(String gameId, String troopInstanceId, short ownerSlot) {
        Set<Channel> gameChannels = ChannelManager.getChannelsByGameId(gameId);
        if (gameChannels == null || gameChannels.isEmpty()) {
            log.warn("No active channels found for game ID: {}", gameId);
            return;
        }

        TroopDeathSend deathMessage = new TroopDeathSend(troopInstanceId, ownerSlot);
        for (Channel channel : gameChannels) {
            if (channel.isActive()) {
                channel.writeAndFlush(deathMessage);
            }
        }
    }

    /**
     * Heal a troop
     */
    public boolean healTroop(String gameId, String troopInstanceId, int healAmount) {
        TroopInstance2 troop = getTroop(gameId, troopInstanceId);
        if (troop == null || !troop.isAlive()) {
            return false;
        }
        
        // TODO: Implement proper healing through health component
        // For now, this is a placeholder
        log.info("Healing troop {} by {} HP", troopInstanceId, healAmount);
        return true;
    }

    /**
     * Get troop count for a player
     */
    public int getPlayerTroopCount(String gameId, short playerSlot) {
        return getPlayerTroops(gameId, playerSlot).size();
    }
    
    /**
     * Clean up all troops for a game
     */
    public void cleanupGameTroops(String gameId) {
        Map<String, TroopInstance2> troops = gameTroops.remove(gameId);
        playerTroops.remove(gameId);
        
        if (troops != null) {
            log.info("Cleaned up {} troops for game {}", troops.size(), gameId);
        }
    }
    
    /**
     * Get troop statistics for a game
     */
    public String getTroopStatistics(String gameId) {
        Collection<TroopInstance2> troops = getGameTroops(gameId);
        if (troops.isEmpty()) {
            return "No troops in game " + gameId;
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Troop Statistics for ").append(gameId).append(":\n");
        
        // Group by player
        Map<Short, List<TroopInstance2>> playerTroopGroups = troops.stream()
                .collect(Collectors.groupingBy(troop -> troop.getOwnerSlot().getSlot()));
        
        for (Map.Entry<Short, List<TroopInstance2>> entry : playerTroopGroups.entrySet()) {
            Short playerSlot = entry.getKey();
            List<TroopInstance2> playerTroopList = entry.getValue();
            
            stats.append(String.format("  Player %d: %d troops%n", playerSlot, playerTroopList.size()));
            
            // Group by troop type
            Map<TroopEnum, Long> typeCount = playerTroopList.stream()
                    .collect(Collectors.groupingBy(TroopInstance2::getTroopEnum, Collectors.counting()));
            
            for (Map.Entry<TroopEnum, Long> typeEntry : typeCount.entrySet()) {
                stats.append(String.format("    %s: %d%n", typeEntry.getKey(), typeEntry.getValue()));
            }
        }
        
        return stats.toString();
    }
}
