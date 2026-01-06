package core;

import org.w3c.dom.events.EventException;

public class Vertex {
    private final Vec3 coord; // 顶点的在空间中的坐标
    private final Vec3 norm_vector; // 顶点对应的法向量
    private final Vec2 tex_verts; // 顶点对应的纹理坐标

    public Vertex(Vec3 coord, Vec3 norm_vector, Vec2 tex_verts) {
        this.coord = coord;
        this.norm_vector = norm_vector;
        this.tex_verts = tex_verts;
    }

    public Vec3 coord() {
        return coord;
    }

    public double x() {
        return coord.x();
    }

    public double y() {
        return coord.y();
    }

    public double z() {
        return coord.z();
    }

    public Vec3 norm_vector() {
        if (norm_vector == null) {
            throw new NullPointerException();
        }
        return norm_vector;
    }

    public Vec2 tex_verts() {
        if (tex_verts == null) {
            throw new NullPointerException();
        }
        return tex_verts;
    }
}
