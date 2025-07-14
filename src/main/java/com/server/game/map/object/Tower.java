package com.server.game.map.object;

import com.server.game.map.MapWorld;
import com.server.game.map.object.abstraction.HealthComponent;
import com.server.game.map.object.abstraction.MapObject;
import com.server.game.map.object.interf4ce.Attackable;

import lombok.experimental.Delegate;


public class Tower extends MapObject implements Attackable {

    @Delegate
    private HealthComponent healthComponent;

    public Tower(int initialHP) {
        this.healthComponent = new HealthComponent(initialHP);
    }

    

    @Override
    public void onTick(float deltaTime, MapWorld world) {
        // Tower logic for each tick can be implemented here
    }

    @Override
    public void attack(MapObject target){}

    @Override
    public int getAttackDamage() { return 0; }
    
    @Override
    public float getAttackRange() { return 0.0f; }
    
    @Override
    public float getAttackCooldown() { return 0.0f; }

    
}
