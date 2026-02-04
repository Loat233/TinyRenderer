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
    private double[] glob_light;     // 三维顶点: 光源global位置
    private double[] eye;       // 三维顶点: 相机global位置
    private double[] center;    // 三维顶点: 观察中心点
    private double[] up;        // 向量
    private double[] eye_light;     // 三维顶点: 光源eye坐标

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
    public void camera(double[] eye, double[] center, double[] up, double fol) {
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
        this.fol = fol;
    }

    // 模型围绕center点旋转
    public void model_direct(double degree) {
        this.degree = degree;
    }

    private double[][] getBases(double[] eye, double[] center, double[] up) {
        double[] z_axis = minus(eye, center);
        double[] x_axis = product(up, z_axis);
        double[] y_axis = product(z_axis, x_axis);
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
         * 标准透视矩阵计算
         * fov = fov * Math.PI / 180.0;
         * double fh = Math.tan(fov / 2);
         * double aspect = (double) Width / (double) Height;
         *
         * return new double[][] {
         * {fh, 0, 0, 0},
         * {0, fh * aspect, 0, 0},
         * {0, 0, (f + n) / (n - f), 2 * f * n / (n - f)},
         * {0, 0, -1, 0}
         * };
         */
         return new double[][] {
                { 1, 0, 0, 0},
                { 0, 1, 0, 0},
                { 0, 0, 1, 0},
                { 0, 0, -1 / f, 1}
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

    private void clear_zbuffer(double[] buffer) {
        Arrays.fill(buffer, Double.NEGATIVE_INFINITY);
    }

    public void render(Model[] models, int[] screen) {
        double[][] coordinate;
        double[][] t_center;
        double[][] modelView;
        double[][] perspective;
        double[][] viewport;
        // 加载阴影图需要的矩阵
        coordinate = getCoordinate(getBases(glob_light, center, up));
        t_center = getTCenter(center);
        modelView = getModelView(coordinate, t_center);
        modelView = getEyeView(modelView);
        perspective = getPerspective(fol);
        viewport = getViewport(startX, startY, Width, Height);
        // 加载合并矩阵
        this.shadClipMatrix = Matrix.product(perspective, modelView);
        // 清空阴影的深度缓冲区
        clear_zbuffer(this.shad_zbuffer);
        // 加载阴影图
        render_shadMap(models, modelView, perspective, viewport);

        // 加载模型需要的矩阵
        coordinate = getCoordinate(getBases(eye, center, up));
        t_center = getTCenter(center);
        modelView = getModelView(coordinate, t_center);
        perspective = getPerspective(fol);
        viewport = getViewport(startX, startY, Width, Height);
        // 加载合并矩阵
        eyeView = getEyeView(modelView);
        this.eye_light = Matrix.vec_product(modelView, glob_light);
        double[][] inv_eyeView = Matrix.inverse(eyeView);
        this.shadowMatrix = Matrix.product(shadClipMatrix, inv_eyeView);
        this.normMatrix = Matrix.eliminate(Matrix.transpose(inv_eyeView));
        // 清空画面的深度缓冲区
        clear_zbuffer(this.zbuffer);
        // 加载模型和光影
        render_model(models, screen, modelView, perspective, viewport);
        // 加载ssao
        // ssao(screen);
    }

    private void render_shadMap(Model[] models, double[][] modelView, double[][] perspective, double[][] viewport) {
        if (modelView == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先加载矩阵!");
        }
        // 遍历所有模型
        for (Model model : models) {
            // 遍历所有三角形
            for (int i = 0; i < model.nfaces(); i++) {
                // global -> clip
                double[] v0 = Matrix.vec_product(shadClipMatrix, model.vert(i, 0));
                double[] v1 = Matrix.vec_product(shadClipMatrix, model.vert(i, 1));
                double[] v2 = Matrix.vec_product(shadClipMatrix, model.vert(i, 2));
                //  clip -> ndc
                scale(v0, 1 / v0[3]);
                scale(v1, 1 / v1[3]);
                scale(v2, 1 / v2[3]);
                //  避免 w = 0.9999...
                v0[3] = 1;
                v1[3] = 1;
                v2[3] = 1;
                //  ndc -> screen
                v0 = Matrix.vec_product(viewport, v0);
                v1 = Matrix.vec_product(viewport, v1);
                v2 = Matrix.vec_product(viewport, v2);
                shadRasterise(v0, v1, v2);
            }
        }
    }

    //  三维顶点:a, b, c
    private void shadRasterise(double[] a, double[] b, double[] c) {
        double sign_area = sign_triangle_area(a, b, c);
        if (sign_area < 1) {
            return;
        }
        for (int x = bbmin(a[0], b[0], c[0]); x < bbmax(a[0], b[0], c[0]); x++) {
            for (int y = bbmin(a[1], b[1], c[1]); y < bbmax(a[1], b[1], c[1]); y++) {
                double[] p = new double[]{x + 0.5, y + 0.5, 0, 1};
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

    private void render_model(Model[] models, int[] screen, double[][] modelView, double[][] perspective,
            double[][] viewport) {
        if (modelView == null || perspective == null || viewport == null) {
            throw new IllegalArgumentException("先加载矩阵!");
        }
        // 遍历所有模型
        for (Model model : models) {
            // 遍历所有三角形
            for (int i = 0; i < model.nfaces(); i++) {
                //  三角形顶点: double[][]{eye顶点, tex顶点, eye空间法向量}
                double[][] a = new double[3][];
                double[][] b = new double[3][];
                double[][] c = new double[3][];

                //  获取eye顶点
                a[0] = Matrix.vec_product(eyeView, model.vert(i, 0));
                b[0] = Matrix.vec_product(eyeView, model.vert(i, 1));
                c[0] = Matrix.vec_product(eyeView, model.vert(i, 2));
                //  获取tex顶点
                a[1] = model.texcoord(i, 0);
                b[1] = model.texcoord(i, 1);
                c[1] = model.texcoord(i, 2);
                //  获取法向量
                a[2] = Matrix.vec_product(normMatrix, model.norm(i, 0));
                b[2] = Matrix.vec_product(normMatrix, model.norm(i, 1));
                c[2] = Matrix.vec_product(normMatrix, model.norm(i, 2));
                //  法向量归一化
                normalize(a[2]);
                normalize(b[2]);
                normalize(c[2]);
                // 初始化shader
                IShader shader = new IShader(model.textures(), normMatrix);
                rasterise(a, b, c, perspective, viewport, shader, screen);
            }
        }
    }

    //  三角形顶点: double[][]{eye顶点, tex顶点, eye空间法向量}
    public void rasterise(double[][] a, double[][] b, double[][] c, double[][] perspective, double[][] viewport, IShader shader, int[] screen) {
        // eye -> clip
        double[] v0 = Matrix.vec_product(perspective, a[0]);
        double[] v1 = Matrix.vec_product(perspective, b[0]);
        double[] v2 = Matrix.vec_product(perspective, c[0]);
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
        v0 = Matrix.vec_product(viewport, v0);
        v1 = Matrix.vec_product(viewport, v1);
        v2 = Matrix.vec_product(viewport, v2);

        double sign_area = sign_triangle_area(v0, v1, v2);

        if (sign_area < 1) {
            return;
        }
        for (int x = bbmin(v0[0], v1[0], v2[0]); x < bbmax(v0[0], v1[0], v2[0]); x++) {
            for (int y = bbmin(v0[1], v1[1], v2[1]); y < bbmax(v0[1], v1[1], v2[1]); y++) {
                double[] p = new double[]{x + 0.5, y + 0.5, 0, 1};
                // 计算在2D屏幕上的插值
                double alpha = sign_triangle_area(p, v1, v2) / sign_area;
                double beta = sign_triangle_area(p, v2, v0) / sign_area;
                double gamma = 1 - alpha - beta;
                double z = alpha * v0[2] + beta * v1[2] + gamma * v2[2];

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
                double[] eye_p = interpolate(a[0], b[0], c[0], eye_alpha, eye_beta, eye_gamma);

                // 计算到达像素点的光线light
                double[] light = minus(eye_light, eye_p);
                // 使用eye空间的片段插值,来计算该像素点的法线(会被IShader修改，但无影响)
                double[] n = interpolate(a[2], b[2], c[2], eye_alpha, eye_beta, eye_gamma);
                normalize(n);

                // 计算阴影映射
                double lightIntensity = 1.0; // 默认受光照 (强度1.0)
                double[] nrm_light = Arrays.copyOf(light, light.length);
                double angle = dot(n, nrm_light);
                // 如果法线方向背对光线，则光照为0,无需查找阴影的深度图
                if (angle < 0) {
                    lightIntensity = 0.0;
                }
                else {
                    //  p: eye -> 光源视角clip
                    double[] light_p = Matrix.vec_product(shadowMatrix, eye_p);
                    //  光源视角clip -> 光源视角ndc
                    scale(light_p, 1.0 / light_p[3]);
                    //  光源视角ndc -> 光源视角screen
                    light_p = Matrix.vec_product(viewport, light_p);
                    int shadow_x = (int) light_p[0];
                    int shadow_y = (int) light_p[1];
                    double cur_z = light_p[2];

                    // 超出光线视野的像素点默认有光线
                    if (shadow_x >= 0 && shadow_x < Width && shadow_y >= 0 && shadow_y < Height) {
                        int shad_index = shadow_x + shadow_y * Width;
                        double closet_depth = shad_zbuffer[shad_index];
                        double bias = 4000;// 容错值
                        if (cur_z + bias < closet_depth) {
                            lightIntensity = 0.0;
                        }
                    }
                }
                // 使用屏幕上的片段插值, 来计算纹理坐标
                double[] t = interpolate(a[1], b[1], c[1], eye_alpha, eye_beta, eye_gamma);
                int screenY;
                if (isUpsideDown) {
                    screenY = y;
                } else {
                    screenY = Height - 1 - y;
                }
                // 对该像素点进行Phong shading
                screen[x + screenY * Width] = shader.fragment(a, b, c, lightIntensity, light, n, t);
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

    /*
    public void ssao(int[] screen) {
        // 采样半径 (World Space / Eye Space 单位)
        double radius = 0.5;

        // 遍历屏幕像素
        for (int x = 0; x < Width; x++) {
            for (int y = 0; y < Height; y++) {
                // 获取当前像素的eye空间坐标
                Vec3 origin = scToEye(x, y);
                if (origin == null) {
                    continue; // 背景不处理
                }
                double occlusion = 0;
                int samples = 8; // 采样数，越高越慢但越平滑
                // 随机采样周围点
                for (int i = 0; i < samples; i++) {
                    // 随机生成球面上的向量(简化版)
                    double r1 = Math.random() * 2 - 1;
                    double r2 = Math.random() * 2 - 1;
                    double r3 = Math.random() * 2 - 1;
                    Vector sampleDir = new Vector(r1, r2, r3).normalize();

                    // 确保采样点在表面法线方向(半球)是更准确的做法
                    // 但这里简化为假设法线指向相机(z<0)
                    if (sampleDir.z() > 0)
                        sampleDir = sampleDir.scale(-1);
                    // 得到采样点 Eye 坐标
                    Vec3 samplePos = new Vec3(
                            origin.x() + sampleDir.x() * radius,
                            origin.y() + sampleDir.y() * radius,
                            origin.z() + sampleDir.z() * radius,
                            1.0);
                    // 将采样点投影回屏幕
                    // Eye -> Clip
                    Vec3 clip = new Vec3(Matrix.product(getPerspective(fol), samplePos.matrix()));
                    if (clip.w() == 0)
                        continue;
                    // Clip -> NDC
                    double invW = 1.0 / clip.w();
                    Vec3 ndc = clip.scale(invW);

                    // NDC -> Screen
                    double[][] viewport = getViewport(startX, startY, Width, Height);
                    Vec3 screenPos = new Vec3(Matrix.product(viewport, ndc.matrix()));

                    int sampleX = (int) screenPos.x();
                    int sampleY = (int) screenPos.y();
                    // 遮挡测试
                    if (sampleX >= 0 && sampleX < Width && sampleY >= 0 && sampleY < Height) {
                        // 获取该采样位置 "实际"几何体在eye空间的坐标
                        Vec3 surfacePoint = scToEye(sampleX, sampleY);
                        if (surfacePoint != null) {
                            // 距离检查: 只遮挡附近的物体，避免远处的背景遮挡
                            double rangeCheck = Math.abs(origin.z() - surfacePoint.z()) < radius ? 1.0 : 0.0;

                            if (surfacePoint.z() > samplePos.z() && rangeCheck > 0.5) {
                                occlusion += 1.0;
                            }
                        }
                    }
                }
                // 应用遮挡
                occlusion = 1.0 - (occlusion / samples);

                // 将AO乘到当前像素颜色上
                int screenY = isUpsideDown ? y : Height - 1 - y;
                int idx = x + screenY * Width;
                int color = screen[idx];
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                r = (int) (r * occlusion);
                g = (int) (g * occlusion);
                b = (int) (b * occlusion);

                screen[idx] = (r << 16) | (g << 8) | b;
            }
        }
    }

    private Vec3 scToEye(int x, int y) {
        int index = x + y * Width;
        double z_screen = zbuffer[index];
        // 忽略背景
        if (z_screen == Double.NEGATIVE_INFINITY) {
            return null;
        }
        // z: screen -> NDC -> eye
        double rp_c = 2.0 / 100000.0;
        double z_ndc = z_screen * rp_c - 1;
        double z_eye = (z_ndc * fol) / (fol + z_ndc);
        // 计算w
        double w = 1.0 - z_eye / fol;
        // x,y: screen -> NDC
        double hf_w = Width / 2.0;
        double hf_h = Height / 2.0;
        double x_ndc = (x - startX - hf_w) / hf_w;
        double y_ndc = (y - startY - hf_h) / hf_h;
        // x,y: NDC -> eye
        double x_eye = x_ndc * w;
        double y_eye = y_ndc * w;
        return new Vec3(x_eye, y_eye, z_eye, 1.0);
    }
     */
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
    public static double[] add(double[] a, double[] b) {
        if (isVert2(a) && isVert2(b)) {
            return new double[]{(a[0] + b[0]), (a[1] + b[1])};
        }
        else if (isVert3(a) && isVert3(b)) {
            return new double[]{(a[0] + b[0]), (a[1] + b[1]), (a[2] + b[2]), (a[3] + b[3])};
        }
        else if (isVector(a) && isVector(b)) {
            return new double[]{(a[0] + b[0]), (a[1] + b[1]), (a[2] + b[2])};
        }
        throw new IllegalArgumentException("相加顶点的坐标数量不同！");
    }

    /*
        输入：二维顶点/三维顶点(w分量为1)/向量
        运算：x,y相减/x, y, z相减/向量相减
        输出：二维向量/三维向量/三维向量
     */
    public static double[] minus(double[] a, double[] b) {
        if (isVert2(a) && isVert2(b)) {
            return new double[]{(a[0] - b[0]), (a[1] - b[1])};
        }
        else if (isVert3(a) && isVert3(b)) {
            if (a[3] == 1 || b[3] == 1) {
                return new double[]{(a[0] - b[0]), (a[1] - b[1]), (a[2] - b[2])};
            }
            throw new IllegalArgumentException("相加顶点的w分量不为1！");
        }
        else if (isVector(a) && isVector(b)) {
            return new double[]{(a[0] - b[0]), (a[1] - b[1]), (a[2] - b[2])};
        }
        throw new IllegalArgumentException("相加顶点的坐标数量不同！");
    }

    //  向量叉乘
    public static double[] product(double[] a, double[] b) {
        if (isVector(a) && isVector(b)) {
            double i = a[1] * b[2] - a[2] * b[1];
            double j = a[2] * b[0] - a[0] * b[2];
            double k = a[0] * b[1] - a[1] * b[0];
            return new double[]{i, j, k};
        }
        throw new IllegalArgumentException("输入参数不是向量！");
    }

    public double[] interpolate(double[] a, double[] b, double[] c, double alpha, double beta, double gamma) {
        double[] v0 = Arrays.copyOf(a, a.length);
        double[] v1 = Arrays.copyOf(b, b.length);
        double[] v2 = Arrays.copyOf(c, c.length);
        scale(v0, alpha);
        scale(v1, beta);
        scale(v2, gamma);
        return add(add(v0, v1), v2);
    }

    /* 以下函数直接修改原数组 */
    //  向量归一化
    public static void normalize(double[] vec) {
        if (!isVector(vec)) {
            throw new IllegalArgumentException("输入参数不是向量！");
        }
        double d = 1 / Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
        for (int i = 0; i < 3; i++) {
            vec[i] *= d;
        }
    }

    //  数量积
    public static void scale(double[] v, double n) {
        if (isVert2(v)) {
            for (int i = 0; i < 2; i++) {
                v[i] *= n;
            }
        }
        else if (isVert3(v)) {
            for (int i = 0; i < 4; i++) {
                v[i] *= n;
            }
        }
        else if (isVector(v)) {
            for (int i = 0; i < 3; i++) {
                v[i] *= n;
            }
        }
        else {
            throw new IllegalArgumentException("相加顶点的坐标数量不同！");
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
