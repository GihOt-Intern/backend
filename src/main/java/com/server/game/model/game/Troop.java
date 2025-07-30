package com.server.game.model.game;


import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.PositionComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.AttackContext;
import com.server.game.model.game.component.attackComponent.AttackedContext;
import com.server.game.model.game.component.attackComponent.TroopAttackStrategy;
import com.server.game.model.game.component.attributeComponent.TroopAttributeComponent;
import com.server.game.netty.handler.SocketSender;
import com.server.game.resource.model.TroopDB;
import com.server.game.util.TroopEnum;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;


@EqualsAndHashCode(callSuper=false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class Troop extends Entity {

    TroopEnum troopEnum;
    String UUID;

    @Delegate
    PositionComponent positionComponent;
    @Delegate
    TroopAttributeComponent attributeComponent;
    @Delegate
    HealthComponent healthComponent;
    @Delegate
    AttackComponent attackComponent;

    public Troop(TroopDB troopDB) {
        this.troopEnum = TroopEnum.fromShort(troopDB.getId());
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
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(PositionComponent.class, positionComponent);
        this.addComponent(TroopAttributeComponent.class, attributeComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }



    @Override // from Attackable implemented by Entity
    public void receiveAttack(AttackContext ctx) {

        // TODO
        // // 2. Process the attack and calculate damage
        // int attackerDamage = ctx.getAttacker().getDamage();
        // int myDefense = this.getDefense();
        // float actualDamage = attackerDamage * (100.0f / (100 * myDefense));
        // this.decreaseHP((int) actualDamage);

        // // 3. Send health update for the target
        // SocketSender.sendHealthUpdate(ctx, (int) actualDamage);
    }
}
