package daa.experiment;

import daa.experiment.InputGenerator.ArrayType;
import daa.experiment.InputGenerator.PointSetType;
import daa.model.Point;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputGeneratorTest {

    @Test
    @DisplayName("every array type has the requested shape")
    void arrayShapes() {
        InputGenerator generator = new InputGenerator(1L);
        int n = 2000;

        int[] random = generator.array(ArrayType.RANDOM, n);
        assertEquals(n, random.length);

        int[] sorted = generator.array(ArrayType.SORTED, n);
        for (int i = 1; i < n; i++) {
            assertTrue(sorted[i - 1] <= sorted[i], "not ascending at " + i);
        }

        int[] reverse = generator.array(ArrayType.REVERSE_SORTED, n);
        for (int i = 1; i < n; i++) {
            assertTrue(reverse[i - 1] >= reverse[i], "not descending at " + i);
        }

        int[] duplicates = generator.array(ArrayType.DUPLICATE_HEAVY, n);
        long distinct = Arrays.stream(duplicates).distinct().count();
        assertTrue(distinct <= 10, "expected at most 10 distinct values, got " + distinct);
    }

    @Test
    @DisplayName("the same seed reproduces the same data")
    void generatorIsDeterministic() {
        int[] first = new InputGenerator(99L).array(ArrayType.RANDOM, 500);
        int[] second = new InputGenerator(99L).array(ArrayType.RANDOM, 500);
        assertTrue(Arrays.equals(first, second));
    }

    @Test
    @DisplayName("point sets have the requested size and shape")
    void pointShapes() {
        InputGenerator generator = new InputGenerator(7L);
        for (PointSetType type : PointSetType.values()) {
            Point[] points = generator.points(type, 300);
            assertEquals(300, points.length, "wrong size for " + type);
            for (Point p : points) {
                assertTrue(Double.isFinite(p.x()) && Double.isFinite(p.y()),
                        "non-finite coordinate for " + type);
            }
        }
    }
}
