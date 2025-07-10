package com.server.game.map.object.abstraction;

import com.server.game.map.object.interf4ce.Attackable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class Character extends MovableObject implements Attackable {
    String name;
    HealthComponent healthComponent;
    Skill skill;
    int level;
    int experience;
    int gold;


    public Character(int initialHP){
        this.healthComponent = new HealthComponent(initialHP);
    }

    public int getCurrentHP() {
        return healthComponent.getCurrentHP();
    }

    public void setCurrentHP(int currentHP) {
        healthComponent.setCurrentHP(currentHP);
    }

    public void takeDamage(int amount) {
        healthComponent.takeDamage(amount);
    }
}
