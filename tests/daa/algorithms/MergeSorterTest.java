package daa.algorithms;

import daa.metrics.Metrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeSorterTest {

    private static final long SEED = 12345L;

    @Test
    @DisplayName("random arrays of many sizes match Arrays.sort")
    void randomArraysMatchReference() {
        Random random = new Random(SEED);
        for (int n = 0; n <= 300; n++) {
            int[] a = random.ints(n, -1000, 1000).toArray();
            int[] expected = a.clone();
            Arrays.sort(expected);
            new MergeSorter().sort(a);
            assertArrayEquals(expected, a, "failed for n = " + n);
        }
    }

    @Test
    @DisplayName("already sorted input stays sorted")
    void sortedInput() {
        int[] a = new int[1000];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        int[] expected = a.clone();
        new MergeSorter().sort(a);
        assertArrayEquals(expected, a);
    }

    @Test
    @DisplayName("reverse-sorted input is fully reversed")
    void reverseSortedInput() {
        int[] a = new int[1000];
        for (int i = 0; i < a.length; i++) {
            a[i] = a.length - i;
        }
        int[] expected = a.clone();
        Arrays.sort(expected);
        new MergeSorter().sort(a);
        assertArrayEquals(expected, a);
    }

    @Test
    @DisplayName("duplicate-heavy input is handled")
    void duplicateHeavyInput() {
        Random random = new Random(SEED);
        int[] a = random.ints(5000, 0, 5).toArray();
        int[] expected = a.clone();
        Arrays.sort(expected);
        new MergeSorter().sort(a);
        assertArrayEquals(expected, a);
    }

    @Test
    @DisplayName("empty, single-element and two-element arrays")
    void degenerateInputs() {
        int[] empty = {};
        assertDoesNotThrow(() -> new MergeSorter().sort(empty));
        assertArrayEquals(new int[] {}, empty);

        int[] single = {42};
        new MergeSorter().sort(single);
        assertArrayEquals(new int[] {42}, single);

        int[] two = {2, 1};
        new MergeSorter().sort(two);
        assertArrayEquals(new int[] {1, 2}, two);

        assertDoesNotThrow(() -> new MergeSorter().sort(null));
    }

    @Test
    @DisplayName("extreme int values do not overflow comparisons")
    void extremeValues() {
        int[] a = {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1, Integer.MIN_VALUE};
        int[] expected = a.clone();
        Arrays.sort(expected);
        new MergeSorter().sort(a);
        assertArrayEquals(expected, a);
    }

    @Test
    @DisplayName("recursion depth stays logarithmic and only one buffer is allocated")
    void depthAndAllocations() {
        int n = 100_000;
        Random random = new Random(SEED);
        int[] a = random.ints(n, -1_000_000, 1_000_000).toArray();

        Metrics metrics = new Metrics("MergeSort");
        new MergeSorter(metrics).sort(a);

        int bound = 2 * (int) (Math.log(n) / Math.log(2)) + 4;
        assertTrue(metrics.maxDepth() <= bound,
                "depth " + metrics.maxDepth() + " exceeded bound " + bound);
        assertEquals(n, metrics.allocations(),
                "exactly one auxiliary buffer of size n must be allocated");
    }

    @Test
    @DisplayName("a cutoff of 1 (pure merge sort) still sorts correctly")
    void cutoffOfOne() {
        Random random = new Random(SEED);
        int[] a = random.ints(2000, -100, 100).toArray();
        int[] expected = a.clone();
        Arrays.sort(expected);
        new MergeSorter(1, new Metrics("MergeSort")).sort(a);
        assertArrayEquals(expected, a);
    }
}
