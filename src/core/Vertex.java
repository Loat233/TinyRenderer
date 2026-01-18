package core;

public class Vertex {
    private final Vec3 eye_coord;   // 顶点的在eye空间中的坐标
    private final Vector eye_norm;  // 顶在eye空间的法向量
    private final Vec3 clip_coord;  // 顶点的clip坐标
    private Vec2 tex_coord;     // 顶点对应的纹理坐标

    public Vertex(Vec3 eye_coord, Vector eye_norm, Vec3 clip_coord, Vec2 tex_coord) {
        this.eye_coord = eye_coord;
        this.eye_norm = eye_norm;
        this.clip_coord = clip_coord;
        this.tex_coord = tex_coord;
    }

    public Vec3 clip_coord() {
        return clip_coord;
    }

    //  返回用来表示顶点在空间中的坐标
    public Vec3 eye_coord() {
        return eye_coord;
    }

    public Vector eye_norm() {
        if (eye_norm == null) {
            throw new NullPointerException();
        }
        return eye_norm;
    }

    public Vec2 tex_coord() {
        if (tex_coord == null) {
            tex_coord = new Vec2(0.0, 0.0);
        }
        return tex_coord;
    }

    public double clip_recip_w() {
        return 1 / clip_coord.w();
    }
}
