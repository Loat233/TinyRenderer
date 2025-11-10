package core;

import java.lang.reflect.Array;

public class Matrix<T extends Number> {
    int row;
    int column;
    T[][] matrix;
    private Class<T> elenmet_class;

    public Matrix(Class<T> elenmet_class, int row, int column) {
        this.elenmet_class = elenmet_class;
        this.row = row;
        this.column = column;
        this.matrix = (T[][]) Array.newInstance(elenmet_class, row, column);
    }

    public Matrix(Class<T> elenmet_class, T[][] matrix) {
        this.elenmet_class = elenmet_class;
        this.row = matrix[0].length;
        this.column = matrix.length;
        this.matrix = matrix;
    }

    public Matrix<T> vector_product(Matrix<T> m) {
        if (column != m.row) {
            throw new IllegalArgumentException("矩阵A的列数必须等于矩阵B的行数!");
        }

        Matrix<T> r = new Matrix<>(elenmet_class, row , m.column);
        for (int x = 0; x < row; x++) {
            for (int y = 0; y < m.column; y++) {
                double sum = 0.0;
                for (int k = 0; k < column; k++) {
                    double a = matrix[x][k].doubleValue();
                    double b = m.matrix[k][y].doubleValue();
                    sum += a*b;
                }
                r.matrix[x][y] = converyT(sum);
            }
        }
        return r;
    }

    public Matrix<T> vector_add(Matrix<T> m) {
        if (row == m.row && column == m.column) {
            Matrix<T> r = new Matrix<>(elenmet_class, row, column);

            for (int x = 0; x < row; x++) {
                for (int y = 0; y < column; y++) {
                    r.matrix[x][y] = converyT(matrix[x][y].doubleValue() + m.matrix[x][y].doubleValue());
                }
            }

            return r;
        }

        throw new IllegalArgumentException("矩阵A的行列数必须等于矩阵B的行列数!");
    }

    private T converyT(double value) {
        if (elenmet_class == Integer.class) {
            return elenmet_class.cast((int) value);
        }
        else if (elenmet_class == Double.class) {
            return elenmet_class.cast(value);
        }
        throw new UnsupportedOperationException("不支持运算的矩阵元素");
    }
}
