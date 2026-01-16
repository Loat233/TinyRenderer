package core;

public class IShader {
    double[][] normMatrix;
    Texture[] textures; // 纹理顺序:norm, diffuse, spec, glow

    public IShader(Texture[] textures, double[][] model_view, double[][] normMatrix) {
        this.normMatrix = normMatrix;
        ;
        this.textures = textures;
    }

    // norm: 像素点的插值法线; tex: 像素点的纹理坐标
    public int[] fragment(Fragment clip, double lightIntensity, Vector light, Vector norm, Vec2 tex) {
        // 获取漫反射diffuse纹理
        int[] diff_color = textures[1].getRGB(tex.x(), 1 - tex.y());
        int[] spec_color = textures[2].getRGB(tex.x(), 1 - tex.y());
        int[] glow_color = textures[3].getRGB(tex.x(), 1 - tex.y());

        if (lightIntensity == 0) {
            int[] rgb = new int[3];
            for (int i = 0; i < 3; i++) {
                double glow = glow_color[i] * 0.6;
                double color = Math.min(255.0, glow);
                rgb[i] = (int) Math.floor(color);
            }
            return rgb;
        }
        // 计算eye空间下像素点法线
        // 直接使用插值法线计算,则直接使用norm
        // 若使用global space normal mapping:
        norm = global_space_norm(tex);
        // 若使用tangent space normal mapping:
        // norm = tangent_space_norm(clip, norm, tex);

        // 计算光线的衰减值(attenuation)
        double kc = 1.0;
        double kl = 0.09;
        double kq = 0.032;

        double lg_dist = light.sqrt();
        double attenuation = 1 / (kc + kl * lg_dist + kq * lg_dist * lg_dist);

        double factor = norm.product(light);
        // 计算反射向量r
        Vector r = norm.scale(factor * 2).minus(light).normalize();
        double diff_light = Math.max(0.0, factor) * attenuation + 0.1;
        double spec_light = Math.pow(Math.max(0.0, r.z()), 10.0) * attenuation;

        int[] rgb = new int[3];
        for (int i = 0; i < 3; i++) {
            double base = diff_color[i] * diff_light;
            double specular = spec_color[i] * spec_light;
            double glow = glow_color[i] * 0.6;
            double color = Math.min(255.0, base + 0.8 * specular + glow); // 环境光glow不衰减
            rgb[i] = (int) Math.floor(color);
        }
        return rgb;
    }

    private Vector global_space_norm(Vec2 tex) {
        Vector v = textures[0].getVector(tex.x(), 1 - tex.y());
        return new Vector(Matrix.product(normMatrix, v.matrix())).normalize();
    }

    // 计算eye空间下像素点法线
    private Vector tangent_space_norm(Fragment clip, Vector norm, Vec2 tex) {
        Vec3 p0 = clip.a().eye_coord();
        Vec3 p1 = clip.b().eye_coord();
        Vec3 p2 = clip.c().eye_coord();

        Vector e0 = p1.minus(p0);
        Vector e1 = p2.minus(p0);

        Vec2 u0 = clip.a().tex_coord();
        Vec2 u1 = clip.b().tex_coord();
        Vec2 u2 = clip.c().tex_coord();

        Vec2 r0 = u1.minus(u0);
        Vec2 r1 = u2.minus(u0);

        double[][] E = new double[][] {
                { e0.x(), e1.x() },
                { e0.y(), e1.y() },
                { e0.z(), e1.z() }
        };
        double[][] U = new double[][] {
                { r0.x(), r1.x() },
                { r0.y(), r1.y() }
        };
        double[][] inv_U = Matrix.inverse(U);
        double[][] T = Matrix.product(E, inv_U);

        // t和b需要单位化
        Vector t = new Vector(T[0][0], T[1][0], T[2][0]).normalize();
        Vector b = new Vector(T[0][1], T[1][1], T[2][1]).normalize();

        double[][] D = new double[][] {
                { t.x(), b.x(), norm.x() },
                { t.y(), b.y(), norm.y() },
                { t.z(), b.z(), norm.z() }
        };
        Vector uv_norm = textures[0].getVector(tex.x(), 1 - tex.y());
        double[][] uv = new double[][] {
                { uv_norm.x() },
                { uv_norm.y() },
                { uv_norm.z() }
        };
        return new Vector(Matrix.product(D, uv)).normalize();
    }
}
