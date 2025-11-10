package core;

public class Vec3 {
    double x;
    double y;
    double z;
    Matrix<Double> matrix;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        Double[][] matrix = new Double[][]{{(Double) x}, {(Double) y}, {(Double) z}};
        this.matrix = new Matrix<>(Double.class, matrix);
    }
    //原点向量
    public Vec3() {
        new Vec3(0, 0, 0);
    }
}
