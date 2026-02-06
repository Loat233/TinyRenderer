package core;

import java.awt.*;
import java.util.Arrays;

public class openGL {
    // 用户输入的参数
    private final int startX;
    private final int startY;
    private final int Height;       //  屏幕高
    private final int Width;        //  屏幕宽
    private double degree;          //  模型自旋转角度
    private double scale;           //  镜头缩放倍数
    private double[] glob_light;    //  三维顶点: 光源global位置
    private double[] eye;           //  三维顶点: 相机global位置
    private double[] center;        //  三维顶点: 观察中心点
    private double[] up;            //  向量
    private double[] eye_light = new double[4];     // 三维顶点: 光源eye坐标

    // 合并矩阵
    private double[][] eyeView;
    private double[][] normMatrix;
    private double[][] shadClipMatrix;
    private double[][] shadowMatrix;
    // 缓存图
    private double[] shad_zbuffer;
    private double[] zbuffer;
    // 调试参数
    private boolean isUpsideDown; // 设置翻转

    public openGL(int x, int y, int Width, int Height) {
        this.startX = x;
        this.startY = y;
        this.Width = Width;
        this.Height = Height;

        int n = Width * Height;

        shad_zbuffer = new double[n];
        zbuffer = new double[n];
        isUpsideDown = false;
    }

    public void init_lightPos(double[] lightPos) {
        if (!isVert3(lightPos)) {
            throw new IllegalArgumentException("灯光坐标不是三维坐标！");
        }
        this.glob_light = lightPos;
    }

    // 用于计算model_view矩阵
    public void camera(double[] eye, double[] center, double[] up, double scale) {
        if (!isVert3(eye)) {
            throw new IllegalArgumentException("视角位置坐标不是三维坐标！");
        }
        if (!isVert3(center)) {
            throw new IllegalArgumentException("视角中心坐标不是三维坐标！");
        }
        if (!isVector(up)) {
            throw new IllegalArgumentException("up不是向量！");
        }
        this.eye = eye;
        this.center = center;
        this.up = up;
        this.scale = scale;
    }

    // 模型围绕center点旋转
    public void model_direct(double degree) {
        this.degree = degree;
    }

    private double[][] getBases(double[] eye, double[] center, double[] up) {
        double[] x_axis = new double[3];
        double[] y_axis = new double[3];
        double[] z_axis = new double[3];
        minus(eye, center, z_axis);         //  赋值z_axis
        product(up, z_axis, x_axis);        //  赋值x_axis
        product(z_axis, x_axis, y_axis);    //  赋值y_axis
        //  不会传递修改
        normalize(x_axis);
        normalize(y_axis);
        normalize(z_axis);
        return new double[][]{x_axis, y_axis, z_axis};
    }

    private double[][] getCoordinate(double[][] bases) {
        double[] x_axis = Arrays.copyOf(bases[0], bases.length + 1);
        double[] y_axis = Arrays.copyOf(bases[1], bases.length + 1);
        double[] z_axis = Arrays.copyOf(bases[2], bases.length + 1);
        return new double[][]{
                x_axis,
                y_axis,
                z_axis,
                {0, 0, 0, 1}
        };
    }

    private double[][] getTCenter(double[] center) {
        return new double[][]{
                {1, 0, 0, -center[0]},
                {0, 1, 0, -center[1]},
                {0, 0, 1, -center[2]},
                {0, 0, 0, 1}
        };
    }

