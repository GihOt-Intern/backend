package com.server.game.model.game;


import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.map.component.Vector2;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;


@EqualsAndHashCode(callSuper=false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public abstract class Building extends DependentEntity implements HasFixedPosition {

    final String dbId; // id of the building in the database
    final Vector2 position;

    final float width;
    final float length;
    final float rotate;

    final Integer defense;

    @Delegate
    final HealthComponent healthComponent;

    public Building(String stringId, SlotState ownerSlot, GameState gameState,
        Integer hp, Integer defense,
        String dbId, Vector2 initPosition,
        float width, float length, float rotate) {

        super(stringId, gameState, ownerSlot);

        this.dbId = dbId;
        this.position = initPosition;

        this.defense = defense;

        this.width = width;
        this.length = length;
        this.rotate = rotate;

        this.healthComponent = new HealthComponent(hp);

        this.addAllComponents();
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(HealthComponent.class, this.healthComponent);
    }
}
