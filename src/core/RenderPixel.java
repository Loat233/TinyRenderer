package core;

import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;
import java.util.Random;

public class RenderPixel {
    Model model;
    int HEIGHT;
    int WIDTH;
    int[][] zbuffer;
    Color[][] scbuffer;

    public RenderPixel(int WIDTH, int HEIGHT) {
        this.WIDTH = WIDTH;
        this.HEIGHT = HEIGHT;
        init_zbuffer();
        init_scbuffer();
        set_background();
    }

    public void load_model(String model_filename) {
        this.model = new Model(model_filename);
        //遍历所有三角形
        for(int i = 0; i < model.nfaces();i++) {
            //获得三角形面上的三个顶点
            Vec3 a = model.vert(i, 0);
            Vec3 b = model.vert(i, 1);
            Vec3 c = model.vert(i, 2);

            a = projectVec3(persp(rotate(a)));
            b = projectVec3(persp(rotate(b)));
            c = projectVec3(persp(rotate(c)));

            triangle_area(a.intX(), a.intY(), a.intZ(), b.intX(), b.intY(), b.intZ(), c.intX(), c.intY(), c.intZ(), pick_ramdom_color());
        }

        //遍历所有顶点
        for (int i = 0; i < model.nverts(); i++) {
            Vec3 v = model.vert(i);
            v = projectVec3(persp(rotate(v)));

            if (out_of_bound(projectX(v.x), projectY(v.y))) {
                continue;
            }
            scbuffer[v.intX()][v.intY()] = Color.WHITE;
        }
    }

    public Color[][] get_scbuffer() {
        return scbuffer;
    }

    private boolean out_of_bound(int x, int y) {
        return (x >= WIDTH || x < 0 || y < 0 || y >= HEIGHT);
    }

    private Vec3 rotate(Vec3 v) {
        double degree = Math.PI / 6.0;
        Double[][] matrix = new Double[][]{
                {Math.cos(degree), 0.0, Math.sin(degree)},
                {0.0, 1.0, 0.0},
                {-Math.sin(degree), 0.0, Math.cos(degree)}
        };

        Matrix<Double> rot_matrix = new Matrix<>(Double.class, matrix);
        return new Vec3(rot_matrix.vector_product(v.matrix));
    }

    private Vec3 persp(Vec3 v) {
        double cameraZ = 10.0;
        double factor = (1 - v.z / cameraZ);
        return v.product(factor);
    }

    private Vec3 projectVec3(Vec3 v) {
        int x = (int) ((v.x + 1) * WIDTH / 2);
        int y = (int) ((v.y + 1) * HEIGHT / 2);
        int z = (int) (v.z + 1) * 255 / 2;
        return new Vec3(x, y, z);
    }

    private int projectX(double x) {
        return (int) ((x + 1) * WIDTH / 2);
    }

    private int projectY(double y) {
        return (int) ((y + 1) * HEIGHT / 2);
    }

    private static int projectZ(double z) {
        return (int) (z + 1) * 255 / 2;
    }

    private static double sign_triangle_area(int ax, int ay, int bx, int by, int cx, int cy) {
        return 0.5 * ((by - ay)*(bx + ax) + (ay - cy)*(ax + cx) + (cy - by)*(cx + bx));
    }

    private void triangle_area(int ax, int ay, int az, int bx, int by, int bz, int cx, int cy, int cz, Color color) {
        int bbminx = Math.min(Math.min(ax, bx) ,cx);
        int bbminy = Math.min(Math.min(ay, by) ,cy);
        int bbmaxx = Math.max(Math.max(ax, bx) ,cx);
        int bbmaxy = Math.max(Math.max(ay, by) ,cy);
        double sign_area = sign_triangle_area(ax, ay, bx, by, cx, cy);
        if (sign_area < 1) {
            return;
        }

        for(int x = bbminx; x < bbmaxx; x++) {
            for (int y = bbminy; y < bbmaxy; y++) {
                if (out_of_bound(x, y)) {
                    continue;
                }

                double alpha = sign_triangle_area(x, y, bx, by, cx, cy) / sign_area;
                double beta = sign_triangle_area(x, y, ax, ay, bx, by) / sign_area;
                double gamma = sign_triangle_area(x, y, cx, cy, ax, ay) / sign_area;
                int z = (int) (alpha * az + beta * bz + gamma * cz);

                if (alpha < 0 || beta < 0 || gamma < 0) {
                    continue;
                }

                if (z <= zbuffer[x][y]) {
                    continue;
                }

                zbuffer[x][y] = z;
                scbuffer[x][y] = color;
            }
        }
    }

    private void line(int ax, int ay, int bx, int by, Color color) {
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

    private void set_background() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                scbuffer[y][x] = Color.BLACK;
            }
        }
    }

    private void init_zbuffer() {
        zbuffer = new int[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                zbuffer[y][x] = -1;
            }
        }
    }

    private void init_scbuffer() {
        scbuffer = new Color[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                zbuffer[y][x] = -1;
            }
        }
    }

    private static Color pick_ramdom_color() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return new Color(r, g, b);
    }
}
