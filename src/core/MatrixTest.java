package core;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MatrixTest {
    @Test
    public void Matrix_test() {
        Double[][] matrix = new Double[][]{
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0},
                {7.0, 8.0, 9.0}
        };
        Matrix<Double> m = new Matrix<>(Double.class, matrix);

        Double[][] matrix2 = new Double[][]{
                {1.0},
                {4.0},
                {7.0}
        };
        Matrix<Double> m2 = new Matrix<>(Double.class, matrix2);
        Matrix<Double> m1 = m.vector_product(m2);
        System.out.println(Arrays.deepToString(m1.matrix));
    }

    @Test
    public void Vec3_Matrix_test() {
        Double[][] matrix = new Double[][]{
                {1.0},
                {4.0},
                {7.0}
        };
        Matrix<Double> m = new Matrix<>(Double.class, matrix);
        Vec3 v = new Vec3(m);
        System.out.printf("x: %f, y: %f, z: %f", v.x, v.y, v.z);
    }
}
