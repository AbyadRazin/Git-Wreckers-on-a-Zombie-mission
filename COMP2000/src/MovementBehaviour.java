import java.awt.geom.Point2D;
import java.util.Iterator;

/**
 * How a {@link Military} unit moves during one tick. It has a single abstract
 * method, so a behaviour is simply a lambda: the two behaviours the soldiers
 * need are built by the factory methods below instead of being separate
 * classes. The {@link MilitaryState} a unit is in decides <em>when</em> each
 * behaviour is used.
 */
@FunctionalInterface
public interface MovementBehaviour {

    /** Moves the given unit one step for the current tick. */
    void move(Military unit, World world);

    /** Charges straight at the unit's current target, if it has one. */
    static MovementBehaviour hunt() {
        return (unit, world) -> unit.getTarget().ifPresent(
                target -> unit.moveTowards(target.getX(), target.getY(), world));
    }

    /** Walks the route forever, moving on to the next waypoint on arrival. */
    static MovementBehaviour patrol(PatrolRoute route) {
        Iterator<Point2D> waypoints = route.cycle();
        double arrivalDistance = 6.0;
        // A lambda can only capture effectively final variables, so the
        // waypoint being approached is held in a one-element array that the
        // lambda is free to modify.
        Point2D[] heading = {waypoints.next()};
        return (unit, world) -> {
            Point2D waypoint = heading[0];
            unit.moveTowards(waypoint.getX(), waypoint.getY(), world);
            double distance = Math.hypot(
                    waypoint.getX() - unit.getX(),
                    waypoint.getY() - unit.getY());
            if (distance <= arrivalDistance) {
                heading[0] = waypoints.next();
            }
        };
    }
}
