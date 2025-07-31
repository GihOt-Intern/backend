package com.server.game.service.troop;

import com.server.game.model.game.TroopInstance2;
import com.server.game.model.map.component.Vector2;
import com.server.game.model.game.TroopInstance2.TroopAIState;
import com.server.game.util.TroopEnum;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.AttackContext;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TroopAI {
    
    TroopManager troopManager;
    
    // AI Constants
    private static final float SIGHT_RANGE = 8.0f;
    private static final float HEAL_RANGE = 6.0f;
    private static final float HEAL_THRESHOLD = 0.7f; // Heal when below 70% health
    private static final float RETREAT_THRESHOLD = 0.3f; // Retreat when below 30% health
    private static final long AI_UPDATE_INTERVAL = 1000; // 1 second
    
    /**
     * Process AI for all troops in a game
     */
    @Async
    public CompletableFuture<Void> processGameAI(String gameId) {
        try {
            var troops = troopManager.getGameTroops(gameId);
            
            for (TroopInstance2 troop : troops) {
                if (troop.isAlive() && shouldUpdateAI(troop)) {
                    processIndividualTroopAI(troop);
                }
            }
        } catch (Exception e) {
            log.error("Error processing AI for game {}: {}", gameId, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Process AI for a single troop
     */
    public void processIndividualTroopAI(TroopInstance2 troop) {
        try {
            // Skip if troop is not alive or is manually controlled
            if (!troop.isAlive() || troop.getAIState() == TroopAIState.MOVING_TO_POSITION) {
                return;
            }
            
            // Update last AI processing time
            troop.updateLastAIUpdate();
            
            // Handle different AI states
            switch (troop.getAIState()) {
                case IDLE:
                    handleIdleState(troop);
                    break;
                case SEEKING:
                    handleSeekingState(troop);
                    break;
                case ATTACKING:
                    handleAttackingState(troop);
                    break;
                case RETREATING:
                    handleRetreatingState(troop);
                    break;
                case HEALING_ALLY:
                    handleHealingState(troop);
                    break;
                default:
                    // Unknown state, reset to idle
                    troop.setAIState(TroopAIState.IDLE);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Error processing AI for troop {}: {}", troop.getStringId(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle IDLE state - look for targets or allies to help
     */
    private void handleIdleState(TroopInstance2 troop) {
        String gameId = troop.getGameId();
        short ownerSlot = troop.getOwnerSlot();
        Vector2 position = troop.getTargetPosition();
        TroopEnum troopType = troop.getTroopType();
        
        // Check if troop needs to retreat
        if (troop.getHealthPercentage() < RETREAT_THRESHOLD) {
            troop.setAIState(TroopAIState.RETREATING);
            log.debug("Troop {} entering retreat state (health: {}%)", 
                    troop.getStringId(), (int)(troop.getHealthPercentage() * 100));
            return;
        }
        
        // Healer troops prioritize healing
        if (troopType == TroopEnum.HEALER) {
            List<TroopInstance2> needsHealing = troopManager.findTroopsNeedingHealing(
                    gameId, ownerSlot, position, HEAL_RANGE, HEAL_THRESHOLD);
            
            if (!needsHealing.isEmpty()) {
                TroopInstance2 targetToHeal = needsHealing.get(0); // Heal lowest health troop
                troop.setTargetTroopId(targetToHeal.getStringId());
                troop.setAIState(TroopAIState.HEALING_ALLY);
                log.debug("Healer {} targeting {} for healing",
                        troop.getStringId(), targetToHeal.getStringId());
                return;
            }
        }
        
        // Look for enemy troops to attack
        TroopInstance2 nearestEnemy = troopManager.findNearestEnemyTroop(
                gameId, ownerSlot, position, SIGHT_RANGE);
        
        if (nearestEnemy != null) {
            troop.setTargetTroopId(nearestEnemy.getStringId());
            troop.setAIState(TroopAIState.SEEKING);
            log.debug("Troop {} found enemy target {} at distance {}",
                    troop.getStringId(), nearestEnemy.getStringId(),
                    String.format("%.2f", troop.distanceTo(nearestEnemy.getCurrentPosition())));
        }
    }
    
    /**
     * Handle SEEKING state - move towards target
     */
    private void handleSeekingState(TroopInstance2 troop) {
        String targetId = troop.getCurrentTargetId();
        if (targetId == null) {
            troop.setAIState(TroopAIState.IDLE);
            return;
        }

        TroopInstance2 target = troopManager.getTroop(troop.getGameId(), targetId);
        if (target == null || !target.isAlive()) {
            troop.setTargetTroopId(null);
            troop.setAIState(TroopAIState.IDLE);
            log.debug("Troop {} lost target, returning to idle", troop.getStringId());
            return;
        }
        
        float distance = troop.distanceTo(target.getCurrentPosition());
        float attackRange = getAttackRange(troop.getTroopType());
        
        // If within attack range, switch to attacking
        if (distance <= attackRange && troop.canAttack()) {
            troop.setAIState(TroopAIState.ATTACKING);
            log.debug("Troop {} in range to attack target {}", 
                    troop.getStringId(), targetId);
        } else {
            // Move towards target
            troop.moveTowards(target.getCurrentPosition());
            log.debug("Troop {} moving towards target {} (distance: {})", 
                    troop.getStringId(), targetId, String.format("%.2f", distance));
        }
    }
    
    /**
     * Handle ATTACKING state - attack the target
     */
    private void handleAttackingState(TroopInstance2 troop) {
        String targetId = troop.getCurrentTargetId();
        if (targetId == null) {
            troop.setAIState(TroopAIState.IDLE);
            return;
        }
        
        TroopInstance2 target = troopManager.getTroop(troop.getGameId(), targetId);
        if (target == null || !target.isAlive()) {
            troop.setTargetTroopId(null);
            troop.setAIState(TroopAIState.IDLE);
            log.debug("Troop {} target destroyed, returning to idle", troop.getStringId());
            return;
        }
        
        float distance = troop.distanceTo(target.getCurrentPosition());
        float attackRange = getAttackRange(troop.getTroopType());
        
        // If still in range and can attack, perform attack
        if (distance <= attackRange && troop.canAttack()) {
            performAttack(troop, target);
        } else {
            // Target moved out of range, go back to seeking
            troop.setAIState(TroopAIState.SEEKING);
            log.debug("Troop {} target out of range, seeking", troop.getStringId());
        }
    }
    
    /**
     * Handle RETREATING state - move away from enemies
     */
    private void handleRetreatingState(TroopInstance2 troop) {
        String gameId = troop.getGameId();
        short ownerSlot = troop.getOwnerSlot();
        Vector2 position = troop.getCurrentPosition();
        
        // If health is restored, return to idle
        if (troop.getHealthPercentage() >= HEAL_THRESHOLD) {
            troop.setAIState(TroopAIState.IDLE);
            log.debug("Troop {} health restored, returning to idle", troop.getStringId());
            return;
        }
        
        // Find nearest enemy to flee from
        TroopInstance2 nearestEnemy = troopManager.findNearestEnemyTroop(
                gameId, ownerSlot, position, SIGHT_RANGE);
        
        if (nearestEnemy != null) {
            // Calculate retreat direction (opposite of enemy)
            Vector2 enemyPos = nearestEnemy.getCurrentPosition();
            Vector2 retreatDirection = new Vector2(
                    position.x() - enemyPos.x(),
                    position.y() - enemyPos.y()
            ).normalize();
            
            Vector2 retreatTarget = new Vector2(
                    position.x() + retreatDirection.x() * 3.0f,
                    position.y() + retreatDirection.y() * 3.0f
            );
            
            troop.moveTowards(retreatTarget);
            log.debug("Troop {} retreating from enemy {}", 
                    troop.getStringId(), nearestEnemy.getStringId());
        } else {
            // No enemies nearby, return to idle
            troop.setAIState(TroopAIState.IDLE);
        }
    }
    
    /**
     * Handle HEALING_ALLY state - heal friendly troops
     */
    private void handleHealingState(TroopInstance2 troop) {
        if (troop.getTroopType() != TroopEnum.HEALER) {
            troop.setAIState(TroopAIState.IDLE);
            return;
        }
        
        String targetId = troop.getCurrentTargetId();
        if (targetId == null) {
            troop.setAIState(TroopAIState.IDLE);
            return;
        }
        
        TroopInstance2 target = troopManager.getTroop(troop.getGameId(), targetId);
        if (target == null || !target.isAlive() || target.getHealthPercentage() >= HEAL_THRESHOLD) {
            troop.setTargetTroopId(null);
            troop.setAIState(TroopAIState.IDLE);
            log.debug("Healer {} finished healing target", troop.getStringId());
            return;
        }
        
        float distance = troop.distanceTo(target.getCurrentPosition());
        
        if (distance <= HEAL_RANGE && troop.canAttack()) { // Use attack cooldown for heal cooldown
            performHeal(troop, target);
        } else if (distance > HEAL_RANGE) {
            // Move closer to target
            troop.moveTowards(target.getCurrentPosition());
        }
    }
    
    /**
     * Perform attack between two troops
     */
    private void performAttack(TroopInstance2 attacker, TroopInstance2 target) {
        // Create attack context
        AttackContext attackContext = new AttackContext(
            attacker.getGameId(),
            (short) -1,             // Not a champion
            attacker.getIdAString(),
            (short) -1,             // Not a champion
            target.getIdAString(),
            attacker,
            target.getCurrentPosition(),
            target,
            System.currentTimeMillis(), // Current tick/time
            System.currentTimeMillis(), // Timestamp
            null                    // No extra data
        );
        
        // Get attack component and perform attack using the strategy pattern
        AttackComponent attackComponent = attacker.getAttackComponent();
        if (attackComponent != null && attackComponent.canAttack(attackContext.getCurrentTick())) {
            attackComponent.performAttack(attackContext);
            
            log.debug("Troop {} attacked {} for {} damage (target health: {}/{})", 
                    attacker.getIdAString(), target.getIdAString(), attacker.getDamage(),
                    target.getCurrentHP(), target.getMaxHP());
            
            // If target died, return to idle
            HealthComponent targetHealth = target.getHealthComponent();
            if (targetHealth != null && !targetHealth.isAlive()) {
                attacker.setCurrentTargetId(null);
                attacker.setAIState(TroopAIState.IDLE); // Note: method name is setAIState with capital AI
                troopManager.removeTroop(target.getGameId(), target.getIdAString());
                log.info("Troop {} killed by {}", target.getIdAString(), attacker.getIdAString());
            }
        }
    }
    
    /**
     * Perform healing
     */
    private void performHeal(TroopInstance2 healer, TroopInstance2 target) {
        // Use a fixed heal amount for healers (could be enhanced to use troop stats)
        int healAmount = 25; // Default heal amount for healers
        
        HealthComponent targetHealth = target.getHealthComponent();
        if (targetHealth != null) {
            targetHealth.increaseHP(healAmount);
            
            // Use attack component to start cooldown (similar to attack cooldown)
            AttackComponent healerAttack = healer.getAttackComponent();
            if (healerAttack != null) {
                // We're using the attack timestamp to track healing cooldown
                healerAttack.performAttack(new AttackContext(
                    healer.getGameId(),
                    (short) -1,
                    healer.getIdAString(),
                    (short) -1,
                    target.getIdAString(),
                    healer,
                    target.getCurrentPosition(),
                    target,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    new HashMap<>() // Empty extra data
                ));
            }
            
            log.debug("Healer {} healed {} for {} HP (target health: {}/{})", 
                    healer.getIdAString(), target.getIdAString(), healAmount,
                    target.getCurrentHP(), target.getMaxHP());
        }
    }
    
    /**
     * Apply attack modifiers based on troop types and abilities
     * 
     * Note: This method is no longer used after migrating to component-based architecture.
     * It's kept for reference as similar logic might be needed in TroopAttackStrategy.
     * 
     * @deprecated Use AttackComponent and TroopAttackStrategy instead
     */
    @Deprecated
    private int applyAttackModifiers(TroopInstance2 attacker, TroopInstance2 target, int baseDamage) {
        int modifiedDamage = baseDamage;
        
        // Axis: Extra damage to all
        if (attacker.getTroopType() == TroopEnum.AXIS) {
            modifiedDamage = (int)(baseDamage * 1.1f); // 10% more damage
        }
        
        // Shadow: Invisibility bonus damage
        if (attacker.getTroopType() == TroopEnum.SHADOW && attacker.isInvisible()) {
            modifiedDamage = (int)(baseDamage * 1.5f); // 50% more damage when invisible
            // Remove invisibility after attack by setting it to false
            attacker.setInvisible(false);
        }
        
        // Crossbawl: Long range precision
        if (attacker.getTroopType() == TroopEnum.CROSSBAWL) {
            float distance = attacker.distanceTo(target.getCurrentPosition());
            if (distance > 3.0f) {
                modifiedDamage = (int)(baseDamage * 1.2f); // 20% more damage at long range
            }
        }
        
        return modifiedDamage;
    }
    
    /**
     * Get attack range for troop type
     */
    private float getAttackRange(TroopEnum troopType) {
        return switch (troopType) {
            case CROSSBAWL -> 6.0f; // Long range
            case AXIS -> 2.0f; // Melee
            case SHADOW -> 2.5f; // Short melee
            case HEALER -> 1.5f; // Very short range
        };
    }
    
    /**
     * Check if troop AI should be updated based on timing
     */
    private boolean shouldUpdateAI(TroopInstance2 troop) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - troop.getLastAIUpdate()) >= AI_UPDATE_INTERVAL;
    }
    
    /**
     * Reset AI state for a troop
     */
    public void resetTroopAI(TroopInstance2 troop) {
        troop.setAIState(TroopAIState.IDLE);
        troop.setTargetTroopId(null);
        troop.setMoveTarget(null);
        log.debug("Reset AI state for troop {}", troop.getStringId());
    }
    
    /**
     * Force troop to attack specific target (for manual control)
     */
    public boolean setTroopTarget(String gameId, String troopId, String targetId) {
        TroopInstance2 troop = troopManager.getTroop(gameId, troopId);
        TroopInstance2 target = troopManager.getTroop(gameId, targetId);
        
        if (troop == null || target == null || !troop.isAlive() || !target.isAlive()) {
            return false;
        }
        
        // Can't attack friendly units (unless healer)
        if (troop.getOwnerSlot() == target.getOwnerSlot() && troop.getTroopType() != TroopEnum.HEALER) {
            return false;
        }
        
        troop.setTargetTroopId(targetId);
        if (troop.getTroopType() == TroopEnum.HEALER && troop.getOwnerSlot() == target.getOwnerSlot()) {
            troop.setAIState(TroopAIState.HEALING_ALLY);
        } else {
            troop.setAIState(TroopAIState.SEEKING);
        }
        
        log.info("Troop {} manually targeted {}", troopId, targetId);
        return true;
    }
}
