package core;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;

public class MatrixTest {
    @Test
    public void Matrix_test() {
        Double[][] matrix = new Double[][]{
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0},
                {7.0, 8.0, 9.0}
        };
        Matrix<Double> m = new Matrix<>(Double.class, matrix);
        Matrix<Double> m1 = m.vector_add(m);
        System.out.println(Arrays.deepToString(m1.matrix));
    }
}
