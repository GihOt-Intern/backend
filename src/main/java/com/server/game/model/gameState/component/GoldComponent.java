package com.server.game.model.gameState.component;

import lombok.Getter;

@Getter
public class GoldComponent {
    private Integer currentGold;


    public GoldComponent(Integer initialGold) {
        this.currentGold = initialGold;
    }
    

    public void setCurrentGold(Integer amount) {
        this.currentGold = amount;
        if (this.currentGold < 0) {
            System.out.println("[GoldComponent] Logic error: Attempted to set negative gold amount. Setting to zero instead.");
            this.currentGold = 0; // Ensure gold cannot be negative
        }
    }

    public void setCurrentGold(Float amount) {
        this.currentGold = Math.round(amount);
        if (this.currentGold < 0) {
            System.out.println("[GoldComponent] Logic error: Attempted to set negative gold amount. Setting to zero instead.");
            this.currentGold = 0; // Ensure gold cannot be negative
        }
    }

    public boolean isEnough(Integer amount) {
        return this.currentGold >= amount;
    }

    public void spendGold(Integer amount) {
        if (isEnough(amount)) {
            this.setCurrentGold(this.currentGold - amount);
        } else {
            System.out.println("Not enough gold");
        }
    }

    public void add(Integer amount) {
        this.setCurrentGold(this.currentGold + amount);
    }

    public void add(Float amount) {
        this.setCurrentGold(this.currentGold + Math.round(amount));
    }
}