    private double[][] getRotate(double d) {
        return new double[][] {
                {Math.cos(d), 0.0, Math.sin(d), 0.0},
                {0.0, 1.0, 0.0, 0.0},
                {-Math.sin(d), 0.0, Math.cos(d), 0.0},
                {0.0, 0.0, 0.0, 1.0}
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
         //  标准透视矩阵计算
         fov = fov * Math.PI / 180.0;
         double cot = 1 / Math.tan(fov / 2);
         double aspect = (double) Width / (double) Height;

         return new double[][] {
                 {cot, 0, 0, 0},
                 {0, cot * aspect, 0, 0},
                 {0, 0, (far + near) / (near - far), 2 * far * near / (near - far)},
                 {0, 0, -1, 0}
         };
          */

        return new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, -1 / f, 1}
        };
    }

    private double[][] getViewport(int x, int y, int w, int h) {
        double a = w / 2.0 * scale;
        double b = h / 2.0 * scale;
        double c = 100000.0 / 2.0;
        return new double[][] {
                {a, 0, 0, x + w / 2.0},
                {0, b, 0, y + h / 2.0},
                {0, 0, c, c},
                {0, 0, 0, 1}
        };
    }

    private void clear_zbuffer(double[] buffer) {
        Arrays.fill(buffer, Double.NEGATIVE_INFINITY);
    }

    public void render(Model[] models, int[] screen) {
        double[][] coordinate;
        double[][] t_center;
        double[][] modelView;
        double[][] perspective;
        double[][] viewport;

        double fx = eye[0] - center[0];
        double fy = eye[1] - center[1];
        double fz = eye[2] - center[2];
        double f = Math.sqrt(fx * fx + fy * fy + fz * fz);

        //  加载阴影图需要的矩阵
        coordinate = getCoordinate(getBases(glob_light, center, up));
        t_center = getTCenter(center);
        modelView = getModelView(coordinate, t_center);
        modelView = getEyeView(modelView);
        perspective = getPerspective(f);
        viewport = getViewport(startX, startY, Width, Height);
        //  加载合并矩阵
        this.shadClipMatrix = Matrix.product(perspective, modelView);
        //  清空阴影的深度缓冲区
        clear_zbuffer(this.shad_zbuffer);
        //  加载阴影图
        render_shadMap(models, modelView, perspective, viewport);

        //  加载模型需要的矩阵
        coordinate = getCoordinate(getBases(eye, center, up));
        t_center = getTCenter(center);
        modelView = getModelView(coordinate, t_center);
        perspective = getPerspective(f);
        viewport = getViewport(startX, startY, Width, Height);
        //  加载合并矩阵
        eyeView = getEyeView(modelView);
        Matrix.vec_product(modelView, glob_light, this.eye_light);
        double[][] inv_eyeView = Matrix.inverse(eyeView);
        this.shadowMatrix = Matrix.product(shadClipMatrix, inv_eyeView);
        this.normMatrix = Matrix.eliminate(Matrix.transpose(inv_eyeView));
        //  清空画面的深度缓冲区
        clear_zbuffer(this.zbuffer);
        //  加载模型和光影
        render_model(models, screen, modelView, perspective, viewport);
        //  加载ssao
        //  ssao(perspective, viewport, screen, f);
    }

    private void render_shadMap(Model[] models, double[][] modelView, double[][] perspective, double[][] viewport) {
        if (modelView == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先加载矩阵!");
        }
        double[] v0 = new double[4];
        double[] v1 = new double[4];
        double[] v2 = new double[4];
        //  遍历所有模型
        for (Model model : models) {
            //  遍历所有三角形
            for (int i = 0; i < model.nfaces(); i++) {
                // global -> clip
                Matrix.vec_product(shadClipMatrix, model.vert(i, 0), v0);
                Matrix.vec_product(shadClipMatrix, model.vert(i, 1), v1);
                Matrix.vec_product(shadClipMatrix, model.vert(i, 2), v2);
                //  clip -> ndc
                scale(v0, 1 / v0[3]);
                scale(v1, 1 / v1[3]);
                scale(v2, 1 / v2[3]);
                //  避免 w = 0.9999...
                v0[3] = 1;
                v1[3] = 1;
                v2[3] = 1;
                //  ndc -> screen
                Matrix.vec_product(viewport, v0, v0);
                Matrix.vec_product(viewport, v1, v1);
                Matrix.vec_product(viewport, v2, v2);
                shadRasterise(v0, v1, v2);
            }
        }
    }

    //  screen空间: 三维顶点:a, b, c
    private void shadRasterise(double[] a, double[] b, double[] c) {
        double sign_area = sign_triangle_area(a, b, c);
        if (sign_area > 0) {
            return;
        }

        double[] p = new double[4];      //  screen空间: 三维顶点p
        for (int x = bbmin(a[0], b[0], c[0]); x < bbmax(a[0], b[0], c[0]); x++) {
            for (int y = bbmin(a[1], b[1], c[1]); y < bbmax(a[1], b[1], c[1]); y++) {
                //  赋值点p
                p[0] = x + 0.5;
                p[1] = y + 0.5;
                p[2] = 0;
                p[3] = 1;

                // 计算在2D屏幕上的插值
                double alpha = sign_triangle_area(p, b, c) / sign_area;
                double beta = sign_triangle_area(p, c, a) / sign_area;
                double gamma = 1 - alpha - beta;

                // 计算屏幕上的点p在eye空间的坐标z值
                double z = alpha * a[2] + beta * b[2] + gamma * c[2];
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

        //  三角形顶点: double[][]{eye顶点, tex顶点, eye空间法向量}
        double[][] a = new double[3][];
        a[0] = new double[4];
        a[1] = new double[2];
        a[2] = new double[3];
        double[][] b = new double[3][];
        b[0] = new double[4];
        b[1] = new double[2];
        b[2] = new double[3];
        double[][] c = new double[3][];
        c[0] = new double[4];
        c[1] = new double[2];
        c[2] = new double[3];
        // 遍历所有模型
        for (Model model : models) {
            // 遍历所有三角形
            for (int i = 0; i < model.nfaces(); i++) {
                //  获取eye顶点
                Matrix.vec_product(eyeView, model.vert(i, 0), a[0]);
                Matrix.vec_product(eyeView, model.vert(i, 1), b[0]);
                Matrix.vec_product(eyeView, model.vert(i, 2), c[0]);
                //  获取tex顶点
                a[1] = model.texcoord(i, 0);
                b[1] = model.texcoord(i, 1);
                c[1] = model.texcoord(i, 2);
                //  获取法向量
                Matrix.vec_product(normMatrix, model.norm(i, 0), a[2]);
                Matrix.vec_product(normMatrix, model.norm(i, 1), b[2]);
                Matrix.vec_product(normMatrix, model.norm(i, 2), c[2]);
                //  法向量归一化
                normalize(a[2]);
                normalize(b[2]);
                normalize(c[2]);
                // 初始化shader
                IShader shader = new IShader(a, b, c, model.textures(), normMatrix);
                rasterise(a, b, c, perspective, viewport, shader, screen);
            }
        }
    }

    //  三角形顶点: double[][]{eye顶点, tex顶点, eye空间法向量}
    public void rasterise(double[][] a, double[][] b, double[][] c, double[][] perspective, double[][] viewport, IShader shader, int[] screen) {
        // eye -> clip
        double[] v0 = new double[4];
        double[] v1 = new double[4];
        double[] v2 = new double[4];
        Matrix.vec_product(perspective, a[0], v0);
        Matrix.vec_product(perspective, b[0], v1);
        Matrix.vec_product(perspective, c[0], v2);
        // 获得w分量的倒数
        double rp_aw = 1 / v0[3];
        double rp_bw = 1 / v1[3];
        double rp_cw = 1 / v2[3];
        // clip -> ndc
        scale(v0, rp_aw);
        scale(v1, rp_bw);
        scale(v2, rp_cw);
        //  避免 w = 0.9999...
        v0[3] = 1;
        v1[3] = 1;
        v2[3] = 1;
        // ndc -> screen
        Matrix.vec_product(viewport, v0, v0);
        Matrix.vec_product(viewport, v1, v1);
        Matrix.vec_product(viewport, v2, v2);

        double sign_area = sign_triangle_area(v0, v1, v2);

        if (sign_area < 1) {
            return;
        }

        double[] p = new double[4];         //  screen空间: 三维顶点p
        double[] eye_p = new double[4];     //  eye空间: 三维顶点p(校正插值)
        double[] light = new double[3];     //  eye空间: 光源到点p的向量
        double[] n = new double[3];         //  eye空间: 点p的法线(校正插值) (会被IShader修改，但无影响)
        double[] light_p = new double[4];     //  光源视角screen空间: 三维顶点p
        double[] tex = new double[2];         //  点p的uv坐标(校正插值)
        for (int x = bbmin(v0[0], v1[0], v2[0]); x < bbmax(v0[0], v1[0], v2[0]); x++) {
            for (int y = bbmin(v0[1], v1[1], v2[1]); y < bbmax(v0[1], v1[1], v2[1]); y++) {
                //  赋值点p
                p[0] = x + 0.5;
                p[1] = y + 0.5;
                p[2] = 0;
                p[3] = 1;

                //  计算在2D屏幕上的插值
                double alpha = sign_triangle_area(p, v1, v2) / sign_area;
                double beta = sign_triangle_area(p, v2, v0) / sign_area;
                double gamma = 1 - alpha - beta;
                double z = alpha * v0[2] + beta * v1[2] + gamma * v2[2];

                //  允许小的容差值
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

                //  计算屏幕上的点p的透视校正后的重心坐标
                double persp_a = alpha * rp_aw;
                double persp_b = beta * rp_bw;
                double persp_c = gamma * rp_cw;
                double area = 1 / (persp_a + persp_b + persp_c);
                double eye_alpha = persp_a * area;
                double eye_beta = persp_b * area;
                double eye_gamma = persp_c * area;

                interpolate(a[0], b[0], c[0], eye_alpha, eye_beta, eye_gamma, eye_p);   //  赋值eye_p: eye空间下点p(校正插值)
                minus(eye_light, eye_p, light);                                         //  赋值light: eye空间下光源到点p的向量

                interpolate(a[2], b[2], c[2], eye_alpha, eye_beta, eye_gamma, n);       //  赋值n: 点p的uv坐标(校正插值)
                normalize(n);

                //  计算阴影映射
                double lightIntensity; //  光照强度
                double angle = dot(n, light);
                //  如果法线方向背对光线，则光照为0,无需查找阴影的深度图
                if (angle < 0) {
                    lightIntensity = 0.0;
                }
                else {
                    //  p: eye -> 光源视角clip
                    Matrix.vec_product(shadowMatrix, eye_p, light_p);
                    //  光源视角clip -> 光源视角ndc
                    scale(light_p, 1.0 / light_p[3]);
                    //  光源视角ndc -> 光源视角screen
                    Matrix.vec_product(viewport, light_p, light_p);
                    int px = (int) light_p[0];
                    int py = (int) light_p[1];
                    double pz = light_p[2];

                    // PCF 3x3 采样 平滑阴影边缘
                    double shadow_sum = 0.0;
                    double bias = 5.0; // 较小的偏移值
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            int nx = px + i;
                            int ny = py + j;
                            if (nx >= 0 && nx < Width && ny >= 0 && ny < Height) {
                                int idx = nx + ny * Width;
                                // 如果点比阴影图记录的深度更“近”或相等(数值更大)，则被照亮
                                if (pz + bias >= shad_zbuffer[idx]) {
                                    shadow_sum += 1.0;
                                }
                            } else {
                                shadow_sum += 1.0; // 越界视为照亮
                            }
                        }
                    }
                    lightIntensity = shadow_sum / 9.0;
                    /*
                    //  超出光线视野的像素点默认有光线
                    if (shadow_x >= 0 && shadow_x < Width && shadow_y >= 0 && shadow_y < Height) {
                        int shad_index = shadow_x + shadow_y * Width;
                        double closet_depth = shad_zbuffer[shad_index];
                        double bias = 20; // 容错值
                        if (cur_z + bias < closet_depth) {
                            lightIntensity = 0.0;
                        }
                    }

                     */
                }
                //  使用屏幕上的片段插值, 来计算纹理坐标
                interpolate(a[1], b[1], c[1], eye_alpha, eye_beta, eye_gamma, tex);
                int screenY;
                if (isUpsideDown) {
                    screenY = y;
                } else {
                    screenY = Height - 1 - y;
                }
                //  对该像素点进行Phong shading
                screen[x + screenY * Width] = shader.fragment(lightIntensity, light, n, tex);
            }
        }
    }

    private static double sign_triangle_area(double[] a, double[] b, double[] c) {
        if (a[3] != 1 || b[3] != 1 || c[3] != 1) {
            throw new IllegalArgumentException("顶点的w分量不为1！");
        }
        return 0.5 * ((b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]));
    }

    private static int bbmin(double x0, double x1, double x2) {
        return (int) Math.floor(Math.min(Math.min(x0, x1), x2)); // 边界框向下取整
    }

    private static int bbmax(double x0, double x1, double x2) {
        return (int) Math.ceil(Math.max(Math.max(x0, x1), x2)); // 边界框向上取整
    }


    public void ssao(double[][] perspective, double[][] viewport, int[] screen, double f) {
        double radius = 0.5; // 采样半径，可调整
        int samples = 16;    // 采样点数量，越多越慢但越平滑

        // 预计算 Viewport 矩阵 (复用 getViewport 逻辑)
        // 预计算 Perspective 矩阵

        double[] samplePos = new double[4];
        double[] clip = new double[4];
        double[] ndc = new double[4];
        double[] screenPos = new double[4];
        double[] origin = new double[4];
        double[] surface = new double[4];
        for (int x = 0; x < Width; x++) {
            for (int y = 0; y < Height; y++) {
                //  获取当前像素的 Eye 坐标
                scToEye(x, y, f, scale, origin);
                if (origin == null) {
                    continue;
                }

                double occlusion = 0;
                //  随机采样
                for (int i = 0; i < samples; i++) {
                    // 简单的随机向量 (建议换成半球采样以获得更好效果，这里先用球体简化)
                    double r1 = Math.random() * 2.0 - 1.0;
                    double r2 = Math.random() * 2.0 - 1.0;
                    double r3 = Math.random() * 2.0 - 1.0;

                    // 归一化随机向量
                    double inv_d = Math.sqrt(r1*r1 + r2*r2 + r3*r3);
                    r1 *= inv_d;
                    r2 *= inv_d;
                    r3 *= inv_d;

                    // 计算采样点 Eye 坐标
                    samplePos[0] = origin[0] + r1 * radius;
                    samplePos[1] = origin[1] + r2 * radius;
                    samplePos[2] = origin[2] + r3 * radius;
                    samplePos[3] = 1.0;

                    //  投影回屏幕(eye -> clip -> NDC -> screen)
                    Matrix.vec_product(perspective, samplePos, clip);
                    if (clip[3] == 0){
                        continue;
                    }

                    //  clip -> NDC
                    double invW = 1.0 / clip[3];
                    ndc[0] = clip[0] * invW;
                    ndc[1] = clip[1] * invW;
                    ndc[2] = clip[2] * invW;
                    ndc[3] = 1.0;

                    //  NDC -> screen
                    Matrix.vec_product(viewport, ndc, screenPos);
                    int sx = (int) screenPos[0];
                    int sy = (int) screenPos[1];

                    //  遮挡测试
                    if (sx >= 0 && sx < Width && sy >= 0 && sy < Height) {
                        // 获取该采样位置对应像素的 "真实" 几何体 Eye 坐标
                        scToEye(sx, sy, f, scale, surface);
                        if (surface != null) {
                            // 比较深度:
                            // surface[2] (几何体) 如果大于 samplePos[2] (采样点)，说明几何体更靠近相机，挡住了采样点
                            // 距离检查: rangeCheck 避免遮挡太远的东西
                            double rangeCheck = Math.abs(origin[2] - surface[2]) < radius ? 1.0 : 0.0;
                            if (surface[2] > samplePos[2] && rangeCheck > 0.5) {
                                occlusion += 1.0;
                            }
                        }
                    }
                }
                //  应用遮挡到颜色
                occlusion = 1.0 - (occlusion / samples);

                // 读取原颜色
                int screenY = isUpsideDown ? y : Height - 1 - y;
                int idx = x + screenY * Width;
                int color = screen[idx];

                // 混合颜色
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                r = (int)(r * occlusion);
                g = (int)(g * occlusion);
                b = (int)(b * occlusion);

                screen[idx] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }

    //  将屏幕像素坐标还原为 Eye 空间坐标
    private void scToEye(int x, int y, double f, double scale, double[] dest) {
        int index = x + y * Width;
        if (index < 0 || index >= zbuffer.length) {
            return;
        }
        double z_screen = zbuffer[index];

        // 忽略背景 (假设背景是负无穷)
        if (z_screen == Double.NEGATIVE_INFINITY) {
            return;
        }
        //  z: screen -> NDC -> eye
        //  z_screen = (z_ndc * c) + c, z_ndc = z_screen / c - 1
        double c = 100000.0 / 2.0;
        double z_ndc = z_screen / c - 1.0;

        //  z_ndc = z_eye / (1 - z_eye/f), z_eye = (z_ndc * f) / (f + z_ndc)
        double z_eye = (z_ndc * f) / (f + z_ndc);

        //  计算w
        double w = 1.0 - z_eye / f;

        double a = Width / 2.0 * scale;
        double b = Height / 2.0 * scale;

        double x_ndc = (x - startX - a) / a;
        double y_ndc = (y - startY - b) / b;

        //  Perspective XY 变换: x_ndc = x_eye / w
        //  x_eye = x_ndc * w
        dest[0] = x_ndc * w;
        dest[1] = y_ndc * w;
        dest[2] = z_eye;
        dest[3] = 1.0;
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






    /* 以下是顶点,向量,矩阵类的计算函数 */
    /*
        变量格式: 二维顶点：double[]{x, y}
                三维顶点：double[]{x, y, z, w}
                向量：double[]{x, y, z}
     */
    private static boolean isVert2(double[] vec) {
        return vec.length == 2;
    }

    private static boolean isVert3(double[] vec) {
        return vec.length == 4;
    }

    private static boolean isVector(double[] vec) {
        return vec.length == 3;
    }

    /* 以下函数返回新数组 */
    /*
        输入：二维顶点/三维顶点/三维向量
        运算：x,y相加/x, y, z, w相加/向量相加
        输出：二维顶点/返回三维顶点/三维向量
     */
    public static void add(double[] a, double[] b, double[] dest) {
        int k;
        if (isVert2(a) && isVert2(b)) {
            k = 2;
        }
        else if (isVert3(a) && isVert3(b)) {
            k = 4;
        }
        else if (isVector(a) && isVector(b)) {
            k = 3;
        }
        else {
            throw new IllegalArgumentException("相加顶点的坐标数量不同！");
        }
        for (int i = 0; i < k; i++) {
            dest[i] = a[i] + b[i];
        }
    }

    /*
        输入：二维顶点/三维顶点(w分量为1)/向量
        运算：x,y相减/x, y, z相减/向量相减
        输出：二维向量/三维向量/三维向量
     */
    public static void minus(double[] a, double[] b, double[] dest) {
        int k;
        if (isVert2(a) && isVert2(b)) {
            if (!isVert2(dest)) {
                throw new IllegalArgumentException("赋值结果的数组不是二维！");
            }
            k = 2;
        }
        else if (isVert3(a) && isVert3(b)) {
            if (!isVector(dest)) {
                throw new IllegalArgumentException("赋值结果的数组不是向量！");
            }
            if (a[3] == 1 || b[3] == 1) {
                k = 3;
            }
            else {
                throw new IllegalArgumentException("相加顶点的w分量不为1！");
            }
        }
        else if (isVector(a) && isVector(b)) {
            if (!isVector(dest)) {
                throw new IllegalArgumentException("赋值结果的数组不是向量！");
            }
            k = 3;
        }
        else {
            throw new IllegalArgumentException("相加顶点的坐标数量不同！");
        }
        for (int i = 0; i < k; i++) {
            dest[i] = a[i] - b[i];
        }
    }

    //  向量叉乘
    public static void product(double[] a, double[] b, double[] dest) {
        if (isVector(a) && isVector(b)) {
            dest[0] = a[1] * b[2] - a[2] * b[1];
            dest[1] = a[2] * b[0] - a[0] * b[2];
            dest[2] = a[0] * b[1] - a[1] * b[0];
        }
        else {
            throw new IllegalArgumentException("输入数组不是向量！");
        }
    }

    public void interpolate(double[] a, double[] b, double[] c, double alpha, double beta, double gamma, double[] dest) {
        if (a.length != b.length && b.length != c.length && c.length != dest.length) {
           throw new IllegalArgumentException("数组a, b, c和dest的长度不全相等！");
        }
        int k;  //  循环次数
        if (isVert2(dest)) {
           k = 2;
        }
        else if (isVert3(dest)) {
           k = 4;
        }
        else if (isVector(dest)) {
           k = 3;
        }
        else {
            throw new IllegalArgumentException("结果数组长度错误！");
        }
        for (int i = 0; i < k; i++) {
            dest[i] = a[i] * alpha + b[i] * beta + c[i] * gamma;
        }
    }

    //  向量归一化
    public static void normalize(double[] dest) {
        if (!isVector(dest)) {
            throw new IllegalArgumentException("结果数组不是向量！");
        }
        double d = 1 / Math.sqrt(dest[0] * dest[0] + dest[1] * dest[1] + dest[2] * dest[2]);
        for (int i = 0; i < 3; i++) {
            dest[i] *= d;
        }
    }

    //  数量积
    public static void scale(double[] dest, double n) {
        int k;
        if (isVert2(dest)) {
            k = 2;
        }
        else if (isVert3(dest)) {
            k = 4;
        }
        else if (isVector(dest)) {
            k = 3;
        }
        else {
            throw new IllegalArgumentException("结果数组长度错误！");
        }
        for (int i = 0; i < k; i++) {
            dest[i] *= n;
        }
    }



    /* 以下函数返回常数 */
    //  向量平方根
    public static double sqrt(double[] vec) {
        if (!isVector(vec)) {
            throw new IllegalArgumentException("输入参数不是向量！");
        }
        return Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
    }

    //  向量点乘
    public static double dot(double[] a, double[] b) {
        if (isVector(a) && isVector(b)) {
            return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        }
        throw new IllegalArgumentException("输入参数不是向量！");
    }
}
