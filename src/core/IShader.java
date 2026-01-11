package core;


public class IShader {
    double[][] normMatrix;
    Vec3 light;
    Texture[] textures;     //纹理顺序:norm, diffuse, spec, glow

    public IShader(Vec3 light, Texture[] textures, double[][] eye_matrix, double[][] normMatrix) {
        this.normMatrix = normMatrix;
        this.light = new Vec3(Matrix.product(eye_matrix, light.matrix())).normalize();
        this.textures = textures;
    }

    //  norm: 像素点的插值法线; tex: 像素点的纹理坐标
    public double[] fragment(Fragment clip, Vec3 norm, Vec2 tex) {
        //获取漫反射diffuse纹理
        double[] diff_color = textures[1].getRGB(tex.x(), 1 - tex.y());
        double[] spec_color = textures[2].getRGB(tex.x(), 1 - tex.y());
        double[] glow_color = textures[3].getRGB(tex.x(), 1 - tex.y());

        //  计算eye空间下像素点法线
        //  直接使用插值法线计算:
            norm = norm.normalize();
        //  使用global space normal mapping:
        //  norm = global_space_norm(tex).normalize();
        //  norm = tangent_space_norm(clip, norm, tex).normalize();

        double factor = norm.product(light);
        //  计算反射向量r
        Vec3 r = norm.scale(factor * 2).minus(light).normalize();

        double ambient = 0.8;
        double diff_light = Math.max(0.0, factor);
        double spec_light = Math.pow(Math.max(0.0, r.z()), 35.0);

        double[] rgb = new double[3];
        for (int i = 0; i < 3; i++) {
            double base = diff_color[i] * diff_light;
            double specular = spec_color[i] * spec_light;
            double glow = glow_color[i] * ambient;
            rgb[i] = Math.min(255.0, (base + 1.3 * specular + 1.2 * glow) * 1.8);
        }
        return rgb;
    }

    private Vec3 global_space_norm(Vec2 tex) {
        Vec3 v = textures[0].getVector(tex.x(), 1 - tex.y());
        return new Vec3(Matrix.product(normMatrix, v.matrix()));
    }

    //  计算eye空间下像素点法线
    private Vec3 tangent_space_norm(Fragment clip, Vec3 norm , Vec2 tex) {
        norm = norm.normalize();
        Vec3 p0 = clip.a().eye_coord();
        Vec3 p1 = clip.b().eye_coord();
        Vec3 p2 = clip.c().eye_coord();

        Vec3 e0 = p1.minus(p0);
        Vec3 e1 = p2.minus(p0);

        Vec2 u0 = clip.a().tex_coord();
        Vec2 u1 = clip.b().tex_coord();
        Vec2 u2 = clip.c().tex_coord();

        Vec2 r0 = u1.minus(u0);
        Vec2 r1 = u2.minus(u0);

        double[][] E = new double[][] {
                {e0.x(), e1.x()},
                {e0.y(), e1.y()},
                {e0.z(), e1.z()}
        };
        double[][] U = new double[][] {
                {r0.x(), r1.x()},
                {r0.y(), r1.y()}
        };
        double[][] inv_U = Matrix.inverse(U);
        double[][] T = Matrix.product(E, inv_U);

        //  t和b需要单位化
        Vec3 t = new Vec3(T[0][0], T[1][0], T[2][0]);
        t.normalize();
        Vec3 b = new Vec3(T[0][1], T[1][1], T[2][1]);
        b.normalize();

        double[][] D = new double[][] {
                {t.x(), b.x(), norm.x()},
                {t.y(), b.y(), norm.y()},
                {t.z(), b.z(), norm.z()}
        };
        Vec3 uv_norm = textures[0].getVector(tex.x(), 1 - tex.y());
        double[][] uv = new double[][] {
                {uv_norm.x()},
                {uv_norm.y()},
                {uv_norm.z()}
        };
        return new Vec3(Matrix.product(Matrix.transpose(D), uv));
    }

}
