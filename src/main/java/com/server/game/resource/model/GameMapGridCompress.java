package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

import com.server.game.model.map.component.Vector2;

import lombok.AccessLevel;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMapGridCompress {
    Short id;
    String name;
    Vector2 cornerA;
    Vector2 cornerB;
    Integer nRows;
    Integer nCols;
    float cellSize;
    List<String> gridCompressed;
}
