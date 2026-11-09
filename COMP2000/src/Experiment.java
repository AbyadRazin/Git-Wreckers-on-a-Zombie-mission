import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
 * <p>Runs are <em>paired</em>: run number {@code i} of every scenario starts
 * from the same layout (same people, zombies, soldiers and traps in the same
 * places), so scenarios are compared on equal starting positions rather than
 * on whichever layouts each one happened to get. Movement after the start is
 * still random, so individual runs are not exactly repeatable.
 *
 * <p>Run it from IntelliJ, or with
 * {@code java -cp out:src Experiment [runs] [ticks] [layoutSeed]}.
 */
public final class Experiment {

    private static final int WORLD_WIDTH = 1000;
    private static final int WORLD_HEIGHT = 750;
    private static final int DEFAULT_RUNS = 200;
    private static final int DEFAULT_TICKS = 1500;
    private static final long DEFAULT_LAYOUT_SEED = 20261109L;

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

        /** Whether this scenario includes any traps. */
        boolean hasTraps() {
            return this == TRAPS_ONLY || this == BOTH;
        }

        /**
         * The normal starting world laid out from the given seed, minus
         * whatever this scenario leaves out. The same seed gives every
         * scenario the same starting positions.
         */
        World createWorld(long layoutSeed) {
            World world = new World(WORLD_WIDTH, WORLD_HEIGHT);
            Main.populateWorld(world, new Random(layoutSeed));
            world.getEntities().stream()
                    .filter(keep.negate())
                    .forEach(Entity::deactivate);
            world.removeInactiveEntities();
            return world;
        }
    }

    private final int runs;
    private final int ticks;
    private final long layoutSeed;

    /**
     * @param runs  how many simulations to run per scenario
     * @param ticks how long each simulation runs for
     * @throws IllegalArgumentException if either value is not positive
     */
    public Experiment(int runs, int ticks) {
        this(runs, ticks, DEFAULT_LAYOUT_SEED);
    }

    /**
     * @param runs       how many simulations to run per scenario
     * @param ticks      how long each simulation runs for
     * @param layoutSeed decides the starting layouts; the same seed gives the same layouts
     * @throws IllegalArgumentException if runs or ticks is not positive
     */
    public Experiment(int runs, int ticks, long layoutSeed) {
        if (runs <= 0 || ticks <= 0) {
            throw new IllegalArgumentException("Runs and ticks must be positive");
        }
        this.runs = runs;
        this.ticks = ticks;
        this.layoutSeed = layoutSeed;
    }

    /** Runs every simulation for a scenario in parallel and summarises them. */
    public ExperimentSummary run(Scenario scenario) {
        return runAll(scenario).stream().collect(ExperimentSummary.collector());
    }

    /** Runs every simulation for a scenario in parallel and keeps each outcome. */
    public List<RunOutcome> runAll(Scenario scenario) {
        return IntStream.range(0, runs)
                .parallel()
                .mapToObj(run -> simulate(scenario, layoutSeedFor(run)))
                .collect(Collectors.toList());
    }

    /** The layout seed for run number {@code run}, the same in every scenario. */
    long layoutSeedFor(int run) {
        return layoutSeed * 1_000_003L + run;
    }

    /**
     * Splits runs into those where a trap caught at least one zombie (true)
     * and those where none did (false), with survivor statistics for each.
     */
    static Map<Boolean, LongSummaryStatistics> survivorsByTrapActivity(List<RunOutcome> outcomes) {
        return outcomes.stream()
                .collect(Collectors.partitioningBy(
                        outcome -> outcome.getTrapCatches() > 0,
                        Collectors.summarizingLong(RunOutcome::getSurvivors)));
    }

    /** One complete run. Everything it touches is created here, on this thread. */
    private RunOutcome simulate(Scenario scenario, long seed) {
        World world = scenario.createWorld(seed);
        // Each run counts its own catches, so no report is shared between threads.
        TrapReport trapReport = new TrapReport();
        Trap.listenToAll(world, trapReport);
        for (int tick = 0; tick < ticks; tick++) {
            world.update();
        }
        return RunOutcome.of(world, trapReport.getCatches());
    }

    public static void main(String[] args) {
        // No window is needed, so keep AWT from opening anything.
        System.setProperty("java.awt.headless", "true");

        int runs = DEFAULT_RUNS;
        int ticks = DEFAULT_TICKS;
        long seed = DEFAULT_LAYOUT_SEED;
        try {
            if (args.length > 0) {
                runs = Integer.parseInt(args[0]);
            }
            if (args.length > 1) {
                ticks = Integer.parseInt(args[1]);
            }
            if (args.length > 2) {
                seed = Long.parseLong(args[2]);
            }
        } catch (NumberFormatException exception) {
            System.out.println("Usage: java Experiment [runs] [ticks] [layoutSeed]");
            return;
        }

        Experiment experiment = new Experiment(runs, ticks, seed);
        System.out.printf("%d paired runs per scenario, %d ticks each, layout seed %d, %d CPU cores%n%n",
                runs, ticks, seed, Runtime.getRuntime().availableProcessors());
        System.out.printf("%-22s %18s %12s %13s %15s%n",
                "Scenario", "Avg survivors", "Avg zombies", "Trap catches", "Contained");

        StringBuilder trapBreakdown = new StringBuilder();
        for (Scenario scenario : Scenario.values()) {
            long start = System.nanoTime();
            List<RunOutcome> outcomes = experiment.runAll(scenario);
            ExperimentSummary summary = outcomes.stream().collect(ExperimentSummary.collector());
            double seconds = (System.nanoTime() - start) / 1e9;
            System.out.printf("%-22s %10.2f +/- %.2f %12.2f %13.2f %14.0f%%   (%.1fs)%n",
                    scenario.getLabel(),
                    summary.getAverageSurvivors(),
                    summary.getSurvivorsStandardError(),
                    summary.getAverageZombies(),
                    summary.getAverageTrapCatches(),
                    summary.getContainedRate() * 100,
                    seconds);
            if (scenario.hasTraps()) {
                Map<Boolean, LongSummaryStatistics> split = survivorsByTrapActivity(outcomes);
                trapBreakdown.append(String.format(
                        "%-22s traps caught something: %3d runs, %.2f survivors | caught nothing: %3d runs, %.2f survivors%n",
                        scenario.getLabel(),
                        split.get(true).getCount(), split.get(true).getAverage(),
                        split.get(false).getCount(), split.get(false).getAverage()));
            }
        }
        System.out.println();
        System.out.println("Contained = at most one zombie left (a new zombie walks in every 300 ticks).");
        System.out.println();
        System.out.print(trapBreakdown);
    }
}
