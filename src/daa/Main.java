package daa;

import daa.algorithms.ClosestPairSolver;
import daa.algorithms.DeterministicSelector;
import daa.algorithms.MergeSorter;
import daa.algorithms.QuickSorter;
import daa.experiment.Experiment;
import daa.experiment.InputGenerator;
import daa.experiment.InputGenerator.ArrayType;
import daa.experiment.InputGenerator.PointSetType;
import daa.metrics.Metrics;
import daa.model.Point;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Entry point.
 *
 * <pre>
 *   java -cp target/classes daa.Main              demo run of all four algorithms
 *   java -cp target/classes daa.Main experiment   full benchmark suite -> results/results.csv
 * </pre>
 */
public final class Main {

    private static final int DEMO_SIZE = 20;
    private static final int DEMO_POINTS = 12;
    private static final long DEMO_SEED = 42L;

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equalsIgnoreCase("experiment")) {
            Path output = Path.of(args.length > 1 ? args[1] : "results/results.csv");
            new Experiment().run(output);
            return;
        }
        runDemo();
    }

    private static void runDemo() {
        InputGenerator generator = new InputGenerator(DEMO_SEED);
        int[] data = boundedRandom(generator, DEMO_SIZE);

        banner("DIVIDE AND CONQUER - DEMO RUN");
        System.out.println("input array (n = " + data.length + "):");
        System.out.println("  " + Arrays.toString(data));

        demoMergeSort(data);
        demoQuickSort(data);
        demoSelect(data);
        demoClosestPair(generator);
        demoEdgeCases();

        banner("DEMO FINISHED");
        System.out.println("Run  java -cp target/classes daa.Main experiment  "
                + "to reproduce results/results.csv");
    }

    private static void demoMergeSort(int[] data) {
        section("1. MergeSort  (reusable buffer, insertion-sort cutoff = "
                + MergeSorter.DEFAULT_CUTOFF + ")");
        Metrics metrics = new Metrics("MergeSort");
        int[] a = data.clone();
        metrics.startTimer();
        new MergeSorter(metrics).sort(a);
        metrics.stopTimer();
        System.out.println("  sorted : " + Arrays.toString(a));
        System.out.println("  matches Arrays.sort : " + matchesReference(data, a));
        System.out.println("  " + metrics);
    }

    private static void demoQuickSort(int[] data) {
        section("2. QuickSort  (randomized pivot, smaller-partition recursion)");
        Metrics metrics = new Metrics("QuickSort");
        int[] a = data.clone();
        metrics.startTimer();
        new QuickSorter(metrics, DEMO_SEED).sort(a);
        metrics.stopTimer();
        System.out.println("  sorted : " + Arrays.toString(a));
        System.out.println("  matches Arrays.sort : " + matchesReference(data, a));
        System.out.println("  " + metrics);
    }

    private static void demoSelect(int[] data) {
        section("3. Deterministic Select  (median of medians, groups of 5)");
        int[] reference = data.clone();
        Arrays.sort(reference);
        Metrics metrics = new Metrics("DeterministicSelect");
        for (int k : new int[] {0, data.length / 2, data.length - 1}) {
            int[] a = data.clone();
            metrics.reset();
            metrics.startTimer();
            int value = new DeterministicSelector(metrics).select(a, k);
            metrics.stopTimer();
            System.out.printf("  k = %-3d -> %-12d expected %-12d %s%n",
                    k, value, reference[k], value == reference[k] ? "OK" : "FAILED");
        }
        System.out.println("  " + metrics);
    }

    private static void demoClosestPair(InputGenerator generator) {
        section("4. Closest Pair of Points  (divide and conquer vs brute force)");
        Point[] points = generator.points(PointSetType.UNIFORM, DEMO_POINTS);
        Metrics metrics = new Metrics("ClosestPair");
        metrics.startTimer();
        ClosestPairSolver.Result fast = new ClosestPairSolver(metrics).findClosestPair(points);
        metrics.stopTimer();
        ClosestPairSolver.Result slow = ClosestPairSolver.bruteForceReference(points);

        System.out.println("  points        : " + DEMO_POINTS);
        System.out.println("  divide&conquer: " + fast);
        System.out.println("  brute force   : " + slow);
        System.out.println("  distances equal: "
                + (Math.abs(fast.distance() - slow.distance()) < 1e-9));
        System.out.println("  " + metrics);
    }

    private static void demoEdgeCases() {
        section("5. Edge cases");
        int[] empty = {};
        int[] single = {7};
        int[] duplicates = {5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5};
        int[] twoElements = {2, 1};

        new MergeSorter().sort(empty);
        new QuickSorter().sort(empty);
        System.out.println("  empty array            : " + Arrays.toString(empty) + " OK");

        new MergeSorter().sort(single);
        new QuickSorter().sort(single);
        System.out.println("  single element         : " + Arrays.toString(single) + " OK");

        new MergeSorter().sort(twoElements);
        System.out.println("  two elements           : " + Arrays.toString(twoElements) + " OK");

        int[] copy = duplicates.clone();
        new QuickSorter().sort(copy);
        System.out.println("  all values equal (n=20): sorted = "
                + isSorted(copy) + ", select(k=10) = "
                + new DeterministicSelector().select(duplicates.clone(), 10));

        try {
            new DeterministicSelector().select(new int[] {1, 2, 3}, 5);
            System.out.println("  out-of-range k         : FAILED (no exception)");
        } catch (IndexOutOfBoundsException expected) {
            System.out.println("  out-of-range k         : IndexOutOfBoundsException OK");
        }

        try {
            new ClosestPairSolver().findClosestPair(new Point[] {new Point(0, 0)});
            System.out.println("  closest pair with n=1  : FAILED (no exception)");
        } catch (IllegalArgumentException expected) {
            System.out.println("  closest pair with n=1  : IllegalArgumentException OK");
        }
    }

    private static int[] boundedRandom(InputGenerator generator, int n) {
        int[] raw = generator.array(ArrayType.RANDOM, n);
        int[] bounded = new int[n];
        for (int i = 0; i < n; i++) {
            bounded[i] = Math.floorMod(raw[i], 100);
        }
        return bounded;
    }

    private static boolean matchesReference(int[] original, int[] sorted) {
        int[] reference = original.clone();
        Arrays.sort(reference);
        return Arrays.equals(reference, sorted);
    }

    private static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=".repeat(78));
        System.out.println("  " + title);
        System.out.println("=".repeat(78));
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("-".repeat(78));
        System.out.println(title);
        System.out.println("-".repeat(78));
    }
}
