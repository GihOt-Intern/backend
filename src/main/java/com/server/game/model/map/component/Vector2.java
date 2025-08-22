package com.server.game.model.map.component;


public record Vector2(float x, float y) {

    public float distanceTo(Vector2 other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public Vector2 add(Vector2 other) { return new Vector2(x + other.x, y + other.y); }

    public Vector2 subtract(Vector2 other) { return new Vector2(x - other.x, y - other.y); }

    public Vector2 multiply(float scalar) { return new Vector2(x * scalar, y * scalar); }

    public float dot(Vector2 other) {
        return x * other.x + y * other.y;
    }

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
    
    public float lengthSquared() {
        return (float) (x * x + y * y);
    }


    public float distance(Vector2 other) {
        return this.subtract(other).length();
    }

    public boolean isInRectangle(Vector2 rectCenter, float width, float length) {
        return rectCenter.x() - width / 2 <= this.x && this.x <= rectCenter.x() + width / 2 &&
               rectCenter.y() - length / 2 <= this.y && this.y <= rectCenter.y() + length / 2;
    }

    public Vector2 directionTo(Vector2 other) {
        return other.subtract(this).normalize();
    }

    // // in radians
    // public float getRotateDegree() {
    //     Vector2 origin = new Vector2(1, 0); // Reference vector (1, 0)
    //     return (float) Math.acos(this.dot(origin) / (this.length() * origin.length()));
    // }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    
}
