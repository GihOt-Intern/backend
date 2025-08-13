package com.server.game.model.game.building;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.server.game.model.game.Entity;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.context.AttackContext;
import com.server.game.resource.model.SlotInfo.BurgDB;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;


@EqualsAndHashCode(callSuper=false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class Burg extends Building {

    public Burg(SlotState ownerSlot, GameState gameState, BurgDB burgDB) {
        super("burg_" + ownerSlot.getSlot() + UUID.randomUUID().toString(),
        ownerSlot, gameState,
        gameState.getGameMap().getBurgHP(), 
        gameState.getGameMap().getBurgDefense(), 
        burgDB.getId(), burgDB.getPosition(),
        burgDB.getWidth(), burgDB.getLength(), burgDB.getRotate());
    }

    @Override
    public boolean receiveAttack(AttackContext ctx) {
        // Calculate the actual damage based on attacker's damage and burg's defense
        int actualDamage = (int) this.calculateActualDamage(ctx);
        
        // Decrease the burg's health
        this.decreaseHP(actualDamage);

        // Add the actual damage to the context for other components to use
        ctx.addActualDamage(actualDamage);
        
        // Send health update to clients
        ctx.getGameStateService().sendHealthUpdate(
            ctx.getGameId(), this, actualDamage, ctx.getTimestamp());

        // Check if burg is destroyed
        if (!this.isAlive()) {
            handleDeath(ctx.getAttacker());
            return true; // Burg is destroyed
        }
        
        return false; // Burg is still alive
    }

    /** 
     * Handle the case when the burg is destroyed
     */
    @Override
    protected void handleDeath(Entity killer) {
        GameState gameState = this.getGameState();
        SlotState ownerSlot = this.getOwnerSlot();
        // short destroyedSlot = ownerSlot.getSlot();

        List<String> removedEntityIds = new ArrayList<>();
        List<Entity> entitiesToRemove = new ArrayList<>();

        for(Entity entity : gameState.getEntities()) {
            if (ownerSlot.equals(entity.getOwnerSlot())) {
                removedEntityIds.add(entity.getStringId());
                entitiesToRemove.add(entity);
            }
        }

        this.getGameStateService().sendEntitiesRemoved(
            this.getGameId(), removedEntityIds, 
            killer.getAttackContext().getTimestamp());
        
        ownerSlot.setEliminated(true);

        for(Entity entity : entitiesToRemove) {
            gameState.removeEntity(entity);
        }

        gameState.decreaseNumSlotsAlive();


        // int remainingBurgs = 0;
        // short lastAliveBurgSlot = -1;

        // for(Entity entity : gameState.getEntities()) {
        //     if (entity instanceof Burg && entity.isAlive()) {
        //         remainingBurgs++;
        //         lastAliveBurgSlot = entity.getOwnerSlot().getSlot();
        //     }
        // }

        // if (remainingBurgs == 1) {

        if (gameState.isGameOver()) {
            this.getGameStateService().sendGameOver(
                this.getGameId(), 
                gameState.getWinnerSlot().getSlot(),
                ownerSlot.getSlot(), 
                killer.getAttackContext().getTimestamp(),
                this.getStringId()
            );
        }
    }
}
