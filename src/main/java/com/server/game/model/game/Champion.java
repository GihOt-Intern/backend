package com.server.game.model.game;


import java.util.UUID;

import com.server.game.model.game.attackStrategy.ChampionAttackStrategy;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.MovingComponent;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.component.attributeComponent.ChampionAttributeComponent;
import com.server.game.model.game.component.skillComponent.DurationSkillComponent;
import com.server.game.model.game.component.skillComponent.SkillComponent;
import com.server.game.model.game.component.skillComponent.SkillFactory;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.context.CastSkillContext;
import com.server.game.resource.model.ChampionDB;
import com.server.game.service.move.MoveService2;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@EqualsAndHashCode(callSuper=false, exclude = "skillComponent")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Slf4j
public final class Champion extends DependentEntity implements SkillReceivable {

    final ChampionEnum championEnum;
    final String name;
    final String role;

    @Delegate
    final ChampionAttributeComponent attributeComponent;
    @Delegate
    final HealthComponent healthComponent;
    @Delegate
    final MovingComponent movingComponent;
    @Delegate
    final SkillComponent skillComponent;
    @Delegate
    final AttackComponent attackComponent;


    public Champion(ChampionDB championDB, SlotState ownerSlot, GameState gameState,
        SkillFactory skillFactory, MoveService2 moveService) {

        super("champion_" + UUID.randomUUID().toString(),
            gameState, ownerSlot);

        this.championEnum = ChampionEnum.fromShort(championDB.getId());
        this.name = championDB.getName();
        this.role = championDB.getRole();
        this.attributeComponent = new ChampionAttributeComponent(
            championDB.getStats().getDefense(),
            championDB.getStats().getGoldMineDamage()
        );
        this.movingComponent = new MovingComponent(
            this,
            gameState.getSpawnPosition(ownerSlot),
            championDB.getStats().getMoveSpeed()
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
        this.addComponent(MovingComponent.class, movingComponent);
        this.addComponent(HealthComponent.class, healthComponent);
        this.addComponent(SkillComponent.class, skillComponent);
        this.addComponent(AttackComponent.class, attackComponent);
    }


    @Override
    public void beforeUpdatePosition() {
        // log.info("Call beforeUpdatePosition for champion, call super method...");
        super.beforeUpdatePosition();
    }


    @Override
    public void afterUpdatePosition() {
        // log.info("Call afterUpdatePosition for champion, check in playground and call super method...");

        this.checkInPlayground();

        super.afterUpdatePosition();

    }

    private void checkInPlayground() {

        boolean nextInPlayground = this.checkInPlayground(
            this.getGameState().getGameMap().getPlayground());

        // System.out.println(">>> [Log in Champion.checkInPlayGround] " + this.stringId + " nextInPlayGround: " +
        //     nextInPlayGround + ", current inPlayGround: " + this.isInPlayground());

        if (nextInPlayground != this.isInPlayground()) {
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

        // 4. Check if champion died and handle death/respawn logic
        if (this.isAlive()) { return true; }


        this.handleDeath(ctx.getAttacker());

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
        
        if (this.isAlive()) { return; }
        
        this.handleDeath(ctx.getCaster());
    }


    @Override
    protected void handleDeath(Entity killer) {
        if (this.isAlive()) { return; }
        log.info("Champion {} is dead, handling death logic...", this.getName());

        this.getGameStateService().checkAndHandleChampionDeath(
                this.getGameState().getGameId(), this.getOwnerSlot().getSlot());


        this.stopAttacking();

        Integer stolenGold = Math.round(this.getCurrentGold()*0.3f);

        killer.increaseGold(stolenGold);
        this.decreaseGold(stolenGold);
    }


    public void useSkill(CastSkillContext ctx) {
        this.skillComponent.use(ctx);
    }

    public void updateDurationSkill() {
        if (this.skillComponent instanceof DurationSkillComponent durationSkillComponent) {
            // log.info("Updating duration skill for champion: {}", this.getName());
            durationSkillComponent.updatePerTick();
        }
    }
}
