package com.server.game.model.map.shape;

import com.server.game.model.map.component.Vector2;

import lombok.Getter;

@Getter
public class CircleShape extends Shape {
    private float radius;

    public CircleShape(Vector2 center, float radius) {
        super(center);
        this.radius = radius;
    }

    @Override
    public boolean intersects(Vector2 thisPos, Shape other, Vector2 otherPos) {
        if (other instanceof CircleShape circle) {
            float distance = thisPos.distanceTo(otherPos);
            return distance <= (this.radius + circle.radius);
        }
        // Nếu va chạm với hình khác (ví dụ Rect), gọi ngược lại
        return other.intersects(otherPos, this, thisPos);
    }

    @Override
    public boolean contains(Vector2 point) {
        return center.distanceTo(point) <= radius;
    }

    @Override
    public float getBoundingRadius() {
        return radius;
    }
}