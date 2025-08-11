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

    @Delegate
    final HealthComponent healthComponent;

    public Building(String stringId, SlotState ownerSlot, GameState gameState,
        Integer hp, String dbId, Vector2 initPosition,
        float width, float length, float rotate) {

        super(stringId, gameState, ownerSlot);

        this.dbId = dbId;
        this.position = initPosition;

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
    
    /**
     * Abstract method to calculate distance to another entity's boundary
     * Each building type should implement this based on their dimensions
     */
    // public final float distanceToEntityBoundary(Entity other)

    /**
     * Calculate the shortest distance from this tower's boundary to another entity's boundary
     * @param other the other entity
     * @return the shortest distance between boundaries
     */
    // public final float distanceToEntityBoundary(Entity other) {

    //     Vector2 otherPosition = other.getCurrentPosition();
    //     // For non-building entities (troops, champions), their dimensions are treated as zero
    //     Float otherWidth = 0f;
    //     Float otherLength = 0f;

    //     if (other instanceof Building otherBuilding) {
    //         otherWidth = otherBuilding.getWidth();
    //         otherLength = otherBuilding.getLength();
    //     }

    //     float dx = Math.max(0, 
    //         Math.abs(this.getPosition().x() - otherPosition.x()) - 
    //             (this.getWidth() + otherWidth) / 2);
    //     float dy = Math.max(0, 
    //         Math.abs(this.getPosition().y() - otherPosition.y()) - 
    //             (this.getLength() + otherLength) / 2);

    //     return (float) Math.sqrt(dx * dx + dy * dy);
    // }
}
