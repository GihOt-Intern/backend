package com.server.game.map.shape;

import com.server.game.map.component.Vector2;

public interface CollisionShape {
    
    boolean intersects(Vector2 thisPos, CollisionShape other, Vector2 otherPos);
    boolean contains(Vector2 thisPos, Vector2 point);
    float getBoundingRadius();


    public static boolean circleVsRect(Vector2 circlePos, float radius, Vector2 rectPos, float width, float height) {
        float dx = Math.abs(circlePos.x() - rectPos.x());
        float dy = Math.abs(circlePos.y() - rectPos.y());

        if (dx > (width / 2 + radius)) return false;
        if (dy > (height / 2 + radius)) return false;

        if (dx <= (width / 2)) return true;
        if (dy <= (height / 2)) return true;

        float cornerDistanceSq = (dx - width / 2) * (dx - width / 2) +
                                 (dy - height / 2) * (dy - height / 2);

        return cornerDistanceSq <= (radius * radius);
    }
}