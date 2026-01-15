package core;

import java.awt.*;
import java.util.Arrays;

public class openGL {
    // 用户输入的参数
    private final int startX;
    private final int startY;
    private final int Height;   // 屏幕高
    private final int Width;    // 屏幕宽
    private double degree;      // 模型自旋转角度
    private double fol;         // 视角广度
    private Vec3 lgPos;         // 光源global位置
    private Vec3 eye;           // 相机global位置
    private Vec3 center;
    private Vector up;

    // 合并矩阵
    private double[][] eyeView;
    private double[][] normMatrix;
    private double[][] shadClipMatrix;
    private double[][] shadowMatrix;
    // 光源eye坐标
    private Vec3 lgEyePos;
    // 深度图
    private double[] shad_zbuffer;
    private double[] zbuffer;
    // 调试参数
    private boolean isUpsideDown; // 设置翻转



    public openGL(int x, int y, int Width, int Height) {
        this.startX = x;
        this.startY = y;
        this.Width = Width;
        this.Height = Height;

        shad_zbuffer = clear_zbuffer(Width, Height);
        zbuffer = clear_zbuffer(Width, Height);
        isUpsideDown = false;
    }

    public void init_lightPos(Vec3 lightPos) {
        this.lgPos = lightPos;
    }
    // 用于计算model_view矩阵
    public void camera(Vec3 eye, Vec3 center, Vector up, double fol) {
        this.eye = eye;
        this.center = center;
        this.up = up;
        this.fol = fol;
    }
    // 模型围绕center点旋转
    public void model_direct(double degree) {
        this.degree = degree;
    }



    private Vector[] getBases(Vec3 eye, Vec3 center, Vector up) {
        Vector z_axis = eye.minus(center).normalize();
        Vector x_axis = up.cross(z_axis).normalize();
        Vector y_axis = z_axis.cross(x_axis).normalize();

        return new Vector[]{x_axis, y_axis, z_axis};
    }

    private double[][] getCoordinate(Vector[] bases) {
        Vector x_axis = bases[0];
        Vector y_axis = bases[1];
        Vector z_axis = bases[2];
        return new double[][] {
                { x_axis.x(), x_axis.y(), x_axis.z(), 0 },
                { y_axis.x(), y_axis.y(), y_axis.z(), 0 },
                { z_axis.x(), z_axis.y(), z_axis.z(), 0 },
                { 0, 0, 0, 1 }
        };
    }

    private double[][] getTCenter(Vec3 center) {
        return new double[][] {
                { 1, 0, 0, -center.x() },
                { 0, 1, 0, -center.y() },
                { 0, 0, 1, -center.z() },
                { 0, 0, 0, 1 }
        };
    }

    private double[][] getRotate(double d) {
        return new double[][] {
                { Math.cos(d), 0.0, Math.sin(d), 0.0 },
                { 0.0, 1.0, 0.0, 0.0 },
                { -Math.sin(d), 0.0, Math.cos(d), 0.0 },
                { 0.0, 0.0, 0.0, 1.0 }
        };
    }

    private double[][] getModelView(double[][] coordinate, double[][] transpose_center) {
        return Matrix.product(coordinate, transpose_center);
    }

    private double[][] getEyeView(double[][] model_view) {
        if (degree == 0) {
            return model_view;
        }
        else {
            double[][] rotate = getRotate(degree);
            return Matrix.product(model_view, rotate);
        }
    }

    private double[][] getPerspective(double f) {
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
        return new double[][] {
                { 1, 0, 0, 0 },
                { 0, 1, 0, 0 },
                { 0, 0, 1, 0 },
                { 0, 0, -1 / f, 1 }
        };
    }

    private double[][] getViewport(int x, int y, int w, int h) {
        double a = w / 2.0;
        double b = h / 2.0;
        double c = 100000.0 / 2.0;
        return new double[][] {
                { a, 0, 0, x + a },
                { 0, b, 0, y + b },
                { 0, 0, c, c },
                { 0, 0, 0, 1 }
        };
    }

