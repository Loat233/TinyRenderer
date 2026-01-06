package core;

public class Vec3 {
    private final double x;
    private final double y;
    private final double z;
    private double[][] matrix;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.matrix = new double[][]{
                {x},
                {y},
                {z},
                {1}
        };
    }

    public Vec3(double[][] M) {
        if (Matrix.row(M) == 1) {
            this.x = M[0][0];
            this.y = M[0][1];
            this.z = M[0][2];
            this.matrix = M;
        }
        else if (Matrix.row(M) == 4) {
            double factor = M[3][0];
            this.x = M[0][0] / factor;
            this.y = M[1][0] / factor;
            this.z = M[2][0] / factor;
            this.matrix = M;
        }
        else {
            throw new IllegalArgumentException("传入的数组不符合规范!");
        }
    }

    public Vec3 add(Vec3 v) {
        return new Vec3(x + v.x, y + v.y, z + v.z);
    }

    public Vec3 minus(Vec3 v) {
        return new Vec3(x - v.x, y - v.y, z - v.z);
    }

    public double product(Vec3 n) {
        return x * n.x + y * n.y + z * n.z;
    }

    public Vec3 product(double n) {
        return new Vec3(x * n, y * n, z * n);
    }

    public Vec3 cross(Vec3 v) {
        double i = y * v.z - z * v.y;
        double j = z * v.x - x * v.z;
        double k = x * v.y - y * v.x;
        return new Vec3(i, j, k);
    }

    public Vec3 normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        return new Vec3(x / length, y / length, z /length);
    }

    public double norm() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public void transpose() {
        matrix = Matrix.transpose(matrix);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double[][] matrix() {
        return matrix;
    }
}