package com.server.game.model.game.component;

import org.springframework.lang.Nullable;

import com.server.game.model.game.Entity;
import com.server.game.model.game.context.MoveContext;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap.Playground;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public class MovingComponent {
    @Getter(AccessLevel.PRIVATE)
    private final Entity owner;
    private Vector2 currentPosition; // WARNING: DO NOT SET THIS DIRECTLY, USE setCurrentPosition() INSTEAD
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

    public boolean setMoveContext(@Nullable MoveContext moveContext, boolean isForced) {
        long currentTick = this.owner.getGameState().getCurrentTick();

        if (isForced) {
            this.moveContext = moveContext;
            lastAcceptedMoveRequestTick = currentTick;
            return true;
        }

        if (currentTick - lastAcceptedMoveRequestTick < MIN_UPDATE_INTERVAL_TICK) {
            return false;
        }

        if (this.owner.isCastingDurationSkill() && !this.owner.canPerformSkillWhileMoving()) {
            log.info("Cannot set move context while casting skill, skipping.");
            return false; // Cannot set move context while casting skill
        }

        this.moveContext = moveContext;

        lastAcceptedMoveRequestTick = currentTick;

        return true;
    }


    public void toggleInPlaygroundFlag(){
        this.inPlayground = !this.inPlayground;
    }

    public boolean isMoving() {
        return this.moveContext != null;
    }

    public void setStop() { this.moveContext = null; }

    public void setCurrentPosition(Vector2 newPosition) {
        owner.beforeUpdatePosition();

        this.currentPosition = newPosition;

        owner.afterUpdatePosition();
    }
    
    public boolean checkInPlayground(Playground playGround) {
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

        float neededMoveDistance = this.distancePerTick;

        if (this.owner.isAttacking()) {
            if (this.owner.inAttackRange()) {
                return false;
            }

            float distanceNeededToReachAttackRange = this.owner.getDistanceNeededToReachAttackRange();
            neededMoveDistance = Math.min(neededMoveDistance, distanceNeededToReachAttackRange);
        }

        while(this.moveContext.getPath().hasNext()) {
            GridCell nextCell = this.moveContext.getPath().peekCurrentCell();
            Vector2 positionAtNextCell = this.moveContext.toPosition(nextCell);
            float distanceToNextCell = this.currentPosition.distance(positionAtNextCell);
            if (neededMoveDistance >= distanceToNextCell) {
                // Move to the next cell
                this.setCurrentPosition(positionAtNextCell);
                neededMoveDistance -= distanceToNextCell;
                this.moveContext.getPath().popCurrentCell(); // pop the cell from the path
            } else {
                // Move towards the next cell by using velocity vector
                Vector2 direction = this.currentPosition.directionTo(positionAtNextCell);
                Vector2 nextPosition = this.getCurrentPosition().add(direction.multiply(neededMoveDistance));

                this.setCurrentPosition(nextPosition);
                break;
            }
        }

        if (!this.moveContext.getPath().hasNext()) {
            this.moveContext = null;
        }
        
        return true;
    }
}