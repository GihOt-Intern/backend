package com.server.game.model.game.component;

import org.springframework.lang.Nullable;

import com.server.game.model.game.Entity;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap.PlayGround;
import com.server.game.util.Util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public class MovingComponent {
    private final Entity owner;
    private Vector2 currentPosition; // WARNING: DO NOT SET THIS DIRECTLY, USE setCurrentPosition() INSTEAD
    private boolean inPlayground;
    private float ownerSpeed;

    private MoveContext moveContext = null;

    private final static int MAX_PATH_FINDING_FAILED_ATTEMPTS = 5;
    private int pathFindingFailedCount = 0;

    private final static int PATH_FINDING_FAILED_ATTEMPT_COOLDOWN_TICK = (int) (300 / Util.getGameTickIntervalMs());
    private long lastPathFindingFailedTick = 0;

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

    public boolean setMoveContext(@Nullable MoveContext moveContext) {
        long currentTick = this.owner.getGameState().getCurrentTick();
        if (currentTick - lastAcceptedMoveRequestTick < MIN_UPDATE_INTERVAL_TICK) {
            log.info(">>> [Log in PositionComponent.setMoveContext] Move request ignored due to rate limiting.");
            log.info(">>> [Log in PositionComponent.setMoveContext] Last accepted tick: " + lastAcceptedMoveRequestTick + ", Current tick: " + currentTick);
            return false;
        }

        this.moveContext = moveContext;

        lastAcceptedMoveRequestTick = currentTick;
        
        if (moveContext == null) { return true; }
        
        // Using Theta* algorithm to find the path, update the moveContext
        boolean foundPath = moveContext.findPath(); // this method already set the path in the moveContext (if found)


        if (this.pathFindingFailedCount >= MAX_PATH_FINDING_FAILED_ATTEMPTS) {
            
            if (currentTick - this.lastPathFindingFailedTick < PATH_FINDING_FAILED_ATTEMPT_COOLDOWN_TICK) {
                log.info("Entity {} in cooldown due to repeated failed pathfinding attempts", owner.getStringId());
                return false;
            }

            this.pathFindingFailedCount = 0; // reset the count if we are not in cooldown
            return true;
        }


        if (!foundPath) {
            ++this.pathFindingFailedCount;
            this.lastPathFindingFailedTick = currentTick;

            if (this.pathFindingFailedCount >= MAX_PATH_FINDING_FAILED_ATTEMPTS) {
                log.warn("Entity {} has failed to find a path {} times, entering cooldown", 
                    owner.getStringId(), this.pathFindingFailedCount);
            }
            return false;
        }

        this.pathFindingFailedCount = 0;
        return true;
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

        owner.afterUpdatePosition();
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
            this.owner.getGameStateService()
                .sendPositionUpdate(this.owner.getGameState(), this.owner);
        }
        return moved;
    }

    private boolean performMove() {
        if (moveContext == null) {
            return false;
        }

        System.out.println(">>> [Log in MovingComponent.performMove] Performing move...");

        float neededMoveDistance = this.distancePerTick;

        if (this.owner.isAttacking()) {
            if (this.owner.inAttackRange()) {
                return false;
            }

            float distanceNeededToReachAttackRange = this.owner.getDistanceNeededToReachAttackRange();
            neededMoveDistance = Math.min(neededMoveDistance, distanceNeededToReachAttackRange);
        }

        while(this.moveContext.getPath().hasNext()) {
            System.out.println(">>> [Log in MovingComponent.performMove] Moving to next cell...");
            GridCell nextCell = this.moveContext.getPath().peekCurrentCell();
            Vector2 positionAtNextCell = this.moveContext.toPosition(nextCell);
            float distanceToNextCell = this.currentPosition.distance(positionAtNextCell);
            System.out.println(">>> [Log in MovingComponent.performMove] Current pos: " + this.currentPosition + 
                ", Needed move distance: " + neededMoveDistance + 
                ", Distance to next cell: " + distanceToNextCell + ", Cell index: " + moveContext.getPath().getIndex() +
                ", Path size: " + moveContext.getPath().size());
            if (neededMoveDistance >= distanceToNextCell) {
                // Move to the next cell
                this.setCurrentPosition(positionAtNextCell);
                neededMoveDistance -= distanceToNextCell;
                this.moveContext.getPath().popCurrentCell(); // pop the cell from the path
            } else {
                // Move towards the next cell by using velocity vector
                Vector2 direction = positionAtNextCell.subtract(this.currentPosition).normalize();
                Vector2 nextPosition = this.getCurrentPosition().add(direction.multiply(neededMoveDistance));

                this.setCurrentPosition(nextPosition);
                break;

                // Check if move towards the next position is not into a wall
                // GridCell nextGridCell = this.moveContext.getGameState().toGridCell(nextPosition);
                // if (this.moveContext.getGameMapGrid().isWalkable(nextGridCell)) {
                //     this.setCurrentPosition(nextPosition);
                //     break;
                // }
  
                // log.info(">>> [Log in MovingComponent.performMove] Next position {} is not walkable...", nextPosition);
                // GridCell walkableCell = this.moveContext.findNearestWalkableCell(nextGridCell);
                // if (walkableCell == null) {
                //     log.warn(">>> [Log in MovingComponent.performMove] No walkable cell found near {}, stopping move.", nextGridCell);
                //     this.moveContext = null; // Stop moving if no walkable cell found
                //     return false;
                // }

                // Vector2 walkablePosition = this.moveContext.toPosition(walkableCell);
                // log.info(">>> [Log in MovingComponent.performMove] Moving to nearest walkable cell {} at position {}", walkableCell, walkablePosition);
                // this.setCurrentPosition(walkablePosition);
                // this.moveContext.getPath().popCurrentCell(); // pop the cell from the path
            }
        }

        if (!this.moveContext.getPath().hasNext()) {
            this.moveContext = null;
        }
        
        return true;
    }
}