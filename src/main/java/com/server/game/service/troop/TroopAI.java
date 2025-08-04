// package com.server.game.service.troop;

// import com.server.game.model.map.component.Vector2;
// import com.server.game.resource.service.TroopService;
// import com.server.game.service.troop.TroopInstance.TroopAIState;
// import com.server.game.util.TroopEnum;

// import lombok.AccessLevel;
// import lombok.RequiredArgsConstructor;
// import lombok.experimental.FieldDefaults;
// import lombok.extern.slf4j.Slf4j;

// import org.springframework.scheduling.annotation.Async;
// import org.springframework.stereotype.Service;

// import java.util.List;
// import java.util.concurrent.CompletableFuture;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// public class TroopAI {
    
//     TroopManager troopManager;
//     TroopService troopService;
    
//     // AI Constants
//     private static final float SIGHT_RANGE = 8.0f;
//     private static final float HEAL_RANGE = 6.0f;
//     private static final float HEAL_THRESHOLD = 0.7f; // Heal when below 70% health
//     private static final float RETREAT_THRESHOLD = 0.3f; // Retreat when below 30% health
//     private static final long AI_UPDATE_INTERVAL = 1000; // 1 second
    
//     /**
//      * Process AI for all troops in a game
//      */
//     @Async
//     public CompletableFuture<Void> processGameAI(String gameId) {
//         try {
//             var troops = troopManager.getGameTroops(gameId);
            
//             for (TroopInstance troop : troops) {
//                 if (troop.isAlive() && shouldUpdateAI(troop)) {
//                     processIndividualTroopAI(troop);
//                 }
//             }
//         } catch (Exception e) {
//             log.error("Error processing AI for game {}: {}", gameId, e.getMessage(), e);
//         }
        
//         return CompletableFuture.completedFuture(null);
//     }
    
//     /**
//      * Process AI for a single troop
//      */
//     public void processIndividualTroopAI(TroopInstance troop) {
//         try {
//             // Skip if troop is not alive or is manually controlled
//             if (!troop.isAlive() || troop.getAIState() == TroopAIState.MOVING_TO_POSITION) {
//                 return;
//             }
            
//             // Update last AI processing time
//             troop.updateLastAIUpdate();
            
//             // Handle different AI states
//             switch (troop.getAIState()) {
//                 case IDLE:
//                     handleIdleState(troop);
//                     break;
//                 case SEEKING:
//                     handleSeekingState(troop);
//                     break;
//                 case ATTACKING:
//                     handleAttackingState(troop);
//                     break;
//                 case RETREATING:
//                     handleRetreatingState(troop);
//                     break;
//                 case HEALING_ALLY:
//                     handleHealingState(troop);
//                     break;
//                 default:
//                     // Unknown state, reset to idle
//                     troop.setAIState(TroopAIState.IDLE);
//                     break;
//             }
            
//         } catch (Exception e) {
//             log.error("Error processing AI for troop {}: {}", troop.getTroopInstanceId(), e.getMessage(), e);
//         }
//     }
    
//     /**
//      * Handle IDLE state - look for targets or allies to help
//      */
//     private void handleIdleState(TroopInstance troop) {
//         String gameId = troop.getGameId();
//         short ownerSlot = troop.getOwnerSlot();
//         Vector2 position = troop.getPosition();
//         TroopEnum troopType = troop.getTroopType();
        
//         // Check if troop needs to retreat
//         if (troop.getHealthPercentage() < RETREAT_THRESHOLD) {
//             troop.setAIState(TroopAIState.RETREATING);
//             log.debug("Troop {} entering retreat state (health: {}%)", 
//                     troop.getTroopInstanceId(), (int)(troop.getHealthPercentage() * 100));
//             return;
//         }
        
//         // Healer troops prioritize healing
//         if (troopType == TroopEnum.HEALER) {
//             List<TroopInstance> needsHealing = troopManager.findTroopsNeedingHealing(
//                     gameId, ownerSlot, position, HEAL_RANGE, HEAL_THRESHOLD);
            
