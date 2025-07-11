package com.server.game.map.object.abstraction;

import com.server.game.map.MapWorld;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public abstract class Skill {
    String name;
    int damage;
    float cooldown; // in seconds

    abstract void cast(MapWorld world);
}
