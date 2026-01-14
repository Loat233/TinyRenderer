package core;

import java.awt.*;

public class openGL {
    private final int Height;
    private final int Width;

    private double[][] model_view;
    private double[][] rotate;
    private double[][] perspective;
    private double[][] viewport;
    // 合并矩阵
    private double[][] globalToEyeMatrix;
    private double[][] normMatrix;

    private Vector light;
    private double[][] zbuffer;

    // 模型数量
    private int model_num;
    // 设置翻转
    private boolean isUpsideDown;
    // 模型自旋转角度
    private double degree;

    public openGL(int x, int y, int Width, int Height, int model_num) {
        this.Width = Width;
        this.Height = Height;
        init_viewport(x, y, Width, Height);
        init_zbuffer();
        this.model_num = model_num;
        isUpsideDown = false;
        degree = 0.0;
    }

    private void init_viewport(int x, int y, int w, int h) {
        double a = w / 2.0;
        double b = h / 2.0;
        double c = 255 / 2.0;
        viewport = new double[][] {
                {a, 0, 0, x + a},
                {0, b, 0, y + b},
                {0, 0, c, c},
                {0, 0, 0, 1}
        };
    }

    private void init_zbuffer() {
        zbuffer = new double[Height][Width];
        for (int y = 0; y < Height; y++) {
            for (int x = 0; x < Width; x++) {
                zbuffer[y][x] = Double.NEGATIVE_INFINITY;
            }
        }
    }

    public void init_light(Vector light) {
        this.light = light;
    }

    // 用于计算model_view矩阵
    public void camera(Vec3 eye, Vec3 center, Vector up, double fol) {
        Vector z_axis = eye.minus(center).normalize();
        Vector x_axis = up.cross(z_axis).normalize();
        Vector y_axis = z_axis.cross(x_axis).normalize();

        double[][] coordinate = new double[][] {
                {x_axis.x(), x_axis.y(), x_axis.z(), 0},
                {y_axis.x(), y_axis.y(), y_axis.z(), 0},
                {z_axis.x(), z_axis.y(), z_axis.z(), 0},
                {0, 0, 0, 1 }
        };
        double[][] transpose_center = new double[][] {
                {1, 0, 0, -center.x()},
                {0, 1, 0, -center.y()},
                {0, 0, 1, -center.z()},
                {0, 0, 0, 1}
        };
        model_view = Matrix.product(coordinate, transpose_center);
        init_perspective(fol);
    }

