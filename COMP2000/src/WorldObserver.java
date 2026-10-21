/** Receives immutable statistics synchronously on the publishing thread. */
@FunctionalInterface
public interface WorldObserver {
    /** Observe values without mutating the world or its entities. */
    void onWorldChanged(WorldStatistics statistics);
}
