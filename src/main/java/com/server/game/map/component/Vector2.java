package com.server.game.map.component;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public record Vector2(float x, float y) {

    public float distanceTo(Vector2 other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public Vector2 add(Vector2 other) { return new Vector2(x + other.x, y + other.y); }

    public Vector2 subtract(Vector2 other) { return new Vector2(x - other.x, y - other.y); }

    public Vector2 multiply(float scalar) { return new Vector2(x * scalar, y * scalar); }

    public Vector2 normalize() {
        float length = this.length();
        if (length == 0) {
            return new Vector2(0, 0); // Avoid division by zero
        }
        return new Vector2(x / length, y / length);
    }   

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }


    public float distance(Vector2 other) {
        return this.subtract(other).length();
    }



    public Point toPoint() {
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate coord = new Coordinate(x, y);
        return geometryFactory.createPoint(coord);
    }

    public static Vector2 fromPoint(Point point) {
        return new Vector2((float) point.getX(), (float) point.getY());
    }


    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    
}
