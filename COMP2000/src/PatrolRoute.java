import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable loop of waypoints. It is the aggregate of the Iterator
 * pattern: callers ask it for an {@link Iterator} via {@link #cycle()} and
 * never see the list or the wrap-around index behind it.
 */
public final class PatrolRoute {

    private final List<Point2D> waypoints;

    /** @throws IllegalArgumentException if the route is null or empty */
    public PatrolRoute(List<Point2D> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException(
                    "Patrol route must contain at least one waypoint");
        }
        // Point2D.Double is mutable, so copy each point as well as the list.
        this.waypoints = Collections.unmodifiableList(waypoints.stream()
                .map(point -> new Point2D.Double(point.getX(), point.getY()))
                .collect(Collectors.toList()));
    }

    /** A diamond (left, up, right, down) around the given centre. */
    public static PatrolRoute diamond(double x, double y) {
        return new PatrolRoute(Stream.of(
                        new double[]{-160, 0},
                        new double[]{0, -110},
                        new double[]{160, 0},
                        new double[]{0, 110})
                .map(offset -> (Point2D) new Point2D.Double(x + offset[0], y + offset[1]))
                .collect(Collectors.toList()));
    }

    public int size() {
        return waypoints.size();
    }

    /**
     * Returns a fresh iterator that visits the waypoints in order and starts
     * again from the first once it reaches the last. {@code hasNext()} is
     * always true, so never use it in a for-each loop.
     */
    public Iterator<Point2D> cycle() {
        return new Iterator<Point2D>() {
            private int nextIndex = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Point2D next() {
                Point2D waypoint = waypoints.get(nextIndex);
                nextIndex = (nextIndex + 1) % waypoints.size();
                return waypoint;
            }
        };
    }
}
