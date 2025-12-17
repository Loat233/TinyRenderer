package core;

import java.awt.*;
import java.util.Random;

public class openGL {
    private final int Height;
    private final int Width;

    private double[][] model_view;
    private double[][] rotate;
    private double[][] perspective;
    private double[][] viewport;
    private double[][] total_matrix;
    private Vec3 light;

    private double[][] zbuffer;

    public openGL(int x, int y, int Width, int Height){
        this.Width = Width;
        this.Height = Height;
        init_viewport(x, y, Width, Height);
        init_zbuffer();
    }

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

    private static Color pick_ramdom_color() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return new Color(r, g, b);
    }



    public void load_model(Model model ,Color[][] scbuffer) {
        if (model_view == null || rotate == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先调用lookAt和camera函数！");
        }
        //清空zbuffer
        init_zbuffer();
        //遍历所有三角形
        for(int i = 0; i < model.nfaces();i++) {
            //获得三角形面上的三个顶点
            Vec3 a = new Vec3(Matrix.product(total_matrix, model.vert(i, 0).matrix()));
            Vec3 b = new Vec3(Matrix.product(total_matrix, model.vert(i, 1).matrix()));
            Vec3 c = new Vec3(Matrix.product(total_matrix, model.vert(i, 2).matrix()));
            Triangle tri = new Triangle(a, b, c);
            //调用shader
            IShader shader = new IShader(tri, light, model_view);
            rasterise(tri ,shader, scbuffer);
        }

        //遍历所有顶点
        /*  for (int i = 0; i < model.nverts(); i++) {
                Vec3 v = new Vec3(transform(model.vert(i).matrix()));
        }
         */
    }

    public void rasterise(Triangle clip, IShader shader, Color[][] scbuffer) {
        double sign_area = clip.sign_triangle_area();

        if (sign_area < 1) {
            return;
        }
        for(int x = clip.bbminx(); x < clip.bbmaxx(); x++) {
            for (int y = clip.bbminy(); y < clip.bbmaxy(); y++) {
                Vec3 p = new Vec3(x, y, 0);
                Triangle A = new Triangle(p, clip.b(), clip.c());
                Triangle B = new Triangle(p, clip.a(), clip.b());
                Triangle C = new Triangle(p, clip.c(), clip.a());

                double alpha = A.sign_triangle_area() / sign_area;
                double beta = B.sign_triangle_area() / sign_area;
                double gamma = C.sign_triangle_area() / sign_area;
                double z = alpha * clip.a().z() + beta * clip.b().z() + gamma * clip.c().z();

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

                int[] c = shader.fragment();
                Color color = new Color(c[0], c[1], c[2]);
                scbuffer[x][y] = color;
            }
        }
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
