package com.server.game.model.game;


import java.util.UUID;

import com.server.game.model.game.attackStrategy.ChampionAttackStrategy;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attackComponent.SkillReceivable;
import com.server.game.model.game.component.attributeComponent.ChampionAttributeComponent;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.game.component.skillComponent.SkillFactory;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.ChampionDB;
import com.server.game.service.move.MoveService;
import com.server.game.service.move.MoveService2;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;


@EqualsAndHashCode(callSuper=false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class Champion extends Entity implements SkillReceivable {

    ChampionEnum championEnum;
    String name;
    String role;

    @Delegate
    ChampionAttributeComponent attributeComponent;
    @Delegate
    HealthComponent healthComponent;
    @Delegate
    SkillComponent skillComponent;
    @Delegate
    AttackComponent attackComponent;


    public Champion(ChampionDB championDB, SlotState ownerSlot, GameState gameState,
        SkillFactory skillFactory, MoveService2 moveService) {

        super("champion_" + UUID.randomUUID().toString(),
            ownerSlot, gameState,
            gameState.getSpawnPosition(ownerSlot),
            championDB.getStats().getMoveSpeed()
        );

        this.championEnum = ChampionEnum.fromShort(championDB.getId());
        this.name = championDB.getName();
        this.role = championDB.getRole();
        this.attributeComponent = new ChampionAttributeComponent(
            championDB.getStats().getDefense(),
            championDB.getStats().getResourceClaimingSpeed()
        );
        this.healthComponent = new HealthComponent(
            championDB.getStats().getInitHP()
        );
        this.skillComponent = skillFactory.createSkillFor(
            this,
            championDB.getAbility()
        );
        this.attackComponent = new AttackComponent(
            this,
            championDB.getStats().getAttack(),
            championDB.getStats().getAttackSpeed(),
            championDB.getStats().getAttackRange(),
            new ChampionAttackStrategy(),
            moveService
        );

        this.addAllComponents();
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(ChampionAttributeComponent.class, attributeComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(SkillComponent.class, skillComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }


    @Override
    protected void afterUpdatePosition(Vector2 newPosition) {
        
        this.checkInPlayGround();

        super.afterUpdatePosition(newPosition);
    }

    private void checkInPlayGround() {

        boolean nextInPlayGround = this.checkInPlayGround(
            this.getGameState().getGameMap().getPlayGround());

        System.out.println(">>> [Log in Champion.checkInPlayGround] " + this.stringId + " nextInPlayGround: " +
            nextInPlayGround + ", current inPlayGround: " + this.isInPlayground());

        if (nextInPlayGround != this.isInPlayground()) {
            this.toggleInPlaygroundFlag(); // Toggle the state
        
            this.getGameStateService()
                .sendInPlaygroundUpdateMessage(
                    this.getGameState(),
                    this.getOwnerSlot(),
                    this.isInPlayground()
                );
        }
    }


    @Override // from Attackable implemented by Entity
    public boolean receiveAttack(AttackContext ctx) {

        // 2. Process the attack and calculate damage
        int actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);

        // 3. Send health update for the target
        ctx.addActualDamage(actualDamage);
        ctx.getGameStateService().sendHealthUpdate(
            ctx.getGameId(), this, actualDamage, ctx.getTimestamp());

        return true; 
    }


    @Override // from SkillReceivable interface
    public void receiveSkillDamage(CastSkillContext ctx) {
        // 2. Process the skill damage and calculate actual damage
        Integer actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);
        
        // 3. Send health update for the target
        ctx.addActualDamage(actualDamage);
        ctx.getGameStateService().sendHealthUpdate(
            ctx.getGameId(), this, actualDamage, ctx.getTimestamp());
    }

    public void updateCastSkill() {
        this.skillComponent.update();
    }
}
