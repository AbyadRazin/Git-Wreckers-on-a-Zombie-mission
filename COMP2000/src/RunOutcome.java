/**
 * What was left standing at the end of one headless simulation run.
 *
 * <p>Survivors are ordinary civilians only (exact {@link Human} class), so
 * medics and soldiers don't inflate the count and scenarios with and without
 * medics can be compared fairly.
 */
public final class RunOutcome {

    private final long survivors;
    private final long zombies;

    public RunOutcome(long survivors, long zombies) {
        if (survivors < 0 || zombies < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
        this.survivors = survivors;
        this.zombies = zombies;
    }

    /** Captures the outcome from a world at the end of a run. */
    public static RunOutcome of(World world) {
        long survivors = world.getEntities().stream()
                .filter(Entity::isActive)
                .filter(entity -> entity.getClass() == Human.class)
                .count();
        long zombies = world.getEntities().stream()
                .filter(Entity::isActive)
                .filter(Zombie.class::isInstance)
                .count();
        return new RunOutcome(survivors, zombies);
    }

    public long getSurvivors() {
        return survivors;
    }

    public long getZombies() {
        return zombies;
    }

    /** The outbreak was contained if no zombie is left. */
    public boolean outbreakContained() {
        return zombies == 0;
    }
}
