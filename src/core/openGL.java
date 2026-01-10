package core;

import java.awt.*;

public class openGL {
    private final int Height;
    private final int Width;

    private double[][] model_view;
    private double[][] rotate;
    private double[][] perspective;
    private double[][] viewport;
    //  合并矩阵
    private double[][] globalToEyeMatrix;
    private double[][] normMatrix;
    private double[][] eyeToScreenMatrix;


    private Vec3 light;
    private double[][] zbuffer;

    //  模型数量
    private int model_num;
    //  设置翻转
    private boolean isUpsideDown;
    //  模型自旋转角度
    private double degree;


    public openGL(int x, int y, int Width, int Height, int model_num){
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
                {a, 0, 0, x+a},
                {0, b, 0, y+b},
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



    public void init_light(Vec3 light) {
        this.light = light;
    }

    //  用于计算model_view矩阵
    public void camera(Vec3 eye, Vec3 center, Vec3 up, double fol) {
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
        init_perspective(fol);
    }
    private void init_perspective(double f) {
        /*  标准透视矩阵计算
        fov = fov * Math.PI / 180.0;
        double fh = Math.tan(fov / 2);
        double aspect = (double) Width / (double) Height;

        perspective = new double[][] {
                {fh, 0, 0, 0},
                {0, fh * aspect, 0, 0},
                {0, 0, (f + n) / (n - f), 2 * f * n / (n - f)},
                {0, 0, -1, 0}
        };
         */
        perspective = new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, -1/f, 1}
        };
    }

    //  模型围绕center点旋转
    public void model_direct(double degree) {
        if (degree != 0.0) {
            this.degree = degree;
            init_rotate(degree);
        }
        init_matrix();
    }
    private void init_rotate(double d) {
        rotate = new double[][]{
                {Math.cos(d), 0.0, Math.sin(d), 0.0},
                {0.0, 1.0, 0.0, 0.0},
                {-Math.sin(d), 0.0, Math.cos(d), 0.0},
                {0.0, 0.0, 0.0, 1.0}
        };
    }

    private void init_matrix() {
        this.eyeToScreenMatrix = Matrix.product(viewport, perspective);
        if (degree == 0) {
            globalToEyeMatrix = model_view;
        }
        else {
            globalToEyeMatrix = Matrix.product(rotate, model_view);
        }
        double[][] inverse = Matrix.inverse(globalToEyeMatrix);
        this.normMatrix = Matrix.transpose(inverse);
    }



    public void render_model(Model model , int[] screen) {
        if (model_view == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先调用camera函数！");
        }

        //  清空zbuffer
        init_zbuffer();
        //  遍历所有三角形
        for (int i = 0; i < model.nfaces();i++) {
            //  获取三角形面上的三个顶点对应的eye坐标,屏幕坐标,eye空间的法向量,纹理坐标
            Vec3[] eye_coords = new Vec3[3];
            Vec3[] view_coords = new Vec3[3];
            Vec3[] eye_norms = new Vec3[3];
            Vec2[] tex_coords = new Vec2[3];

            for(int j = 0; j < 3; j++) {
                //  计算顶点的eye空间坐标
                double[][] M = Matrix.product(globalToEyeMatrix, model.vert(i, j).matrix());
                eye_coords[j] = new Vec3(M[0][0], M[1][0], M[2][0]);
                //  计算顶点的屏幕坐标
                M = Matrix.product(eyeToScreenMatrix, M);
                view_coords[j] = new Vec3(M);    //  这里会进行透视除法
                //  利用文件提供的顶点在global空间的法向量，计算顶点在eye空间的法向量
                eye_norms[j] = new Vec3(Matrix.product(normMatrix, model.norm(i, j).matrix()));
                tex_coords[j] = model.texcoord(i, j);
            }

            Vertex a = new Vertex(eye_coords[0], view_coords[0], eye_norms[0], tex_coords[0]);
            Vertex b = new Vertex(eye_coords[1], view_coords[1], eye_norms[1], tex_coords[1]);
            Vertex c = new Vertex(eye_coords[2], view_coords[2], eye_norms[2], tex_coords[2]);

            Fragment clip = new Fragment(a, b, c);
            //  调用shader
            IShader shader = new IShader(light, model.textures(), globalToEyeMatrix);
            rasterise(clip, shader, screen);
        }
    }



    public void rasterise(Fragment clip, IShader shader, int[] screen) {
        Vec3 a = clip.a().view_coord();
        Vec3 b = clip.b().view_coord();
        Vec3 c = clip.c().view_coord();

        double sign_area = sign_triangle_area(a, b, c);

        if (sign_area < 1) {
            return;
        }
        for(int x = bbmin(a.x(), b.x(), c.x()); x < bbmax(a.x(), b.x(), c.x()); x++) {
            for (int y = bbmin(a.y(), b.y(), c.y()); y < bbmax(a.y(), b.y(), c.y()); y++) {
                Vec3 p = new Vec3(x + 0.5, y + 0.5, 0);

                //  计算在2D屏幕上的插值
                double alpha = sign_triangle_area(p, b, c) / sign_area;
                double beta = sign_triangle_area(p, a, b) / sign_area;
                double gamma = 1 - alpha - beta;
                double z = alpha * a.z() + beta * b.z() + gamma * c.z();

                //  允许小的容差值
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

                /*
                //  计算屏幕上的点p对应在eye空间上的插值
                double persp_a = alpha / clip.a().view_coord().z();
                double persp_b = beta / clip.b().view_coord().z();
                double persp_c = gamma / clip.c().view_coord().z();
                double persp_w = persp_a + persp_b + persp_c;

                double persp_alpha = persp_a / persp_w;
                double persp_beta = persp_b / persp_w;
                double persp_gamma = persp_c / persp_w;
                 */

                //  计算屏幕上的点p对应在eye空间上的对应点ep
                Vec3 ep = clip.p_interpolate(alpha, beta, gamma);

                //  计算ep在eye空间上的插值
                Vec3 ea = clip.a().eye_coord();
                Vec3 eb = clip.b().eye_coord();
                Vec3 ec = clip.c().eye_coord();

                double eye_sign_area = sign_triangle_area(ea, eb, ec);
                double eye_alpha = sign_triangle_area(ep, eb, ec) / eye_sign_area;
                double eye_beta = sign_triangle_area(ep, ea, eb) / eye_sign_area;
                double eye_gamma = 1 - eye_alpha - eye_beta;


                //  使用eye空间的片段插值, 来计算该像素点的法线
                Vec3 n = clip.norm_interpolate(eye_alpha, eye_beta, eye_gamma);
                //  使用屏幕上的片段插值, 来计算纹理坐标
                Vec2 t = clip.tex_interpolate(alpha, beta, gamma);

                double[] colors = shader.fragment(clip, n, t);

                //  对该像素点着色
                int R = (int) colors[0];
                int G = (int) colors[1];
                int B = (int) colors[2];

                int screenY;
                if (isUpsideDown) {
                    screenY = y;
                }
                else {
                    screenY = Height - 1 - y;
                }
                screen[x + screenY * Width] = (R << 16) | (G << 8) | B;
            }
        }
    }

    private static double sign_triangle_area(Vec3 a, Vec3 b, Vec3 c) {
        return 0.5 * ((b.x() - a.x())*(c.y() - a.y()) - (c.x() - a.x())*(b.y() - a.y()));
    }

    public static int bbmin(double x0, double x1, double x2) {
        return (int) Math.floor(Math.min(Math.min(x0, x1), x2));    //  边界框向下取整
    }

    public static int bbmax(double x0, double x1, double x2) {
        return (int) Math.ceil(Math.max(Math.max(x0, x1), x2));     //  边界框向上取整
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

    public void setUpsideDown() {
        isUpsideDown = true;
    }
}
