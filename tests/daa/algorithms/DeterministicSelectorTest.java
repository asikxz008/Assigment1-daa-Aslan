package daa.algorithms;

import daa.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicSelectorTest {

    private static final long SEED = 2_026L;

    @Test
    @DisplayName("200 random trials agree with Arrays.sort(a)[k]")
    void randomTrialsAgreeWithSortedReference() {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 200; trial++) {
            int n = 1 + random.nextInt(400);
            int k = random.nextInt(n);
            int[] a = random.ints(n, -500, 500).toArray();

            int[] reference = a.clone();
            Arrays.sort(reference);

            int actual = new DeterministicSelector().select(a.clone(), k);
            assertEquals(reference[k], actual,
                    "trial " + trial + " (n = " + n + ", k = " + k + ")");
        }
    }

    @Test
    @DisplayName("smallest, median and largest element of a large array")
    void orderStatisticsOfLargeArray() {
        Random random = new Random(SEED);
        int n = 50_000;
        int[] a = random.ints(n, -1_000_000, 1_000_000).toArray();
        int[] reference = a.clone();
        Arrays.sort(reference);

        assertEquals(reference[0], new DeterministicSelector().select(a.clone(), 0));
        assertEquals(reference[n / 2], new DeterministicSelector().select(a.clone(), n / 2));
        assertEquals(reference[n - 1], new DeterministicSelector().select(a.clone(), n - 1));
    }

    @Test
    @DisplayName("sorted, reverse-sorted and duplicate-heavy inputs")
    void structuredInputs() {
        int n = 10_000;
        int[] sorted = new int[n];
        int[] reverse = new int[n];
        for (int i = 0; i < n; i++) {
            sorted[i] = i;
            reverse[i] = n - i;
        }
        assertEquals(n / 2, new DeterministicSelector().select(sorted.clone(), n / 2));

        int[] referenceReverse = reverse.clone();
        Arrays.sort(referenceReverse);
        assertEquals(referenceReverse[n / 3],
                new DeterministicSelector().select(reverse.clone(), n / 3));

        Random random = new Random(SEED);
        int[] duplicates = random.ints(n, 0, 4).toArray();
        int[] referenceDuplicates = duplicates.clone();
        Arrays.sort(referenceDuplicates);
        for (int k : new int[] {0, n / 4, n / 2, n - 1}) {
            assertEquals(referenceDuplicates[k],
                    new DeterministicSelector().select(duplicates.clone(), k),
                    "k = " + k);
        }

        int[] allEqual = new int[n];
        Arrays.fill(allEqual, 9);
        assertEquals(9, new DeterministicSelector().select(allEqual, n / 2));
    }

    @Test
    @DisplayName("single-element array and invalid arguments")
    void degenerateInputs() {
        assertEquals(5, new DeterministicSelector().select(new int[] {5}, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new DeterministicSelector().select(new int[] {}, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new DeterministicSelector().select(null, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> new DeterministicSelector().select(new int[] {1, 2, 3}, 3));
        assertThrows(IndexOutOfBoundsException.class,
                () -> new DeterministicSelector().select(new int[] {1, 2, 3}, -1));
    }

    @Test
    @DisplayName("work stays linear: comparisons grow proportionally to n")
    void linearNumberOfComparisons() {
        Random random = new Random(SEED);
        int small = 20_000;
        int large = 4 * small;

        Metrics smallMetrics = new Metrics("DeterministicSelect");
        new DeterministicSelector(smallMetrics)
                .select(random.ints(small, -1_000_000, 1_000_000).toArray(), small / 2);

        Metrics largeMetrics = new Metrics("DeterministicSelect");
        new DeterministicSelector(largeMetrics)
                .select(random.ints(large, -1_000_000, 1_000_000).toArray(), large / 2);

        double ratio = (double) largeMetrics.comparisons() / smallMetrics.comparisons();
        assertTrue(ratio < 6.0,
                "comparison ratio " + ratio + " suggests super-linear behaviour");
    }

    @Test
    @DisplayName("recursion depth is logarithmic, not linear")
    void recursionDepthIsBounded() {
        Random random = new Random(SEED);
        int n = 100_000;
        Metrics metrics = new Metrics("DeterministicSelect");
        new DeterministicSelector(metrics)
                .select(random.ints(n, -1_000_000, 1_000_000).toArray(), n / 2);

        int bound = 8 * (int) (Math.log(n) / Math.log(2));
        assertTrue(metrics.maxDepth() <= bound,
                "depth " + metrics.maxDepth() + " exceeded bound " + bound);
    }
}
