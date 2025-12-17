package core;


public class IShader {
    Vec3 light;
    Triangle tri;

    public IShader(Triangle tri,Vec3 light, double[][] model_view) {
        this.light = new Vec3(Matrix.product(model_view, light.matrix()));
        this.tri = tri;
    }

    public int[] fragment() {
        int[] brightness = new int[]{255, 255, 255};
        Vec3 n = tri.a().minus(tri.c()).cross(tri.a().minus(tri.b())).normalize();
        Vec3 r = n.product(n.product(light) * 2).minus(light);

        double ambient = 0.3;
        double diffuse = Math.max(0.0, n.product(light));
        double specular = Math.pow(Math.max(0.0, r.z()), 35.0);

        for (int i = 0; i < 3; i++) {
            brightness[i] *= (int) Math.min(1.0, ambient + 0.4 * diffuse + 0.9 * specular);
        }
        return brightness;
    }
}
