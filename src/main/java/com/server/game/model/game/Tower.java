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

    public Tower(SlotState ownerSlot, GameState gameState, Integer hp, TowerDB towerDB) {
        super("tower_" + ownerSlot.getSlot() + UUID.randomUUID().toString(),
        ownerSlot, gameState, hp, towerDB.getId(), towerDB.getPosition());

        this.attackComponent = new AttackComponent(
            this,
            100, // Example damage
            1.0f, // Example attack speed
            5.0f, // Example attack range
            new TowerAttackStrategy(),
            null
        );

        this.addAllComponents();
    }

    @Override
    public boolean receiveAttack(AttackContext ctx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveAttack'");
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(AttackComponent.class, this.attackComponent);
    }

    @Override
    protected void handleDeath(Entity killer) {
        // TODO: put handleDeath logic for Tower here
    }
}
