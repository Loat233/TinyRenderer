package core;


public class IShader {
    Vec3 light;
    Texture[] textures;     //纹理顺序:norm, diffuse, spec, glow

    public IShader(Vec3 light, Texture[] textures, double[][] model_view) {
        this.light = new Vec3(Matrix.product(model_view, light.matrix())).normalize();
        this.textures = textures;
    }

    public double[] fragment(Vec2 tex) {
        //获取法线采样纹理
        Vec3 norm = textures[0].getNorm(tex.x(), 1 - tex.y());
        return fragment(norm.normalize(), tex);
    }

    public double[] fragment(Vec3 norm, Vec2 tex) {
        //获取漫反射diffuse纹理
        double[] diff_color = textures[1].getRGB(tex.x(), 1 - tex.y());
        double[] spec_color = textures[2].getRGB(tex.x(), 1 - tex.y());
        double[] glow_color = textures[3].getRGB(tex.x(), 1 - tex.y());

        //计算反射向量r
        norm = norm.normalize();
        Vec3 r = norm.product(norm.product(light) * 2).minus(light);

        double ambient = 0.3;
        double diff_light = Math.max(0.0, norm.product(light));
        double spec_light = Math.pow(Math.max(0.0, r.z()), 35.0);

        double[] rgb = new double[3];
        for (int i = 0; i < 3; i++) {
            double base = diff_color[i] * diff_light;
            double specular = spec_color[i] * spec_light;
            double glow = glow_color[i] * ambient;
            rgb[i] = Math.min(255.0, base + specular + glow);
        }
        return rgb;
    }

}
