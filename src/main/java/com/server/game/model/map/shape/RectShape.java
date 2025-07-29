package com.server.game.model.map.shape;

import com.server.game.model.map.component.Vector2;

public class RectShape implements CollisionShape {
    private float width;
    private float height;

    public RectShape(float width, float height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean intersects(Vector2 thisPos, CollisionShape other, Vector2 otherPos) {
        if (other instanceof RectShape rect) {
            return Math.abs(thisPos.x() - otherPos.x()) <= (this.width + rect.width) / 2 &&
                   Math.abs(thisPos.y() - otherPos.y()) <= (this.height + rect.height) / 2;
        }
        if (other instanceof CircleShape circle) {
            // Kiểm tra va chạm Circle vs Rect (AABB vs Circle)
            return CollisionShape.circleVsRect(otherPos, circle.getRadius(), thisPos, width, height);
        }
        return false;
    }

    @Override
    public boolean contains(Vector2 thisPos, Vector2 point) {
        return Math.abs(thisPos.x() - point.x()) <= width / 2 &&
               Math.abs(thisPos.y() - point.y()) <= height / 2;
    }


    // Returns the bounding radius of the rectangle, which is half the diagonal length
    // This is used for collision detection optimizations
    @Override
    public float getBoundingRadius() {
        return (float) Math.sqrt((width * width + height * height)) / 2;
    }
}