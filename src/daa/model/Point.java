package daa.model;

import java.util.Comparator;
import java.util.Objects;

/** An immutable point in the Euclidean plane. */
public final class Point {

    public static final Comparator<Point> BY_X =
            Comparator.comparingDouble((Point p) -> p.x).thenComparingDouble(p -> p.y);

    public static final Comparator<Point> BY_Y =
            Comparator.comparingDouble((Point p) -> p.y).thenComparingDouble(p -> p.x);

    private final double x;
    private final double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** Euclidean distance to {@code other}. */
    public double distanceTo(Point other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Squared distance, useful when only ordering matters. */
    public double squaredDistanceTo(Point other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return dx * dx + dy * dy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Point)) {
            return false;
        }
        Point other = (Point) o;
        return Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("(%.4f, %.4f)", x, y);
    }
}
