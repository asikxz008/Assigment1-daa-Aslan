package daa.algorithms;

import daa.metrics.Metrics;

import java.util.Random;

/**
 * Randomized QuickSort with a bounded recursion stack.
 *
 * <p>Design points required by the assignment:</p>
 * <ul>
 *   <li>random pivot, so the expected running time is Theta(n log n)
 *       for every fixed input;</li>
 *   <li>in-place Hoare partitioning (robust on duplicate-heavy data);</li>
 *   <li>the algorithm recurses into the <em>smaller</em> partition and iterates
 *       over the larger one, which bounds the recursion depth by
 *       O(log n) in the worst case;</li>
 *   <li>worst-case running time is still O(n^2), but it happens with
 *       negligible probability.</li>
 * </ul>
 */
public final class QuickSorter {

    public static final int DEFAULT_CUTOFF = 16;

    private final int cutoff;
    private final Metrics metrics;
    private final Random random;

    public QuickSorter() {
        this(DEFAULT_CUTOFF, new Metrics("QuickSort"), new Random());
    }

    public QuickSorter(Metrics metrics) {
        this(DEFAULT_CUTOFF, metrics, new Random());
    }

    public QuickSorter(Metrics metrics, long seed) {
        this(DEFAULT_CUTOFF, metrics, new Random(seed));
    }

    public QuickSorter(int cutoff, Metrics metrics, Random random) {
        if (cutoff < 1) {
            throw new IllegalArgumentException("cutoff must be >= 1");
        }
        this.cutoff = cutoff;
        this.metrics = metrics;
        this.random = random;
    }

    public Metrics metrics() {
        return metrics;
    }

    /** Sorts {@code a} in ascending order, in place. */
    public void sort(int[] a) {
        if (a == null || a.length < 2) {
            return;
        }
        quickSort(a, 0, a.length - 1);
    }

    private void quickSort(int[] a, int lo, int hi) {
        metrics.enterRecursion();
        try {
            while (lo < hi) {
                if (hi - lo + 1 <= cutoff) {
                    insertionSort(a, lo, hi);
                    return;
                }
                int split = partition(a, lo, hi);
                int leftSize = split - lo + 1;
                int rightSize = hi - split;
                if (leftSize < rightSize) {
                    quickSort(a, lo, split);
                    lo = split + 1;
                } else {
                    quickSort(a, split + 1, hi);
                    hi = split;
                }
            }
        } finally {
            metrics.exitRecursion();
        }
    }

    /**
     * Hoare partition around a uniformly random pivot.
     *
     * @return index {@code j} such that a[lo..j] &le; pivot &le; a[j+1..hi]
     */
    private int partition(int[] a, int lo, int hi) {
        int pivotIndex = lo + random.nextInt(hi - lo + 1);
        swap(a, lo, pivotIndex);
        int pivot = a[lo];

        int i = lo - 1;
        int j = hi + 1;
        while (true) {
            do {
                i++;
            } while (metrics.compare(a[i], pivot) < 0);
            do {
                j--;
            } while (metrics.compare(a[j], pivot) > 0);
            if (i >= j) {
                return j;
            }
            swap(a, i, j);
        }
    }

    private void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= lo && metrics.compare(a[j], key) > 0) {
                a[j + 1] = a[j];
                metrics.addMoves(1);
                j--;
            }
            a[j + 1] = key;
            metrics.addMoves(1);
        }
    }

    private void swap(int[] a, int i, int j) {
        if (i == j) {
            return;
        }
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
        metrics.addSwap();
    }
}
