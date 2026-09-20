package daa.algorithms;

import daa.metrics.Metrics;

/**
 * Classic top-down MergeSort.
 *
 * <p>Design points required by the assignment:</p>
 * <ul>
 *   <li>linear merge of two already sorted halves;</li>
 *   <li>one reusable auxiliary buffer allocated once per {@link #sort(int[])} call;</li>
 *   <li>small-input cutoff to insertion sort;</li>
 *   <li>running time Theta(n log n), extra space Theta(n).</li>
 * </ul>
 */
public final class MergeSorter {

    public static final int DEFAULT_CUTOFF = 16;

    private final int cutoff;
    private final Metrics metrics;

    public MergeSorter() {
        this(DEFAULT_CUTOFF, new Metrics("MergeSort"));
    }

    public MergeSorter(Metrics metrics) {
        this(DEFAULT_CUTOFF, metrics);
    }

    public MergeSorter(int cutoff, Metrics metrics) {
        if (cutoff < 1) {
            throw new IllegalArgumentException("cutoff must be >= 1");
        }
        this.cutoff = cutoff;
        this.metrics = metrics;
    }

    public Metrics metrics() {
        return metrics;
    }

    /** Sorts {@code a} in ascending order. Null or arrays of length &lt; 2 are left untouched. */
    public void sort(int[] a) {
        if (a == null || a.length < 2) {
            return;
        }
        int[] buffer = new int[a.length];
        metrics.addAllocation(a.length);
        sort(a, buffer, 0, a.length - 1);
    }

    private void sort(int[] a, int[] buffer, int lo, int hi) {
        metrics.enterRecursion();
        try {
            if (hi - lo + 1 <= cutoff) {
                insertionSort(a, lo, hi);
                return;
            }
            int mid = lo + (hi - lo) / 2;
            sort(a, buffer, lo, mid);
            sort(a, buffer, mid + 1, hi);
            if (metrics.compare(a[mid], a[mid + 1]) <= 0) {
                return;
            }
            merge(a, buffer, lo, mid, hi);
        } finally {
            metrics.exitRecursion();
        }
    }

    /** Linear merge of a[lo..mid] and a[mid+1..hi] through the shared buffer. */
    private void merge(int[] a, int[] buffer, int lo, int mid, int hi) {
        System.arraycopy(a, lo, buffer, lo, hi - lo + 1);
        metrics.addMoves(hi - lo + 1);

        int i = lo;
        int j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a[k] = buffer[j++];
            } else if (j > hi) {
                a[k] = buffer[i++];
            } else if (metrics.compare(buffer[j], buffer[i]) < 0) {
                a[k] = buffer[j++];
            } else {
                a[k] = buffer[i++];
            }
            metrics.addMoves(1);
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
}
