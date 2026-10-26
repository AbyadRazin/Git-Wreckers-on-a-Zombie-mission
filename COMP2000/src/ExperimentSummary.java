import java.util.stream.Collector;

/**
 * Averages over many {@link RunOutcome}s, built by a custom {@link Collector}.
 *
 * <p>The collector is safe to use with a parallel stream without any locking:
 * each worker thread gets its own {@link Tally} from the supplier, adds only
 * the outcomes it processes, and finished tallies are merged pairwise by the
 * combiner. No tally is ever shared between threads while it is being
 * updated, and merging is associative, so the result is the same however the
 * runs are split up.
 */
public final class ExperimentSummary {

    private final long runs;
    private final double averageSurvivors;
    private final double survivorsStandardError;
    private final double averageZombies;
    private final double containedRate;

    private ExperimentSummary(long runs, double averageSurvivors, double survivorsStandardError,
                              double averageZombies, double containedRate) {
        this.runs = runs;
        this.averageSurvivors = averageSurvivors;
        this.survivorsStandardError = survivorsStandardError;
        this.averageZombies = averageZombies;
        this.containedRate = containedRate;
    }

    /** Collects run outcomes into a summary; works for sequential and parallel streams. */
    public static Collector<RunOutcome, ?, ExperimentSummary> collector() {
        return Collector.of(
                Tally::new,
                Tally::add,
                Tally::merge,
                Tally::finish,
                Collector.Characteristics.UNORDERED);
    }

    public long getRuns() {
        return runs;
    }

    public double getAverageSurvivors() {
        return averageSurvivors;
    }

    /**
     * How much the average survivor count could move by chance alone; two
     * scenarios only really differ if their averages are several of these apart.
     */
    public double getSurvivorsStandardError() {
        return survivorsStandardError;
    }

    public double getAverageZombies() {
        return averageZombies;
    }

    /** Fraction of runs (0 to 1) that ended with no zombies left. */
    public double getContainedRate() {
        return containedRate;
    }

    /** Mutable running totals, confined to one thread until merged. */
    private static final class Tally {
        private long runs;
        private long survivors;
        private long survivorsSquared;
        private long zombies;
        private long contained;

        void add(RunOutcome outcome) {
            runs++;
            survivors += outcome.getSurvivors();
            survivorsSquared += outcome.getSurvivors() * outcome.getSurvivors();
            zombies += outcome.getZombies();
            if (outcome.outbreakContained()) {
                contained++;
            }
        }

        Tally merge(Tally other) {
            runs += other.runs;
            survivors += other.survivors;
            survivorsSquared += other.survivorsSquared;
            zombies += other.zombies;
            contained += other.contained;
            return this;
        }

        ExperimentSummary finish() {
            if (runs == 0) {
                return new ExperimentSummary(0, 0, 0, 0, 0);
            }
            double mean = (double) survivors / runs;
            double standardError = 0;
            if (runs > 1) {
                double variance = (survivorsSquared - runs * mean * mean) / (runs - 1);
                standardError = Math.sqrt(Math.max(0, variance) / runs);
            }
            return new ExperimentSummary(runs, mean, standardError,
                    (double) zombies / runs,
                    (double) contained / runs);
        }
    }
}
