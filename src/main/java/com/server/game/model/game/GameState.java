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
import com.server.game.resource.model.GameMap.Playground;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.model.SlotInfo;
import com.server.game.service.gameState.GameStateService;
import com.server.game.util.ChampionEnum;
import com.server.game.util.Util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameState {
    final String gameId;
    final GameMap gameMap;
    final GameMapGrid gameMapGrid;

    long currentTick = 0;
    long nextGoldMineGenerationTick = Util.seconds2GameTick(10f); // TODO: adjust this value based on game design

    final Map<Short, SlotState> slotStates = new ConcurrentHashMap<>();
    final Map<String, Entity> stringId2Entity = new ConcurrentHashMap<>();
    final Map<GridCell, Set<Entity>> grid2Entity = new ConcurrentHashMap<>();

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

    public boolean inGoldMineGenerationTick() {
        return this.currentTick >= this.nextGoldMineGenerationTick;
    }

    public void updateNextGoldMineGenerationTick() {
        this.nextGoldMineGenerationTick = this.currentTick + Util.seconds2GameTick(
            this.getGameMap().getGoldMineGenerationIntervalSeconds());
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

        // Also update the grid to entity mapping
        GridCell gridCell = toGridCell(entity.getCurrentPosition());
        grid2Entity.computeIfAbsent(gridCell, k -> ConcurrentHashMap.newKeySet()).add(entity);
    }

    public void removeEntity(Entity entity) {
        if (entity == null || entity.getStringId() == null) {
            System.err.println(">>> [Log in GameState.removeEntity] Invalid entity or stringId");
            return;
        }

        stringId2Entity.remove(entity.getStringId());


        // Also update the grid to entity mapping
        GridCell gridCell = entity.getCurrentGridCell();
        if (gridCell != null) {
            Set<Entity> entitiesAtCell = grid2Entity.get(gridCell);
            if (entitiesAtCell != null) {
                entitiesAtCell.remove(entity);
                if (entitiesAtCell.isEmpty()) {
                    grid2Entity.remove(gridCell);
                }
            }
        }
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

    public Playground getPlayground() {
        return gameMap.getPlayground();
    }

    public Integer getInitialGold() {
        return gameMap.getInitialGoldEachSlot();
    }

    public Integer getGoldGeneratedPerSecond() {
        return gameMap.getGoldGeneratedPerSecond();
    }

    public Integer getTowersInitHP() {
        return gameMap.getTowerHP();
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

        float relativeX = position.x() - origin.x();
        float relativeY = origin.y() - position.y(); // flip Y axis

        int col = (int) (relativeX / cellSize);
        int row = (int) (relativeY / cellSize);

        // Kẹp giá trị để đảm bảo nó luôn nằm trong phạm vi hợp lệ của grid
        col = Math.max(0, Math.min(col, gameMapGrid.getNCols() - 1));
        row = Math.max(0, Math.min(row, gameMapGrid.getNRows() - 1));

        return new GridCell(row, col);
    }


    public Integer peekGold(Short slot) {
        if (slot != null) {
            SlotState slotState = this.getSlotState(slot);
            return peekGold(slotState);
        }
        return null;
    }
    
    public Integer peekGold(SlotState slotState) {
        if (slotState != null) {
            return slotState.getCurrentGold();
        }
        return null;
    }

    public void increaseGoldFor(SlotState slotState, Integer amount) {
        this.setGold(slotState, this.peekGold(slotState) + amount);
    }

    public void spendGold(SlotState slotState, Integer amount) {
        Integer currentGold = this.peekGold(slotState);
        if (currentGold != null && currentGold >= amount) {
            this.setGold(slotState, currentGold - amount);
        } else {
            System.err.println(">>> [Log in GameState.spendGold] Not enough gold for slot. Current: " + currentGold + ", Required: " + amount);
        }
    }

    public void setGold(SlotState slotState, Integer newAmount) {
        if (slotState == null) {
            System.err.println(">>> [Log in GameState.setGold] Slot not found in game state for gameId: " + gameId);
            return;
        }
        slotState.setCurrentGold(newAmount);

        slotState.handleGoldChange(gameId);
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
        // System.out.println(">>> [Log in GameState.incrementTick] Current tick of game: " + this.getGameId() + " incremented to: " + this.currentTick);
    }

    public boolean isValidGridCell(GridCell cell) {
        return cell != null && 
               cell.r() >= 0 && cell.r() < gameMapGrid.getNRows() &&
               cell.c() >= 0 && cell.c() < gameMapGrid.getNCols();
    }


    public Integer getBurgsInitHP() {
        return gameMap.getBurgHP();
    }
}