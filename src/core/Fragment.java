package core;

public class Fragment {
    private final Vertex a;
    private final Vertex b;
    private final Vertex c;

    public Fragment(Vertex a, Vertex b, Vertex c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    //  计算屏幕上的p在eye空间下的插值坐标
    public Vec3 p_interpolate(double alpha, double beta, double gamma) {
        Vec3 v0 = a.eye_coord().scale(alpha);
        Vec3 v1 = b.eye_coord().scale(beta);
        Vec3 v2 = c.eye_coord().scale(gamma);
        return v0.add(v1).add(v2);
    }

    //  计算在eye空间下的插值法线
    public Vector norm_interpolate(double alpha, double beta, double gamma) {
        Vector v0 = a.eye_norm().scale(alpha);
        Vector v1 = b.eye_norm().scale(beta);
        Vector v2 = c.eye_norm().scale(gamma);
        return v0.add(v1).add(v2);
    }

    //  计算在uv平面纹理图下的插值坐标(即该像素点对应的纹理坐标)
    public Vec2 tex_interpolate(double alpha, double beta, double gamma) {
        Vec2 v0 = a.tex_coord().product(alpha);
        Vec2 v1 = b.tex_coord().product(beta);
        Vec2 v2 = c.tex_coord().product(gamma);
        return v0.add(v1).add(v2);
    }

    public Vertex a() {
        return a;
    }
    public Vertex b() {
        return b;
    }
    public Vertex c() {
        return c;
    }
}
