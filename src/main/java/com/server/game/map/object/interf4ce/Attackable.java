package com.server.game.map.object.interf4ce;

import com.server.game.map.object.abstraction.MapObject;


public interface Attackable {
    void attack(MapObject target);
    int getAttackDamage();
    float getAttackRange();
    float getAttackCooldown();
}