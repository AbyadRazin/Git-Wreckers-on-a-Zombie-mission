/** Longer reach in exchange for slower, more careful shots. */
public class ScopeDecorator extends WeaponDecorator {

    private static final double EXTRA_RANGE = 70.0;
    private static final int EXTRA_FIRE_INTERVAL_TICKS = 3;

    public ScopeDecorator(Weapon wrapped) {
        super(wrapped);
    }

    @Override
    public double range() {
        return super.range() + EXTRA_RANGE;
    }

    @Override
    public int fireIntervalTicks() {
        return super.fireIntervalTicks() + EXTRA_FIRE_INTERVAL_TICKS;
    }

    @Override
    public String describe() {
        return super.describe() + " + Scope";
    }
}
