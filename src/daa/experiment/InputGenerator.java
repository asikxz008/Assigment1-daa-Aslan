package daa.experiment;

import daa.model.Point;

import java.util.Arrays;
import java.util.Random;

/** Deterministic generators for the experimental input data. */
public final class InputGenerator {

    /** Supported array shapes. */
    public enum ArrayType {
        RANDOM,
        SORTED,
        REVERSE_SORTED,
        DUPLICATE_HEAVY
    }

    /** Supported point-set shapes. */
    public enum PointSetType {
        UNIFORM,
        CLUSTERED,
        ON_LINE
    }

    private final Random random;

    public InputGenerator(long seed) {
        this.random = new Random(seed);
    }

    public int[] array(ArrayType type, int n) {
        switch (type) {
            case RANDOM:
                return randomArray(n);
            case SORTED:
                int[] sorted = randomArray(n);
                Arrays.sort(sorted);
                return sorted;
            case REVERSE_SORTED:
                int[] reverse = randomArray(n);
                Arrays.sort(reverse);
                reverseInPlace(reverse);
                return reverse;
            case DUPLICATE_HEAVY:
                return duplicateHeavyArray(n);
            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
    }

    public Point[] points(PointSetType type, int n) {
        Point[] points = new Point[n];
        switch (type) {
            case UNIFORM:
                for (int i = 0; i < n; i++) {
                    points[i] = new Point(random.nextDouble() * 10_000.0,
                            random.nextDouble() * 10_000.0);
                }
                return points;
            case CLUSTERED:
                int clusters = Math.max(1, n / 100);
                for (int i = 0; i < n; i++) {
                    int cluster = random.nextInt(clusters);
                    double cx = (cluster * 9973 % 10_000);
                    double cy = (cluster * 7919 % 10_000);
                    points[i] = new Point(cx + random.nextGaussian() * 5.0,
                            cy + random.nextGaussian() * 5.0);
                }
                return points;
            case ON_LINE:
                for (int i = 0; i < n; i++) {
                    double t = random.nextDouble() * 10_000.0;
                    points[i] = new Point(t, 0.5 * t + 3.0);
                }
                return points;
            default:
                throw new IllegalArgumentException("unknown type: " + type);
        }
    }

    private int[] randomArray(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt();
        }
        return a;
    }

    /** About ten distinct values, so roughly n/10 copies of each. */
    private int[] duplicateHeavyArray(int n) {
        int[] a = new int[n];
        int distinct = Math.max(2, Math.min(10, n));
        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt(distinct);
        }
        return a;
    }

    private void reverseInPlace(int[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }
}
