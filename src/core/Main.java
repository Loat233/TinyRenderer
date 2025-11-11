package core;

import edu.princeton.cs.algs4.StdDraw;

public class Main {
    public static void main(String[] args) {
        int WIDTH = 800;
        int HEIGHT = 800;

        StdDraw.setCanvasSize(WIDTH, HEIGHT);
        StdDraw.enableDoubleBuffering();
        StdDraw.setXscale(0, WIDTH);
        StdDraw.setYscale(0, HEIGHT);
        RenderPixel renderer = new RenderPixel(WIDTH, HEIGHT);
        renderer.load_model("src/obj/diablo3_pose.obj");

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                StdDraw.setPenColor(renderer.get_scbuffer()[x][y]);
                StdDraw.point(x, y);
            }
        }

        StdDraw.show();
    }
}