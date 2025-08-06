package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.model.map.component.GridCell;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.deserializer.GameMapGridDeserializer;

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
}
