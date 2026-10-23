import java.util.Collection;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;

/**
 * A summary of how the soldiers are doing, produced by a custom
 * {@link Collector}: {@code soldiers.stream().collect(BattleReport.collector())}.
 *
 * <p>The collector keeps every bit of mutable state in a per-thread
 * accumulation container and merges containers with an associative combiner,
 * so it gives the same answer for {@code stream()} and
 * {@code parallelStream()} without any locking.
 */
public final class BattleReport {

    private final int soldiers;
    private final int totalKills;
    private final int bestKills;
    private final double averageKills;
    private final Map<String, Long> stateCounts;

    private BattleReport(IntSummaryStatistics kills, Map<String, Long> stateCounts) {
        this.soldiers = (int) kills.getCount();
        this.totalKills = (int) kills.getSum();
        // An empty IntSummaryStatistics reports Integer.MIN_VALUE as its max.
        this.bestKills = soldiers == 0 ? 0 : kills.getMax();
        this.averageKills = kills.getAverage();
        this.stateCounts = Collections.unmodifiableMap(new TreeMap<>(stateCounts));
    }

    /** Reports on every active {@link Military} unit in the given entities. */
    public static BattleReport of(Collection<? extends Entity> entities) {
        return entities.stream()
                .filter(Entity::isActive)
                .filter(Military.class::isInstance)
                .map(Military.class::cast)
                .collect(collector());
    }

    /** The custom collector: kill statistics plus a head-count per state. */
    public static Collector<Military, ?, BattleReport> collector() {
        return Collector.of(
                Tally::new,
                Tally::add,
                Tally::merge,
                Tally::finish,
                Collector.Characteristics.UNORDERED);
    }

    public int getSoldiers() {
        return soldiers;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getBestKills() {
        return bestKills;
    }

    public double getAverageKills() {
        return averageKills;
    }

    /** Soldiers per state name, sorted by name, e.g. {Engaging=1, Patrolling=2}. */
    public Map<String, Long> getStateCounts() {
        return stateCounts;
    }

    /** The mutable accumulation container; never shared between threads. */
    private static final class Tally {
        private final IntSummaryStatistics kills = new IntSummaryStatistics();
        private final Map<String, Long> states = new TreeMap<>();

        void add(Military soldier) {
            kills.accept(soldier.getKills());
            states.merge(soldier.getStateName(), 1L, Long::sum);
        }

        Tally merge(Tally other) {
            kills.combine(other.kills);
            other.states.forEach((state, count) -> states.merge(state, count, Long::sum));
            return this;
        }

        BattleReport finish() {
            return new BattleReport(kills, states);
        }
    }
}
