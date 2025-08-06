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

        this.defenseRange = this.attributeComponent.getDetectionRange() * 2.0f;

        this.addAllComponents();

        log.debug("Created troop instance {} of type {} for player {} of gameid={}, at position {}",
            stringId, troopEnum, ownerSlot, gameState.getGameId(), this.getCurrentPosition());
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(TroopAttributeComponent.class, attributeComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }

    /**
     * Updates the defense position and re-enables the defensive stance.
     * This is called when a player manually moves the troop.
     */
    public void updateDefensePosition(Vector2 newPosition) {
        this.defensePosition = newPosition;
        this.inDefensiveStance = true;
        this.defensiveTarget = null; // Clear previous target
        this.attackComponent.setAttackContext(null); // Stop any current attack
        log.debug("Troop {} defense position updated to {}. Re-engaging defensive stance.", stringId, defensePosition);
    }

    /**
     * Checks if a target is within the troop's defense range.
     */
    public boolean isWithinDefenseRange(Entity target) {
        if (target == null || defensePosition == null) {
            return false;
        }
        return defensePosition.distance(target.getCurrentPosition()) <= defenseRange;
    }

    @Override // from Attackable implemented by Entity
    public boolean receiveAttack(AttackContext ctx) {

        int actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);
        log.debug(stringId + " received attack from " + ctx.getAttacker().getStringId() +
            " with actual damage: " + actualDamage);

        ctx.addExtraData("actualDamage", actualDamage);
        ctx.getGameStateService().sendHealthUpdate(ctx.getGameId(), ctx.getTarget(), ctx.getActualDamage(), System.currentTimeMillis());

        return true; // Indicate that the attack was received successfully
    }

    @Override
    public void afterUpdatePosition() {
        super.afterUpdatePosition();
    }

    @Override
    public void receiveSkillDamage(CastSkillContext ctx) {
        // TODO: Implement skill damage logic
    }
}