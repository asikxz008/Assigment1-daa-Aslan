package daa.metrics;

/**
 * Collects algorithmic counters for a single run of an algorithm.
 *
 * <p>Tracked values: comparisons, swaps, element moves (array writes),
 * allocations (number of allocated array slots), recursive calls, current and
 * maximum recursion depth, plus the wall-clock time measured with
 * {@link System#nanoTime()}.</p>
 */
public final class Metrics {

    private final String algorithm;

    private long comparisons;
    private long swaps;
    private long moves;
    private long allocations;
    private long recursiveCalls;

    private int currentDepth;
    private int maxDepth;

    private long startNanos;
    private long elapsedNanos;

    public Metrics(String algorithm) {
        this.algorithm = algorithm;
    }

    public String algorithm() {
        return algorithm;
    }

    /** Resets every counter so the instance can be reused for the next trial. */
    public void reset() {
        comparisons = 0;
        swaps = 0;
        moves = 0;
        allocations = 0;
        recursiveCalls = 0;
        currentDepth = 0;
        maxDepth = 0;
        startNanos = 0;
        elapsedNanos = 0;
    }

    /** Must be called on entry of every recursive method. */
    public void enterRecursion() {
        recursiveCalls++;
        currentDepth++;
        if (currentDepth > maxDepth) {
            maxDepth = currentDepth;
        }
    }

    /** Must be called on exit of every recursive method (use a finally block). */
    public void exitRecursion() {
        currentDepth--;
    }

    /** Counts one comparison and returns the standard three-way result. */
    public int compare(int left, int right) {
        comparisons++;
        return Integer.compare(left, right);
    }

    /** Counts one comparison of two doubles (used by Closest Pair). */
    public int compare(double left, double right) {
        comparisons++;
        return Double.compare(left, right);
    }

    public void addComparisons(long count) {
        comparisons += count;
    }

    public void addSwap() {
        swaps++;
        moves += 2;
    }

    public void addMoves(long count) {
        moves += count;
    }

    public void addAllocation(long slots) {
        allocations += slots;
    }

    public void startTimer() {
        startNanos = System.nanoTime();
    }

    public void stopTimer() {
        elapsedNanos = System.nanoTime() - startNanos;
    }

    public long comparisons() {
        return comparisons;
    }

    public long swaps() {
        return swaps;
    }

    public long moves() {
        return moves;
    }

    public long allocations() {
        return allocations;
    }

    public long recursiveCalls() {
        return recursiveCalls;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public int currentDepth() {
        return currentDepth;
    }

    public long elapsedNanos() {
        return elapsedNanos;
    }

    public double elapsedMillis() {
        return elapsedNanos / 1_000_000.0;
    }

    @Override
    public String toString() {
        return String.format(
                "%s: time=%.3f ms, maxDepth=%d, comparisons=%d, swaps=%d, moves=%d, "
                        + "allocations=%d, recursiveCalls=%d",
                algorithm, elapsedMillis(), maxDepth, comparisons, swaps, moves,
                allocations, recursiveCalls);
    }
}
