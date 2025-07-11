package com.server.game.map.object.abstraction;

import com.server.game.map.object.interf4ce.Attackable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;

@Getter
@AllArgsConstructor
public abstract class Character extends MovableObject implements Attackable {
    String name;
    
    @Delegate
    protected final HealthComponent healthComponent;
    
    @Delegate
    protected final Skill skill;

    protected int level;
    protected int experience;
    protected int gold;


    public Character(int initialHP, Skill skill) {
        this.healthComponent = new HealthComponent(initialHP);
        this.skill = skill;
    }

}
