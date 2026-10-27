import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Runs the simulation many times without a window to measure whether medics
 * and traps actually change how the outbreak plays out.
 *
 * <p>Each scenario starts from the normal setup in {@link Main} and removes
 * the additions it leaves out. Every run builds its own {@link World} and
 * entities, so runs share no mutable state and can safely run in parallel;
 * the results are combined with {@link ExperimentSummary#collector()}.
 *
 * <p>Run it from IntelliJ, or with {@code java -cp out:src Experiment [runs] [ticks]}.
 */
public final class Experiment {

    private static final int WORLD_WIDTH = 1000;
    private static final int WORLD_HEIGHT = 750;
    private static final int DEFAULT_RUNS = 200;
    private static final int DEFAULT_TICKS = 1500;

    /** Which of the medics and traps are kept in the starting world. */
    enum Scenario {
        NEITHER("No medics, no traps",
                entity -> !(entity instanceof Medic) && !(entity instanceof Trap)),
        TRAPS_ONLY("Traps only", entity -> !(entity instanceof Medic)),
        MEDICS_ONLY("Medics only", entity -> !(entity instanceof Trap)),
        BOTH("Medics and traps", entity -> true);

        private final String label;
        private final Predicate<Entity> keep;

        Scenario(String label, Predicate<Entity> keep) {
            this.label = label;
            this.keep = keep;
        }

        String getLabel() {
            return label;
        }

        /** The normal starting world, minus whatever this scenario leaves out. */
        World createWorld() {
            World world = new World(WORLD_WIDTH, WORLD_HEIGHT);
            Main.populateWorld(world, new Random());
            world.getEntities().stream()
                    .filter(keep.negate())
                    .forEach(Entity::deactivate);
            world.removeInactiveEntities();
            return world;
        }
    }

    private final int runs;
    private final int ticks;

    /**
     * @param runs  how many simulations to run per scenario
     * @param ticks how long each simulation runs for
     * @throws IllegalArgumentException if either value is not positive
     */
    public Experiment(int runs, int ticks) {
        if (runs <= 0 || ticks <= 0) {
            throw new IllegalArgumentException("Runs and ticks must be positive");
        }
        this.runs = runs;
        this.ticks = ticks;
    }

    /** Runs every simulation for a scenario in parallel and summarises them. */
    public ExperimentSummary run(Scenario scenario) {
        return IntStream.range(0, runs)
                .parallel()
                .mapToObj(run -> simulate(scenario))
                .collect(ExperimentSummary.collector());
    }

    /** One complete run. Everything it touches is created here, on this thread. */
    private RunOutcome simulate(Scenario scenario) {
        World world = scenario.createWorld();
        for (int tick = 0; tick < ticks; tick++) {
            world.update();
        }
        return RunOutcome.of(world);
    }

    public static void main(String[] args) {
        // No window is needed, so keep AWT from opening anything.
        System.setProperty("java.awt.headless", "true");

        int runs = DEFAULT_RUNS;
        int ticks = DEFAULT_TICKS;
        try {
            if (args.length > 0) {
                runs = Integer.parseInt(args[0]);
            }
            if (args.length > 1) {
                ticks = Integer.parseInt(args[1]);
            }
        } catch (NumberFormatException exception) {
            System.out.println("Usage: java Experiment [runs] [ticks]");
            return;
        }

        Experiment experiment = new Experiment(runs, ticks);
        System.out.printf("%d runs per scenario, %d ticks each, %d CPU cores%n%n",
                runs, ticks, Runtime.getRuntime().availableProcessors());
        System.out.printf("%-22s %18s %12s %18s%n",
                "Scenario", "Avg survivors", "Avg zombies", "Zombie-free at end");

        for (Scenario scenario : Scenario.values()) {
            long start = System.nanoTime();
            ExperimentSummary summary = experiment.run(scenario);
            double seconds = (System.nanoTime() - start) / 1e9;
            System.out.printf("%-22s %10.2f +/- %.2f %12.2f %17.0f%%   (%.1fs)%n",
                    scenario.getLabel(),
                    summary.getAverageSurvivors(),
                    summary.getSurvivorsStandardError(),
                    summary.getAverageZombies(),
                    summary.getContainedRate() * 100,
                    seconds);
        }
    }
}
