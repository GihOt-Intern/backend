package com.server.game.map.object;

import com.server.game.map.MapWorld;
import com.server.game.map.object.abstraction.MovableMapObject;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public class Minion extends MovableMapObject {

    
    @Override
    public void onTick(float deltaTime, MapWorld world) {}
    
}
