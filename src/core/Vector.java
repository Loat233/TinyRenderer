package core;

public class Vector {
    private final double x;
    private final double y;
    private final double z;
    private double[][] matrix;

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.matrix = new double[][]{
                {x},
                {y},
                {z},
        };
    }

    public Vector(double[][] M) {

        if (Matrix.row(M) == 1) {
            this.x = M[0][0];
            this.y = M[0][1];
            this.z = M[0][2];
            this.matrix = M;
        }
        else if (Matrix.row(M) == 3 && Matrix.column(M) == 1) {
            this.x = M[0][0];
            this.y = M[1][0];
            this.z = M[2][0];
            this.matrix = new double[][]{
                    {x},
                    {y},
                    {z},
            };
        }
        else {
            throw new IllegalArgumentException("传入的数组不符合规范!");
        }
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("x:");
        string.append(x());
        string.append(", y:");
        string.append(y());
        string.append(", z:");
        string.append(z());
        return string.toString();
    }

    public Vector add(Vector v) {
        return new Vector(x + v.x, y + v.y, z + v.z);
    }

    public Vector minus(Vector v) {
        return new Vector(x - v.x, y - v.y, z - v.z);
    }

    public double product(Vector n) {
        return x * n.x + y * n.y + z * n.z;
    }

    public Vector scale(double n) {
        return new Vector(x * n, y * n, z * n);
    }

    public Vector cross(Vector v) {
        double i = y * v.z - z * v.y;
        double j = z * v.x - x * v.z;
        double k = x * v.y - y * v.x;
        return new Vector(i, j, k);
    }

    public Vector normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        return new Vector(x / length, y / length, z /length);
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