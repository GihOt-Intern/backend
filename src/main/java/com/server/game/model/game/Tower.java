package com.server.game.model.game;


import java.util.UUID;

import com.server.game.model.game.attackStrategy.TowerAttackStrategy;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.resource.model.SlotInfo.TowerDB;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;


@EqualsAndHashCode(callSuper=false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class Tower extends Building {

    @Delegate
    final AttackComponent attackComponent;

    public Tower(SlotState ownerSlot, GameState gameState, TowerDB towerDB) {
        super("tower_" + ownerSlot.getSlot() + UUID.randomUUID().toString(),
        ownerSlot, gameState, 
        gameState.getGameMap().getTowerHP(), 
        gameState.getGameMap().getTowerDefense(), 
        towerDB.getId(), towerDB.getPosition(),
        towerDB.getWidth(), towerDB.getLength(), towerDB.getRotate());

        this.attackComponent = new AttackComponent(
            this,
            gameState.getGameMap().getTowerAttack(),
            gameState.getGameMap().getTowerAttackSpeed(),
            gameState.getGameMap().getTowerAttackRange(), 
            new TowerAttackStrategy()
        );

        this.addAllComponents();
    }

    @Override
    public boolean receiveAttack(AttackContext ctx) {

        int actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);

        ctx.addExtraData("actualDamage", actualDamage);
        ctx.getGameStateService().sendHealthUpdate(ctx.getGameId(), ctx.getTarget(), ctx.getActualDamage(), System.currentTimeMillis());

        if (!this.isAlive()) {
            this.handleDeath(ctx.getAttacker());
            return true;
        }

        return true;
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(AttackComponent.class, this.attackComponent);
    }

    @Override
    protected void handleDeath(Entity killer) {
        this.getGameStateService().sendTowerDeathMessage(this.getGameId(), this);
        this.getGameStateService().removeEntity(this.getGameState(), this);
        this.stopAttacking();
    }
}
