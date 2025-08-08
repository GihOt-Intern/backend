package com.server.game.model.game;

import com.server.game.model.game.attackStrategy.TroopAttackStrategy;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.MovingComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.SkillReceiver;
import com.server.game.model.game.component.attributeComponent.TroopAttributeComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.TroopDB;
import com.server.game.service.move.MoveService2;
import com.server.game.util.TroopEnum;

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
public class TroopInstance2 extends SkillReceiver {

    // This field below is inherited from Entity
    // private final String stringId; 

    final TroopEnum troopEnum;

    // Three fields below have been inherited from Entity
    // private final short ownerSlot;
    // private final GameState gameState;

    Vector2 targetPosition;

    @Delegate
    final TroopAttributeComponent attributeComponent;
    @Delegate
    final MovingComponent movingComponent;
    @Delegate
    final HealthComponent healthComponent;
    @Delegate
    final AttackComponent attackComponent;
    
    // private String currentTargetId; // Can be another troop instance ID or player slot as string
    private Entity stickEntity; // Can be another troop instance or champion

    Vector2 defensePosition;
    float defenseRange;
    boolean inDefensiveStance = true;
    Entity defensiveTarget = null;

    public TroopInstance2(TroopDB troopDB, GameState gameState, SlotState ownerSlot, MoveService2 moveService) {
        super("troop_" + UUID.randomUUID().toString(),
            ownerSlot, gameState);

        this.troopEnum = TroopEnum.fromShort(troopDB.getId());

        this.attributeComponent = new TroopAttributeComponent(
            troopDB.getStats().getDefense(),
            troopDB.getStats().getDetectionRange(),
            troopDB.getStats().getHealingPower(),
            troopDB.getStats().getHealingRange(),
            troopDB.getStats().getCost()
        );
        System.out.println("TroopInstance2 created with attributes: " + attributeComponent + " and enum: " + troopEnum);

        this.movingComponent = new MovingComponent(
            this,
            gameState.getSpawnPosition(ownerSlot),
            troopDB.getStats().getMoveSpeed()
        );

        this.healthComponent = new HealthComponent(
            troopDB.getStats().getHp()
        );
        this.attackComponent = new AttackComponent(
            this,
            troopDB.getStats().getAttack(),
            troopDB.getStats().getAttackSpeed(),
            troopDB.getStats().getAttackRange(),
            new TroopAttackStrategy(),
            moveService
        );

        this.defenseRange = this.attributeComponent.getDetectionRange() * 1.5f;

        this.addAllComponents();

        log.debug("Created troop instance {} of type {} for player {} of gameid={}, at position {}",
            stringId, troopEnum, ownerSlot, gameState.getGameId(), this.getCurrentPosition());
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(TroopAttributeComponent.class, attributeComponent);
        this.addComponent(MovingComponent.class, movingComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }

    
    @Override
    public void beforeUpdatePosition() {
        super.beforeUpdatePosition();
    }
    /**
     * Updates the defense position. This does NOT automatically enable defensive stance.
     * Defensive stance should be managed separately based on context availability.
     */
    public void updateDefensePosition(Vector2 newPosition) {
        this.defensePosition = newPosition;
        this.defensiveTarget = null; // Clear previous target
        this.attackComponent.setAttackContext(null); // Stop any current attack
        log.debug("Troop {} defense position updated to {}.", stringId, defensePosition);
    }

    /**
     * Checks if a target is within the troop's defense range (use detection range for consistency)
     */
    public boolean isWithinDefenseRange(Entity target) {
        if (target == null || defensePosition == null) {
            return false;
        }
        // Use detection range instead of defense range for consistency
        return defensePosition.distance(target.getCurrentPosition()) <= this.getDetectionRange();
    }

    /**
     * Checks if the troop itself is within its defense range (circle around defense position).
     */
    public boolean isWithinOwnDefenseRange() {
        if (defensePosition == null) {
            return true; // If no defense position set, consider it in range
        }
        return getCurrentPosition().distance(defensePosition) <= defenseRange;
    }

    /**
     * Checks if the troop has any active manual commands (move or attack contexts)
     */
public boolean hasActiveManualCommands() {
    // Only consider it manual if it's not a defensive action
    boolean hasManualAttack = this.attackComponent.isAttacking() && !inDefensiveStance;
    boolean hasManualMove = this.movingComponent.isMoving() && !inDefensiveStance;
    return hasManualAttack || hasManualMove;
}

    /**
     * Automatically enable defensive stance if no manual commands are active
     */
    public void checkAndEnableDefensiveStance() {
        if (!hasActiveManualCommands() && !inDefensiveStance) {
            this.inDefensiveStance = true;
            log.debug("Troop {} re-enabled defensive stance (no active manual commands)", stringId);
        }
    }

    @Override // from Attackable implemented by Entity
    public boolean receiveAttack(AttackContext ctx) {

        int actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);
        log.debug(stringId + " received attack from " + ctx.getAttacker().getStringId() +
            " with actual damage: " + actualDamage);

        ctx.addExtraData("actualDamage", actualDamage);
        ctx.getGameStateService().sendHealthUpdate(ctx.getGameId(), ctx.getTarget(), ctx.getActualDamage(), System.currentTimeMillis());

        // Check if troop died and handle death logic
        if (this.getCurrentHP() <= 0) {
            // Note: TroopManager.checkAndHandleAllTroopDeaths() will handle the cleanup in the next game tick
            log.info("Troop {} has died and will be cleaned up in next game tick", this.getStringId());
        }

        return true; // Indicate that the attack was received successfully
    }

    @Override
    public void afterUpdatePosition() {
        // Check if troop is in defensive stance and has moved outside defense range
        if (inDefensiveStance && defensePosition != null && !isWithinOwnDefenseRange()) {
            // If troop has no target or target is no longer valid, return to defense position
            if (defensiveTarget == null || !defensiveTarget.isAlive() || !isWithinDefenseRange(defensiveTarget)) {
                // Clear any current attack and return to defense position
                this.attackComponent.setAttackContext(null);
                this.defensiveTarget = null;
                
                // Set movement back to defense position
                // Note: We need access to MoveService2 to do this properly
                // This will be handled by DefensiveStanceService in the next tick
                log.trace("Troop {} is outside defense range. Will return to defense position {} in next tick.", 
                    stringId, defensePosition);
            }
        }
        
        super.afterUpdatePosition();
    }

    @Override
    public void receiveSkillDamage(CastSkillContext ctx) {
        // Process the skill damage and calculate actual damage
        int actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);
        
        // Send health update for the target
        ctx.addActualDamage(actualDamage);
        ctx.getGameStateService().sendHealthUpdate(
            ctx.getGameId(), this, actualDamage, ctx.getTimestamp());
        
        log.debug("Troop {} received skill damage: {}, current HP: {}", 
                stringId, actualDamage, this.getCurrentHP());

        // Check if troop died and handle death logic
        if (this.getCurrentHP() <= 0) {
            // Note: TroopManager.checkAndHandleAllTroopDeaths() will handle the cleanup in the next game tick
            log.info("Troop {} has died and will be cleaned up in next game tick", this.getStringId());
        }
    }
}