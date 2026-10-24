/**
 * What a {@link Military} unit can fire. This is the component interface of
 * the Decorator pattern: a plain {@link Rifle} and any stack of
 * {@link WeaponDecorator}s all look identical to the soldier using them.
 */
public interface Weapon {

    /** How far (in pixels) a target can be and still be hit. */
    double range();

    /** Minimum ticks between two shots. */
    int fireIntervalTicks();

    /** Rounds available before the unit has to reload. */
    int magazineSize();

    /** Ticks a reload takes. */
    int reloadTicks();

    /** Short description for debugging, e.g. {@code "Rifle + Scope"}. */
    String describe();
}