    private double[] clear_zbuffer(int width, int height) {
        double[] buffer = new double[height * width];
        Arrays.fill(buffer, Double.NEGATIVE_INFINITY);
        return buffer;
    }

    public void render(Model[] models, int[] screen) {
        double[][] coordinate;
        double[][] t_center;
        double[][] modelView;
        double[][] perspective;
        double[][] viewport;
        //  加载阴影图需要的矩阵
        coordinate = getCoordinate(getBases(lgPos, center, up));
        t_center = getTCenter(center);
        modelView = getModelView(coordinate, t_center);
        perspective = getPerspective(fol);
        viewport = getViewport(startX, startY, Width, Height);
        //  加载合并矩阵
        this.shadClipMatrix = Matrix.product(perspective, modelView);
        //  加载阴影图
        render_shadMap(models, modelView, perspective, viewport);

        //  加载模型需要的矩阵
        coordinate = getCoordinate(getBases(eye, center, up));
        t_center = getTCenter(center);
        modelView = getModelView(coordinate, t_center);
        perspective = getPerspective(fol);
        viewport = getViewport(startX, startY, Width, Height);
        //  加载合并矩阵
        this.eyeView = getEyeView(modelView);
        this.lgEyePos = new Vec3(Matrix.product(modelView, lgPos.matrix()));
        double[][] inv_eyeView = Matrix.inverse(eyeView);
        this.shadowMatrix = Matrix.product(shadClipMatrix, inv_eyeView);
        this.normMatrix = Matrix.eliminate(Matrix.transpose(inv_eyeView));
        //  加载模型和光影
        render_model(models, screen, modelView, perspective, viewport);
    }

    private void render_shadMap(Model[] models, double[][] modelView, double[][] perspective, double[][] viewport) {
        if (modelView == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先加载矩阵!");
        }
        // 遍历所有模型
        for (Model model : models) {
            // 遍历所有三角形
            for (int i = 0; i < model.nfaces(); i++) {
                // 获取三角形面上的三个顶点对应的屏幕坐标
                Vec3[] coords = new Vec3[3];
                for (int j = 0; j < 3; j++) {
                    Vec3 clip_v = new Vec3(Matrix.product(shadClipMatrix, model.vert(i, j).matrix())); // 计算顶点的clip空间坐标
                    Vec3 ndc_v = clip_v.scale(1 / clip_v.w()); // 计算顶点的ndc坐标
                    coords[j] = new Vec3(Matrix.product(viewport, ndc_v.matrix()));
                }
                shadRasterise(coords[0], coords[1], coords[2]);
            }
        }
    }

