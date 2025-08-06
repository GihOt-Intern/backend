package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.model.game.Entity;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.deserializer.GameMapGridDeserializer;
import com.server.game.util.Util;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@JsonDeserialize(using = GameMapGridDeserializer.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMapGrid {
    short id;
    String name;
    Vector2 cornerA;
    Vector2 cornerB;
    Integer nRows;
    Integer nCols;
    float cellSize;
    boolean[][] grid;

    public Vector2 getOrigin() {
        return cornerA;
    }

    public boolean isValid(GridCell cell) {
        return cell.r() >= 0 && cell.r() < nRows && cell.c() >= 0 && cell.c() < nCols;
    }

    public boolean isWalkable(GridCell cell) {
        return grid[cell.r()][cell.c()];
    }

    // public GridCell findNearestWalkableCell(GridCell startCell) {
    //     int dir[][] = Util.EIGHT_DIRECTIONS;

    //     HashSet<GridCell> visitedCells = new HashSet<>();
        
    //     // Use BFS to find the nearest walkable cell
        
    //     Queue<GridCell> cellsToCheck = new ConcurrentLinkedQueue<>();
    //     cellsToCheck.offer(startCell);
    //     visitedCells.add(startCell);

    //     while(!cellsToCheck.isEmpty()) {
    //         GridCell currentCell = cellsToCheck.poll();
    //         if (currentCell == null) {
    //             continue;
    //         }

    //         // Check neighboring cells
    //         for (int[] direction : dir) {
    //             GridCell neighborCell = currentCell.add(direction[0], direction[1]);
                
    //             if (this.isWalkable(neighborCell)) { return neighborCell; } 
                
    //             if(visitedCells.contains(neighborCell)) {
    //                 continue; // Already visited
    //             }
                
    //             cellsToCheck.offer(neighborCell);
    //             visitedCells.add(neighborCell);
    //         }
    //     }
    
    //     return null;
    // }
}
