package daa.algorithms;

import daa.metrics.Metrics;

/**
 * Deterministic selection of the k-th smallest element (Median of Medians, BFPRT).
 *
 * <p>Design points required by the assignment:</p>
 * <ul>
 *   <li>the range is split into groups of 5, each group is sorted in place and its
 *       median is moved to the front of the range;</li>
 *   <li>the median of those medians is found by a recursive call and used as pivot;</li>
 *   <li>partitioning is in place; a three-way (Dutch national flag) split keeps the
 *       algorithm linear on duplicate-heavy inputs;</li>
 *   <li>recursion goes only into the partition that can still contain the answer;</li>
 *   <li>worst-case running time Theta(n), extra space O(1) beyond the recursion stack.</li>
 * </ul>
 *
 * <p>Note: the methods rearrange the array passed to them. Pass a copy if the original
 * order must be preserved.</p>
 */
public final class DeterministicSelector {

    private static final int GROUP = 5;

    private final Metrics metrics;

    public DeterministicSelector() {
        this(new Metrics("DeterministicSelect"));
    }

    public DeterministicSelector(Metrics metrics) {
        this.metrics = metrics;
    }

    public Metrics metrics() {
        return metrics;
    }

    /**
     * Returns the k-th smallest element of {@code a} (0-based), rearranging {@code a}.
     *
     * @throws IllegalArgumentException  if the array is null or empty
     * @throws IndexOutOfBoundsException if {@code k} is outside {@code [0, a.length)}
     */
    public int select(int[] a, int k) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("array must be non-empty");
        }
        if (k < 0 || k >= a.length) {
            throw new IndexOutOfBoundsException("k out of range: " + k);
        }
        return select(a, 0, a.length - 1, k);
    }

    private int select(int[] a, int lo, int hi, int k) {
        metrics.enterRecursion();
        try {
            if (lo == hi) {
                return a[lo];
            }
            if (hi - lo + 1 <= GROUP) {
                insertionSort(a, lo, hi);
                return a[k];
            }

            int pivot = medianOfMedians(a, lo, hi);
            int[] bounds = partitionThreeWay(a, lo, hi, pivot);
            int lt = bounds[0];
            int gt = bounds[1];

            if (k < lt) {
                return select(a, lo, lt - 1, k);
            }
            if (k > gt) {
                return select(a, gt + 1, hi, k);
            }
            return a[k];
        } finally {
            metrics.exitRecursion();
        }
    }

    /**
     * Returns the value "median of the medians of groups of five" for a[lo..hi].
     * Group medians are compacted into a[lo..lo+groups-1] and the median of that
     * block is found by a recursive {@link #select} call.
     */
    private int medianOfMedians(int[] a, int lo, int hi) {
        int n = hi - lo + 1;
        int groups = (n + GROUP - 1) / GROUP;

        for (int g = 0; g < groups; g++) {
            int groupLo = lo + g * GROUP;
            int groupHi = Math.min(groupLo + GROUP - 1, hi);
            insertionSort(a, groupLo, groupHi);
            int median = groupLo + (groupHi - groupLo) / 2;
            swap(a, lo + g, median);
        }

        int medianBlockHi = lo + groups - 1;
        int medianIndex = lo + groups / 2;
        return select(a, lo, medianBlockHi, medianIndex);
    }

    /**
     * Dutch-national-flag partition of a[lo..hi] around {@code pivot}.
     *
     * @return {@code {lt, gt}} where a[lo..lt-1] &lt; pivot, a[lt..gt] == pivot,
     *         a[gt+1..hi] &gt; pivot
     */
    private int[] partitionThreeWay(int[] a, int lo, int hi, int pivot) {
        int lt = lo;
        int i = lo;
        int gt = hi;
        while (i <= gt) {
            int cmp = metrics.compare(a[i], pivot);
            if (cmp < 0) {
                swap(a, lt++, i++);
            } else if (cmp > 0) {
                swap(a, i, gt--);
            } else {
                i++;
            }
        }
        return new int[] {lt, gt};
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
