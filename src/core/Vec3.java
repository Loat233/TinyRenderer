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
        this.matrix = create_matrix();
    }
    //原点向量
    public Vec3(Matrix<Double> m) {
        this.x = m.matrix[0][0];
        this.y = m.matrix[1][0];
        this.z = m.matrix[2][0];
        this.matrix = m;
    }

    public Vec3 product(double n) {
        return new Vec3(x * n, y * n, z * n);
    }

    public int intX() {
        return (int) x;
    }

    public int intY() {
        return (int) y;
    }

    public int intZ() {
        return (int) z;
    }

    private Matrix<Double> create_matrix() {
        Double[][] matrix = new Double[][]{{(Double) x}, {(Double) y}, {(Double) z}};
        return new Matrix<>(Double.class, matrix);
    }
}
