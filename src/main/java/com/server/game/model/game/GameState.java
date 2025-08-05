package com.server.game.model.game;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.server.game.factory.SlotStateFactory;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.model.SlotInfo;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameState {
    String gameId;
    GameMap gameMap;
    GameMapGrid gameMapGrid;
    long currentTick = 0;

    final Map<Short, SlotState> slotStates = new ConcurrentHashMap<>();
    final Map<String, Entity> stringId2Entity = new ConcurrentHashMap<>();
    final Map<GridCell, Set<Entity>> grid2Entity = new ConcurrentHashMap<>();
    final Map<Entity, GridCell> entity2Grid = new ConcurrentHashMap<>();

    final GameStateService gameStateService;


    public GameState(String gameId, GameMap gameMap,
        GameMapGrid gameMapGrid, 
        Map<Short, ChampionEnum> slot2ChampionEnum, 
        GameStateService gameStateService, 
        SlotStateFactory slotStateFactory) {

        this.gameId = gameId;
        this.gameMap = gameMap;
        this.gameMapGrid = gameMapGrid;

        for (Map.Entry<Short, ChampionEnum> entry : slot2ChampionEnum.entrySet()) {
            Short slot = entry.getKey();
            ChampionEnum championEnum = entry.getValue();

            SlotState slotState = slotStateFactory.createSlotState(this, slot, championEnum);
            this.slotStates.put(slot, slotState);
        }

        this.gameStateService = gameStateService;
    }


    public int getNumPlayers() {
        return slotStates.size();
    }

    public void addEntity(Entity entity) {
        if (entity == null || entity.getStringId() == null) {
            System.err.println(">>> [Log in GameState.addEntity] Invalid entity or stringId");
            return;
        }

        stringId2Entity.put(entity.getStringId(), entity);
        GridCell gridCell = toGridCell(entity.getCurrentPosition());
        grid2Entity.computeIfAbsent(gridCell, k -> ConcurrentHashMap.newKeySet()).add(entity);
        entity2Grid.put(entity, gridCell);
    }

    public Set<Entity> getEntities() {
        return stringId2Entity.values()
            .stream()
            .collect(Collectors.toSet());
    }
    
    public Set<Champion> getChampions() {
        return slotStates.values().stream()
            .map(SlotState::getChampion)
            .collect(Collectors.toSet());
    }

    public Map<Short, Champion> getSlot2Champions() {
        return slotStates.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, 
                    entry -> entry.getValue().getChampion()));
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


    public Vector2 getSpawnPosition(SlotState slotState) {
        return this.getSpawnPosition(slotState.getSlot());
    }

    public float getSpawnRotate(SlotState slotState) {
        Float rotateValue = gameMap.getInitialRotate(slotState.getSlot());
        if (rotateValue == null) {
            Vector2 spawnPos = getSpawnPosition(slotState);
            if (spawnPos != null) {
                return (float) Math.atan2(-spawnPos.y(), -spawnPos.x());
            }
            return 0.0f;
        }
        return rotateValue;
    }


    public Entity getEntityByStringId(String stringId) {
        return stringId2Entity.get(stringId);
    }

    public Integer getInitialGold() {
        return gameMap.getInitialGoldEachSlot();
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