//             if (!needsHealing.isEmpty()) {
//                 TroopInstance targetToHeal = needsHealing.get(0); // Heal lowest health troop
//                 troop.setTargetTroopId(targetToHeal.getTroopInstanceId());
//                 troop.setAIState(TroopAIState.HEALING_ALLY);
//                 log.debug("Healer {} targeting {} for healing", 
//                         troop.getTroopInstanceId(), targetToHeal.getTroopInstanceId());
//                 return;
//             }
//         }
        
//         // Look for enemy troops to attack
//         TroopInstance nearestEnemy = troopManager.findNearestEnemyTroop(
//                 gameId, ownerSlot, position, SIGHT_RANGE);
        
//         if (nearestEnemy != null) {
//             troop.setTargetTroopId(nearestEnemy.getTroopInstanceId());
//             troop.setAIState(TroopAIState.SEEKING);
//             log.debug("Troop {} found enemy target {} at distance {}", 
//                     troop.getTroopInstanceId(), nearestEnemy.getTroopInstanceId(),
//                     String.format("%.2f", troop.distanceTo(nearestEnemy.getPosition())));
//         }
//     }
    
//     /**
//      * Handle SEEKING state - move towards target
//      */
//     private void handleSeekingState(TroopInstance troop) {
//         String targetId = troop.getTargetTroopId();
//         if (targetId == null) {
//             troop.setAIState(TroopAIState.IDLE);
//             return;
//         }
        
//         TroopInstance target = troopManager.getTroop(troop.getGameId(), targetId);
//         if (target == null || !target.isAlive()) {
//             troop.setTargetTroopId(null);
//             troop.setAIState(TroopAIState.IDLE);
//             log.debug("Troop {} lost target, returning to idle", troop.getTroopInstanceId());
//             return;
//         }
        
//         float distance = troop.distanceTo(target.getPosition());
//         float attackRange = getAttackRange(troop.getTroopType());
        
//         // If within attack range, switch to attacking
//         if (distance <= attackRange && troop.canAttack()) {
//             troop.setAIState(TroopAIState.ATTACKING);
//             log.debug("Troop {} in range to attack target {}", 
//                     troop.getTroopInstanceId(), targetId);
//         } else {
//             // Move towards target
//             troop.moveTowards(target.getPosition());
//             log.debug("Troop {} moving towards target {} (distance: {})", 
//                     troop.getTroopInstanceId(), targetId, String.format("%.2f", distance));
//         }
//     }
    
//     /**
//      * Handle ATTACKING state - attack the target
//      */
//     private void handleAttackingState(TroopInstance troop) {
//         String targetId = troop.getTargetTroopId();
//         if (targetId == null) {
//             troop.setAIState(TroopAIState.IDLE);
//             return;
//         }
        
//         TroopInstance target = troopManager.getTroop(troop.getGameId(), targetId);
//         if (target == null || !target.isAlive()) {
//             troop.setTargetTroopId(null);
//             troop.setAIState(TroopAIState.IDLE);
//             log.debug("Troop {} target destroyed, returning to idle", troop.getTroopInstanceId());
//             return;
//         }
        
//         float distance = troop.distanceTo(target.getPosition());
//         float attackRange = getAttackRange(troop.getTroopType());
        
//         // If still in range and can attack, perform attack
//         if (distance <= attackRange && troop.canAttack()) {
//             performAttack(troop, target);
//         } else {
//             // Target moved out of range, go back to seeking
//             troop.setAIState(TroopAIState.SEEKING);
//             log.debug("Troop {} target out of range, seeking", troop.getTroopInstanceId());
//         }
//     }
    
