package com.server.game.model.game.component;

import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap.PlayGround;
import com.server.game.service.move.MoveService;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PositionComponent {
    private Vector2 currentPosition;
    private Vector2 targetPosition;
    private boolean isMoving;
    private boolean inPlayground;

    private final MoveService moveService;

    public PositionComponent(Vector2 initPosition, MoveService moveService) {
        this.currentPosition = initPosition;
        this.targetPosition = initPosition;
        this.inPlayground = false;
        this.isMoving = false;

        this.moveService = moveService;
    }

    public void toggleInPlaygroundFlag(){
        this.inPlayground = !this.inPlayground;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean isMoving) {
        this.isMoving = isMoving;
    }

    public void setStop() { this.isMoving = false; }

    public void setMove() { this.isMoving = true; }

    public void setCurrentPosition(Vector2 newPosition) {
        this.currentPosition = newPosition;
    }
    
    public void setTargetPosition(Vector2 newTargetPosition) {
        this.targetPosition = newTargetPosition;
    }

    public boolean checkInPlayGround(PlayGround playGround) {
        return currentPosition.isInRectangle(
            playGround.getPosition(), playGround.getWidth(), playGround.getLength());
    }

    public float distanceTo(Vector2 otherPosition) {
        return currentPosition.distance(otherPosition);
    }
}