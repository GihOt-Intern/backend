package com.server.game.model.game;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


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

    // Burg dimensions
    final float width;
    final float length;
    final float rotate;

    public Burg(SlotState ownerSlot, GameState gameState, Integer hp, BurgDB burgDB) {
        super("burg_" + ownerSlot.getSlot() + UUID.randomUUID().toString(),
        ownerSlot, gameState, hp, burgDB.getId(), burgDB.getPosition());
        
        // Store burg dimensions
        this.width = burgDB.getWidth();
        this.length = burgDB.getLength();
        this.rotate = burgDB.getRotate();
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
        if (this.getCurrentHP() <= 0) {
            handleBurgDestroyed(ctx);
            return true; // Burg is destroyed
        }
        
        return false; // Burg is still alive
    }

    @Override
    protected void addAllComponents() {
        super.addAllComponents(); // Add components from Building class
    }
    
    /**
     * Calculate the shortest distance from this burg's boundary to another entity's boundary
     * @param other the other entity
     * @return the shortest distance between boundaries
     */
    public float distanceToEntityBoundary(Entity other) {
        if (other instanceof Building) {
            Building otherBuilding = (Building) other;
            return calculateBuildingToBuildingDistance(this, otherBuilding);
        } else {
            // For non-building entities (troops, champions), treat as point entities
            return calculateBuildingToPointDistance(this, other.getCurrentPosition());
        }
    }
    
    /**
     * Calculate distance between two buildings considering their dimensions
     */
    private float calculateBuildingToBuildingDistance(Building building1, Building building2) {
        // Get building dimensions (default to 1x1 if not available)
        float w1 = (building1 instanceof Tower) ? ((Tower) building1).getWidth() : 
                  (building1 instanceof Burg) ? ((Burg) building1).getWidth() : 1.0f;
        float l1 = (building1 instanceof Tower) ? ((Tower) building1).getLength() : 
                  (building1 instanceof Burg) ? ((Burg) building1).getLength() : 1.0f;
        
        float w2 = (building2 instanceof Tower) ? ((Tower) building2).getWidth() : 
                  (building2 instanceof Burg) ? ((Burg) building2).getWidth() : 1.0f;
        float l2 = (building2 instanceof Tower) ? ((Tower) building2).getLength() : 
                  (building2 instanceof Burg) ? ((Burg) building2).getLength() : 1.0f;
        
        // Calculate axis-aligned bounding box distance
        float dx = Math.max(0, Math.abs(building1.getPosition().x() - building2.getPosition().x()) - (w1 + w2) / 2);
        float dy = Math.max(0, Math.abs(building1.getPosition().y() - building2.getPosition().y()) - (l1 + l2) / 2);
        
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate distance from building boundary to a point
     */
    private float calculateBuildingToPointDistance(Building building, com.server.game.model.map.component.Vector2 point) {
        float w = (building instanceof Tower) ? ((Tower) building).getWidth() : 
                 (building instanceof Burg) ? ((Burg) building).getWidth() : 1.0f;
        float l = (building instanceof Tower) ? ((Tower) building).getLength() : 
                 (building instanceof Burg) ? ((Burg) building).getLength() : 1.0f;
        
        // Calculate distance from point to building's bounding box
        float dx = Math.max(0, Math.abs(building.getPosition().x() - point.x()) - w / 2);
        float dy = Math.max(0, Math.abs(building.getPosition().y() - point.y()) - l / 2);
        
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** 
     * Handle the case when the burg is destroyed
     */
    private void handleBurgDestroyed(AttackContext ctx) {
        SlotState ownerSlot = this.getOwnerSlot();
        GameState gameState = this.getGameState();
        short destroyedSlot = ownerSlot.getSlot();

        List<String> removedEntityIds = new ArrayList<>();
        List<Entity> entitiesToRemove = new ArrayList<>();

        for(Entity entity : gameState.getEntities()) {
            if (entity.getOwnerSlot().getSlot() == destroyedSlot) {
                removedEntityIds.add(entity.getStringId());
                entitiesToRemove.add(entity);
            }
        }

        for(Entity entity : entitiesToRemove) {
            gameState.removeEntity(entity);
        }

        ctx.getGameStateService().sendEntitiesRemoved(
            ctx.getGameId(), removedEntityIds, ctx.getTimestamp());
        
        ownerSlot.setEliminated(true);

        int remainingBurgs = 0;
        short lastAliveBurgSlot = -1;

        for(Entity entity : gameState.getEntities()) {
            if (entity instanceof Burg && entity.isAlive()) {
                remainingBurgs++;
                lastAliveBurgSlot = entity.getOwnerSlot().getSlot();
            }
        }

        if (remainingBurgs == 1) {
            ctx.getGameStateService().sendGameOver(
                ctx.getGameId(), lastAliveBurgSlot, ctx.getTimestamp()
            );
        }
    }
}
