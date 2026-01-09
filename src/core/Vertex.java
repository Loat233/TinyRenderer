package core;

public class Vertex {
    private final Vec3 eye_coord; // 顶点的在eye空间中的坐标
    private final Vec3 view_coord; //   顶点的屏幕坐标
    private final Vec3 eye_norm; // 顶在eye空间的法向量
    private Vec2 tex_coord; // 顶点对应的纹理坐标

    public Vertex(Vec3 eye_coord, Vec3 view_coord, Vec3 eye_norm, Vec2 tex_coord) {
        this.eye_coord = eye_coord;
        this.view_coord = view_coord;
        this.eye_norm = eye_norm;
        this.tex_coord = tex_coord;
    }

    public Vec3 view_coord() {
        return view_coord;
    }

    //  返回用来表示顶点在空间中的坐标
    public Vec3 eye_coord() {
        return eye_coord;
    }

    public double x() {
        return eye_coord.x();
    }

    public double y() {
        return eye_coord.y();
    }

    public double z() {
        return eye_coord.z();
    }


    public Vec3 eye_norm() {
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
}