    private void init_perspective(double f) {
        /*
         * 标准透视矩阵计算
         * fov = fov * Math.PI / 180.0;
         * double fh = Math.tan(fov / 2);
         * double aspect = (double) Width / (double) Height;
         * 
         * perspective = new double[][] {
         * {fh, 0, 0, 0},
         * {0, fh * aspect, 0, 0},
         * {0, 0, (f + n) / (n - f), 2 * f * n / (n - f)},
         * {0, 0, -1, 0}
         * };
         */
        perspective = new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, -1 / f, 1}
        };
    }

    // 模型围绕center点旋转
    public void model_direct(double degree) {
        if (degree != 0.0) {
            this.degree = degree;
            init_rotate(degree);
        }
        init_matrix();
    }

    private void init_rotate(double d) {
        rotate = new double[][] {
                {Math.cos(d), 0.0, Math.sin(d), 0.0},
                {0.0, 1.0, 0.0, 0.0},
                {-Math.sin(d), 0.0, Math.cos(d), 0.0},
                {0.0, 0.0, 0.0, 1.0}
        };
    }

    private void init_matrix() {
        if (degree == 0) {
            globalToEyeMatrix = model_view;
        } else {
            globalToEyeMatrix = Matrix.product(model_view, rotate);
        }
        double[][] M = Matrix.inverse(globalToEyeMatrix);
        this.normMatrix = Matrix.eliminate(Matrix.transpose(M));
    }

    public void render_model(Model model, int[] screen) {
        if (model_view == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先调用camera函数！");
        }

        // 清空zbuffer
        init_zbuffer();
        // 遍历所有三角形
        for (int i = 0; i < model.nfaces(); i++) {
            // 获取三角形面上的三个顶点对应的eye坐标,clip坐标,eye空间的法向量,纹理坐标
            Vec3[] eye_coords = new Vec3[3];
            Vec3[] clip_coords = new Vec3[3];
            Vector[] eye_norms = new Vector[3];
            Vec2[] tex_coords = new Vec2[3];

            for (int j = 0; j < 3; j++) {
                // 计算顶点的eye空间坐标
                double[][] M = Matrix.product(globalToEyeMatrix, model.vert(i, j).matrix());
                eye_coords[j] = new Vec3(M[0][0], M[1][0], M[2][0], M[3][0]);
                // 计算顶点的clip坐标
                clip_coords[j] = new Vec3(Matrix.product(perspective, M));
                // 利用文件提供的顶点在global空间的法向量，计算顶点在eye空间的法向量
                eye_norms[j] = new Vector(Matrix.product(normMatrix, model.norm(i, j).matrix())).normalize();
                tex_coords[j] = model.texcoord(i, j);
            }

            Vertex a = new Vertex(eye_coords[0], eye_norms[0], clip_coords[0], tex_coords[0]);
            Vertex b = new Vertex(eye_coords[1], eye_norms[1], clip_coords[1], tex_coords[1]);
            Vertex c = new Vertex(eye_coords[2], eye_norms[2], clip_coords[2], tex_coords[2]);

            Fragment clip = new Fragment(a, b, c);
            // 调用shader
            IShader shader = new IShader(light, model.textures(), Matrix.eliminate(model_view), normMatrix);
            rasterise(clip, shader, screen);
        }
    }

    public void rasterise(Fragment clip, IShader shader, int[] screen) {
        //  除以w分量
        double rp_aw = clip.a().clip_recip_w();
        double rp_bw = clip.b().clip_recip_w();
        double rp_cw = clip.c().clip_recip_w();
        //  得到ndc坐标点
        Vec3 a = clip.a().clip_coord().scale(rp_aw);
        Vec3 b = clip.b().clip_coord().scale(rp_bw);
        Vec3 c = clip.c().clip_coord().scale(rp_cw);

        // 得到屏幕坐标
        a = new Vec3(Matrix.product(viewport, a.matrix()));
        b = new Vec3(Matrix.product(viewport, b.matrix()));
        c = new Vec3(Matrix.product(viewport, c.matrix()));

        double sign_area = sign_triangle_area(a, b, c);

        if (sign_area < 1) {
            return;
        }
        for (int x = bbmin(a.x(), b.x(), c.x()); x < bbmax(a.x(), b.x(), c.x()); x++) {
            for (int y = bbmin(a.y(), b.y(), c.y()); y < bbmax(a.y(), b.y(), c.y()); y++) {
                Vec3 p = new Vec3(x + 0.5, y + 0.5, 0, 1);

                // 计算在2D屏幕上的插值
                double alpha = sign_triangle_area(p, b, c) / sign_area;
                double beta = sign_triangle_area(p, c, a) / sign_area;
                double gamma = 1 - alpha - beta;
                double z = alpha * a.z() + beta * b.z() + gamma * c.z();

                // 允许小的容差值
                final double Epsilon = 1e-10;
                if (alpha < -Epsilon || beta < -Epsilon || gamma < -Epsilon) {
                    continue;
                }
                if (x < 0 || y < 0 || x >= Width || y >= Height) {
                    continue;
                }
                if (z <= zbuffer[y][x]) {
                    continue;
                }
                zbuffer[y][x] = z;

                // 计算屏幕上的点p的透视校正后的重心坐标
                double persp_a = alpha * rp_aw;
                double persp_b = beta * rp_bw;
                double persp_c = gamma * rp_cw;
                double area = 1 / (persp_a + persp_b + persp_c);

                double eye_alpha = persp_a * area;
                double eye_beta = persp_b * area;
                double eye_gamma = persp_c * area;

                // 使用eye空间的片段插值, 来计算该像素点的法线
                Vector n = clip.norm_interpolate(eye_alpha, eye_beta, eye_gamma);
                // 使用屏幕上的片段插值, 来计算纹理坐标
                Vec2 t = clip.tex_interpolate(eye_alpha, eye_beta, eye_gamma);

                // 对该像素点着色
                int[] colors = shader.fragment(clip, n, t);
                int screenY;
                if (isUpsideDown) {
                    screenY = y;
                } else {
                    screenY = Height - 1 - y;
                }
                screen[x + screenY * Width] = (colors[0] << 16) | (colors[1] << 8) | colors[2];
            }
        }
    }

    private static double sign_triangle_area(Vec3 a, Vec3 b, Vec3 c) {
        return 0.5 * ((b.x() - a.x()) * (c.y() - a.y()) - (c.x() - a.x()) * (b.y() - a.y()));
    }

    public static int bbmin(double x0, double x1, double x2) {
        return (int) Math.floor(Math.min(Math.min(x0, x1), x2)); // 边界框向下取整
    }

    public static int bbmax(double x0, double x1, double x2) {
        return (int) Math.ceil(Math.max(Math.max(x0, x1), x2)); // 边界框向上取整
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
            } else {
                scbuffer[x][y] = color;
            }

            error += 2 * Math.abs(by - ay);
            if (error > bx - ax) {
                y += (by > ay) ? 1 : -1;
                error -= 2 * (bx - ax);
            }
        }
    }

    public void setUpsideDown() {
        isUpsideDown = true;
    }
}
