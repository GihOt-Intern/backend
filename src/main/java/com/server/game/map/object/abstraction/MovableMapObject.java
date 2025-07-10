package com.server.game.map.object.abstraction;

import com.server.game.map.MapWorld;
import com.server.game.map.component.Vector2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class MovableMapObject extends MapObject {
    protected Vector2 velocity; // velocity vector in units per second

    public abstract void onTick(float deltaTime, MapWorld world);
}
