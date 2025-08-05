package com.server.game.model.game;

import com.server.game.model.game.attackStrategy.TroopAttackStrategy;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attributeComponent.TroopAttributeComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.TroopDB;
import com.server.game.service.move.MoveService;
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
public class TroopInstance2 extends Entity {

    // This field below is inherited from Entity
    // private final String stringId; 

    private final TroopEnum troopEnum;

    // Three fields below have been inherited from Entity
    // private final short ownerSlot;
    // private final GameState gameState;
    // @Delegate
    // PositionComponent positionComponent;

    Vector2 targetPosition;

    @Delegate
    TroopAttributeComponent attributeComponent;
    @Delegate
    HealthComponent healthComponent;
    @Delegate
    AttackComponent attackComponent;
    
    // private String currentTargetId; // Can be another troop instance ID or player slot as string
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


    public TroopInstance2(TroopDB troopDB, GameState gameState, SlotState ownerSlot,
        MoveService moveService) {
        super("troop_" + UUID.randomUUID().toString(),
            ownerSlot, gameState,
            gameState.getSpawnPosition(ownerSlot), moveService);

        this.troopEnum = TroopEnum.fromShort(troopDB.getId());

        this.attributeComponent = new TroopAttributeComponent(
            troopDB.getStats().getDefense(),
            troopDB.getStats().getMoveSpeed(),
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
            troopDB.getStats().getAttackRange(),
            new TroopAttackStrategy()
        );

        this.addAllComponents();

        this.lastAIUpdate = System.currentTimeMillis();
        this.stateChangeTime = System.currentTimeMillis();

        log.debug("Created troop instance {} of type {} for player {} of gameid={}, at position {}",
            stringId, troopEnum, ownerSlot, gameState.getGameId(), this.getCurrentPosition());
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(TroopAttributeComponent.class, attributeComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }

   

    @Override // from Attackable implemented by Entity
    public boolean receiveAttack(AttackContext ctx) {

        int actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);
        log.debug(stringId + " received attack from " + ctx.getAttacker().getStringId() +
            " with actual damage: " + actualDamage);

        ctx.addExtraData("actualDamage", actualDamage);
        ctx.getGameStateService().sendHealthUpdate(ctx);

        return true; // Indicate that the attack was received successfully
    }

    @Override
    protected void afterUpdatePosition(Vector2 newPosition) {
        super.afterUpdatePosition(newPosition);
    }
}