//     /**
//      * Handle RETREATING state - move away from enemies
//      */
//     private void handleRetreatingState(TroopInstance troop) {
//         String gameId = troop.getGameId();
//         short ownerSlot = troop.getOwnerSlot();
//         Vector2 position = troop.getPosition();
        
//         // If health is restored, return to idle
//         if (troop.getHealthPercentage() >= HEAL_THRESHOLD) {
//             troop.setAIState(TroopAIState.IDLE);
//             log.debug("Troop {} health restored, returning to idle", troop.getTroopInstanceId());
//             return;
//         }
        
//         // Find nearest enemy to flee from
//         TroopInstance nearestEnemy = troopManager.findNearestEnemyTroop(
//                 gameId, ownerSlot, position, SIGHT_RANGE);
        
//         if (nearestEnemy != null) {
//             // Calculate retreat direction (opposite of enemy)
//             Vector2 enemyPos = nearestEnemy.getPosition();
//             Vector2 retreatDirection = new Vector2(
//                     position.x() - enemyPos.x(),
//                     position.y() - enemyPos.y()
//             ).normalize();
            
//             Vector2 retreatTarget = new Vector2(
//                     position.x() + retreatDirection.x() * 3.0f,
//                     position.y() + retreatDirection.y() * 3.0f
//             );
            
//             troop.moveTowards(retreatTarget);
//             log.debug("Troop {} retreating from enemy {}", 
//                     troop.getTroopInstanceId(), nearestEnemy.getTroopInstanceId());
//         } else {
//             // No enemies nearby, return to idle
//             troop.setAIState(TroopAIState.IDLE);
//         }
//     }
    
//     /**
//      * Handle HEALING_ALLY state - heal friendly troops
//      */
//     private void handleHealingState(TroopInstance troop) {
//         if (troop.getTroopType() != TroopEnum.HEALER) {
//             troop.setAIState(TroopAIState.IDLE);
//             return;
//         }
        
//         String targetId = troop.getTargetTroopId();
//         if (targetId == null) {
//             troop.setAIState(TroopAIState.IDLE);
//             return;
//         }
        
//         TroopInstance target = troopManager.getTroop(troop.getGameId(), targetId);
//         if (target == null || !target.isAlive() || target.getHealthPercentage() >= HEAL_THRESHOLD) {
//             troop.setTargetTroopId(null);
//             troop.setAIState(TroopAIState.IDLE);
//             log.debug("Healer {} finished healing target", troop.getTroopInstanceId());
//             return;
//         }
        
//         float distance = troop.distanceTo(target.getPosition());
        
//         if (distance <= HEAL_RANGE && troop.canAttack()) { // Use attack cooldown for heal cooldown
//             performHeal(troop, target);
//         } else if (distance > HEAL_RANGE) {
//             // Move closer to target
//             troop.moveTowards(target.getPosition());
//         }
//     }
    
//     /**
//      * Perform attack between two troops
//      */
//     private void performAttack(TroopInstance attacker, TroopInstance target) {
//         int damage = troopService.calculateTroopDamage(attacker.getTroopType());
        
//         // Apply special abilities based on troop type
//         damage = applyAttackModifiers(attacker, target, damage);
        
//         // Perform the attack
//         target.takeDamage(damage);
//         attacker.resetAttackCooldown(); // Start cooldown
        
//         log.debug("Troop {} attacked {} for {} damage (target health: {}/{})", 
//                 attacker.getTroopInstanceId(), target.getTroopInstanceId(), damage,
//                 target.getCurrentHP(), target.getMaxHP());
        
//         // If target died, return to idle
//         if (!target.isAlive()) {
//             attacker.setTargetTroopId(null);
//             attacker.setAIState(TroopAIState.IDLE);
//             troopManager.removeTroop(target.getGameId(), target.getTroopInstanceId());
//             log.info("Troop {} killed by {}", target.getTroopInstanceId(), attacker.getTroopInstanceId());
//         }
//     }
    
