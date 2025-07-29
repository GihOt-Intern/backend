package com.server.game.service.attack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.service.champion.ChampionService;
import com.server.game.service.move.MoveService;
import com.server.game.service.position.PositionService;
import com.server.game.service.pvp.PvPService;
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
        
        // Get attacker's real-time position (whether moving or not)
        Vector2 attackerPosition = moveService.getCurrentRealTimePosition(gameId, attackerSlot);
        if (attackerPosition == null) {
            // Fallback to cached position if no real-time position available
            var attackerPos = positionService.getPlayerPosition(gameId, attackerSlot);
            if (attackerPos == null) {
                return false;
            }
            attackerPosition = attackerPos.getPosition();
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
        
        float distance = attackerPosition.distanceTo(targetPosition);
        boolean inRange = distance <= attackRange;
        
        log.debug("Attack range check for slot {}: distance={}, range={}, inRange={}", 
                attackerSlot, distance, attackRange, inRange);
        
        return inRange;
    }
    
    // Optimized target following constants
    private static final long POSITION_UPDATE_COOLDOWN = 200; // 200ms to reduce server load
    private final Map<String, Map<Short, Long>> lastPositionUpdate = new ConcurrentHashMap<>();

    /**
     * Process continuous attacking while in range - Simplified with 200ms recalculation
     */
    public boolean processContinuousAttack(String gameId, short attackerSlot) {
        AttackTarget target = getAttackTarget(gameId, attackerSlot);
        if (target == null) {
            return false;
        }

        // Check if we're in attack range
        if (isInAttackRange(gameId, attackerSlot)) {
            // In range - stop movement and check if we can attack (cooldown)
            moveService.clearMoveTarget(gameId, attackerSlot);
            
            // Get champion info to check cooldown
            ChampionEnum attackerChampion = getChampionForSlot(gameId, attackerSlot);
            if (attackerChampion != null) {
                // Check if champion can attack (not on cooldown)
                boolean canAttack = pvpService.canChampionAttack(gameId, attackerSlot, attackerChampion);
                if (canAttack) {
                    // Trigger attack based on target type
                    long currentTime = System.currentTimeMillis();
                    if (target.getType() == AttackTargetType.CHAMPION) {
                        pvpService.handleChampionAttackChampion(gameId, attackerSlot, target.getChampionSlot(), currentTime);
                    } else {
                        pvpService.handleChampionAttackTarget(gameId, attackerSlot, target.getTargetId(), currentTime);
                    }
                    return true;
                } else {
                    // Still in range but on cooldown, stay in position
                    return true;
                }
            }
            return true;
        }

        // Out of range - use simple recalculation every 100ms
        return handleTargetRecalculation(gameId, attackerSlot, target);
    }

    /**
     * Handle target position recalculation every 100ms
     */
    private boolean handleTargetRecalculation(String gameId, short attackerSlot, AttackTarget target) {
        long currentTime = System.currentTimeMillis();
        
        // Check 300ms cooldown
        Map<Short, Long> gameLastUpdates = lastPositionUpdate.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        Long lastUpdate = gameLastUpdates.get(attackerSlot);
        if (lastUpdate != null && (currentTime - lastUpdate) < POSITION_UPDATE_COOLDOWN) {
            return false; // Still in cooldown
        }

        // Get current target position
        Vector2 currentTargetPos = getTargetPosition(gameId, target);
        if (currentTargetPos == null) {
            return false;
        }

        // Update tracking and set new move target
        gameLastUpdates.put(attackerSlot, currentTime);
        
        // Use simple setMoveTarget to the current target position
        moveService.setMoveTarget(gameId, attackerSlot, currentTargetPos);
        
        log.debug("Recalculated target position for slot {} attacking {} at position {}", 
                attackerSlot, 
                target.getType() == AttackTargetType.CHAMPION ? "champion " + target.getChampionSlot() : "target " + target.getTargetId(),
                currentTargetPos);
        
        return false; // Still moving towards target
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
     * Move champion towards their attack target - Simplified to use setMoveTarget
     */
    private void moveToAttackTarget(String gameId, short attackerSlot, AttackTarget target) {
        Vector2 targetPosition = getTargetPosition(gameId, target);
        if (targetPosition == null) {
            log.warn("Could not find target position for attack target: {}", target);
            return;
        }
        
        // Use simple setMoveTarget for all types of targets
        moveService.setMoveTarget(gameId, attackerSlot, targetPosition);
        
        String targetInfo = target.getType() == AttackTargetType.CHAMPION 
            ? "champion in slot " + target.getChampionSlot() 
            : "target " + target.getTargetId();
        log.debug("Moving slot {} towards {} at position {}", attackerSlot, targetInfo, targetPosition);
    }
    
    /**
     * Get position of the target (champion or NPC)
     */
    private Vector2 getTargetPosition(String gameId, AttackTarget target) {
        switch (target.getType()) {
            case CHAMPION:
                // Use real-time position from move service first, then fall back to cached position
                Vector2 realTimePos = moveService.getCurrentRealTimePosition(gameId, target.getChampionSlot());
                if (realTimePos != null) {
                    return realTimePos;
                }
                
                // Fallback to cached position if no real-time position available
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
        lastPositionUpdate.remove(gameId);
        log.info("Cleared attack targets and tracking data for game {}", gameId);
    }

    /**
     * Set attack target by champion slot (for PvP combat)
     */
    public void setChampionAttackTarget(String gameId, short attackerSlot, short targetSlot) {
        AttackTarget target = new AttackTarget(targetSlot, System.currentTimeMillis());
        setAttackTarget(gameId, attackerSlot, target);
        log.info("Set champion attack target: slot {} attacking slot {} in game {}", 
                attackerSlot, targetSlot, gameId);
    }

    /**
     * Set attack target by target ID (for PvE combat)
     */
    public void setTargetAttackTarget(String gameId, short attackerSlot, String targetId) {
        AttackTarget target = new AttackTarget(targetId, System.currentTimeMillis());
        setAttackTarget(gameId, attackerSlot, target);
        log.info("Set target attack target: slot {} attacking target {} in game {}", 
                attackerSlot, targetId, gameId);
    }

    /**
     * Check if a champion has any attack target
     */
    public boolean hasAttackTarget(String gameId, short attackerSlot) {
        return getAttackTarget(gameId, attackerSlot) != null;
    }

    /**
     * Get current combat status for debugging
     */
    public String getCombatStatus(String gameId, short attackerSlot) {
        AttackTarget target = getAttackTarget(gameId, attackerSlot);
        if (target == null) {
            return "No target";
        }
        
        boolean inRange = isInAttackRange(gameId, attackerSlot);
        String targetInfo = target.getType() == AttackTargetType.CHAMPION 
            ? "champion " + target.getChampionSlot() 
            : "target " + target.getTargetId();
            
        return String.format("Attacking %s - %s", targetInfo, inRange ? "IN RANGE" : "OUT OF RANGE");
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
