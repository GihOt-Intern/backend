package com.server.game.model.game.component;

import lombok.Getter;

@Getter
public class HealthComponent {
    private final int initialHP;
    private int currentHP;
    private int maxHP;

    public HealthComponent(int initHP) {
        this.initialHP = initHP;
        this.maxHP = initHP;
        this.currentHP = initHP;
    }

    public float getHealthPercentage() {
        if (maxHP == 0) {
            return 0.0f; // Avoid division by zero
        }
        return (float) currentHP / maxHP * 100;
    }

    

    public void setCurrentHP(int newHP) {
        this.currentHP = Math.max(0, Math.min(newHP, this.maxHP));
    }

    public void setMaxHP(int newMaxHP) {
        this.maxHP = Math.max(0, newMaxHP);
        // Adjust current HP if it exceeds new max HP
        if (this.currentHP > this.maxHP) {
            this.currentHP = this.maxHP;
        }
    }

    public void takeDamage(int amount) {
        this.currentHP = Math.max(0, currentHP - amount);
    }

    public boolean isAlive() {
        return this.currentHP > 0;
    }
}
