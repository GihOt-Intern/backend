package com.server.game.model.game.component;

import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap.PlayGround;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PositionComponent {
    private Vector2 currentPosition;
    private boolean isMoving;
    private boolean inPlayground;

    public PositionComponent(Vector2 initPosition){
        this.currentPosition = initPosition;
        this.inPlayground = false;
        this.isMoving = false;
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

    public boolean checkInPlayGround(PlayGround playGround) {
        return currentPosition.isInRectangle(playGround.getPosition(), playGround.getWidth(), playGround.getLength());
    }

    public float distanceTo(Vector2 otherPosition) {
        return currentPosition.distance(otherPosition);
    }
}