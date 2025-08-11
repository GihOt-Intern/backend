package com.server.game.model.game.component;

import lombok.Getter;

@Getter
public class GoldComponent {
    private Integer currentGold;


    public GoldComponent(Integer initialGold) {
        this.currentGold = initialGold;
    }
    

    public void increaseGold(Integer amount) {
        if (amount < 0) {
            System.out.println("[GoldComponent] Logic error: Attempted to increase gold by a negative amount.");
            return; // Prevent negative increments
        }
        this.setCurrentGold(this.currentGold + amount);
    }

    public void setCurrentGold(Integer amount) {
        this.currentGold = amount;
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
}