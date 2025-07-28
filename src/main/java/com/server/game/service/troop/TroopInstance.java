package com.server.game.service.troop;

import com.server.game.map.component.Vector2;
import com.server.game.util.TroopEnum;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Getter
@Setter
public class TroopInstance {
    
    private final String troopInstanceId;
    private final TroopEnum troopType;
    private final short ownerSlot;
    private final String gameId;
    
    // Position and movement
    private Vector2 position;
    private Vector2 targetPosition;
    private boolean isMoving = false;
    
    // Health and combat
    private int currentHP;
    private final int maxHP;
    private boolean isDead = false;
    private long lastDamageTime = 0;
    private long lastAttackTime = 0;
    
    // AI state
    private TroopAIState aiState = TroopAIState.IDLE;
    private String currentTargetId; // Can be another troop instance ID or player slot as string
    private long lastAIUpdate = 0;
    private long stateChangeTime = 0;
    
    // Special states
    private boolean isInvisible = false;
    private long invisibilityEndTime = 0;
    private boolean isBuffed = false;
    private long buffEndTime = 0;
    private float damageMultiplier = 1.0f;
    private float defenseMultiplier = 1.0f;
    
    // Ability cooldown
    private long lastAbilityUse = 0;
    
    public TroopInstance(String gameId, TroopEnum troopType, short ownerSlot, Vector2 spawnPosition, int maxHP) {
        this.troopInstanceId = "troop_" + UUID.randomUUID().toString();
        this.gameId = gameId;
        this.troopType = troopType;
        this.ownerSlot = ownerSlot;
        this.position = spawnPosition;
        this.maxHP = maxHP;
        this.currentHP = maxHP;
        this.lastAIUpdate = System.currentTimeMillis();
        this.stateChangeTime = System.currentTimeMillis();
        
        log.debug("Created troop instance {} of type {} for player {} at position ({}, {})", 
                troopInstanceId, troopType, ownerSlot, position.x(), position.y());
    }
    
    /**
     * Apply damage to this troop
     */
    public void takeDamage(int damage) {
        if (isDead || damage <= 0) {
            return;
        }
        
        // Apply defense multiplier
        int actualDamage = Math.max(1, (int) (damage / defenseMultiplier));
        
        int oldHP = this.currentHP;
        this.currentHP = Math.max(0, this.currentHP - actualDamage);
        this.lastDamageTime = System.currentTimeMillis();
        
        if (this.currentHP == 0 && oldHP > 0) {
            this.isDead = true;
            this.aiState = TroopAIState.DEAD;
            log.info("Troop {} died after taking {} damage", troopInstanceId, actualDamage);
        }
        
        log.debug("Troop {} took {} damage: {} -> {}", troopInstanceId, actualDamage, oldHP, this.currentHP);
    }
    
    /**
     * Heal this troop
     */
    public void heal(int healAmount) {
        if (isDead || healAmount <= 0) {
            return;
        }
        
        int oldHP = this.currentHP;
        this.currentHP = Math.min(this.maxHP, this.currentHP + healAmount);
        
        log.debug("Troop {} healed for {} HP: {} -> {}", troopInstanceId, healAmount, oldHP, this.currentHP);
    }
    
    /**
     * Check if troop is alive
     */
    public boolean isAlive() {
        return !isDead && currentHP > 0;
    }
    
    /**
     * Get health percentage
     */
    public float getHealthPercentage() {
        return (float) currentHP / maxHP;
    }
    
    /**
     * Check if troop can attack (cooldown check)
     */
    public boolean canAttack(float attackSpeed) {
        long currentTime = System.currentTimeMillis();
        long attackCooldown = (long) (1000 / attackSpeed); // Convert attacks per second to milliseconds
        return (currentTime - lastAttackTime) >= attackCooldown;
    }
    
    /**
     * Record an attack
     */
    public void recordAttack() {
        this.lastAttackTime = System.currentTimeMillis();
    }
    
    /**
     * Check if ability is off cooldown
     */
    public boolean canUseAbility(float abilityCooldown) {
        long currentTime = System.currentTimeMillis();
        long cooldownMs = (long) (abilityCooldown * 1000);
        return (currentTime - lastAbilityUse) >= cooldownMs;
    }
    
    /**
     * Use ability
     */
    public void useAbility() {
        this.lastAbilityUse = System.currentTimeMillis();
    }
    
    /**
     * Apply temporary invisibility
     */
    public void applyInvisibility(float durationSeconds) {
        this.isInvisible = true;
        this.invisibilityEndTime = System.currentTimeMillis() + (long) (durationSeconds * 1000);
        log.debug("Troop {} became invisible for {} seconds", troopInstanceId, durationSeconds);
    }
    
