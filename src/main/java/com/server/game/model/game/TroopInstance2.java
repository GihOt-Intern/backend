package com.server.game.model.game;

import com.server.game.config.SpringContextHolder;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.PositionComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.AttackContext;
import com.server.game.model.game.component.attackComponent.TroopAttackStrategy;
import com.server.game.model.game.component.attributeComponent.TroopAttributeComponent;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.TroopDB;
import com.server.game.resource.service.TroopService;
import com.server.game.util.TroopEnum;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TroopInstance2 extends Entity {

    private final String stringId;
    private final TroopEnum troopEnum;

    // Two field above have been inherited from Entity
    // private final short ownerSlot;
    // private final GameState gameState;


    @Delegate
    PositionComponent positionComponent;
    Vector2 targetPosition;

    @Delegate
    TroopAttributeComponent attributeComponent;
    @Delegate
    HealthComponent healthComponent;
    @Delegate
    AttackComponent attackComponent;


    // AI state
    private TroopAIState aiState = TroopAIState.IDLE;
    
    private String currentTargetId; // Can be another troop instance ID or player slot as string
    private Entity stickEntity; // Can be another troop instance or champion

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


    public TroopInstance2(TroopCreateContext ctx) {
        super(ctx.getOwnerSlot(), ctx.getGameState(), ctx.getGameId());

        this.stringId = "troop_" + UUID.randomUUID().toString();
        this.troopEnum = ctx.getTroopEnum();

        TroopDB troopDB = SpringContextHolder.getBean(TroopService.class)
            .getTroopDBById(troopEnum);


        this.positionComponent = new PositionComponent(
            this.gameState.getSpawnPosition(this.ownerSlot)
        );

        this.attributeComponent = new TroopAttributeComponent(
            troopDB.getStats().getDefense(),
            troopDB.getStats().getMoveSpeed(),
            troopDB.getStats().getAttackRange(),
            troopDB.getStats().getDetectionRange(),
            troopDB.getStats().getHealingPower(),
            troopDB.getStats().getHealingRange(),
            troopDB.getStats().getCost()
        );

        this.healthComponent = new HealthComponent(
            troopDB.getStats().getHp()
        );
        this.attackComponent = new AttackComponent(
            this,
            troopDB.getStats().getAttack(),
            troopDB.getStats().getAttackSpeed(),
            new TroopAttackStrategy()
        );

        this.addAllComponents();



        this.lastAIUpdate = System.currentTimeMillis();
        this.stateChangeTime = System.currentTimeMillis();

        log.debug("Created troop instance {} of type {} for player {} of gameid={}, at position {}",
            stringId, troopEnum, ownerSlot, gameState.getGameId(), positionComponent.getCurrentPosition());
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(PositionComponent.class, positionComponent);
        this.addComponent(TroopAttributeComponent.class, attributeComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }

    @Override
    public String getIdAString() { return this.getStringId(); }



    @Override // from Attackable implemented by Entity
    public void receiveAttack(AttackContext ctx) {
        if (!isAlive()) {
            return;
        }

        // Process the attack and calculate damage
        int attackerDamage = ctx.getAttacker().getComponent(AttackComponent.class).getDamage();
        int myDefense = this.getDefense();
        
        // Calculate actual damage with defense
        float defenseMultiplier = this.isBuffed ? this.defenseMultiplier : 1.0f;
        int actualDamage = Math.max(1, (int)(attackerDamage * (100.0f / (100 + myDefense * defenseMultiplier))));
        
        // Apply damage using health component
        this.getHealthComponent().takeDamage(actualDamage);
        
        log.debug("Troop {} received {} damage (defense: {}, actual damage: {})", 
                this.getStringId(), attackerDamage, myDefense, actualDamage);
                
        // Send health update via the appropriate channel or manager
        // This will be handled by TroopManager.applyDamageToTroop instead
    }

    
    

    
    /**
     * Apply damage to this troop
     */
    /**
     * @deprecated See {@link TroopInstance2#receiveAttack(AttackContext)} instead
     */
    @Deprecated
    public void takeDamage(int damage) {
        if (!isAlive() || damage <= 0) {
            return;
        }
        
        // Apply defense multiplier if buffed
        float defMultiplier = this.isBuffed ? this.defenseMultiplier : 1.0f;
        int actualDamage = Math.max(1, (int) (damage / defMultiplier));
        
        // Use health component to apply damage
        int oldHP = this.getCurrentHP();
        this.healthComponent.takeDamage(actualDamage);
        
        // Update AI state if died
        if (this.getCurrentHP() == 0 && oldHP > 0) {
            this.setAIState(TroopAIState.DEAD);
            log.info("Troop {} died after taking {} damage", stringId, actualDamage);
        }
    }
    
    /**
     * Heal this troop
     */
    public void heal(int healAmount) {
        if (!this.isAlive() || healAmount <= 0) {
            return;
        }
        
        int oldHP = this.getCurrentHP();
        this.increaseHP(healAmount);

        log.debug("Troop {} healed for {} HP: {} -> {}", stringId, healAmount, oldHP, this.getCurrentHP());
    }
    
    
    /**
     * Check if troop can attack (cooldown check)
     */
    /**
     * @deprecated See {@link AttackComponent#canAttack(float)} instead
     */
    @Deprecated
    public Boolean canAttack(float attackSpeed) {
        // long currentTime = System.currentTimeMillis();
        // long attackCooldown = (long) (1000 / attackSpeed); // Convert attacks per second to milliseconds
        // return (currentTime - lastAttackTime) >= attackCooldown;
        return null;
    }
    
    /**
     * Record an attack
     */
    /**
     * @deprecated See {@link AttackComponent} instead
     */
    @Deprecated
    public void recordAttack() {
        // this.lastAttackTime = System.currentTimeMillis();
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
        log.debug("Troop {} became invisible for {} seconds", stringId, durationSeconds);
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
              stringId, damageMultiplier, defenseMultiplier, durationSeconds);
    }
    
    /**
     * Update time-based effects
     */
    public void updateEffects() {
        long currentTime = System.currentTimeMillis();
        
        // Check invisibility
        if (isInvisible && currentTime >= invisibilityEndTime) {
            isInvisible = false;
            log.debug("Troop {} invisibility ended", stringId);
        }
        
        // Check buffs
        if (isBuffed && currentTime >= buffEndTime) {
            isBuffed = false;
            damageMultiplier = 1.0f;
            defenseMultiplier = 1.0f;
            log.debug("Troop {} buff ended", stringId);
        }
    }
    

    /**
     * Calculate distance to another troop
     */
    /**
     * @deprecated See {@link Entity#distanceTo(Entity)} instead
     */
    @Deprecated
    public float distanceTo(TroopInstance2 other) {
        if (other == null) return Float.MAX_VALUE;
        return this.getCurrentPosition().distance(other.getCurrentPosition());
    }
    

    /**
     * Set AI state with logging
     */
    public void setAIState(TroopAIState newState) {
        if (this.aiState != newState) {
            log.debug("Troop {} AI state changed: {} -> {}", stringId, this.aiState, newState);
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
     * Move towards target position using ThetaStarPathfinder
     */
    public void moveTowards(Vector2 target, float moveSpeed, float deltaTime) {
        if (target == null || !this.isAlive()) return;

        // Get pathfinding service
        com.server.game.service.troop.TroopPathfindingService pathfindingService = 
            com.server.game.config.SpringContextHolder.getBean(com.server.game.service.troop.TroopPathfindingService.class);
        
        // Get current waypoint or calculate new path
        Vector2 currentWaypoint = pathfindingService.getNextWaypoint(this.getGameId(), this.getStringId());
        
        // If no waypoint exists, calculate a new path  
        if (currentWaypoint == null) {
            List<Vector2> path = pathfindingService.calculatePath(this.getGameState(), this, target);
            if (path.isEmpty()) {
                // If no path is found, fall back to direct movement
                directMoveTowards(target, moveSpeed, deltaTime);
                return;
            }
            currentWaypoint = path.get(0);
        }
        
        // Move towards current waypoint
        Vector2 currentPosition = this.getCurrentPosition();
        float distance = currentPosition.distance(currentWaypoint);
        
        if (distance <= 0.2f) { // Close enough to waypoint
            // Mark waypoint as reached
            pathfindingService.waypointReached(this.getGameId(), this.getStringId());
            
            // Get next waypoint
            Vector2 nextWaypoint = pathfindingService.getNextWaypoint(this.getGameId(), this.getStringId());
            
            if (nextWaypoint != null) {
                // Continue to next waypoint
                directMoveTowards(nextWaypoint, moveSpeed, deltaTime);
            } else {
                // Path completed or no more waypoints
                if (currentPosition.distance(target) <= 0.2f) {
                    // Reached final target
                    this.setCurrentPosition(target);
                    this.setStop();
                    this.targetPosition = null;
                } else {
                    // Need to recalculate path
                    List<Vector2> newPath = pathfindingService.calculatePath(this.getGameState(), this, target);
                    if (!newPath.isEmpty()) {
                        directMoveTowards(newPath.get(0), moveSpeed, deltaTime);
                    } else {
                        // No path found, try direct movement
                        directMoveTowards(target, moveSpeed, deltaTime);
                    }
                }
            }
        } else {
            // Continue moving towards current waypoint
            directMoveTowards(currentWaypoint, moveSpeed, deltaTime);
        }
    }
    
    /**
     * Direct movement towards a target (fallback method)
     */
    private void directMoveTowards(Vector2 target, float moveSpeed, float deltaTime) {
        if (target == null || !this.isAlive()) return;

        float distance = this.distanceTo(target);
        if (distance <= 0.1f) { // Close enough
            this.setCurrentPosition(target);
            this.setStop();
            this.targetPosition = null;
            return;
        }
        
        Vector2 currentPosition = this.getCurrentPosition();
        Vector2 dPosition = target.subtract(currentPosition);
        float moveDistance = moveSpeed * deltaTime;
        
        if (moveDistance >= distance) {
            // Will reach target this frame
            this.setCurrentPosition(target);
            this.setStop();
            this.targetPosition = null;
        } else {
            // Move towards target
            float ratio = moveDistance / distance;
            Vector2 newPosition = currentPosition.add(dPosition.multiply(ratio));
            this.setCurrentPosition(newPosition);
            this.setMove();
        }
    }
    
    /**
     * Set movement target
     */
    public void setMoveTarget(Vector2 target) {
        // Clear existing path when a new target is set
        if (target != null && (this.targetPosition == null || !target.equals(this.targetPosition))) {
            com.server.game.service.troop.TroopPathfindingService pathfindingService = 
                com.server.game.config.SpringContextHolder.getBean(com.server.game.service.troop.TroopPathfindingService.class);
            pathfindingService.clearTroopPath(this.getGameId(), this.getStringId());
        }
        
        this.targetPosition = target;
        this.setMoving(target != null);
    }
    
    /** 
     * Set moving towards a target
     */
    public void setTargetTroopId(String targetTroopId) {
        this.currentTargetId = targetTroopId;
    }
    
    // AI and target management
    public TroopAIState getAIState() {
        return aiState;
    }

    public TroopEnum getTroopType() {
        return troopEnum;
    }
    
    public String getStickEntityId() {
        return stickEntity.getIdAString();
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
    
    // public void resetAttackCooldown() {
    //     this.lastAttackTime = System.currentTimeMillis();
    // }
    
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
                stringId, troopEnum, ownerSlot, this.getCurrentHP(), this.getMaxHP(), 
                aiState, this.getCurrentPosition().x(), this.getCurrentPosition().y()
        );
    }
}
