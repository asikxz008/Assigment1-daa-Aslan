package daa.algorithms;

import daa.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickSorterTest {

    private static final long SEED = 987654321L;

    @Test
    @DisplayName("random arrays of many sizes match Arrays.sort")
    void randomArraysMatchReference() {
        Random random = new Random(SEED);
        for (int n = 0; n <= 300; n++) {
            int[] a = random.ints(n, -1000, 1000).toArray();
            int[] expected = a.clone();
            Arrays.sort(expected);
            new QuickSorter(new Metrics("QuickSort"), SEED + n).sort(a);
            assertArrayEquals(expected, a, "failed for n = " + n);
        }
    }

    @Test
    @DisplayName("sorted and reverse-sorted inputs are handled without degradation")
    void sortedAndReverseInputs() {
        int n = 20_000;
        int[] sorted = new int[n];
        int[] reverse = new int[n];
        for (int i = 0; i < n; i++) {
            sorted[i] = i;
            reverse[i] = n - i;
        }

        int[] expectedSorted = sorted.clone();
        int[] expectedReverse = reverse.clone();
        Arrays.sort(expectedReverse);

        Metrics m1 = new Metrics("QuickSort");
        new QuickSorter(m1, SEED).sort(sorted);
        assertArrayEquals(expectedSorted, sorted);

        Metrics m2 = new Metrics("QuickSort");
        new QuickSorter(m2, SEED).sort(reverse);
        assertArrayEquals(expectedReverse, reverse);

        int bound = depthBound(n);
        assertTrue(m1.maxDepth() <= bound, "sorted input depth " + m1.maxDepth());
        assertTrue(m2.maxDepth() <= bound, "reverse input depth " + m2.maxDepth());
    }

    @Test
    @DisplayName("duplicate-heavy and all-equal inputs are handled")
    void duplicateInputs() {
        Random random = new Random(SEED);
        int[] duplicates = random.ints(10_000, 0, 3).toArray();
        int[] expected = duplicates.clone();
        Arrays.sort(expected);
        new QuickSorter(new Metrics("QuickSort"), SEED).sort(duplicates);
        assertArrayEquals(expected, duplicates);

        int[] allEqual = new int[10_000];
        Arrays.fill(allEqual, 7);
        Metrics metrics = new Metrics("QuickSort");
        new QuickSorter(metrics, SEED).sort(allEqual);
        int[] expectedEqual = new int[10_000];
        Arrays.fill(expectedEqual, 7);
        assertArrayEquals(expectedEqual, allEqual);
        assertTrue(metrics.maxDepth() <= depthBound(10_000),
                "all-equal input must not blow up the stack: " + metrics.maxDepth());
    }

    @Test
    @DisplayName("empty, single-element and two-element arrays")
    void degenerateInputs() {
        int[] empty = {};
        assertDoesNotThrow(() -> new QuickSorter().sort(empty));
        assertArrayEquals(new int[] {}, empty);

        int[] single = {42};
        new QuickSorter().sort(single);
        assertArrayEquals(new int[] {42}, single);

        int[] two = {2, 1};
        new QuickSorter().sort(two);
        assertArrayEquals(new int[] {1, 2}, two);

        assertDoesNotThrow(() -> new QuickSorter().sort(null));
    }

    @Test
    @DisplayName("recursion depth stays close to 2*log2(n) over many random seeds")
    void recursionDepthIsBounded() {
        int n = 100_000;
        Random random = new Random(SEED);
        int bound = depthBound(n);
        for (int run = 0; run < 20; run++) {
            int[] a = random.ints(n, -1_000_000, 1_000_000).toArray();
            Metrics metrics = new Metrics("QuickSort");
            new QuickSorter(metrics, SEED + run).sort(a);
            assertTrue(metrics.maxDepth() <= bound,
                    "run " + run + ": depth " + metrics.maxDepth() + " > " + bound);
            assertTrue(isSorted(a), "run " + run + ": array not sorted");
        }
    }

    /**
     * Recursing into the smaller partition guarantees that each recursive level at
     * least halves the sub-array, so the depth cannot exceed floor(log2 n) + O(1).
     */
    private static int depthBound(int n) {
        return (int) (Math.log(n) / Math.log(2)) + 4;
    }

    private static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }
}
