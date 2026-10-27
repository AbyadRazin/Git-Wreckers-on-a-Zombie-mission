/**
 * What was left standing at the end of one headless simulation run.
 *
 * <p>Survivors are ordinary civilians only (exact {@link Human} class), so
 * medics and soldiers don't inflate the count and scenarios with and without
 * medics can be compared fairly. Trap catches are counted during the run by a
 * {@link TrapReport}, because worn-out traps leave the world.
 */
public final class RunOutcome {

    private final long survivors;
    private final long zombies;
    private final long trapCatches;

    public RunOutcome(long survivors, long zombies, long trapCatches) {
        if (survivors < 0 || zombies < 0 || trapCatches < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
        this.survivors = survivors;
        this.zombies = zombies;
        this.trapCatches = trapCatches;
    }

    /**
     * Captures the outcome from a world at the end of a run.
     *
     * @param trapCatches how many zombies traps caught during the run
     */
    public static RunOutcome of(World world, long trapCatches) {
        long survivors = world.getEntities().stream()
                .filter(Entity::isActive)
                .filter(entity -> entity.getClass() == Human.class)
                .count();
        long zombies = world.getEntities().stream()
                .filter(Entity::isActive)
                .filter(Zombie.class::isInstance)
                .count();
        return new RunOutcome(survivors, zombies, trapCatches);
    }

    public long getSurvivors() {
        return survivors;
    }

    public long getZombies() {
        return zombies;
    }

    public long getTrapCatches() {
        return trapCatches;
    }

    /** The outbreak was contained if no zombie is left. */
    public boolean outbreakContained() {
        return zombies == 0;
    }
}
