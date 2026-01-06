package core;

import org.w3c.dom.events.EventException;

public class Vertex {
    private final Vec3 coord; // the coordinate of the vertex
    private final Vec3 norm_vector; // the normal vector of the vertex
    private final Vec2 tex_verts; // the vertices from texture response to the vertex

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
