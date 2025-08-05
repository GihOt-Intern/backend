package com.server.game.model.game.component;

import com.server.game.model.game.Entity;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap.PlayGround;
import com.server.game.util.Util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MovingComponent {
    private final Entity owner;
    private Vector2 currentPosition;
    private boolean inPlayground;
    private float ownerSpeed;

    private MoveContext moveContext = null;

    // 2 ticks = 66ms, to prevent spamming move requests
    private static final long MIN_UPDATE_INTERVAL_TICK = 2;
    private long lastAcceptedMoveRequestTick = 0;

    private float distancePerTick;

    public MovingComponent(Entity owner, Vector2 initPosition, float ownerSpeed) {
        this.owner = owner;
        this.currentPosition = initPosition;
        this.ownerSpeed = ownerSpeed;
        this.inPlayground = false;

        this.distancePerTick = Util.getGameTickIntervalMs() * ownerSpeed / 1000f;
    }

    public void setMoveContext(MoveContext moveContext) {
        long currentTick = moveContext.getCurrentTick();
        if (currentTick - lastAcceptedMoveRequestTick < MIN_UPDATE_INTERVAL_TICK) {
            System.out.println(">>> [Log in PositionComponent.setMoveContext] Move request ignored due to rate limiting.");
            System.out.println(">>> [Log in PositionComponent.setMoveContext] Last accepted tick: " + lastAcceptedMoveRequestTick + ", Current tick: " + currentTick);
            return;
        }

        // Using Theta* algorithm to find the path, update the moveContext
        moveContext.findPath();
        this.moveContext = moveContext;

        lastAcceptedMoveRequestTick = currentTick;
    }

    public void setMoveTargetPoint(Vector2 targetPoint) {
        if (moveContext == null) {
            System.err.println(">>> [Log in MovingComponent.setMoveTargetPoint] Move context is null, cannot set target point.");
            return;
        }

        // Update the target point in the move context
        moveContext.setTargetPoint(targetPoint);
    }

    public void toggleInPlaygroundFlag(){
        this.inPlayground = !this.inPlayground;
    }

    public boolean isMoving() {
        return this.moveContext != null;
    }

    public void setStop() { this.moveContext = null; }

    public void setCurrentPosition(Vector2 newPosition) {
        this.currentPosition = newPosition;
    }
    
    public boolean checkInPlayGround(PlayGround playGround) {
        return currentPosition.isInRectangle(
            playGround.getPosition(), playGround.getWidth(), playGround.getLength());
    }

    public float distanceTo(Vector2 otherPosition) {
        return currentPosition.distance(otherPosition);
    }

    public boolean performMoveAndBroadcast() {
        boolean moved = this.performMove();
        if (moved) {
            this.moveContext.getGameStateService()
                .sendPositionUpdate(this.moveContext.getGameState(), this.owner);
        }
        return moved;
    }

    private boolean performMove() {
        if (moveContext == null) {
            return false;
        }

        System.out.println(">>> [Log in MovingComponent.performMove] Performing move...");

        float neededMoveDistance = this.distancePerTick;

        while(this.moveContext.getPath().hasNext()) {
            GridCell nextCell = this.moveContext.getPath().peekCurrentCell();
            Vector2 positionAtNextCell = this.moveContext.toPosition(nextCell);
            float distanceToNextCell = this.currentPosition.distance(positionAtNextCell);
            if (neededMoveDistance >= distanceToNextCell) {
                // Move to the next cell
                this.currentPosition = positionAtNextCell;
                neededMoveDistance -= distanceToNextCell;
                this.moveContext.getPath().popCurrentCell(); // pop the cell from the path
            } else {
                // Move towards the next cell by using velocity vector
                Vector2 direction = positionAtNextCell.subtract(this.currentPosition).normalize();
                this.currentPosition = this.currentPosition.add(direction.multiply(neededMoveDistance));
                break;
            }
        }
        
        return true;
    }
}