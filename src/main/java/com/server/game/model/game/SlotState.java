package com.server.game.model.game;

import com.server.game.config.SpringContextHolder;
import com.server.game.model.game.component.GoldComponent;
import com.server.game.model.game.component.PositionComponent;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.handler.PlayGroundHandler;
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
    private PositionComponent positionComponent;

    @Delegate
    private GoldComponent goldComponent;

    private boolean inPlayGround;
    private boolean isAlive;

    private int troopCount;

    public SlotState(Short slot, Champion champion, Vector2 initialPosition, Integer initialGold) {
        this.slot = slot;
        this.champion = champion;
        this.positionComponent = new PositionComponent(initialPosition);
        this.goldComponent = new GoldComponent(initialGold);
        this.inPlayGround = false;
        this.isAlive = true;

        this.troopCount = 0; // TODO: add troop component to this class
    }


    public void checkInPlayGround(String gameId, PlayGround playGround) {
        boolean nextInPlayGround = this.positionComponent.isInPlayGround(playGround);
        System.out.println(">>> [Log in SlotState.checkInPlayGround] Slot " + slot + " nextInPlayGround: " + nextInPlayGround + ", current inPlayGround: " + this.inPlayGround);
        if (nextInPlayGround != this.inPlayGround) {
            this.inPlayGround = !this.inPlayGround; // Toggle the state
            PlayGroundHandler playGroundHandler =
                SpringContextHolder.getBean(PlayGroundHandler.class);
            playGroundHandler.sendInPlayGroundUpdateMessage(gameId, this.slot, this.inPlayGround);
        }
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

    public void setAlive() {
        this.isAlive = true;
    }

    public void setDead() {
        this.isAlive = false;
    }

    public void handleGoldChange(String gameId) {
        PlayGroundHandler playGroundHandler = 
            SpringContextHolder.getBean(PlayGroundHandler.class);
        playGroundHandler.sendGoldChangeMessage(gameId, this.slot, this.getCurrentGold());
    }

    /**
     * Get player status summary
     */
    public String getStatusSummary() {
        return String.format("Slot %d (%s): HP %d/%d, Gold: %d, Troops: %d, Alive: %s",
                slot, champion.getId(), getCurrentHP(), getMaxHP(), getCurrentGold(), getTroopCount(),
                isAlive());
    }
}