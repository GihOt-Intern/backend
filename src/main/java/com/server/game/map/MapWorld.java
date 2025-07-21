package com.server.game.map;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;


import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class MapWorld {
    private final short id;
    private final int width;
    private final int height;
    private final float cellSize;
    private final Polygon boundary;
    private final boolean[][] walkable;

    public MapWorld(short id, Polygon boundary, float cellSize) {
        this.id = id;
        this.boundary = boundary;
        this.cellSize = cellSize;

        Envelope bounds = boundary.getEnvelopeInternal();
        this.width = (int) Math.ceil(bounds.getWidth() / cellSize);
        this.height = (int) Math.ceil(bounds.getHeight() / cellSize);

        walkable = new boolean[height][width];
        buildWalkableGrid(bounds.getMinX(), bounds.getMinY());
    }


    private void buildWalkableGrid(double originX, double originY) {
        GeometryFactory factory = new GeometryFactory();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double centerX = originX + x * cellSize + cellSize / 2;
                double centerY = originY + y * cellSize + cellSize / 2;
                Point p = factory.createPoint(new Coordinate(centerX, centerY));
                walkable[y][x] = boundary.contains(p);
            }
        }
    }

    public boolean isWalkable(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height && walkable[y][x];
    }
}
