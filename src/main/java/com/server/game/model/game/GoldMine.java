package com.server.game.model.game;

import java.util.UUID;

import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.game.entityIface.HasFixedPosition;
import com.server.game.model.map.component.Vector2;
import com.server.game.service.gameState.GameStateService;

import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

// abstract class to represent an entity in the game
// (Champion, Troop, Tower, Burg)
@Getter
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class GoldMine extends Entity implements HasFixedPosition {

    int goldAmount;
    Vector2 position;
    @Delegate
    HealthComponent healthComponent;

    public GoldMine(GameState gameState, int goldAmount, int initHP, Vector2 position) {
        super("gold_mine_" + UUID.randomUUID().toString(), gameState);
        this.goldAmount = goldAmount;
        this.position = position;
        this.healthComponent = new HealthComponent(initHP);

        this.addAllComponents();
    }


    @Override
    protected void addAllComponents() {
        this.addComponent(HealthComponent.class, this.healthComponent);
    }

    @Override
    public boolean receiveAttack(AttackContext ctx) {
        Integer damage = ctx.getAttacker().getGoldMineDamage();
        this.decreaseHP(damage);
        log.info("GoldMine {} received {} damage from {}", this.getStringId(), damage, ctx.getAttacker().getStringId());
        
        ctx.getGameStateService().sendHealthUpdate(
            ctx.getGameId(), this, damage, ctx.getTimestamp());
        
        
        if (!this.isAlive()) {
            this.handleDeath(ctx.getAttacker());
        }

        return true;
    }


    @Override
    protected void handleDeath(Entity killer) {
        log.info("GoldMine {} is destroyed", this.getStringId());
        killer.increaseGold(this.goldAmount);

        GameStateService gameStateService = this.gameState.getGameStateService();

        gameStateService.sendEntityDeathMessage(
            this.gameState, this.getStringId());

        gameStateService.stopChampionsAttackingTo(this.gameState, this);

        this.getGameState().removeEntity(this);

        this.getGameState().decreaseCurrentNumGoldMine();
    }
}
