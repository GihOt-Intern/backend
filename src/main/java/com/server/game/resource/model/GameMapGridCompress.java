package com.server.game.resource.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;


import lombok.AccessLevel;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameMapGridCompress {
    Short id;
    String name;
    Coordinate cornerA;
    Coordinate cornerB;
    Integer nRows;
    Integer nCols;
    List<String> gridCompressed;
}
