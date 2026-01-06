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

    public Vec3 norm_interpolate(double alpha, double beta, double gamma) {
        Vec3 v0 = a.norm_vector().product(alpha);
        Vec3 v1 = b.norm_vector().product(beta);
        Vec3 v2 = c.norm_vector().product(gamma);
        return v0.add(v1).add(v2);
    }

    public Vec2 tex_interpolate(double alpha, double beta, double gamma) {
        Vec2 v0 = a.tex_verts().product(alpha);
        Vec2 v1 = b.tex_verts().product(beta);
        Vec2 v2 = c.tex_verts().product(gamma);
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
