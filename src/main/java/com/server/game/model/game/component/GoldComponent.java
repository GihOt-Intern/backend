package com.server.game.model.game.component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class GoldComponent {
    private Integer currentGold;


    public GoldComponent(Integer initialGold) {
        this.currentGold = initialGold;
    }
    

    public void increaseGold(Integer amount) {
        if (amount < 0) {
            log.error("[GoldComponent] Logic error: Attempted to increase gold by a negative amount.");
            return; // Prevent negative increments
        }
        this.setCurrentGold(this.currentGold + amount);
    }
    
    public void decreaseGold(Integer amount) {
        if (amount < 0) {
            log.error("[GoldComponent] Logic error: Attempted to decrease gold by a negative amount.");
            return; // Prevent negative decrease
        }
        this.setCurrentGold(this.currentGold - amount);
    }

    public void setCurrentGold(Integer amount) {
        this.currentGold = amount;
        if (this.currentGold < 0) {
            log.error("[GoldComponent] Logic error: Attempted to set negative gold amount. Setting to zero instead.");
            this.currentGold = 0; // Ensure gold cannot be negative
        }
    }

    public boolean isEnough(Integer amount) {
        return this.currentGold >= amount;
    }

    public void spendGold(Integer amount) {
        this.decreaseGold(amount);
    }
}