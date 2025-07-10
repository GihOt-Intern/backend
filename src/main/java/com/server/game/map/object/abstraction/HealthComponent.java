package com.server.game.map.object.abstraction;

import lombok.Getter;

@Getter
public class HealthComponent {
    protected int currentHP;
    protected int maxHP;

    public HealthComponent(int maxHP) {
        this.maxHP = maxHP;
        this.currentHP = maxHP;
    }

    public void setCurrentHP(int newHP) {
        this.currentHP = Math.max(0, Math.min(newHP, this.maxHP));
    }

    public void takeDamage(int amount) {
        this.currentHP = Math.max(0, currentHP - amount);
    }

    public boolean isAlive() {
        return this.currentHP > 0;
    }
}
