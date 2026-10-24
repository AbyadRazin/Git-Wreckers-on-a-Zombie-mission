/** More rounds per magazine, but a longer reload. */
public class ExtendedMagazineDecorator extends WeaponDecorator {

    private static final int EXTRA_ROUNDS = 6;
    private static final int EXTRA_RELOAD_TICKS = 12;

    public ExtendedMagazineDecorator(Weapon wrapped) {
        super(wrapped);
    }

    @Override
    public int magazineSize() {
        return super.magazineSize() + EXTRA_ROUNDS;
    }

    @Override
    public int reloadTicks() {
        return super.reloadTicks() + EXTRA_RELOAD_TICKS;
    }

    @Override
    public String describe() {
        return super.describe() + " + Extended Magazine";
    }
}
