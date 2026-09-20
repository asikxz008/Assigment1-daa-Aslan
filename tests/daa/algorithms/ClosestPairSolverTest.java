package daa.algorithms;

import daa.metrics.Metrics;
import daa.model.Point;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClosestPairSolverTest {

    private static final long SEED = 777L;
    private static final double EPS = 1e-9;

    @Test
    @DisplayName("50 random point sets (n <= 500) match the O(n^2) brute force")
    void randomSetsMatchBruteForce() {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 50; trial++) {
            int n = 2 + random.nextInt(499);
            Point[] points = randomPoints(random, n, 1000.0);

            double fast = new ClosestPairSolver().findClosestPair(points).distance();
            double slow = ClosestPairSolver.bruteForceReference(points).distance();

            assertEquals(slow, fast, EPS, "trial " + trial + " with n = " + n);
        }
    }

    @Test
    @DisplayName("large set of 2000 points matches the brute force")
    void largeSetMatchesBruteForce() {
        Random random = new Random(SEED);
        Point[] points = randomPoints(random, 2000, 10_000.0);

        double fast = new ClosestPairSolver().findClosestPair(points).distance();
        double slow = ClosestPairSolver.bruteForceReference(points).distance();

        assertEquals(slow, fast, EPS);
    }

    @Test
    @DisplayName("the returned pair really is at the reported distance")
    void returnedPairIsConsistent() {
        Random random = new Random(SEED);
        Point[] points = randomPoints(random, 500, 500.0);

        ClosestPairSolver.Result result = new ClosestPairSolver().findClosestPair(points);
        assertEquals(result.first().distanceTo(result.second()), result.distance(), EPS);
    }

    @Test
    @DisplayName("duplicate points give distance zero")
    void duplicatePoints() {
        Point[] points = {
                new Point(1, 1), new Point(5, 5), new Point(9, 2),
                new Point(5, 5), new Point(-3, 7), new Point(0, 0)
        };
        assertEquals(0.0, new ClosestPairSolver().findClosestPair(points).distance(), EPS);
    }

    @Test
    @DisplayName("collinear points and clustered points")
    void degenerateGeometries() {
        Point[] line = new Point[500];
        for (int i = 0; i < line.length; i++) {
            line[i] = new Point(i, 2.0 * i);
        }
        double expectedLine = ClosestPairSolver.bruteForceReference(line).distance();
        assertEquals(expectedLine,
                new ClosestPairSolver().findClosestPair(line).distance(), EPS);

        Random random = new Random(SEED);
        Point[] clustered = new Point[600];
        for (int i = 0; i < clustered.length; i++) {
            double cx = (i % 6) * 1000.0;
            clustered[i] = new Point(cx + random.nextGaussian(), random.nextGaussian());
        }
        assertEquals(ClosestPairSolver.bruteForceReference(clustered).distance(),
                new ClosestPairSolver().findClosestPair(clustered).distance(), EPS);
    }

    @Test
    @DisplayName("exactly two and exactly three points")
    void smallestValidInputs() {
        Point[] two = {new Point(0, 0), new Point(3, 4)};
        assertEquals(5.0, new ClosestPairSolver().findClosestPair(two).distance(), EPS);

        Point[] three = {new Point(0, 0), new Point(3, 4), new Point(0, 1)};
        assertEquals(1.0, new ClosestPairSolver().findClosestPair(three).distance(), EPS);
    }

    @Test
    @DisplayName("fewer than two points is rejected")
    void invalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClosestPairSolver().findClosestPair(new Point[] {new Point(0, 0)}));
        assertThrows(IllegalArgumentException.class,
                () -> new ClosestPairSolver().findClosestPair(new Point[] {}));
        assertThrows(IllegalArgumentException.class,
                () -> new ClosestPairSolver().findClosestPair(null));
    }

    @Test
    @DisplayName("the input array is not modified by the solver")
    void inputArrayIsNotModified() {
        Random random = new Random(SEED);
        Point[] points = randomPoints(random, 200, 100.0);
        Point[] copy = points.clone();

        new ClosestPairSolver().findClosestPair(points);

        for (int i = 0; i < points.length; i++) {
            assertEquals(copy[i], points[i], "element " + i + " was moved");
        }
    }

    @Test
    @DisplayName("recursion depth is logarithmic in n")
    void recursionDepthIsLogarithmic() {
        Random random = new Random(SEED);
        int n = 50_000;
        Point[] points = randomPoints(random, n, 100_000.0);

        Metrics metrics = new Metrics("ClosestPair");
        new ClosestPairSolver(metrics).findClosestPair(points);

        int bound = (int) (Math.log(n) / Math.log(2)) + 4;
        assertTrue(metrics.maxDepth() <= bound,
                "depth " + metrics.maxDepth() + " exceeded bound " + bound);
    }

    private static Point[] randomPoints(Random random, int n, double range) {
        Point[] points = new Point[n];
        for (int i = 0; i < n; i++) {
            points[i] = new Point(random.nextDouble() * range, random.nextDouble() * range);
        }
        return points;
    }
}
