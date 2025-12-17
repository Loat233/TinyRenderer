package Test;
import core.Matrix;
import core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MatrixTest {
    @Test
    public void Matrix_test() {
        double[][] matrix = new double[][]{
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0},
                {7.0, 8.0, 9.0}
        };

        double[][] matrix2 = new double[][]{
                {1.0},
                {4.0},
                {7.0}
        };
        double[][] m = Matrix.product(matrix, matrix2);
        System.out.println(Arrays.deepToString(m));
    }

    @Test
    public void Vec3_Matrix_test() {
        double[][] matrix = new double[][]{
                {1.0},
                {4.0},
                {7.0}
        };
        Vec3 v = new Vec3(matrix);
        System.out.printf("x: %f, y: %f, z: %f", v.x(), v.y(), v.z());
    }

    @Test
    public void transpose_test() {
        double[][] matrix = new double[][]{
                {1.0},
                {4.0},
                {7.0}
        };
        Vec3 v = new Vec3(matrix);
        System.out.printf("row: %d, column: %d\n", Matrix.row(v.matrix()), Matrix.column(v.matrix()));
        v.transpose();
        System.out.printf("row: %d, column: %d", Matrix.row(v.matrix()), Matrix.column(v.matrix()));
    }
}
