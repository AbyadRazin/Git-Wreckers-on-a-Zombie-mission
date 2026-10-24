import java.util.Objects;

/**
 * Base class for weapon upgrades. A decorator <em>is a</em> {@link Weapon}
 * and <em>has a</em> Weapon: it forwards every call to the weapon it wraps
 * and subclasses override only the statistic they change. Upgrades can
 * therefore be stacked in any order at run time without a subclass for every
 * combination (Rifle, ScopedRifle, ExtendedScopedRifle, ...).
 */
public abstract class WeaponDecorator implements Weapon {

    private final Weapon wrapped;

    protected WeaponDecorator(Weapon wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "Weapon cannot be null");
    }

    @Override
    public double range() {
        return wrapped.range();
    }

    @Override
    public int fireIntervalTicks() {
        return wrapped.fireIntervalTicks();
    }

    @Override
    public int magazineSize() {
        return wrapped.magazineSize();
    }

    @Override
    public int reloadTicks() {
        return wrapped.reloadTicks();
    }

    @Override
    public String describe() {
        return wrapped.describe();
    }
}
