package core;


public class IShader {
    Vec3 light;

    public IShader(Vec3 light, double[][] model_view) {
        this.light = new Vec3(Matrix.product(model_view, light.matrix())).normalize();

    }

    public double[] fragment(Vec3 n) {
        n = n.normalize();
        double[] brightness = new double[]{255.0, 255.0 ,255.0};
        Vec3 r = n.product(n.product(light) * 2).minus(light);
        r = r.normalize();

        double ambient = 0.3;
        double diffuse = Math.max(0.0, n.product(light));
        double specular = Math.pow(Math.max(0.0, r.z()), 35.0);

        for (int i = 0; i < 3; i++) {
            brightness[i] *= Math.min(1.0, ambient + 0.4 * diffuse + 0.9 * specular);
        }
        return brightness;
    }
}
