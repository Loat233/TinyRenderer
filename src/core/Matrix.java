package core;

public class Matrix {
    public static int column(double[][] A) {
        return A[0].length;
    }

    public static int row(double[][] A) {
        return A.length;
    }

    public static double[][] product(double[][] A, double[][] B) {
        if (column(A) != row(B)) {
            throw new IllegalArgumentException("矩阵A的列数必须等于矩阵B的行数!");
        }


        int row = row(A);
        int column = column(B);
        double[][] M = new double[row][column];

        for (int x = 0; x < row; x++) {
            for (int y = 0; y < column; y++) {
                double sum = 0.0;
                for (int k = 0; k < column(A); k++) {
                    double a = A[x][k];
                    double b = B[k][y];
                    sum += a * b;
                }
                M[x][y] = sum;
            }
        }
        return M;
    }

    public static double[][] transpose(double[][] A) {
        int row = row(A);
        int column = column(A);
        double[][] M = new double[column][row];
        for (int x = 0; x < row; x++) {
             for (int y = 0; y < column; y++) {
                 M[y][x] = A[x][y];
             }
        }
        return M;
    }



    public double[][] add(double[][] A, double[][] B) {
        if (row(A) == row(B) && column(A) == column(B)) {
            int row = row(A);
            int column = column(A);
            double[][] M = new double[row][column];

            for (int x = 0; x < row; x++) {
                for (int y = 0; y < column; y++) {
                    M[x][y] = A[x][y] + B[x][y];
                }
            }
            return M;
        }
        throw new IllegalArgumentException("矩阵A的行列数必须等于矩阵B的行列数!");
    }
}
