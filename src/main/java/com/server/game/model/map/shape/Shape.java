package com.server.game.model.map.shape;

import com.server.game.model.map.component.Vector2;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class Shape {

    protected Vector2 center;

    public abstract boolean intersects(Vector2 thisPos, Shape other, Vector2 otherPos);
    public abstract boolean contains(Vector2 point);
    public abstract float getBoundingRadius();


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