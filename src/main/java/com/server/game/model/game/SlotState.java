package com.server.game.model.game;

import java.util.HashSet;
import java.util.Set;

import com.server.game.model.game.building.Burg;
import com.server.game.model.game.building.Tower;
import com.server.game.model.game.component.GoldComponent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;


@Data
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotState {
    final Short slot;

    final GameState gameState;

    // Cannot use @Delegate here because Champion has heathComponent and attributeComponent
    // which have already had @Delegate annotations.
    Champion champion;

    Set<Tower> towers;

    Burg burg;

    @Getter @Setter
    private boolean eliminated = false;

    @Delegate
    final GoldComponent goldComponent;

    final Set<Troop> troops;

    public SlotState(GameState gameState, Short slot, Champion champion, Set<Tower> towers, Burg bug, Integer initialGold) {
        this.gameState = gameState;
        this.slot = slot;
        this.champion = champion;
        this.towers = (towers != null) ? towers : new HashSet<>();
        this.burg = bug;
        this.goldComponent = new GoldComponent(initialGold);
        this.troops = new HashSet<>();
    }

    public void addTroop(Troop troop) {
        if (troop == null) {
            log.error(">>> [SlotState] Cannot add null troop");
            return;
        }
        this.troops.add(troop);
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

    public void addTroopInstance(Troop troopInstance){
        this.troops.add(troopInstance);
    }

    public int getTroopCount(){ return this.troops.size(); }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (!(other instanceof SlotState otherSlotState)) { return false; }
        if (this == other) { return true; }
        return this.slot == otherSlotState.slot;
    }

    /**
     * Get player status summary
     */
    public String getStatusSummary() {
        return String.format("Slot %d (%s): HP %d/%d, Gold: %d, Troops: %d, Alive: %s",
                slot, champion.getChampionEnum(), getCurrentHP(), getMaxHP(), getCurrentGold(), getTroopCount(),
                champion.isAlive());
    }
}