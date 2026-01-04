package core;

import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;

public class Main {
    public static void main(String[] args) {
        int WIDTH = 800;
        int HEIGHT = 800;

        StdDraw.setCanvasSize(WIDTH, HEIGHT);
        StdDraw.enableDoubleBuffering();
        StdDraw.setXscale(0, WIDTH);
        StdDraw.setYscale(0, HEIGHT);

        openGL render = new openGL(0, 0, WIDTH, HEIGHT);
        Model model = new Model("src/obj/diablo3_pose.obj");

        render.init_light(new Vec3(1, 1, 1));
        for (int d = 0; d < 360; d += 10) {
            Color[][] scbuffer = new Color[WIDTH][HEIGHT];

            double n = d * Math.PI / 180.0;
            double eye_x = 5 * Math.cos(n);
            double eye_z = 5 * Math.sin(n);

            Vec3 eye = new Vec3(new double[][]{{eye_x, 0, eye_z}});
            Vec3 center = new Vec3(new double[][]{{0, 0, 0}});
            Vec3 up = new Vec3(new double[][]{{0, 1, 0}});

            render.lookAt(eye, center, up);
            render.camera(0, eye.minus(center).norm());
            render.load_model(model, scbuffer);

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    Color p = scbuffer[y][x];
                    p = (p == null) ? Color.BLACK : p;
                    StdDraw.setPenColor(p);
                    StdDraw.point(y, x);
                }
            }

            StdDraw.show();
            StdDraw.clear();
        }
    }
}