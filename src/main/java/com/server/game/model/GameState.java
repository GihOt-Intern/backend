package com.server.game.model;

import java.util.List;
import java.util.Map;

import com.server.game.map.component.GridCell;
import com.server.game.map.component.Vector2;
import com.server.game.resource.model.Champion;
import com.server.game.resource.model.GameMap;
import com.server.game.resource.model.GameMapGrid;
import com.server.game.resource.model.SlotInfo;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameState {
    private GameMap gameMap;
    private GameMapGrid gameMapGrid;
    private Map<Short, Champion> champions;

    public Float getSpeed(Short slot) {
        Champion champion = champions.get(slot);
        if (champion != null) {
            return champion.getMoveSpeed();
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
        return champions.get(slot);
    }

    public Vector2 getSpawnPosition(Short slot) {
        return gameMap.getSpawnPosition(slot);
    }

    public Vector2 toPosition(GridCell gridCell) {
        Vector2 origin = gameMapGrid.getOrigin();
        float cellSize = gameMapGrid.getCellSize();
        return new Vector2(
            origin.x() + gridCell.c() * cellSize + cellSize / 2,
            origin.y() + gridCell.r() * cellSize + cellSize / 2
        );
    }


    public GridCell toGridCell(Vector2 position) {
        Vector2 origin = gameMapGrid.getOrigin();
        float cellSize = gameMapGrid.getCellSize();

        int row = (int) ((position.x() - origin.x()) / cellSize);
        int col = (int) ((position.y() - origin.y()) / cellSize);
        return new GridCell(row, col);
    }
}