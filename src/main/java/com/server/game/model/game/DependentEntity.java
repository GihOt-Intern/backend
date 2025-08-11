package com.server.game.model.game;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

// abstract class to represent an entity that depends on a slot state
// (Champion, Troop, Tower, Burg)
@Getter
@Slf4j
public abstract class DependentEntity extends Entity {

    protected final SlotState ownerSlot;

    private final Map<Class<?>, Object> components = new HashMap<>();

    public DependentEntity(String stringId, GameState gameState, SlotState ownerSlot) {
        super(stringId, gameState);
        this.ownerSlot = ownerSlot;
    }

}