    /**
     * Apply temporary buff
     */
    public void applyBuff(float damageMultiplier, float defenseMultiplier, float durationSeconds) {
        this.isBuffed = true;
        this.damageMultiplier = damageMultiplier;
        this.defenseMultiplier = defenseMultiplier;
        this.buffEndTime = System.currentTimeMillis() + (long) (durationSeconds * 1000);
        log.debug("Troop {} buffed with damage x{} and defense x{} for {} seconds", 
                troopInstanceId, damageMultiplier, defenseMultiplier, durationSeconds);
    }
    
    /**
     * Update time-based effects
     */
    public void updateEffects() {
        long currentTime = System.currentTimeMillis();
        
        // Check invisibility
        if (isInvisible && currentTime >= invisibilityEndTime) {
            isInvisible = false;
            log.debug("Troop {} invisibility ended", troopInstanceId);
        }
        
        // Check buffs
        if (isBuffed && currentTime >= buffEndTime) {
            isBuffed = false;
            damageMultiplier = 1.0f;
            defenseMultiplier = 1.0f;
            log.debug("Troop {} buff ended", troopInstanceId);
        }
    }
    
    /**
     * Calculate distance to another position
     */
    public float distanceTo(Vector2 targetPos) {
        if (targetPos == null) return Float.MAX_VALUE;
        
        float dx = position.x() - targetPos.x();
        float dy = position.y() - targetPos.y();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate distance to another troop
     */
    public float distanceTo(TroopInstance other) {
        if (other == null) return Float.MAX_VALUE;
        return distanceTo(other.getPosition());
    }
    
    /**
     * Set AI state with logging
     */
    public void setAIState(TroopAIState newState) {
        if (this.aiState != newState) {
            log.debug("Troop {} AI state changed: {} -> {}", troopInstanceId, this.aiState, newState);
            this.aiState = newState;
            this.stateChangeTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Get time since state change
     */
    public long getTimeSinceStateChange() {
        return System.currentTimeMillis() - stateChangeTime;
    }
    
    /**
     * Move towards target position
     */
    public void moveTowards(Vector2 target, float moveSpeed, float deltaTime) {
        if (target == null || isDead) return;
        
        float distance = distanceTo(target);
        if (distance <= 0.1f) { // Close enough
            this.position = target;
            this.isMoving = false;
            this.targetPosition = null;
            return;
        }
        
        // Calculate movement
        float dx = target.x() - position.x();
        float dy = target.y() - position.y();
        float moveDistance = moveSpeed * deltaTime;
        
        if (moveDistance >= distance) {
            // Will reach target this frame
            this.position = target;
            this.isMoving = false;
            this.targetPosition = null;
        } else {
            // Move towards target
            float ratio = moveDistance / distance;
            float newX = position.x() + dx * ratio;
            float newY = position.y() + dy * ratio;
            this.position = new Vector2(newX, newY);
            this.isMoving = true;
        }
    }
    
    /**
     * Set movement target
     */
    public void setMoveTarget(Vector2 target) {
        this.targetPosition = target;
        this.isMoving = target != null;
    }
    
    // AI and target management
    public TroopAIState getAIState() {
        return aiState;
    }
    
    public String getTargetTroopId() {
        return currentTargetId;
    }
    
    public void setTargetTroopId(String targetTroopId) {
        this.currentTargetId = targetTroopId;
    }
    
    public long getLastAIUpdate() {
        return lastAIUpdate;
    }
    
    public void updateLastAIUpdate() {
        this.lastAIUpdate = System.currentTimeMillis();
    }
    
    // Enhanced movement methods
    public void moveTowards(Vector2 target) {
        moveTowards(target, 1.0f, 0.1f); // Default speed and tolerance
    }
    
    public boolean canAttack() {
        return canAttack(1.0f); // Default attack speed
    }
    
    public void resetAttackCooldown() {
        this.lastAttackTime = System.currentTimeMillis();
    }
    
    public enum TroopAIState {
        IDLE,
        SEEKING,
        ATTACKING,
        RETREATING,
        HEALING_ALLY,
        MOVING_TO_POSITION,
        DEAD
    }
    
    @Override
    public String toString() {
        return String.format("TroopInstance{id='%s', type=%s, owner=%d, hp=%d/%d, state=%s, pos=(%.1f,%.1f)}", 
                troopInstanceId, troopType, ownerSlot, currentHP, maxHP, aiState, position.x(), position.y());
    }
}
