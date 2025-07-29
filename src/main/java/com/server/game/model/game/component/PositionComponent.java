package com.server.game.model.game.component;

import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap.PlayGround;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PositionComponent {
    private Vector2 currentPosition;


    public void setCurrentPosition(Vector2 newPosition) {
        this.currentPosition = newPosition;
    }


    public boolean checkInPlayGround(PlayGround playGround) {
        return currentPosition.isInRectangle(playGround.getPosition(), playGround.getWidth(), playGround.getLength());
    }
}