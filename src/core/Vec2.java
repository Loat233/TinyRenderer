package core;

public class Vec2 {
    private final double x;
    private final double y;

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 add(Vec2 v) {
        return new Vec2(x + v.x, y + v.y);
    }

    public Vec2 minus(Vec2 v) {
        return new Vec2(x - v.x, y - v.y);
    }

    public Vec2 product(double n) {
        return new Vec2(x * n, y * n);
    }

    public double product(Vec2 n) {
        return x * n.x + y * n.y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }
}
