package daa.experiment;

import daa.algorithms.ClosestPairSolver;
import daa.algorithms.DeterministicSelector;
import daa.algorithms.MergeSorter;
import daa.algorithms.QuickSorter;
import daa.experiment.InputGenerator.ArrayType;
import daa.experiment.InputGenerator.PointSetType;
import daa.metrics.Metrics;
import daa.model.Point;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Benchmark driver.
 *
 * <p>Every configuration (algorithm, input type, n) is executed {@value #TRIALS} times
 * after {@value #WARMUP_ROUNDS} warm-up rounds that give the JIT compiler a chance to
 * reach steady state. Every single trial is written to the CSV file, so the raw data can
 * be re-aggregated later; the console prints the median of the trials.</p>
 */
public final class Experiment {

    public static final int[] SORT_SIZES =
            {100, 500, 1_000, 5_000, 10_000, 50_000, 100_000, 200_000};

    public static final int[] POINT_SIZES =
            {100, 500, 1_000, 2_000, 5_000, 10_000, 50_000, 100_000};

    public static final int TRIALS = 7;
    public static final int WARMUP_ROUNDS = 10;

    /**
     * Number of untimed repetitions executed before the timed trials of a configuration.
     * Small inputs need many more repetitions than large ones before the JIT compiler
     * promotes the hot methods to fully optimised machine code.
     */
    public static int warmupRepeats(int n) {
        return Math.max(3, Math.min(2_000, 400_000 / Math.max(1, n)));
    }
    public static final long SEED = 20_260_920L;

    private static final String CSV_HEADER =
            "algorithm,input_type,n,trial,time_ns,time_ms,max_depth,comparisons,"
                    + "swaps,moves,allocations,recursive_calls";

    private final List<String> rows = new ArrayList<>();
    private final InputGenerator generator = new InputGenerator(SEED);

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args.length > 0 ? args[0] : "results/results.csv");
        new Experiment().run(output);
    }

    public void run(Path csvPath) throws IOException {
        System.out.println("=".repeat(78));
        System.out.println("  DIVIDE AND CONQUER - EXPERIMENTAL EVALUATION");
        System.out.printf("  trials per configuration: %d, global warm-up rounds: %d, "
                + "per-configuration warm-up: %d..%d, seed: %d%n",
                TRIALS, WARMUP_ROUNDS, warmupRepeats(Integer.MAX_VALUE),
                warmupRepeats(1), SEED);
        System.out.println("=".repeat(78));

        rows.add(CSV_HEADER);

        warmUp();
        benchmarkSorting();
        benchmarkSelection();
        benchmarkClosestPair();

        writeCsv(csvPath);
        System.out.printf("%nRaw results written to %s (%d data rows)%n",
                csvPath.toAbsolutePath(), rows.size() - 1);
    }

    /** Runs every algorithm a few times so the JIT compiles the hot paths. */
    private void warmUp() {
        System.out.println("\n[warm-up]");
        int[] a = generator.array(ArrayType.RANDOM, 100_000);
        Point[] p = generator.points(PointSetType.UNIFORM, 50_000);
        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            new MergeSorter(new Metrics("warmup")).sort(a.clone());
            new QuickSorter(new Metrics("warmup"), SEED).sort(a.clone());
            new DeterministicSelector(new Metrics("warmup")).select(a.clone(), a.length / 2);
            new ClosestPairSolver(new Metrics("warmup")).findClosestPair(p);
        }
        System.out.println("  done");
    }

    private void benchmarkSorting() {
        System.out.println("\n[1/3] Sorting: MergeSort, QuickSort, Arrays.sort (baseline)");
        printSortHeader();

        for (ArrayType type : ArrayType.values()) {
            for (int n : SORT_SIZES) {
                int[] base = generator.array(type, n);

                measureSort("MergeSort", type, n, base, (a, m) -> new MergeSorter(m).sort(a));
                measureSort("QuickSort", type, n, base,
                        (a, m) -> new QuickSorter(m, SEED).sort(a));
                measureSort("Arrays.sort", type, n, base, (a, m) -> Arrays.sort(a));
            }
        }
    }

    private void benchmarkSelection() {
        System.out.println("\n[2/3] Selection: DeterministicSelect (median of medians) "
                + "vs Arrays.sort + index");
        printSortHeader();

        for (ArrayType type : ArrayType.values()) {
            for (int n : SORT_SIZES) {
                int[] base = generator.array(type, n);
                int k = n / 2;

                int[] reference = base.clone();
                Arrays.sort(reference);
                int expected = reference[k];

                Metrics warmup = new Metrics("DeterministicSelect");
                for (int i = 0; i < warmupRepeats(n); i++) {
                    warmup.reset();
                    new DeterministicSelector(warmup).select(base.clone(), k);
                }

                double[] times = new double[TRIALS];
                Metrics metrics = new Metrics("DeterministicSelect");
                for (int trial = 0; trial < TRIALS; trial++) {
                    int[] a = base.clone();
                    metrics.reset();
                    metrics.startTimer();
                    int actual = new DeterministicSelector(metrics).select(a, k);
                    metrics.stopTimer();
                    if (actual != expected) {
                        throw new IllegalStateException("select returned a wrong value for n="
                                + n + ", type=" + type);
                    }
                    times[trial] = metrics.elapsedMillis();
                    addRow("DeterministicSelect", type.name(), n, trial, metrics);
                }
                printSortRow("DeterministicSelect", type.name(), n, median(times), metrics);

                Metrics baseline = new Metrics("Sort+Index");
                for (int trial = 0; trial < TRIALS; trial++) {
                    int[] a = base.clone();
                    baseline.reset();
                    baseline.startTimer();
                    Arrays.sort(a);
                    int ignored = a[k];
                    baseline.stopTimer();
                    times[trial] = baseline.elapsedMillis();
                    addRow("Sort+Index", type.name(), n, trial, baseline);
                }
                printSortRow("Sort+Index", type.name(), n, median(times), baseline);
            }
        }
    }

    private void benchmarkClosestPair() {
        System.out.println("\n[3/3] Closest pair: divide and conquer vs brute force");
        printSortHeader();

        for (PointSetType type : PointSetType.values()) {
            for (int n : POINT_SIZES) {
                Point[] points = generator.points(type, n);

                Metrics warmup = new Metrics("ClosestPair");
                for (int i = 0; i < Math.min(50, warmupRepeats(n)); i++) {
                    warmup.reset();
                    new ClosestPairSolver(warmup).findClosestPair(points);
                }

                double[] times = new double[TRIALS];
                Metrics metrics = new Metrics("ClosestPair");
                double distance = 0.0;
                for (int trial = 0; trial < TRIALS; trial++) {
                    metrics.reset();
                    metrics.startTimer();
                    distance = new ClosestPairSolver(metrics).findClosestPair(points).distance();
                    metrics.stopTimer();
                    times[trial] = metrics.elapsedMillis();
                    addRow("ClosestPair", type.name(), n, trial, metrics);
                }
                printSortRow("ClosestPair", type.name(), n, median(times), metrics);

                if (n <= 2_000) {
                    Metrics brute = new Metrics("ClosestPairBruteForce");
                    for (int i = 0; i < Math.min(50, warmupRepeats(n)); i++) {
                        ClosestPairSolver.bruteForceReference(points);
                    }
                    double reference = 0.0;
                    for (int trial = 0; trial < TRIALS; trial++) {
                        brute.reset();
                        brute.startTimer();
                        reference = ClosestPairSolver.bruteForceReference(points).distance();
                        brute.stopTimer();
                        times[trial] = brute.elapsedMillis();
                        addRow("ClosestPairBruteForce", type.name(), n, trial, brute);
                    }
                    if (Math.abs(reference - distance) > 1e-9) {
                        throw new IllegalStateException("closest pair mismatch for n=" + n
                                + ", type=" + type + ": " + distance + " vs " + reference);
                    }
                    printSortRow("ClosestPairBruteForce", type.name(), n, median(times), brute);
                }
            }
        }
    }

    private interface SortRoutine {
        void sort(int[] a, Metrics metrics);
    }

    private void measureSort(String name, ArrayType type, int n, int[] base,
                             SortRoutine routine) {
        int[] expected = base.clone();
        Arrays.sort(expected);

        Metrics warmup = new Metrics(name);
        for (int i = 0; i < warmupRepeats(n); i++) {
            warmup.reset();
            routine.sort(base.clone(), warmup);
        }

        double[] times = new double[TRIALS];
        Metrics metrics = new Metrics(name);
        for (int trial = 0; trial < TRIALS; trial++) {
            int[] a = base.clone();
            metrics.reset();
            metrics.startTimer();
            routine.sort(a, metrics);
            metrics.stopTimer();
            if (!Arrays.equals(a, expected)) {
                throw new IllegalStateException(name + " produced a wrong order for n=" + n
                        + ", type=" + type);
            }
            times[trial] = metrics.elapsedMillis();
            addRow(name, type.name(), n, trial, metrics);
        }
        printSortRow(name, type.name(), n, median(times), metrics);
    }

    private void addRow(String algorithm, String inputType, int n, int trial, Metrics m) {
        rows.add(String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%.6f,%d,%d,%d,%d,%d,%d",
                algorithm, inputType, n, trial, m.elapsedNanos(), m.elapsedMillis(),
                m.maxDepth(), m.comparisons(), m.swaps(), m.moves(), m.allocations(),
                m.recursiveCalls()));
    }

    private void printSortHeader() {
        System.out.printf("%n%-24s %-16s %10s %12s %7s %14s%n",
                "algorithm", "input", "n", "median ms", "depth", "comparisons");
        System.out.println("-".repeat(88));
    }

    private void printSortRow(String algorithm, String input, int n, double medianMs, Metrics m) {
        System.out.printf(Locale.ROOT, "%-24s %-16s %10d %12.4f %7d %14d%n",
                algorithm, input, n, medianMs, m.maxDepth(), m.comparisons());
    }

    private static double median(double[] values) {
        double[] copy = values.clone();
        Arrays.sort(copy);
        int mid = copy.length / 2;
        return copy.length % 2 == 1 ? copy[mid] : (copy[mid - 1] + copy[mid]) / 2.0;
    }

    private void writeCsv(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            rows.forEach(writer::println);
        }
    }
}
