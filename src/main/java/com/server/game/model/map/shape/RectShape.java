package com.server.game.model.map.shape;

import com.server.game.model.map.component.Vector2;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RectShape extends Shape {
    private float width;
    private float length;
    private Vector2 direction = new Vector2(1, 0); // Default direction (horizontal)

    public RectShape(Vector2 center, float width, float length, Vector2 direction) {
        super(center);
        this.width = width;
        this.length = length;
        this.direction = direction.normalize(); // Ensure direction is normalized
    }

    public RectShape(Vector2 center, float width, float length) {
        super(center);
        this.width = width;
        this.length = length;
    }


    @Override
    public boolean contains(Vector2 point) {

        log.info("Hitbox: center={}, direction={}, width={}, length={}",
            this.center, this.direction, this.width, this.length);

        log.info("Point={}", point);
        
        Vector2 toPoint = point.subtract(center); // vector từ center đến point

        Vector2 dir = direction.normalize(); // trục chính
        Vector2 perp = new Vector2(-dir.y(), dir.x()); // trục phụ (vuông góc)

        float projLength = toPoint.dot(dir);  // chiếu lên trục chính
        float projWidth  = toPoint.dot(perp); // chiếu lên trục phụ

        return Math.abs(projLength) <= length / 2 && Math.abs(projWidth) <= width / 2;
    }


    private boolean satIntersects(RectShape a, Vector2 posA, RectShape b, Vector2 posB) {
        Vector2[] axes = new Vector2[] {
            a.direction,
            new Vector2(-a.direction.y(), a.direction.x()),
            b.direction,
            new Vector2(-b.direction.y(), b.direction.x())
        };

        for (Vector2 axis : axes) {
            // project both rectangles onto axis
            float[] projA = projectRectOntoAxis(a, posA, axis);
            float[] projB = projectRectOntoAxis(b, posB, axis);

            // Check for gap
            if (projA[1] < projB[0] || projB[1] < projA[0]) {
                return false; // có trục phân tách
            }
        }
        return true; // không có trục phân tách → giao nhau
    }

    private float[] projectRectOntoAxis(RectShape rect, Vector2 pos, Vector2 axis) {
        Vector2 dir = rect.direction;
        Vector2 perp = new Vector2(-dir.y(), dir.x());

        Vector2[] corners = new Vector2[] {
            pos.add(dir.multiply(rect.length / 2)).add(perp.multiply(rect.width / 2)),
            pos.add(dir.multiply(rect.length / 2)).add(perp.multiply(-rect.width / 2)),
            pos.add(dir.multiply(-rect.length / 2)).add(perp.multiply(rect.width / 2)),
            pos.add(dir.multiply(-rect.length / 2)).add(perp.multiply(-rect.width / 2)),
        };

        float min = axis.dot(corners[0]);
        float max = min;
        for (int i = 1; i < 4; i++) {
            float proj = axis.dot(corners[i]);
            min = Math.min(min, proj);
            max = Math.max(max, proj);
        }
        return new float[] {min, max};
    }

    private boolean rectVsCircle(Vector2 rectCenter, Vector2 dir, float width, float length,
                              Vector2 circleCenter, float radius) {
        // Tương tự như contains(), chuyển circle về hệ tọa độ local
        Vector2 toCircle = circleCenter.subtract(rectCenter);

        Vector2 perp = new Vector2(-dir.y(), dir.x());

        float projLength = toCircle.dot(dir);
        float projWidth = toCircle.dot(perp);

        float clampedX = Math.max(-length / 2, Math.min(projLength, length / 2));
        float clampedY = Math.max(-width / 2, Math.min(projWidth, width / 2));

        Vector2 closest = rectCenter
            .add(dir.multiply(clampedX))
            .add(perp.multiply(clampedY));

        float distSq = circleCenter.subtract(closest).lengthSquared();
        return distSq <= radius * radius;
    }


    @Override
    public boolean intersects(Vector2 thisPos, Shape other, Vector2 otherPos) {
        if (other instanceof RectShape rect) {
            return satIntersects(this, thisPos, rect, otherPos);
        }
        if (other instanceof CircleShape circle) {
            return rectVsCircle(thisPos, direction, width, length, otherPos, circle.getRadius());
        }
        return false;
    }


    // Returns the bounding radius of the rectangle, which is half the diagonal length
    // This is used for collision detection optimizations
    @Override
    public float getBoundingRadius() {
        return (float) Math.sqrt((width * width + length * length)) / 2;
    }
}