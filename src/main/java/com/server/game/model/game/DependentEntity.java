package com.server.game.model.game;


import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

// abstract class to represent an entity that depends on a slot state
// (Champion, Troop, Tower, Burg)
@Getter
@Slf4j
public abstract class DependentEntity extends Entity {

    protected final SlotState ownerSlot;

    public DependentEntity(String stringId, GameState gameState, SlotState ownerSlot) {
        super(stringId, gameState);
        this.ownerSlot = ownerSlot;
    }

    public int getCurrentGold() {
        return this.ownerSlot.getCurrentGold();
    }

    @Override
    public void increaseGold(int amount) {
        // must increase gold through gameState to ensure proper handling 
        this.ownerSlot.getGameState().increaseGold(this.ownerSlot, amount);
    }

    @Override
    public void decreaseGold(int amount) {
        // must decrease gold through gameState to ensure proper handling
        this.ownerSlot.getGameState().decreaseGold(this.ownerSlot, amount);
    }
}
