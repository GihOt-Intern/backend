package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.locationtech.jts.geom.Coordinate;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.server.game.resource.deserializer.GameMapGridDeserializer;


import lombok.AccessLevel;


@Data
@AllArgsConstructor
@JsonDeserialize(using = GameMapGridDeserializer.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMapGrid {
    short id;
    String name;
    Coordinate cornerA;
    Coordinate cornerB;
    Integer nRows;
    Integer nCols;
    boolean[][] grid;
}