    private void shadRasterise(Vec3 a, Vec3 b, Vec3 c) {
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
                int index = x + y * Width;
                if (z <= shad_zbuffer[index]) {
                    continue;
                }
                shad_zbuffer[index] = z;
            }
        }
    }

    private void render_model(Model[] models, int[] screen, double[][] modelView, double[][] perspective, double[][] viewport) {
        if (modelView == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先加载矩阵!");
        }
        // 遍历所有模型
        for (Model model : models) {
            // 遍历所有三角形
            for (int i = 0; i < model.nfaces(); i++) {
                // 获取三角形面上的三个顶点对应的eye坐标,clip坐标,eye空间的法向量,纹理坐标,光线视角下的ndc坐标
                Vec3[] eye_coords = new Vec3[3];
                Vec3[] clip_coords = new Vec3[3];
                Vector[] eye_norms = new Vector[3];
                Vec2[] tex_coords = new Vec2[3];
                Vec3[] shad_coords = new Vec3[3];

                for (int j = 0; j < 3; j++) {
                    // 计算顶点的eye空间坐标
                    double[][] M = Matrix.product(eyeView, model.vert(i, j).matrix());
                    eye_coords[j] = new Vec3(M[0][0], M[1][0], M[2][0], M[3][0]);
                    // 计算顶点的clip坐标
                    clip_coords[j] = new Vec3(Matrix.product(perspective, M));
                    // 利用文件提供的顶点在global空间的法向量，计算顶点在eye空间的法向量
                    eye_norms[j] = new Vector(Matrix.product(normMatrix, model.norm(i, j).matrix())).normalize();
                    tex_coords[j] = model.texcoord(i, j);
                    //  计算顶点在光线视角下的ndc坐标
                    Vec3 lgClip = new Vec3(Matrix.product(shadowMatrix, eye_coords[j].matrix()));
                    shad_coords[j] = lgClip.scale(1 / lgClip.w());
                }

                Vertex a = new Vertex(eye_coords[0], eye_norms[0], clip_coords[0], tex_coords[0], shad_coords[0]);
                Vertex b = new Vertex(eye_coords[1], eye_norms[1], clip_coords[1], tex_coords[1], shad_coords[1]);
                Vertex c = new Vertex(eye_coords[2], eye_norms[2], clip_coords[2], tex_coords[2], shad_coords[2]);

                Fragment clip = new Fragment(a, b, c);

                // 调用shader
                IShader shader = new IShader(model.textures(), modelView, normMatrix);
                rasterise(clip, shader, screen, viewport);
            }
        }
    }

    public void rasterise(Fragment clip, IShader shader, int[] screen, double[][] viewport) {
        // 除以w分量
        double rp_aw = clip.a().clip_recip_w();
        double rp_bw = clip.b().clip_recip_w();
        double rp_cw = clip.c().clip_recip_w();
        // 得到ndc坐标点
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
                int index = x + y * Width;
                if (z <= zbuffer[index]) {
                    continue;
                }
                zbuffer[index] = z;

                // 计算屏幕上的点p的透视校正后的重心坐标
                double persp_a = alpha * rp_aw;
                double persp_b = beta * rp_bw;
                double persp_c = gamma * rp_cw;
                double area = 1 / (persp_a + persp_b + persp_c);

                double eye_alpha = persp_a * area;
                double eye_beta = persp_b * area;
                double eye_gamma = persp_c * area;

                // 用eye空间的片段插值, 来计算该像素点的eye空间的插值坐标
                Vec3 eyePos = clip.eyePos_interpolate(eye_alpha, eye_beta, eye_gamma);
                // 计算像素点所接收到的光线light
                Vector light = lgEyePos.minus(eyePos);

                // 计算阴影映射
                double lightIntensity = 1.0; // 默认受光照 (强度1.0)

                //  用eye空间的片段插值, 来计算像素点以光线为视角的屏幕坐标
                Vec3 ndc_v = clip.shadPos_interpolate(eye_alpha, eye_beta, eye_gamma);
                Vec3 v = new Vec3(Matrix.product(viewport, ndc_v.matrix()));

                int shadow_x = (int) v.x();
                int shadow_y = (int) v.y();
                double cur_z = v.z();

                //  超出光线视野的像素点默认有光线
                if (shadow_x >= 0 && shadow_x < Width && shadow_y >= 0 && shadow_y < Height) {
                    int shad_index = shadow_x + shadow_y * Width;
                    double closet_depth = shad_zbuffer[shad_index];
                    double bias = 0.01;// 容错值
                    if (cur_z - bias < closet_depth) {
                        lightIntensity = 0.0;
                    }
                }

                // 使用eye空间的片段插值, 来计算该像素点的法线
                Vector n = clip.norm_interpolate(eye_alpha, eye_beta, eye_gamma);
                // 使用屏幕上的片段插值, 来计算纹理坐标
                Vec2 t = clip.tex_interpolate(eye_alpha, eye_beta, eye_gamma);

                // 对该像素点进行Phong shading
                int[] colors = shader.fragment(clip, lightIntensity, light, n, t);
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
