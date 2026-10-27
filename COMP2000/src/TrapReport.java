/**
 * Keeps a running count of the zombies caught by traps.
 *
 * <p>This can't be worked out by looking at the world afterwards: a trap is
 * removed from the world as soon as it wears out, and its catches go with it.
 * So the report listens to the traps and counts each catch as it happens.
 *
 * <p>Not thread-safe. Use one report per world; the experiment gives every
 * run its own.
 */
public final class TrapReport implements TrapListener {

    private long catches;

    @Override
    public void onZombieCaught(Trap trap, Zombie zombie) {
        catches++;
    }

    /** How many zombies traps have caught since the start or the last reset. */
    public long getCatches() {
        return catches;
    }

    /** Starts counting from zero again, for a fresh run. */
    public void reset() {
        catches = 0;
    }
}
