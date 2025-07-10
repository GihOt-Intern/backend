package com.server.game.map.object.abstraction;


import com.server.game.map.object.interf4ce.Attackable;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public abstract class Minion extends MovableObject implements Attackable {
    HealthComponent healthComponent;

    public Minion(int initialHP) {
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
