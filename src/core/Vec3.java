package core;

public class Vec3 {
    private final double x;
    private final double y;
    private final double z;
    private final double w;
    private double[][] matrix;

    public Vec3(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.matrix = new double[][]{
                {x},
                {y},
                {z},
                {1}
        };
    }

    public Vec3(double[][] M) {
        //  横向矩阵
        if (Matrix.row(M) == 1) {
            this.x = M[0][0];
            this.y = M[0][1];
            this.z = M[0][2];
            this.w = M[0][3];
            this.matrix = M;
        }
        //  纵向矩阵
        else if (Matrix.row(M) == 4 && Matrix.column(M) == 1) {
            this.x = M[0][0];
            this.y = M[1][0];
            this.z = M[2][0];
            this.w = M[3][0];
            this.matrix = new double[][] {
                    {x},
                    {y},
                    {z},
                    {w}
            };
        }
        else {
            throw new IllegalArgumentException("传入的数组不符合规范!");
        }
    }

      //  会舍弃w分量
    public Vector minus(Vec3 v) {
        return new Vector(x - v.x, y - v.y, z - v.z);
    }

    public Vec3 scale(double n) {
        return new Vec3(x * n, y * n, z * n, w * n);
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

    public double w() {
        return w;
    }

    public double[][] matrix() {
        return matrix;
    }
}
