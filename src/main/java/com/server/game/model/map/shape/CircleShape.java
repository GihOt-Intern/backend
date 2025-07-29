package com.server.game.model.map.shape;

import com.server.game.model.map.component.Vector2;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CircleShape implements CollisionShape {
    private float radius;


    @Override
    public boolean intersects(Vector2 thisPos, CollisionShape other, Vector2 otherPos) {
        if (other instanceof CircleShape circle) {
            float distance = thisPos.distanceTo(otherPos);
            return distance <= (this.radius + circle.radius);
        }
        // Nếu va chạm với hình khác (ví dụ Rect), gọi ngược lại
        return other.intersects(otherPos, this, thisPos);
    }

    @Override
    public boolean contains(Vector2 thisPos, Vector2 point) {
        return thisPos.distanceTo(point) <= radius;
    }

    @Override
    public float getBoundingRadius() {
        return radius;
    }
}