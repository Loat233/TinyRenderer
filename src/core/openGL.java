package core;

import java.awt.*;

public class openGL {
    private final int Height;
    private final int Width;

    private double[][] model_view;
    private double[][] rotate;
    private double[][] perspective;
    private double[][] viewport;
    private double[][] total_matrix;

    private Vec3 light;
    private Texture texture;

    private double[][] zbuffer;

    public openGL(int x, int y, int Width, int Height){
        this.Width = Width;
        this.Height = Height;
        init_viewport(x, y, Width, Height);
        init_zbuffer();
    }

    //用于计算model_view矩阵
    public void lookAt(Vec3 eye, Vec3 center, Vec3 up) {
        Vec3 z_axis = eye.minus(center).normalize();
        Vec3 x_axis = up.cross(z_axis).normalize();
        Vec3 y_axis = z_axis.cross(x_axis).normalize();

        double[][] coordinate = new double[][] {
                {x_axis.x(), x_axis.y(), x_axis.z(), 0},
                {y_axis.x(), y_axis.y(), y_axis.z(), 0},
                {z_axis.x(), z_axis.y(), z_axis.z(), 0},
                {0, 0, 0, 1}
        };
        double[][] transpose_center = new double[][] {
                {1, 0, 0 , -center.x()},
                {0, 1, 0, -center.y()},
                {0, 0, 1, -center.z()},
                {0, 0, 0, 1}
        };
        model_view = Matrix.product(coordinate, transpose_center);
    }

    public void camera(double degree, double fol) {
        init_rotate(degree);
        init_perspective(fol);

        double[][] matrix = Matrix.product(viewport, perspective);
        matrix = Matrix.product(matrix, rotate);
        matrix = Matrix.product(matrix, model_view);
        this.total_matrix = matrix;
    }

    private void init_zbuffer() {
        zbuffer = new double[Height][Width];
        for (int y = 0; y < Height; y++) {
            for (int x = 0; x < Width; x++) {
                zbuffer[y][x] = -1000;
            }
        }
    }

    public void init_light(Vec3 light) {
        this.light = light;
    }

    public void init_texture(Texture texture) {
        this.texture = texture;
    }

    private void init_rotate(double d) {
        rotate = new double[][]{
                {Math.cos(d), 0.0, Math.sin(d), 0.0},
                {0.0, 1.0, 0.0, 0.0},
                {-Math.sin(d), 0.0, Math.cos(d), 0.0},
                {0.0, 0.0, 0.0, 1.0}
        };
    }

    private void init_perspective(double f) {
        perspective = new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, -1/f, 1}
        };
    }

    private void init_viewport(int x, int y, int w, int h) {
        double a = w / 2.0;
        double b = h / 2.0;
        double c = 255 / 2.0;
        viewport = new double[][] {
                {a, 0, 0, x+a},
                {0, b, 0, y+b},
                {0, 0, c, c},
                {0, 0, 0, 1}
        };
    }

    public void load_model(Model model ,Color[][] scbuffer) {
        if (model_view == null || rotate == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先调用lookAt和camera函数！");
        }
        if (texture == null) {
            texture = new Texture("");
            System.out.println("使用default纹理图片");
        }
        //清空zbuffer
        init_zbuffer();
        //遍历所有三角形
        for(int i = 0; i < model.nfaces();i++) {
            //获取三角形面上的三个顶点对应的坐标，法向量，纹理坐标
            Vec3[] coords = new Vec3[3];
            Vec3[] norms = new Vec3[3];
            Vec2[] tex_verts = new Vec2[3];
            for(int j = 0; j < 3; j++) {
                coords[j] = new Vec3(Matrix.product(total_matrix, model.vert(i, j).matrix()));
                norms[j] = new Vec3(Matrix.product(model_view, model.norm(i, j).matrix()));
                tex_verts[j] = model.texcoord(i, j);
            }

            System.out.println();
            Vertex a = new Vertex(coords[0], norms[0], tex_verts[0]);
            Vertex b = new Vertex(coords[1], norms[1], tex_verts[1]);
            Vertex c = new Vertex(coords[2], norms[2], tex_verts[2]);

            Fragment clip = new Fragment(a, b, c);
            //调用shader
            IShader shader = new IShader(light, texture, model_view);
            rasterise(clip, shader, scbuffer);
        }
    }

    public void rasterise(Fragment clip, IShader shader, Color[][] scbuffer) {
        Vec3 a = clip.a().coord();
        Vec3 b = clip.b().coord();
        Vec3 c = clip.c().coord();
        double sign_area = sign_triangle_area(a, b, c);

        if (sign_area < 1) {
            return;
        }
        for(int x = bbmin(a.x(), b.x(), c.x()); x < bbmax(a.x(), b.x(), c.x()); x++) {
            for (int y = bbmin(a.y(), b.y(), c.y()); y < bbmax(a.y(), b.y(), c.y()); y++) {
                Vec3 p = new Vec3(x + 0.5, y + 0.5, 0);

                double alpha = sign_triangle_area(p, b, c) / sign_area;
                double beta = sign_triangle_area(p, a, b) / sign_area;
                double gamma = sign_triangle_area(p, c, a) / sign_area;
                double z = alpha * a.z() + beta * b.z() + gamma * c.z();

                if (alpha < 0 || beta < 0 || gamma < 0) {
                    continue;
                }
                if (x < 0 || y < 0 || x >= Width || y >= Height) {
                    continue;
                }
                if (z <= zbuffer[x][y]) {
                    continue;
                }
                zbuffer[x][y] = z;

                //计算该点重心插值后的法向量，纹理坐标
                Vec3 n = clip.norm_interpolate(alpha, beta, gamma);
                Vec2 t = clip.tex_interpolate(alpha, beta, gamma);

                //对该像素点着色
                double[] colors = shader.fragment(n, t);
                Color color = new Color((int) colors[0], (int) colors[1], (int) colors[2]);
                scbuffer[x][y] = color;
            }
        }
    }

    private static double sign_triangle_area(Vec3 a, Vec3 b, Vec3 c) {
        return 0.5 * ((b.x() - a.x())*(c.y() - a.y()) - (c.x() - a.x())*(b.y() - a.y()));
    }

    public static int bbmin(double x0, double x1, double x2) {
        return (int) Math.min(Math.min(x0, x1), x2);
    }

    public static int bbmax(double x0, double x1, double x2) {
        return (int) Math.max(Math.max(x0, x1), x2);
    }

    private void line(int ax, int ay, int bx, int by, Color color, Color[][] scbuffer) {
        boolean steep = Math.abs(ax - bx) <= Math.abs(ay - by);

        if (steep) {
            int tmp = ax;
            ax = ay;
            ay = tmp;

            tmp = bx;
            bx = by;
            by = tmp;
        }
        if (bx < ax) {
            int tmpX = ax;
            ax = bx;
            bx = tmpX;

            int tmpY = ay;
            ay = by;
            by = tmpY;
        }

        int y = ay;
        double error = 0;
        for (int x = ax; x <= bx; x++) {
            if (steep) {
                scbuffer[y][x] = color;
            }
            else {
                scbuffer[x][y] = color;
            }

            error += 2 * Math.abs(by - ay);
            if (error > bx - ax) {
                y += (by > ay) ? 1 : -1;
                error -= 2 * (bx - ax);
            }
        }
    }
}
