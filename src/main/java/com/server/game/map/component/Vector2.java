package com.server.game.map.component;

public record Vector2(float x, float y) {

    public float distanceTo(Vector2 other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public Vector2 add(Vector2 other) { return new Vector2(x + other.x, y + other.y); }

    public Vector2 subtract(Vector2 other) { return new Vector2(x - other.x, y - other.y); }

    public Vector2 multiply(float scalar) { return new Vector2(x * scalar, y * scalar); }

    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }


    public float distance(Vector2 other) {
        return this.subtract(other).length();
    }

    // Ray crossing algorithm to check if a point is inside a polygon
    public boolean inside(NavPolygon navPolygon) {
        int crossingCount = 0;
        int n = navPolygon.getNumVertices();
        for (int i = 0; i < n; i++) {
            Vector2 a = navPolygon.getVertex(i);
            Vector2 b = navPolygon.getVertex((i + 1) % n);

            // Check if the ray crosses the edge
            if ((a.y > this.y) != (b.y > this.y)) {
                float xIntersection = (this.y - a.y()) * (b.x() - a.x()) / (b.y() - a.y()) + a.x();
                if (xIntersection > this.x) {
                    crossingCount++;
                }
            }
        }

        return crossingCount % 2 != 0; // Odd crossings mean inside
    }
}
