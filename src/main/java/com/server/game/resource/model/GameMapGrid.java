package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.map.component.Vector2;
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
}
