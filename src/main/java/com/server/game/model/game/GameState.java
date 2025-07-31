package com.server.game.model.game;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.server.game.config.SpringContextHolder;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.model.SlotInfo;
import com.server.game.service.gameState.GameStateService;

import lombok.Getter;

@Getter
public class GameState {
    private String gameId;
    private GameMap gameMap;
    private GameMapGrid gameMapGrid;
    private Map<Short, SlotState> slotStates;
    private long currentTick = 0;

    private final Map<GridCell, Set<Entity>> grid2Entity = new ConcurrentHashMap<>();
    private final Map<Entity, GridCell> entity2Grid = new ConcurrentHashMap<>();

    public GameState(String gameId, GameMap gameMap, GameMapGrid gameMapGrid, Map<Short, Champion> slot2Champion) {
        this.gameId = gameId;
        this.gameMap = gameMap;
        this.gameMapGrid = gameMapGrid;
        this.slotStates = slot2Champion.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, 
                entry -> {
                    Short slot = entry.getKey();
                    Champion champion = entry.getValue();
                    Vector2 initialPosition = gameMap.getSpawnPosition(slot);
                    Integer initialGold = gameMap.getInitialGoldEachSlot();
                    return new SlotState(slot, champion, initialPosition, initialGold);
                })
            );
    }

    public int getNumPlayers() {
        return slotStates.size();
    }

    public void setChampionPosition(Short slot, Vector2 newPosition) {
        SlotState slotState = slotStates.get(slot);
        if (slotState != null) {
            slotState.getChampion().setCurrentPosition(newPosition);

            SpringContextHolder.getBean(GameStateService.class)
                .updateEntityGridCellMapping(this, slotState.getChampion());

            slotState.checkInPlayGround(this.gameId, gameMap.getPlayGround());
            return;
        }
        System.err.println(">>> [Log in GameState.setSlotPosition] Slot " + slot + " not found in game state for gameId: " + gameId);
    }

    public void setTroopPosition(TroopInstance2 troop, Vector2 newPosition) {
        SlotState slotState = slotStates.get(troop.getOwnerSlot());
        if (slotState != null) {
            slotState.getTroops().stream()
                .filter(t -> t.getStringId().equals(troop.getStringId()))
                .findFirst()
                .ifPresent(t -> t.setCurrentPosition(newPosition));

            SpringContextHolder.getBean(GameStateService.class)
                .updateEntityGridCellMapping(this, troop);

            return;
        }
        System.err.println(">>> [Log in GameState.setTroopPosition] Slot " + troop.getOwnerSlot() + " not found in game state for gameId: " + gameId);
    }

    public Float getSpeed(Short slot) {
        SlotState slotState = slotStates.get(slot);
        if (slotState != null) {
            Champion champion = slotState.getChampion();
            if (champion != null) {
                return champion.getMoveSpeed();
            }
        }
        return null;
    }

    public Short getGameMapId() {
        return gameMap.getId();
    }

    public List<SlotInfo> getSlotInfos() {
        return gameMap.getSlotInfos();
    }

    public Champion getChampionBySlot(Short slot) {
        SlotState slotState = slotStates.get(slot);
        return slotState != null ? slotState.getChampion() : null;
    }

    public Vector2 getSpawnPosition(Short slot) {
        return gameMap.getSpawnPosition(slot);
    }

    public float getSpawnRotate(Short slot) {
        Float rotateValue = gameMap.getInitialRotate(slot);
        if (rotateValue == null) {
            Vector2 spawnPos = getSpawnPosition(slot);
            if (spawnPos != null) {
                return (float) Math.atan2(-spawnPos.y(), -spawnPos.x());
            }
            return 0.0f;
        }
        return rotateValue;
    }

    public Integer getGoldGeneratedPerSecond() {
        return gameMap.getGoldGeneratedPerSecond();
    }

    public Vector2 toPosition(GridCell gridCell) {
        Vector2 origin = gameMapGrid.getOrigin();
        float cellSize = gameMapGrid.getCellSize();
        float x = origin.x() + gridCell.c() * cellSize + cellSize / 2;
        float y = origin.y() - gridCell.r() * cellSize - cellSize / 2;
        return new Vector2(x, y);
    }


    public GridCell toGridCell(Vector2 position) {
        Vector2 origin = gameMapGrid.getOrigin(); // (-100; 30)
        float cellSize = gameMapGrid.getCellSize();

        int col = (int) ((position.x() - origin.x()) / cellSize);
        int row = (int) ((origin.y() - position.y()) / cellSize); // flip Y axis
        return new GridCell(row, col);
    }


    public Integer peekGold(Short slot) {
        SlotState slotState = slotStates.get(slot);
        if (slotState != null) {
            return slotState.getCurrentGold();
        }
        return null;
    }

    public void addGold(Short slot, Integer amount) {
        this.setGold(slot, this.peekGold(slot) + amount);
    }

    public void spendGold(Short slot, Integer amount) {
        Integer currentGold = this.peekGold(slot);
        if (currentGold != null && currentGold >= amount) {
            this.setGold(slot, currentGold - amount);
        } else {
            System.err.println(">>> [Log in GameState.spendGold] Not enough gold for slot " + slot + ". Current: " + currentGold + ", Required: " + amount);
        }
    }

    public void setGold(Short slot, Integer newAmount) {
        SlotState slotState = slotStates.get(slot);
        if (slotState != null) {
            slotState.setCurrentGold(newAmount);

            slotState.handleGoldChange(gameId);
            return;
        }
        System.err.println(">>> [Log in GameState.setGold] Slot " + slot + " not found in game state for gameId: " + gameId);
    }

    public Map<Short, Champion> getChampions() {
        return slotStates.values().stream()
            .filter(slotState -> slotState.getChampion() != null)
            .collect(Collectors.toMap(SlotState::getSlot, SlotState::getChampion));
    }

    public SlotState getSlotState(short slot) {
        return slotStates.get(slot);
    }

    public int getCurrentHP(short slot) {
        SlotState slotState = slotStates.get(slot);
        return slotState != null ? slotState.getCurrentHP() : -1;
    }

    public int getMaxHP(short slot) {
        SlotState slotState = slotStates.get(slot);
        return slotState != null ? slotState.getMaxHP() : -1;
    }

    public void incrementTick() {
        this.currentTick++;
    }

    public boolean isValidGridCell(GridCell cell) {
        return cell != null && 
               cell.r() >= 0 && cell.r() < gameMapGrid.getNRows() &&
               cell.c() >= 0 && cell.c() < gameMapGrid.getNCols();
    }
}