import java.util.Objects;

/**
 * Lets the user place a limited number of extra traps by hand.
 *
 * <p>Placing works in two steps: {@link #toggleArmed()} switches into
 * placing mode (from the on-screen button), then the next
 * {@link #placeAt(double, double)} (a click on the map) drops a trap there
 * and switches placing mode off again. Once the supply runs out, placing
 * mode can no longer be switched on until {@link #reset()}.
 */
public class TrapPlacer {

    private static final int USES_PER_TRAP = 2;

    private final World world;
    private final int maxTraps;
    private int trapsLeft;
    private boolean armed;
    private Runnable onChange = () -> { };

    /**
     * @param world    the world placed traps are added to
     * @param maxTraps how many traps the user may place per run
     * @throws IllegalArgumentException if {@code maxTraps} is not positive
     * @throws NullPointerException if {@code world} is null
     */
    public TrapPlacer(World world, int maxTraps) {
        if (maxTraps <= 0) {
            throw new IllegalArgumentException(
                    "The trap supply must be positive"
            );
        }

        this.world = Objects.requireNonNull(world, "World cannot be null");
        this.maxTraps = maxTraps;
        this.trapsLeft = maxTraps;
    }

    /** Sets what to run whenever the supply or placing mode changes. */
    public void setOnChange(Runnable listener) {
        onChange = Objects.requireNonNull(listener, "Listener cannot be null");
    }

    public int getTrapsLeft() {
        return trapsLeft;
    }

    /** Whether the next click on the map will place a trap. */
    public boolean isArmed() {
        return armed;
    }

    /** Switches placing mode on or off; stays off when no traps are left. */
    public void toggleArmed() {
        armed = !armed && trapsLeft > 0;
        onChange.run();
    }

    /**
     * Places a trap at the given point if placing mode is on.
     *
     * @return {@code true} if a trap was placed
     */
    public boolean placeAt(double x, double y) {
        if (!armed) {
            return false;
        }

        world.addEntity(new Trap(x, y, USES_PER_TRAP));
        trapsLeft--;
        armed = false;
        onChange.run();
        return true;
    }

    /** Refills the supply for a fresh run. */
    public void reset() {
        trapsLeft = maxTraps;
        armed = false;
        onChange.run();
    }
}
