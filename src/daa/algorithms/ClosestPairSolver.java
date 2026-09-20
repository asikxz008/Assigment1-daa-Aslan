package daa.algorithms;

import daa.metrics.Metrics;
import daa.model.Point;

import java.util.Arrays;

/**
 * Closest pair of points in the plane, solved by divide and conquer.
 *
 * <p>Design points required by the assignment:</p>
 * <ul>
 *   <li>the points are sorted by x once, before the recursion starts;</li>
 *   <li>each recursive call returns its sub-array sorted by y (merge step),
 *       so no sorting happens inside the recursion — this keeps the algorithm at
 *       Theta(n log n) instead of Theta(n log^2 n);</li>
 *   <li>the strip around the dividing line is scanned in y order and every point is
 *       compared with at most the next 7 points;</li>
 *   <li>a brute-force O(n^2) reference implementation is provided for testing.</li>
 * </ul>
 */
public final class ClosestPairSolver {

    /** Result of a closest-pair query. */
    public static final class Result {
        private final Point first;
        private final Point second;
        private final double distance;

        public Result(Point first, Point second, double distance) {
            this.first = first;
            this.second = second;
            this.distance = distance;
        }

        public Point first() {
            return first;
        }

        public Point second() {
            return second;
        }

        public double distance() {
            return distance;
        }

        @Override
        public String toString() {
            return String.format("%s - %s, distance = %.6f", first, second, distance);
        }
    }

    private static final int BRUTE_FORCE_CUTOFF = 3;

    private final Metrics metrics;

    public ClosestPairSolver() {
        this(new Metrics("ClosestPair"));
    }

    public ClosestPairSolver(Metrics metrics) {
        this.metrics = metrics;
    }

    public Metrics metrics() {
        return metrics;
    }

    /**
     * Finds the closest pair among {@code points} in Theta(n log n).
     * The input array is not modified.
     *
     * @throws IllegalArgumentException if fewer than two points are given
     */
    public Result findClosestPair(Point[] points) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("at least two points are required");
        }
        Point[] byX = points.clone();
        metrics.addAllocation(points.length);
        Arrays.sort(byX, Point.BY_X);

        Point[] buffer = new Point[points.length];
        Point[] strip = new Point[points.length];
        metrics.addAllocation(2L * points.length);

        return closest(byX, buffer, strip, 0, byX.length - 1);
    }

    /**
     * Solves the sub-problem a[lo..hi] and leaves that range sorted by y.
     */
    private Result closest(Point[] a, Point[] buffer, Point[] strip, int lo, int hi) {
        metrics.enterRecursion();
        try {
            int n = hi - lo + 1;
            if (n <= BRUTE_FORCE_CUTOFF) {
                Result best = bruteForce(a, lo, hi);
                insertionSortByY(a, lo, hi);
                return best;
            }

            int mid = lo + (hi - lo) / 2;
            double midX = a[mid].x();

            Result left = closest(a, buffer, strip, lo, mid);
            Result right = closest(a, buffer, strip, mid + 1, hi);
            Result best = left.distance() <= right.distance() ? left : right;

            mergeByY(a, buffer, lo, mid, hi);

            int stripSize = 0;
            for (int i = lo; i <= hi; i++) {
                if (Math.abs(a[i].x() - midX) < best.distance()) {
                    strip[stripSize++] = a[i];
                }
            }

            for (int i = 0; i < stripSize; i++) {
                for (int j = i + 1; j < stripSize && j <= i + 7; j++) {
                    if (strip[j].y() - strip[i].y() >= best.distance()) {
                        break;
                    }
                    double d = strip[i].distanceTo(strip[j]);
                    if (metrics.compare(d, best.distance()) < 0) {
                        best = new Result(strip[i], strip[j], d);
                    }
                }
            }
            return best;
        } finally {
            metrics.exitRecursion();
        }
    }

    private Result bruteForce(Point[] a, int lo, int hi) {
        Result best = new Result(a[lo], a[lo], Double.POSITIVE_INFINITY);
        for (int i = lo; i <= hi; i++) {
            for (int j = i + 1; j <= hi; j++) {
                double d = a[i].distanceTo(a[j]);
                if (metrics.compare(d, best.distance()) < 0) {
                    best = new Result(a[i], a[j], d);
                }
            }
        }
        return best;
    }

    /** Reference O(n^2) solution used to validate the divide-and-conquer result. */
    public static Result bruteForceReference(Point[] points) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("at least two points are required");
        }
        Result best = new Result(points[0], points[1], points[0].distanceTo(points[1]));
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                double d = points[i].distanceTo(points[j]);
                if (d < best.distance()) {
                    best = new Result(points[i], points[j], d);
                }
            }
        }
        return best;
    }

    private void mergeByY(Point[] a, Point[] buffer, int lo, int mid, int hi) {
        System.arraycopy(a, lo, buffer, lo, hi - lo + 1);
        metrics.addMoves(hi - lo + 1);

        int i = lo;
        int j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a[k] = buffer[j++];
            } else if (j > hi) {
                a[k] = buffer[i++];
            } else if (metrics.compare(buffer[j].y(), buffer[i].y()) < 0) {
                a[k] = buffer[j++];
            } else {
                a[k] = buffer[i++];
            }
            metrics.addMoves(1);
        }
    }

    private void insertionSortByY(Point[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            Point key = a[i];
            int j = i - 1;
            while (j >= lo && metrics.compare(a[j].y(), key.y()) > 0) {
                a[j + 1] = a[j];
                metrics.addMoves(1);
                j--;
            }
            a[j + 1] = key;
            metrics.addMoves(1);
        }
    }
}
