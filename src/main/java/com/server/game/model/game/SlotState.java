package com.server.game.model.game;

import java.util.HashSet;
import java.util.Set;

import com.server.game.config.SpringContextHolder;
import com.server.game.model.game.component.GoldComponent;
import com.server.game.model.game.component.PositionComponent;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.handler.PlaygroundHandler;
import com.server.game.resource.model.GameMap.PlayGround;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Delegate;


@Data
@AllArgsConstructor
public class SlotState {
    private Short slot;

    // Cannot use @Delegate here because Champion has heathComponent and attributeComponent
    // which have already had @Delegate annotations.
    private Champion champion;

    @Delegate
    private GoldComponent goldComponent;

    private Set<TroopInstance2> troops;

    public SlotState(Short slot, Champion champion, Vector2 initialPosition, Integer initialGold) {
        this.slot = slot;
        this.champion = champion;
        this.goldComponent = new GoldComponent(initialGold);
        this.troops = new HashSet<>();
    }


    public void checkInPlayGround(String gameId, PlayGround playGround) {
        boolean nextInPlayGround = this.champion.checkInPlayGround(playGround);
        
        System.out.println(">>> [Log in SlotState.checkInPlayGround] Slot " + slot + " nextInPlayGround: " + 
            nextInPlayGround + ", current inPlayGround: " + this.champion.isInPlayground());

        if (nextInPlayGround != this.champion.isInPlayground()) {
            this.champion.toggleInPlaygroundFlag(); // Toggle the state
            PlaygroundHandler playgroundHandler =
                SpringContextHolder.getBean(PlaygroundHandler.class);
            playgroundHandler.sendInPlaygroundUpdateMessage(gameId, this.slot, this.champion.isInPlayground());
        }
    }

    public boolean isChampionAlive(){
        return this.champion.isAlive();
    }


    public float getMoveSpeed() {
        return this.champion.getMoveSpeed();
    }

    public int getCurrentHP() {
        return this.champion.getCurrentHP();
    }

    public int getMaxHP() {
        return this.champion.getMaxHP();
    }

    public float getHealthPercentage() {
        return this.champion.getHealthPercentage();
    }

    public void setCurrentHP(int hp) {
        this.champion.setCurrentHP(hp);
    }

    public void setChampionDead() {
        this.setCurrentHP(0);
    }

    public void setChampionRevive() {
        this.setCurrentHP(this.getMaxHP());
    }

    public void handleGoldChange(String gameId) {
        PlaygroundHandler playGroundHandler = 
            SpringContextHolder.getBean(PlaygroundHandler.class);
        playGroundHandler.sendGoldChangeMessage(gameId, this.slot, this.getCurrentGold());
    }


    public void addTroopInstance(TroopInstance2 troopInstance){
        this.troops.add(troopInstance);
    }

    public int getTroopCount(){ return this.troops.size(); }

    /**
     * Get player status summary
     */
    public String getStatusSummary() {
        return String.format("Slot %d (%s): HP %d/%d, Gold: %d, Troops: %d, Alive: %s",
                slot, champion.getChampionEnum(), getCurrentHP(), getMaxHP(), getCurrentGold(), getTroopCount(),
                champion.isAlive());
    }
}