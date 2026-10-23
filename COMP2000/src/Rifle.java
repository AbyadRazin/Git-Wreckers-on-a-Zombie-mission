/** The standard-issue weapon: every soldier starts with one of these. */
public class Rifle implements Weapon {

    private static final double RANGE = 140.0;
    private static final int FIRE_INTERVAL_TICKS = 12;
    private static final int MAGAZINE_SIZE = 6;
    private static final int RELOAD_TICKS = 30;

    @Override
    public double range() {
        return RANGE;
    }

    @Override
    public int fireIntervalTicks() {
        return FIRE_INTERVAL_TICKS;
    }

    @Override
    public int magazineSize() {
        return MAGAZINE_SIZE;
    }

    @Override
    public int reloadTicks() {
        return RELOAD_TICKS;
    }

    @Override
    public String describe() {
        return "Rifle";
    }
}
