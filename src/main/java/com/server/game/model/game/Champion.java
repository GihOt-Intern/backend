package com.server.game.model.game;


import com.server.game.model.game.component.GoldComponent;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.ChampionAttackStrategy;
import com.server.game.model.game.component.attributeComponent.ChampionAttributeComponent;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.game.component.skillComponent.SkillFactory;
import com.server.game.netty.handler.SocketSender;
import com.server.game.resource.model.ChampionDB;
import com.server.game.util.ChampionEnum;
import com.server.game.model.game.component.attackComponent.AttackContext;


import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;


@EqualsAndHashCode(callSuper=false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class Champion extends Entity {

    ChampionEnum championEnum;
    String name;
    String role;

    @Delegate
    ChampionAttributeComponent attributeComponent;
    @Delegate
    HealthComponent healthComponent;
    @Delegate
    GoldComponent goldComponent;
    @Delegate
    SkillComponent skillComponent;
    @Delegate
    AttackComponent attackComponent;

    public Champion(ChampionDB championDB) {
        this(championDB, null, null, null);
    }

    public Champion(ChampionDB championDB, Short ownerSlot, GameState gameState, String gameId) {
        super(ownerSlot, gameState, gameId);
        this.championEnum = ChampionEnum.fromShort(championDB.getId());
        this.name = championDB.getName();
        this.role = championDB.getRole();
        this.attributeComponent = new ChampionAttributeComponent(
            championDB.getStats().getDefense(),
            // championDB.getStats().getAttack(),
            championDB.getStats().getMoveSpeed(),
            // championDB.getStats().getAttackSpeed(),
            championDB.getStats().getAttackRange(),
            championDB.getStats().getResourceClaimingSpeed()
        );
        this.healthComponent = new HealthComponent(
            championDB.getStats().getInitHP()
        );
        this.skillComponent = SkillFactory.createSkillFor(
            this.championEnum,
            championDB.getAbility()
        );
        this.attackComponent = new AttackComponent(
            this,
            championDB.getStats().getAttack(),
            championDB.getStats().getAttackSpeed(),
            new ChampionAttackStrategy()
        );

        this.addAllComponents();
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(ChampionAttributeComponent.class, attributeComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(GoldComponent.class, goldComponent);
        this.addComponent(SkillComponent.class, skillComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }

    @Override
    public String getIdAString() { return this.getOwnerSlot().toString(); }


    @Override // from Attackable implemented by Entity
    public void receiveAttack(AttackContext ctx) {

        // 2. Process the attack and calculate damage
        int attackerDamage = ctx.getAttacker().getDamage();
        int myDefense = this.getDefense();
        float actualDamage = attackerDamage * (100.0f / (100 * myDefense));
        this.decreaseHP((int) actualDamage);

        // 3. Send health update for the target
        SocketSender.sendHealthUpdate(ctx, (int) actualDamage);
    }
}
