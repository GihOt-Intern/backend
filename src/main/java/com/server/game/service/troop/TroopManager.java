package com.server.game.service.troop;

import com.server.game.map.component.Vector2;
import com.server.game.model.gameState.SlotState;
import com.server.game.resource.service.TroopService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.service.troop.TroopInstance.TroopAIState;
import com.server.game.util.TroopEnum;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TroopManager {
    
    TroopService troopService;
    GameStateService gameStateService;
    
    // gameId -> Map<troopInstanceId, TroopInstance>
    private final Map<String, Map<String, TroopInstance>> gameTroops = new ConcurrentHashMap<>();
    
    // gameId -> Map<ownerSlot, List<troopInstanceId>>
    private final Map<String, Map<Short, List<String>>> playerTroops = new ConcurrentHashMap<>();
    
    /**
     * Create a new troop instance for a player
     */
    public TroopInstance createTroop(String gameId, short ownerSlot, TroopEnum troopType, Vector2 spawnPosition) {
        // Check if player can afford the troop
        SlotState slotState = gameStateService.getGameStateById(gameId).getSlotState(ownerSlot);

        if (slotState == null) {
            log.warn("Cannot create troop: Player state not found for slot {} in game {}", ownerSlot, gameId);
            return null;
        }
        
        int troopCost = troopService.getTroopCost(troopType);
        if (slotState.getCurrentGold() < troopCost) {
            log.warn("Player {} cannot afford troop {} (cost: {}, gold: {})", 
                    ownerSlot, troopType, troopCost, slotState.getCurrentGold());
            return null;
        }
        
        // Get troop stats
        Integer maxHP = troopService.getTroopInitialHP(troopType);
        if (maxHP == null) {
            log.error("Failed to get initial HP for troop type: {}", troopType);
            return null;
        }
        
        // Create troop instance
        TroopInstance troop = new TroopInstance(gameId, troopType, ownerSlot, spawnPosition, maxHP);
        
        // Deduct cost from player
        slotState.spendGold(troopCost);
        
        // Add to game troops
        gameTroops.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                  .put(troop.getTroopInstanceId(), troop);
        
        // Add to player troops
        playerTroops.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(ownerSlot, k -> new ArrayList<>())
                   .add(troop.getTroopInstanceId());
        
        log.info("Created troop {} of type {} for player {} at position ({}, {}) for {} gold", 
                troop.getTroopInstanceId(), troopType, ownerSlot, 
                spawnPosition.x(), spawnPosition.y(), troopCost);
        
        return troop;
    }
    
    /**
     * Remove a troop instance (when it dies or is manually removed)
     */
    public boolean removeTroop(String gameId, String troopInstanceId) {
        Map<String, TroopInstance> troops = gameTroops.get(gameId);
        if (troops == null) {
            return false;
        }
        
        TroopInstance troop = troops.remove(troopInstanceId);
        if (troop == null) {
            return false;
        }
        
        // Remove from player troops
        Map<Short, List<String>> playerTroopMap = playerTroops.get(gameId);
        if (playerTroopMap != null) {
            List<String> playerTroopList = playerTroopMap.get(troop.getOwnerSlot());
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
    public Collection<TroopInstance> getGameTroops(String gameId) {
        Map<String, TroopInstance> troops = gameTroops.get(gameId);
        return troops != null ? troops.values() : Collections.emptyList();
    }
    
    /**
     * Get troops owned by a specific player
     */
    public List<TroopInstance> getPlayerTroops(String gameId, short ownerSlot) {
        Map<Short, List<String>> playerTroopMap = playerTroops.get(gameId);
        if (playerTroopMap == null) {
            return Collections.emptyList();
        }
        
        List<String> troopIds = playerTroopMap.get(ownerSlot);
        if (troopIds == null) {
            return Collections.emptyList();
        }
        
        Map<String, TroopInstance> troops = gameTroops.get(gameId);
        if (troops == null) {
            return Collections.emptyList();
        }
        
        return troopIds.stream()
                .map(troops::get)
                .filter(Objects::nonNull)
                .filter(TroopInstance::isAlive)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a specific troop instance
     */
    public TroopInstance getTroop(String gameId, String troopInstanceId) {
        Map<String, TroopInstance> troops = gameTroops.get(gameId);
        return troops != null ? troops.get(troopInstanceId) : null;
    }
    
    /**
     * Get troops within range of a position
     */
    public List<TroopInstance> getTroopsInRange(String gameId, Vector2 position, float range) {
        return getGameTroops(gameId).stream()
                .filter(TroopInstance::isAlive)
                .filter(troop -> troop.distanceTo(position) <= range)
                .collect(Collectors.toList());
    }
    
    /**
     * Get enemy troops for a specific player
     */
    public List<TroopInstance> getEnemyTroops(String gameId, short playerSlot) {
        return getGameTroops(gameId).stream()
                .filter(TroopInstance::isAlive)
                .filter(troop -> troop.getOwnerSlot() != playerSlot)
                .collect(Collectors.toList());
    }
    
    /**
     * Get friendly troops for a specific player
     */
    public List<TroopInstance> getFriendlyTroops(String gameId, short playerSlot) {
        return getGameTroops(gameId).stream()
                .filter(TroopInstance::isAlive)
                .filter(troop -> troop.getOwnerSlot() == playerSlot)
                .collect(Collectors.toList());
    }
    
    /**
     * Find nearest enemy troop to a position
     */
    public TroopInstance findNearestEnemyTroop(String gameId, short playerSlot, Vector2 position, float maxRange) {
        return getEnemyTroops(gameId, playerSlot).stream()
                .filter(troop -> troop.distanceTo(position) <= maxRange)
                .min(Comparator.comparing(troop -> troop.distanceTo(position)))
                .orElse(null);
    }
    
    /**
     * Find friendly troops needing healing
     */
    public List<TroopInstance> findTroopsNeedingHealing(String gameId, short playerSlot, 
            Vector2 healerPosition, float healRange, float healthThreshold) {
        return getFriendlyTroops(gameId, playerSlot).stream()
                .filter(troop -> troop.distanceTo(healerPosition) <= healRange)
                .filter(troop -> troop.getHealthPercentage() < healthThreshold)
                .sorted(Comparator.comparing(TroopInstance::getHealthPercentage))
                .collect(Collectors.toList());
    }
    
    /**
     * Apply damage to a troop and handle death
     */
    public boolean applyDamageToTroop(String gameId, String troopInstanceId, int damage) {
        TroopInstance troop = getTroop(gameId, troopInstanceId);
        if (troop == null || !troop.isAlive()) {
            return false;
        }
        
        troop.takeDamage(damage);
        
        // If troop died, remove it
        if (!troop.isAlive()) {
            removeTroop(gameId, troopInstanceId);
            log.info("Troop {} died and was removed from game {}", troopInstanceId, gameId);
        }
        
        return true;
    }
    
    /**
     * Heal a troop
     */
    public boolean healTroop(String gameId, String troopInstanceId, int healAmount) {
        TroopInstance troop = getTroop(gameId, troopInstanceId);
        if (troop == null || !troop.isAlive()) {
            return false;
        }
        
        troop.heal(healAmount);
        return true;
    }
    
    /**
     * Move troops for a player (placeholder for future implementation)
     */
    public void moveTroops(String gameId, short playerSlot, List<String> troopIds, Vector2 targetPosition) {
        log.info("Move command received for player {} troops {} to position ({}, {}) - Implementation pending", 
                playerSlot, troopIds, targetPosition.x(), targetPosition.y());
        
        // For now, just set the target position for each troop
        for (String troopId : troopIds) {
            TroopInstance troop = getTroop(gameId, troopId);
            if (troop != null && troop.getOwnerSlot() == playerSlot && troop.isAlive()) {
                troop.setMoveTarget(targetPosition);
                troop.setAIState(TroopAIState.MOVING_TO_POSITION);

                // Clear any previous attack target
                troop.setTargetTroopId(null);
            }
        }
    }

    /**
     * Move troop to attack a specific target
     * @param attackerTroopId
     * @param targetTroopId
     * @return true if the attack was initiated, false if the troop is not found or not alive
     */
    public boolean moveToAttackTarget(String gameId, String attackerTroopId, String targetTroopId) {
        TroopInstance attacker = getTroop(gameId, attackerTroopId);
        TroopInstance target = getTroop(gameId, targetTroopId);

        if (attacker == null || target == null || !attacker.isAlive() || !target.isAlive()) {
            log.debug("Cannot move to attack");
            return false;
        }

        boolean isEnemy = attacker.getOwnerSlot() != target.getOwnerSlot();
        boolean isHealer = attacker.getTroopType() == TroopEnum.HEALER;

        if (!isEnemy && !isHealer) {
            log.warn("Troop {} cannot attack target {} - not an enemy or healer", attackerTroopId, targetTroopId);
            return false;
        }

        // Set the target and update AI state
        attacker.setTargetTroopId(targetTroopId);

        if (isHealer && !isEnemy) {
            attacker.setAIState(TroopAIState.HEALING_ALLY);
            log.debug("Troop {} is healing ally {}", attackerTroopId, targetTroopId);
        } else {
            attacker.setAIState(TroopAIState.SEEKING);
            log.debug("Troop {} set to attack enemy {}", attackerTroopId, targetTroopId);
        }

        // Calculate attack range based on troop type
        float attackRange = troopService.getTroopAttackRange(attacker.getTroopType());
        float currentDistance = attacker.distanceTo(target.getPosition());

        if (currentDistance <= attackRange) {
            attacker.setAIState(isHealer ? TroopAIState.HEALING_ALLY : TroopAIState.ATTACKING);
            log.debug("Troop {} is within attack range of {}: {}", attackerTroopId, targetTroopId, target);
            return true;
        }

        return true;
    }

    /**
     * Update troop movements towards their targets
     * Called periodically by the game logic
     */
    public void updateTroopMovements(String gameId, float deltaTime) {
        Collection<TroopInstance> troops = getGameTroops(gameId);

        for (TroopInstance troop : troops) {
            if (!troop.isAlive()) continue;
            
            switch (troop.getAIState()) {
                case SEEKING, RETREATING -> updateSeekingMovement(troop, deltaTime);
                case HEALING_ALLY -> updateHealingMovement(troop, deltaTime);
                case MOVING_TO_POSITION -> updateDirectMovement(troop, deltaTime);
                default -> {}
            }
        }
    }

    /**
     * Update movement for troops seeking enemies
     */
    private void updateSeekingMovement(TroopInstance troop, float deltaTime) {
        String targetId = troop.getTargetTroopId();
        if (targetId == null) return;
        
        TroopInstance target = getTroop(troop.getGameId(), targetId);
        if (target == null || !target.isAlive()) {
            // Target is gone, reset to idle
            troop.setTargetTroopId(null);
            troop.setAIState(TroopAIState.IDLE);
            return;
        }
        
        float attackRange = troopService.getTroopAttackRange(troop.getTroopType());
        float currentDistance = troop.distanceTo(target.getPosition());
        
        // If reached attack range, switch to attacking
        if (currentDistance <= attackRange) {
            troop.setAIState(TroopAIState.ATTACKING);
            return;
        }
        
        // Move toward target with appropriate speed
        float moveSpeed = troopService.getTroopMovementSpeed(troop.getTroopType());
        troop.moveTowards(target.getPosition(), moveSpeed, deltaTime);
    }

    /**
     * Update movement for troops healing allies
     */
    private void updateHealingMovement(TroopInstance troop, float deltaTime) {
        // Similar to seeking, but for healing allies
        String targetId = troop.getTargetTroopId();
        if (targetId == null) return;
        
        TroopInstance target = getTroop(troop.getGameId(), targetId);
        if (target == null || !target.isAlive() || target.getHealthPercentage() >= 0.9f) {
            // Target is gone or fully healed
            troop.setTargetTroopId(null);
            troop.setAIState(TroopAIState.IDLE);
            return;
        }
        
        float healRange = 4.0f; // Healing range
        float currentDistance = troop.distanceTo(target.getPosition());
        
        // If in heal range, stop and heal
        if (currentDistance <= healRange) {
            // Stay in healing state but stop moving
            return;
        }
        
        // Move toward target that needs healing
        float moveSpeed = troopService.getTroopMovementSpeed(troop.getTroopType());
        troop.moveTowards(target.getPosition(), moveSpeed, deltaTime);
    }

    /**
     * Update movement for troops moving to a position (not targeting)
     */
    private void updateDirectMovement(TroopInstance troop, float deltaTime) {
        Vector2 targetPos = troop.getTargetPosition();
        if (targetPos == null) {
            troop.setAIState(TroopAIState.IDLE);
            return;
        }

        float moveSpeed = troopService.getTroopMovementSpeed(troop.getTroopType());
        float arrivalThreshold = 0.5f;
        
        // Check if we've arrived at the target position
        if (troop.distanceTo(targetPos) <= arrivalThreshold) {
            troop.setMoveTarget(null);
            troop.setAIState(TroopAIState.IDLE);
            return;
        }
        
        // Continue moving toward target position
        troop.moveTowards(targetPos, moveSpeed, deltaTime);
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
        Map<String, TroopInstance> troops = gameTroops.remove(gameId);
        playerTroops.remove(gameId);
        
        if (troops != null) {
            log.info("Cleaned up {} troops for game {}", troops.size(), gameId);
        }
    }
    
    /**
     * Get troop statistics for a game
     */
    public String getTroopStatistics(String gameId) {
        Collection<TroopInstance> troops = getGameTroops(gameId);
        if (troops.isEmpty()) {
            return "No troops in game " + gameId;
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Troop Statistics for ").append(gameId).append(":\n");
        
        // Group by player
        Map<Short, List<TroopInstance>> playerTroopGroups = troops.stream()
                .collect(Collectors.groupingBy(TroopInstance::getOwnerSlot));
        
        for (Map.Entry<Short, List<TroopInstance>> entry : playerTroopGroups.entrySet()) {
            Short playerSlot = entry.getKey();
            List<TroopInstance> playerTroopList = entry.getValue();
            
            stats.append(String.format("  Player %d: %d troops%n", playerSlot, playerTroopList.size()));
            
            // Group by troop type
            Map<TroopEnum, Long> typeCount = playerTroopList.stream()
                    .collect(Collectors.groupingBy(TroopInstance::getTroopType, Collectors.counting()));
            
            for (Map.Entry<TroopEnum, Long> typeEntry : typeCount.entrySet()) {
                stats.append(String.format("    %s: %d%n", typeEntry.getKey(), typeEntry.getValue()));
            }
        }
        
        return stats.toString();
    }
}
