package com.server.game.map.object;

import com.server.game.map.MapWorld;
import com.server.game.map.object.abstraction.MovableMapObject;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Character extends MovableMapObject {
    private int hp;
    
    @Override
    public void onTick(float deltaTime, MapWorld world) {}
}
