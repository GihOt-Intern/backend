package com.server.game.map;

import org.locationtech.jts.geom.Coordinate;

public class GridUtils {
    public static int[] toGrid(double x, double y, double originX, double originY, double cellSize) {
        int gx = (int) ((x - originX) / cellSize);
        int gy = (int) ((y - originY) / cellSize);
        return new int[]{gx, gy};
    }

    public static Coordinate toWorld(int gx, int gy, double originX, double originY, double cellSize) {
        double x = originX + gx * cellSize + cellSize / 2;
        double y = originY + gy * cellSize + cellSize / 2;
        return new Coordinate(x, y);
    }
}
