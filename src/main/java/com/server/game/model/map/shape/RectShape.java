package com.server.game.model.map.shape;

import com.server.game.model.map.component.Vector2;

public class RectShape extends Shape {
    private float width;
    private float length;

    public RectShape(Vector2 center, float width, float length) {
        super(center);
        this.width = width;
        this.length = length;
    }

    @Override
    public boolean intersects(Vector2 thisPos, Shape other, Vector2 otherPos) {
        if (other instanceof RectShape rect) {
            return Math.abs(thisPos.x() - otherPos.x()) <= (this.width + rect.width) / 2 &&
                   Math.abs(thisPos.y() - otherPos.y()) <= (this.length + rect.length) / 2;
        }
        if (other instanceof CircleShape circle) {
            // Kiểm tra va chạm Circle vs Rect (AABB vs Circle)
            return Shape.circleVsRect(otherPos, circle.getRadius(), thisPos, width, length);
        }
        return false;
    }

    @Override
    public boolean contains(Vector2 point) {
        return Math.abs(center.x() - point.x()) <= width / 2 &&
               Math.abs(center.y() - point.y()) <= length / 2;
    }


    // Returns the bounding radius of the rectangle, which is half the diagonal length
    // This is used for collision detection optimizations
    @Override
    public float getBoundingRadius() {
        return (float) Math.sqrt((width * width + length * length)) / 2;
    }
}