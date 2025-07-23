package com.server.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.resource.service.ChampionService;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttackTargetingService {
    
    PositionService positionService;
    MoveService moveService;
    ChampionService championService;
    
    @Lazy // Lazy loading to avoid circular dependency
    PvPService pvpService;
    
    // Store attack targets for each game and player
    private final Map<String, Map<Short, AttackTarget>> attackTargets = new ConcurrentHashMap<>();
    
    /**
     * Set an attack target for a champion
     */
    public void setAttackTarget(String gameId, short attackerSlot, AttackTarget target) {
        attackTargets.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                    .put(attackerSlot, target);
        
        log.debug("Set attack target for slot {} in game {}: {}", attackerSlot, gameId, target);
        
        // Start moving towards the target
        moveToAttackTarget(gameId, attackerSlot, target);
    }
    
    /**
     * Clear attack target for a champion (when they manually move)
     */
    public void clearAttackTarget(String gameId, short attackerSlot) {
        Map<Short, AttackTarget> gameTargets = attackTargets.get(gameId);
        if (gameTargets != null) {
            AttackTarget removed = gameTargets.remove(attackerSlot);
            if (removed != null) {
                log.debug("Cleared attack target for slot {} in game {}", attackerSlot, gameId);
            }
        }
    }
    
    /**
     * Get current attack target for a champion
     */
    public AttackTarget getAttackTarget(String gameId, short attackerSlot) {
        Map<Short, AttackTarget> gameTargets = attackTargets.get(gameId);
        return gameTargets != null ? gameTargets.get(attackerSlot) : null;
    }
    
    /**
     * Check if champion is in attack range of their target
     */
    public boolean isInAttackRange(String gameId, short attackerSlot) {
        AttackTarget target = getAttackTarget(gameId, attackerSlot);
        if (target == null) {
            return false;
        }
        
        // Get attacker's position
        var attackerPosition = positionService.getPlayerPosition(gameId, attackerSlot);
        if (attackerPosition == null) {
            return false;
        }
        
        // Get attacker's champion stats for attack range
        ChampionEnum attackerChampion = getChampionForSlot(gameId, attackerSlot);
        if (attackerChampion == null) {
            return false;
        }
        
        float attackRange = getChampionAttackRange(attackerChampion);
        Vector2 targetPosition = getTargetPosition(gameId, target);
        
        if (targetPosition == null) {
            return false;
        }
        
        float distance = attackerPosition.getPosition().distanceTo(targetPosition);
        boolean inRange = distance <= attackRange;
        
        log.debug("Attack range check for slot {}: distance={}, range={}, inRange={}", 
                attackerSlot, distance, attackRange, inRange);
        
        return inRange;
    }
    
    /**
     * Process continuous attacking while in range
     */
    public boolean processContinuousAttack(String gameId, short attackerSlot) {
        if (!isInAttackRange(gameId, attackerSlot)) {
            // Target moved out of range, move towards it again
            AttackTarget target = getAttackTarget(gameId, attackerSlot);
            if (target != null) {
                moveToAttackTarget(gameId, attackerSlot, target);
            }
            return false;
        }
        
        // Stop movement if in range
        moveService.clearMoveTarget(gameId, attackerSlot);
        return true;
    }
    
    /**
     * Process all attackers in a game (called by game loop)
     */
    public void processAllAttackers(String gameId) {
        Map<Short, AttackTarget> gameTargets = attackTargets.get(gameId);
        if (gameTargets == null || gameTargets.isEmpty()) {
            return;
        }
        
        // Process each attacker
        for (Map.Entry<Short, AttackTarget> entry : gameTargets.entrySet()) {
            short attackerSlot = entry.getKey();
            AttackTarget target = entry.getValue();
            
            try {
                // Check if attacker is in range and can attack
                if (processContinuousAttack(gameId, attackerSlot)) {
                    // In range, trigger attack based on attack speed
                    // This could be enhanced with actual attack cooldowns
                    long currentTime = System.currentTimeMillis();
                    long lastAttackTime = target.getTimestamp();
                    
                    // Simple attack cooldown - attack every 1000ms (1 second)
                    if (currentTime - lastAttackTime >= 1000) {
                        performAttack(gameId, attackerSlot, target);
                        target.setTimestamp(currentTime); // Update last attack time
                    }
                }
            } catch (Exception e) {
                log.error("Error processing attacker {} in game {}: {}", attackerSlot, gameId, e.getMessage());
            }
        }
    }
    
    /**
     * Perform actual attack logic
     */
    private void performAttack(String gameId, short attackerSlot, AttackTarget target) {
        switch (target.getType()) {
            case CHAMPION:
                // Champion attacking another champion
                pvpService.handleChampionAttackChampion(gameId, attackerSlot, target.getChampionSlot(), System.currentTimeMillis());
                break;
            case TARGET:
                // Champion attacking a target/NPC
                pvpService.handleChampionAttackTarget(gameId, attackerSlot, target.getTargetId(), System.currentTimeMillis());
                break;
        }
    }
    
    /**
     * Move champion towards their attack target
     */
    private void moveToAttackTarget(String gameId, short attackerSlot, AttackTarget target) {
        Vector2 targetPosition = getTargetPosition(gameId, target);
        if (targetPosition == null) {
            log.warn("Could not find target position for attack target: {}", target);
            return;
        }
        
        // Set movement target
        moveService.setMoveTarget(gameId, attackerSlot, targetPosition);
        
        log.debug("Moving slot {} towards attack target at position {}", attackerSlot, targetPosition);
    }
    
    /**
     * Get position of the target (champion or NPC)
     */
    private Vector2 getTargetPosition(String gameId, AttackTarget target) {
        switch (target.getType()) {
            case CHAMPION:
                var championPos = positionService.getPlayerPosition(gameId, target.getChampionSlot());
                return championPos != null ? championPos.getPosition() : null;
            case TARGET:
                // TODO: Get target/NPC position from game map or target service
                // For now, return a placeholder position
                return new Vector2(100.0f + target.getTargetId().hashCode() % 100, 
                                 100.0f + target.getTargetId().hashCode() % 100);
            default:
                return null;
        }
    }
    
    /**
     * Get champion enum for a slot
     */
    private ChampionEnum getChampionForSlot(String gameId, short slot) {
        Map<Short, ChampionEnum> slot2ChampionId = ChannelManager.getSlot2ChampionId(gameId);
        return slot2ChampionId != null ? slot2ChampionId.get(slot) : null;
    }
    
    /**
     * Get champion's attack range
     */
    private float getChampionAttackRange(ChampionEnum championEnum) {
        var champion = championService.getChampionById(championEnum);
        return champion != null ? champion.getAttackRange() : 1.0f; // Default range
    }
    
    /**
     * Clear all attack targets for a game (when game ends)
     */
    public void clearGameTargets(String gameId) {
        attackTargets.remove(gameId);
        log.info("Cleared attack targets for game {}", gameId);
    }
    
    /**
     * Attack target data class
     */
    @Data
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttackTarget {
        AttackTargetType type;
        short championSlot; // Used when type is CHAMPION
        String targetId;    // Used when type is TARGET
        long timestamp;
        
        // Constructor for attacking a champion
        public AttackTarget(short championSlot, long timestamp) {
            this.type = AttackTargetType.CHAMPION;
            this.championSlot = championSlot;
            this.targetId = null;
            this.timestamp = timestamp;
        }
        
        // Constructor for attacking a target/NPC
        public AttackTarget(String targetId, long timestamp) {
            this.type = AttackTargetType.TARGET;
            this.championSlot = -1;
            this.targetId = targetId;
            this.timestamp = timestamp;
        }
        
        // Setter for timestamp to update last attack time
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Attack target type enum
     */
    public enum AttackTargetType {
        CHAMPION,
        TARGET
    }
}
