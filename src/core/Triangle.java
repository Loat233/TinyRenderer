package core;

public class Triangle {
    private final Vec3 a;
    private final Vec3 b;
    private final Vec3 c;

    public Triangle(Vec3 a, Vec3 b, Vec3 c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public double sign_triangle_area() {
        return 0.5 * ((b.x() - a.x())*(c.y() - a.y()) - (c.x() - a.x())*(b.y() - a.y()));
    }

    public int bbminx() {
        return (int) Math.min(Math.min(a.x(), b.x()), c.x());
    }
    public int bbmaxx() {
        return (int) Math.max(Math.max(a.x(), b.x()), c.x());
    }
    public int bbminy() {
        return (int) Math.min(Math.min(a.y(), b.y()), c.y());
    }
    public int bbmaxy() {
        return (int) Math.max(Math.max(a.y(), b.y()), c.y());
    }


    public Vec3 a() {
        return a;
    }
    public Vec3 b() {
        return b;
    }
    public Vec3 c() {
        return c;
    }
}
