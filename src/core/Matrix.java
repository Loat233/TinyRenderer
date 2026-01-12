package core;

import java.util.Arrays;

public class Matrix {
    public static int column(double[][] A) {
        return A[0].length;
    }

    public static int row(double[][] A) {
        return A.length;
    }

    // 计算矩阵乘法
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

    //  计算转置矩阵
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

    // 计算矩阵的逆
    public static double[][] inverse(double[][] A) {
        if (row(A) != column(A)) {
            throw new IllegalArgumentException("矩阵必须是方阵才能求逆!");
        }

        int n = row(A);
        double[][] augmented = new double[n][2 * n];

        // 构建增广矩阵 [A|I]
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented[i][j] = A[i][j];
                augmented[i][j + n] = (i == j) ? 1.0 : 0.0;
            }
        }

        // 高斯-约旦消元
        for (int i = 0; i < n; i++) {
            // 寻找主元
            if (Math.abs(augmented[i][i]) < 1e-10) {
                // 寻找非零行进行交换
                boolean found = false;
                for (int k = i + 1; k < n; k++) {
                    if (Math.abs(augmented[k][i]) > 1e-10) {
                        // 交换行
                        double[] temp = augmented[i];
                        augmented[i] = augmented[k];
                        augmented[k] = temp;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("矩阵不可逆!");
                }
            }

            // 归一化主元行
            double pivot = augmented[i][i];
            for (int j = 0; j < 2 * n; j++) {
                augmented[i][j] /= pivot;
            }

            // 消元
            for (int k = 0; k < n; k++) {
                if (k != i && Math.abs(augmented[k][i]) > 1e-10) {
                    double factor = augmented[k][i];
                    for (int j = 0; j < 2 * n; j++) {
                        augmented[k][j] -= factor * augmented[i][j];
                    }
                }
            }
        }

        // 提取逆矩阵部分
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                inverse[i][j] = augmented[i][j + n];
            }
        }

        return inverse;
    }

    public static double[][] add(double[][] A, double[][] B) {
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

    public static double[][] eliminate(double[][] A) {
        if (Matrix.row(A) == 4 && Matrix.column(A) == 4) {
            return new double[][]{
                    Arrays.copyOf(A[0], 3),
                    Arrays.copyOf(A[1], 3),
                    Arrays.copyOf(A[2], 3)
            };
        }
        throw new IllegalArgumentException("传入矩阵的行列数必须都为4!");
    }
}
