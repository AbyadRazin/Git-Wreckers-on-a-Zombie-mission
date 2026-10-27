/**
 * Something that wants to know whenever a {@link Trap} catches a zombie.
 *
 * <p>This is the observer side of the Observer pattern: a trap keeps a list
 * of listeners and tells all of them about each catch, without knowing what
 * they do with it. The HUD counter, the blast effect and the experiment each
 * listen for their own reasons.
 *
 * <p>Listeners are called on the thread that updates the world, straight
 * after the zombie has been caught.
 */
@FunctionalInterface
public interface TrapListener {

    /**
     * Called once for every zombie a trap catches.
     *
     * @param trap   the trap that went off (it may have just worn out)
     * @param zombie the zombie it caught, already removed from play
     */
    void onZombieCaught(Trap trap, Zombie zombie);
}
