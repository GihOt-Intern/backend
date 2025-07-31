package com.server.game.service.troop;

import com.server.game.model.game.Entity;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.TroopCreateContext;
import com.server.game.model.game.TroopInstance2;
import com.server.game.model.game.TroopInstance2.TroopAIState;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.attackComponent.AttackContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.service.TroopService;
import com.server.game.service.attack.AttackTargetingService;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.TroopEnum;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.server.game.netty.ChannelManager;
import com.server.game.netty.sendObject.pvp.HealthUpdateSend;
import com.server.game.netty.sendObject.troop.TroopDeathSend;

import io.netty.channel.Channel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TroopManager {
    private final TroopService troopService;
    private final GameStateService gameStateService;
    private final AttackTargetingService attackTargetingService;
    
    // gameId -> Map<troopInstanceId, TroopInstance2>
    private final Map<String, Map<String, TroopInstance2>> gameTroops = new ConcurrentHashMap<>();
    
    // gameId -> Map<ownerSlot, List<troopInstanceId>>
    private final Map<String, Map<Short, List<String>>> playerTroops = new ConcurrentHashMap<>();
    
    // Constructor with dependencies
    public TroopManager(
            TroopService troopService, 
            GameStateService gameStateService,
            AttackTargetingService attackTargetingService,
            com.server.game.service.position.TargetPositionBroadcastService targetPositionBroadcastService) {
        this.troopService = troopService;
        this.gameStateService = gameStateService;
        this.attackTargetingService = attackTargetingService;
    }
    
    /**
     * Create a new troop instance for a player
     */
    public TroopInstance2 createTroop(String gameId, short ownerSlot, TroopEnum troopType, Vector2 spawnPosition) {
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
        
        // Create troop create context
        TroopCreateContext ctx = new TroopCreateContext(
            troopType,
            ownerSlot,
            gameStateService.getGameStateById(gameId),
            gameId
        );
        
        // Create troop instance
        TroopInstance2 troop = troopService.createInstanceOf(ctx);
        
        // Deduct cost from player
        slotState.spendGold(troopCost);
        
        // Add to game troops
        gameTroops.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                  .put(troop.getIdAString(), troop);
        
        // Add to player troops
        playerTroops.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(ownerSlot, k -> new ArrayList<>())
                   .add(troop.getIdAString());
        
        // Add to slot state
        slotState.addTroopInstance(troop);
        
        log.info("Created troop {} of type {} for player {} for {} gold", 
                troop.getIdAString(), troopType, ownerSlot, troopCost);
        
        return troop;
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
                .filter(t -> t.isAlive())
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
                .filter(t -> t.isAlive())
                .filter(troop -> troop.distanceTo(position) <= range)
                .collect(Collectors.toList());
    }
    
    /**
     * Get enemy troops for a specific player
     */
    public List<TroopInstance2> getEnemyTroops(String gameId, short playerSlot) {
        return getGameTroops(gameId).stream()
                .filter(t -> t.isAlive())
                .filter(troop -> troop.getOwnerSlot() != playerSlot)
                .collect(Collectors.toList());
    }
    
    /**
     * Get friendly troops for a specific player
     */
    public List<TroopInstance2> getFriendlyTroops(String gameId, short playerSlot) {
        return getGameTroops(gameId).stream()
                .filter(t -> t.isAlive())
                .filter(troop -> troop.getOwnerSlot() == playerSlot)
                .collect(Collectors.toList());
    }
    
    /**
     * Find nearest enemy troop to a position
     */
    public TroopInstance2 findNearestEnemyTroop(String gameId, short playerSlot, Vector2 position, float maxRange) {
        return getEnemyTroops(gameId, playerSlot).stream()
                .filter(troop -> troop.distanceTo(position) <= maxRange)
                .min(Comparator.comparing(troop -> troop.distanceTo(position)))
                .orElse(null);
    }
    
    /**
     * Find friendly troops needing healing
     */
    public List<TroopInstance2> findTroopsNeedingHealing(String gameId, short playerSlot, 
            Vector2 healerPosition, float healRange, float healthThreshold) {
        return getFriendlyTroops(gameId, playerSlot).stream()
                .filter(troop -> troop.distanceTo(healerPosition) <= healRange)
                .filter(troop -> troop.getHealthPercentage() < healthThreshold)
                .sorted(Comparator.comparing(troop -> troop.getHealthPercentage()))
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
        
        // Store HP before damage to detect death
        int oldHP = troop.getCurrentHP();
        
        // Create an AttackContext for the damage and use the proper component-based system
        AttackContext ctx = new AttackContext(
            gameId,
            (short) -1,             // Not a champion
            "system",               // System-initiated damage
            (short) -1,             // Not a champion
            troopInstanceId,
            null,                   // No attacker entity
            troop.getCurrentPosition(),
            troop,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            null                    // No extra data
        );
        
        // Use the proper attack system - invoke receiveAttack on the target entity
        troop.receiveAttack(ctx);
        
        // If troop died, remove it
        if (!troop.isAlive() && oldHP > 0) {
            // Update AI state
            troop.setAIState(TroopInstance2.TroopAIState.DEAD);
            
            // Clear all attack targets that were targeting this dead troop
            attackTargetingService.clearTargetsAttackingTarget(gameId, troopInstanceId);
            
            // Notify game state service about troop death
            broadcastTroopDeath(gameId, troopInstanceId, troop.getOwnerSlot());

            removeTroop(gameId, troopInstanceId);
            log.info("Troop {} died and was removed from game {}", troopInstanceId, gameId);
        } else {
            com.server.game.netty.sendObject.pvp.HealthUpdateSend healthUpdate = new HealthUpdateSend(
                troopInstanceId,
                troop.getCurrentHP(),
                troop.getMaxHP(),
                damage,
                System.currentTimeMillis()
            );

            Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(healthUpdate);
            } else {
                log.warn("No active channel found for game ID: {}", gameId);
            }
        }
        
        return true;
    }

    /**
     * Apply healing to a troop
     */
    public boolean applyHealingToTroop(String gameId, String troopInstanceId, int healAmount, Entity healer) {
        TroopInstance2 troop = getTroop(gameId, troopInstanceId);
        if (troop == null || !troop.isAlive() || troop.getCurrentHP() >= troop.getMaxHP()) {
            return false;
        }
        
        // Store HP before healing
        int oldHP = troop.getCurrentHP();
        
        // Apply healing through the health component
        HealthComponent healthComponent = troop.getHealthComponent();
        if (healthComponent != null) {
            healthComponent.increaseHP(healAmount);
            
            // Send health update message
            com.server.game.netty.sendObject.pvp.HealthUpdateSend healthUpdate = new HealthUpdateSend(
                troopInstanceId,
                troop.getCurrentHP(),
                troop.getMaxHP(),
                -healAmount, // Negative value indicates healing
                System.currentTimeMillis()
            );

            Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(healthUpdate);
            } else {
                log.warn("No active channel found for game ID: {}", gameId);
            }
            
            log.debug("Troop {} healed for {} HP (from {} to {})", 
                troopInstanceId, healAmount, oldHP, troop.getCurrentHP());
            return true;
        }
        
        return false;
    }

    /**
     * Broadcast a troop death event
     */
    private void broadcastTroopDeath(String gameId, String troopInstanceId, short ownerSlot) {
        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel == null || !channel.isActive()) {
            log.warn("No active channel found for game ID: {}", gameId);
            return;
        }

        TroopDeathSend deathMessage = new TroopDeathSend(troopInstanceId, ownerSlot);
        channel.writeAndFlush(deathMessage);
    }

    /**
     * Heal a troop
     */
    public boolean healTroop(String gameId, String troopInstanceId, int healAmount) {
        TroopInstance2 troop = getTroop(gameId, troopInstanceId);
        if (troop == null || !troop.isAlive()) {
            return false;
        }
        
        // Store HP before healing
        int oldHP = troop.getCurrentHP();
        
        // Use the proper heal method from TroopInstance2
        troop.heal(healAmount);
        
        // Send health update to clients for the UI
        HealthUpdateSend healthUpdate = new HealthUpdateSend(
            troopInstanceId,
            troop.getCurrentHP(),
            troop.getMaxHP(),
            -healAmount, // Negative damage means healing
            System.currentTimeMillis()
        );

        Channel channel = ChannelManager.getAnyChannelByGameId(gameId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(healthUpdate);
        } else {
            log.warn("No active channel found for game ID: {}", gameId);
        }
        
        log.debug("Healed troop {} for {} HP: {} -> {}", 
                troopInstanceId, healAmount, oldHP, troop.getCurrentHP());
                
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
            TroopInstance2 troop = getTroop(gameId, troopId);
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
        TroopInstance2 attacker = getTroop(gameId, attackerTroopId);
        TroopInstance2 target = getTroop(gameId, targetTroopId);

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
        float currentDistance = attacker.distanceTo(target.getCurrentPosition());

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
        Collection<TroopInstance2> troops = getGameTroops(gameId);

        for (TroopInstance2 troop : troops) {
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
    private void updateSeekingMovement(TroopInstance2 troop, float deltaTime) {
        String targetId = troop.getCurrentTargetId();
        if (targetId == null) return;

        TroopInstance2 target = getTroop(troop.getGameId(), targetId);
        if (target == null || !target.isAlive()) {
            // Target is gone, reset to idle
            troop.setTargetTroopId(null);
            troop.setAIState(TroopAIState.IDLE);
            return;
        }
        
        float attackRange = troopService.getTroopAttackRange(troop.getTroopType());
        float currentDistance = troop.distanceTo(target.getCurrentPosition());
        
        // If reached attack range, switch to attacking
        if (currentDistance <= attackRange) {
            troop.setAIState(TroopAIState.ATTACKING);
            return;
        }
        
        // Move toward target with appropriate speed
        float moveSpeed = troopService.getTroopMovementSpeed(troop.getTroopType());
        troop.moveTowards(target.getCurrentPosition(), moveSpeed, deltaTime);
    }

    /**
     * Update movement for troops healing allies
     */
    private void updateHealingMovement(TroopInstance2 troop, float deltaTime) {
        // Similar to seeking, but for healing allies
        String targetId = troop.getCurrentTargetId();
        if (targetId == null) {
            troop.setAIState(TroopAIState.IDLE);
            return;
        }

        TroopInstance2 target = getTroop(troop.getGameId(), targetId);
        if (target == null || !target.isAlive() || target.getHealthPercentage() >= 0.9f) {
            // Target is gone or fully healed
            troop.setTargetTroopId(null);
            troop.setAIState(TroopAIState.IDLE);
            return;
        }
        
        // Use a fixed healing range or get from component
        float healRange = troop.getHealingRange();
        float currentDistance = troop.distanceTo(target.getCurrentPosition());
        
        // If in heal range, stop and heal
        if (currentDistance <= healRange) {
            // Apply healing using health component
            if (troop.getAttackComponent().canAttack(System.currentTimeMillis())) {
                int healAmount = troop.getHealingPower();
                target.heal(healAmount);
                // Record the attack/heal
                // Create a healing context and perform healing attack
                AttackContext healContext = new AttackContext(
                    troop.getGameId(),
                    (short) -1,
                    troop.getIdAString(),
                    (short) -1,
                    target.getIdAString(),
                    troop,
                    target.getCurrentPosition(),
                    target,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    null
                );
                troop.getAttackComponent().performAttack(healContext);
                
                log.debug("Troop {} healed {} for {} HP (health now: {}/{})",
                        troop.getIdAString(), target.getIdAString(), healAmount,
                        target.getCurrentHP(), target.getMaxHP());
                
                // If target is fully healed, find a new target
                if (target.getCurrentHP() >= target.getMaxHP()) {
                    troop.setCurrentTargetId(null);
                    troop.setAIState(TroopAIState.IDLE);
                }
            }
            return;
        }
        
        // Move toward target that needs healing
        float moveSpeed = troopService.getTroopMovementSpeed(troop.getTroopType());
        troop.moveTowards(target.getCurrentPosition(), moveSpeed, deltaTime);
    }

    /**
     * Update movement for troops moving to a position (not targeting)
     */
    private void updateDirectMovement(TroopInstance2 troop, float deltaTime) {
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
                .collect(Collectors.groupingBy(TroopInstance2::getOwnerSlot));
        
        for (Map.Entry<Short, List<TroopInstance2>> entry : playerTroopGroups.entrySet()) {
            Short playerSlot = entry.getKey();
            List<TroopInstance2> playerTroopList = entry.getValue();
            
            stats.append(String.format("  Player %d: %d troops%n", playerSlot, playerTroopList.size()));
            
            // Group by troop type
            Map<TroopEnum, Long> typeCount = playerTroopList.stream()
                    .collect(Collectors.groupingBy(TroopInstance2::getTroopType, Collectors.counting()));
            
            for (Map.Entry<TroopEnum, Long> typeEntry : typeCount.entrySet()) {
                stats.append(String.format("    %s: %d%n", typeEntry.getKey(), typeEntry.getValue()));
            }
        }
        
        return stats.toString();
    }
}
