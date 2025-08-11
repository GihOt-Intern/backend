package com.server.game.model.game;


import java.util.UUID;

import com.server.game.model.game.attackStrategy.TowerAttackStrategy;
import com.server.game.model.game.component.attackComponent.AttackComponent;
import com.server.game.model.game.context.AttackContext;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.SlotInfo.TowerDB;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;


@EqualsAndHashCode(callSuper=false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class Tower extends Building {

    @Delegate
    final AttackComponent attackComponent;

    public Tower(SlotState ownerSlot, GameState gameState, Integer hp, TowerDB towerDB) {
        super("tower_" + ownerSlot.getSlot() + UUID.randomUUID().toString(),
        ownerSlot, gameState, hp, towerDB.getId(), towerDB.getPosition(),
        towerDB.getWidth(), towerDB.getLength(), towerDB.getRotate());

        this.attackComponent = new AttackComponent(
            this,
            100, // Example damage
            1.0f, // Example attack speed
            15.0f, // Example attack range
            new TowerAttackStrategy(),
            null
        );

        this.addAllComponents();
    }

    @Override
    public boolean receiveAttack(AttackContext ctx) {

        int actualDamage = (int) this.calculateActualDamage(ctx);
        this.decreaseHP(actualDamage);

        ctx.addExtraData("actualDamage", actualDamage);
        ctx.getGameStateService().sendHealthUpdate(ctx.getGameId(), ctx.getTarget(), ctx.getActualDamage(), System.currentTimeMillis());

        if (!this.isAlive()) {
            this.handleDeath(ctx.getAttacker());
            return true;
        }

        return true;
    }

    @Override
    protected void addAllComponents() {
        this.addComponent(AttackComponent.class, this.attackComponent);
    }

    @Override
    protected void handleDeath(Entity killer) {
        this.getGameStateService().sendTowerDeathMessage(this.getGameId(), this);
        this.getGameStateService().removeEntity(this.getGameState(), this);
        this.setAttackContext(null);
    }
    
    // /**
    //  * Calculate the shortest distance from this tower's boundary to another entity's boundary
    //  * @param other the other entity
    //  * @return the shortest distance between boundaries
    //  */
    // public float distanceToEntityBoundary(Entity other) {
    //     if (other instanceof Building) {
    //         Building otherBuilding = (Building) other;
    //         return calculateBuildingToBuildingDistance(this, otherBuilding);
    //     } else {
    //         // For non-building entities (troops, champions), treat as point entities
    //         return calculateBuildingToPointDistance(this, other.getCurrentPosition());
    //     }
    // }
    
    // /**
    //  * Calculate distance between two buildings considering their dimensions
    //  */
    // private float calculateBuildingToBuildingDistance(Building building1, Building building2) {
    //     // Get building dimensions (default to 1x1 if not available)
    //     float w1 = (building1 instanceof Tower) ? ((Tower) building1).getWidth() : 
    //               (building1 instanceof Burg) ? ((Burg) building1).getWidth() : 1.0f;
    //     float l1 = (building1 instanceof Tower) ? ((Tower) building1).getLength() : 
    //               (building1 instanceof Burg) ? ((Burg) building1).getLength() : 1.0f;
        
    //     float w2 = (building2 instanceof Tower) ? ((Tower) building2).getWidth() : 
    //               (building2 instanceof Burg) ? ((Burg) building2).getLength() : 1.0f;
    //     float l2 = (building2 instanceof Tower) ? ((Tower) building2).getLength() : 
    //               (building2 instanceof Burg) ? ((Burg) building2).getLength() : 1.0f;
        
    //     // Calculate axis-aligned bounding box distance
    //     float dx = Math.max(0, Math.abs(building1.getPosition().x() - building2.getPosition().x()) - (w1 + w2) / 2);
    //     float dy = Math.max(0, Math.abs(building1.getPosition().y() - building2.getPosition().y()) - (l1 + l2) / 2);
        
    //     return (float) Math.sqrt(dx * dx + dy * dy);
    // }
    
    // /**
    //  * Calculate distance from building boundary to a point
    //  */
    // private float calculateBuildingToPointDistance(Building building, Vector2 point) {
    //     float w = (building instanceof Tower tower) ? tower.getWidth() : 
    //              (building instanceof Burg burg) ? burg.getWidth() : 1.0f;
    //     float l = (building instanceof Tower tower) ? tower.getLength() : 
    //              (building instanceof Burg burg) ? burg.getLength() : 1.0f;

    //     // Calculate distance from point to building's bounding box
    //     float dx = Math.max(0, Math.abs(building.getPosition().x() - point.x()) - w / 2);
    //     float dy = Math.max(0, Math.abs(building.getPosition().y() - point.y()) - l / 2);
        
    //     return (float) Math.sqrt(dx * dx + dy * dy);
    // }
}
