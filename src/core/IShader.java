package core;


public class IShader {
    Vec3 light;
    Texture texture;

    public IShader(Vec3 light, Texture texture,double[][] model_view) {
        this.light = new Vec3(Matrix.product(model_view, light.matrix())).normalize();
        this.texture = texture;
    }

    public double[] fragment(Vec3 norm, Vec2 tex) {
        norm = norm.normalize();
        double[] rgb = texture.getRGB(tex.x(), tex.y());
        Vec3 r = norm.product(norm.product(light) * 2).minus(light);
        r = r.normalize();

        double ambient = 0.3;
        double diffuse = Math.max(0.0, norm.product(light));
        double specular = Math.pow(Math.max(0.0, r.z()), 35.0);
        double brightness = Math.min(1.0, ambient + 0.4 * diffuse + 0.9 * specular);

        for (int i = 0; i < 3; i++) {
            rgb[i] *= brightness;
        }
        return rgb;
    }
}
