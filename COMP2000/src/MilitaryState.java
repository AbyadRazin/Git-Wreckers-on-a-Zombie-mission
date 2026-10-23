/**
 * One mode of behaviour for a {@link Military} unit (the State pattern). The
 * unit delegates its whole turn to its current state, and each state decides
 * for itself when to hand over to another with
 * {@code unit.transitionTo(...)}, so {@code Military.update()} contains no
 * if/else chain over modes.
 */
public interface MilitaryState {

    /** Label shown above the unit, e.g. {@code "Patrolling"}. */
    String name();

    /** Runs one tick of this state and performs any transition. */
    void update(Military unit, World world);

    /** Whether the unit should show its firing-range ring in this state. */
    default boolean isEngaging() {
        return false;
    }
}
