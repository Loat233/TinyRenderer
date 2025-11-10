package core;

import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;
import java.util.Random;

public class RenderPixel {
    Model model;
    int HEIGHT;
    int WIDTH;
    int[][] zbuffer;
    Color[][] screen_buffer;

    public RenderPixel(int HEIGHT, int WIDTH) {
        this.HEIGHT = HEIGHT;
        this.WIDTH = WIDTH;
        init_zbuffer();
    }

    public void load_model(String model_filename) {
        this.model = new Model(model_filename);
    }

    public static void set_background_color(Color color, int width, int height) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                StdDraw.setPenColor(color);
                StdDraw.point(x, y);
            }
        }
    }

    public static Color pick_ramdom_color() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return new Color(r, g, b);
    }

    private void init_zbuffer() {
        zbuffer = new int[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                zbuffer[y][x] = -1;
            }
        }
    }

    public static double sign_triangle_area(int ax, int ay, int bx, int by, int cx, int cy) {
        return 0.5 * ((by - ay)*(bx + ax) + (ay - cy)*(ax + cx) + (cy - by)*(cx + bx));
    }

    public static void triangle_area(int ax, int ay, int bx, int by, int cx, int cy, Color color) {
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
                double alpha = sign_triangle_area(x, y, bx, by, cx, cy) / sign_area;
                double beta = sign_triangle_area(x, y, ax, ay, bx, by) / sign_area;
                double gamma = sign_triangle_area(x, y, cx, cy, ax, ay) / sign_area;

                if (alpha < 0 || beta < 0 || gamma < 0) {
                    continue;
                }

                StdDraw.setPenColor(color);
                StdDraw.point(x, y);
            }
        }
    }

    public static void triangle_area(int ax, int ay, int az, int bx, int by, int bz, int cx, int cy, int cz, int[][] zbuffer, Color color) {
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
                StdDraw.setPenColor(color);
                StdDraw.point(x, y);
            }
        }
    }

    public static void triangle_area(int ax, int ay, int az, int bx, int by, int bz, int cx, int cy, int cz) {
        int bbminx = Math.min(Math.min(ax, bx) ,cx);
        int bbminy = Math.min(Math.min(ay, by) ,cy);
        int bbmaxx = Math.max(Math.max(ax, bx) ,cx);
        int bbmaxy = Math.max(Math.max(ay, by) ,cy);
        double sign_area = sign_triangle_area(ax, ay, bx, by, cx, cy);

        for(int x = bbminx; x < bbmaxx; x++) {
            for (int y = bbminy; y < bbmaxy; y++) {
                double alpha = sign_triangle_area(x, y, bx, by, cx, cy) / sign_area;
                double beta = sign_triangle_area(x, y, cx, cy, ax, ay) / sign_area;
                double gamma = sign_triangle_area(x, y, ax, ay, bx, by) / sign_area;

                if (alpha < 0 || beta < 0 || gamma < 0) {
                    continue;
                }

                Color color;
                double bound = 0.1;
                if (alpha > bound && alpha < 1 - bound && beta > bound && beta < 1 - bound && gamma > bound && gamma < 1 - bound) {
                    color = Color.BLACK;
                }
                else {
                    color = new Color((int) (alpha * 255), (int) (beta * 255), (int) (gamma * 255));
                }
                StdDraw.setPenColor(color);
                StdDraw.point(x, y);
            }
        }
    }

    public static void line(int ax, int ay, int bx, int by, Color color) {
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

        long y = ay;
        double error = 0;
        for (int x = ax; x <= bx; x++) {

            StdDraw.setPenColor(color);
            if (steep) {
                StdDraw.point(y, x);
            }
            else {
                StdDraw.point(x, y);
            }

            error += 2 * Math.abs(by - ay);
            if (error > bx - ax) {
                y += (by > ay) ? 1 : -1;
                error -= 2 * (bx - ax);
            }
        }
    }

    public static int projectX(double x, int width) {
        return (int) ((x + 1) * width / 2);
    }

    public static int projectY(double y, int height) {
        return (int) ((y + 1) * height / 2);
    }

    public static int projectZ(double z) {
        return (int) (z + 1) * 255 / 2;
    }
}