//     /**
//      * Perform healing
//      */
//     private void performHeal(TroopInstance healer, TroopInstance target) {
//         // Use a fixed heal amount for healers (could be enhanced to use troop stats)
//         int healAmount = 25; // Default heal amount for healers
        
//         target.heal(healAmount);
//         healer.resetAttackCooldown(); // Use attack cooldown as heal cooldown
        
//         log.debug("Healer {} healed {} for {} HP (target health: {}/{})", 
//                 healer.getTroopInstanceId(), target.getTroopInstanceId(), healAmount,
//                 target.getCurrentHP(), target.getMaxHP());
//     }
    
//     /**
//      * Apply attack modifiers based on troop types and abilities
//      */
//     private int applyAttackModifiers(TroopInstance attacker, TroopInstance target, int baseDamage) {
//         int modifiedDamage = baseDamage;
        
//         // Axis: Extra damage to all
//         if (attacker.getTroopType() == TroopEnum.AXIS) {
//             modifiedDamage = (int)(baseDamage * 1.1f); // 10% more damage
//         }
        
//         // Shadow: Invisibility bonus damage
//         if (attacker.getTroopType() == TroopEnum.SHADOW && attacker.isInvisible()) {
//             modifiedDamage = (int)(baseDamage * 1.5f); // 50% more damage when invisible
//             // Remove invisibility after attack by setting it to false
//             attacker.setInvisible(false);
//         }
        
//         // Crossbawl: Long range precision
//         if (attacker.getTroopType() == TroopEnum.CROSSBAWL) {
//             float distance = attacker.distanceTo(target.getPosition());
//             if (distance > 3.0f) {
//                 modifiedDamage = (int)(baseDamage * 1.2f); // 20% more damage at long range
//             }
//         }
        
//         return modifiedDamage;
//     }
    
//     /**
//      * Get attack range for troop type
//      */
//     private float getAttackRange(TroopEnum troopType) {
//         return switch (troopType) {
//             case CROSSBAWL -> 6.0f; // Long range
//             case AXIS -> 2.0f; // Melee
//             case SHADOW -> 2.5f; // Short melee
//             case HEALER -> 1.5f; // Very short range
//         };
//     }
    
//     /**
//      * Check if troop AI should be updated based on timing
//      */
//     private boolean shouldUpdateAI(TroopInstance troop) {
//         long currentTime = System.currentTimeMillis();
//         return (currentTime - troop.getLastAIUpdate()) >= AI_UPDATE_INTERVAL;
//     }
    
//     /**
//      * Reset AI state for a troop
//      */
//     public void resetTroopAI(TroopInstance troop) {
//         troop.setAIState(TroopAIState.IDLE);
//         troop.setTargetTroopId(null);
//         troop.setMoveTarget(null);
//         log.debug("Reset AI state for troop {}", troop.getTroopInstanceId());
//     }
    
//     /**
//      * Force troop to attack specific target (for manual control)
//      */
//     public boolean setTroopTarget(String gameId, String troopId, String targetId) {
//         TroopInstance troop = troopManager.getTroop(gameId, troopId);
//         TroopInstance target = troopManager.getTroop(gameId, targetId);
        
//         if (troop == null || target == null || !troop.isAlive() || !target.isAlive()) {
//             return false;
//         }
        
//         // Can't attack friendly units (unless healer)
//         if (troop.getOwnerSlot() == target.getOwnerSlot() && troop.getTroopType() != TroopEnum.HEALER) {
//             return false;
//         }
        
//         troop.setTargetTroopId(targetId);
//         if (troop.getTroopType() == TroopEnum.HEALER && troop.getOwnerSlot() == target.getOwnerSlot()) {
//             troop.setAIState(TroopAIState.HEALING_ALLY);
//         } else {
//             troop.setAIState(TroopAIState.SEEKING);
//         }
        
//         log.info("Troop {} manually targeted {}", troopId, targetId);
//         return true;
//     }
// }
