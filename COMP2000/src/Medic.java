import java.util.Comparator;

/**
 * A civilian with first-aid training who keeps other survivors alive.
 *
 * <p>While it has energy to spare, a medic looks for the most urgent
 * patient nearby (the visible civilian with the lowest energy), walks over
 * and heals them, leaving shelter if it has to. It won't walk into danger:
 * patients with a zombie close by are skipped, and if a zombie comes close
 * to the medic it stops helping and looks after itself. When nobody needs
 * help, the medic is running low, or a zombie is near, it falls back to
 * ordinary {@link Human} survival: sheltering, eating and wandering.
 *
 * <p>Medics get hungry like everyone else, because {@link Human#update(World)}
 * drains energy before calling {@link #behave(World)}. Zombies can still
 * catch and infect them.
 *
 * <p>Medics reuse the normal human animations with their own sprites from
 * {@code MedicAssets}: a white jacket and trousers, a red shirt and a red
 * first-aid backpack.
 */
public class Medic extends Human {

    private static final double HEAL_DISTANCE = 20.0;
    private static final double HEAL_AMOUNT = 30.0;
    private static final double HEAL_THRESHOLD = 60.0;
    // Same point at which civilians leave shelter to find food: above it the
    // medic can afford to help others, at or below it the medic must eat.
    private static final double ON_DUTY_ENERGY = 35.0;
    private static final double DANGER_RADIUS = 100.0;
    private static final double SEARCH_RADIUS = 300.0;
    private static final int HEAL_COOLDOWN_TICKS = 15;

    private int cooldown;

    public Medic(double x, double y) {
        super(x, y, "/MedicAssets/");
    }

    /** Ticks left before this medic can heal again (0 means ready). */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Heals a nearby patient if there is one, the medic is fit enough and no
     * zombie is close; otherwise behaves like any other human.
     */
    @Override
    protected void behave(World world) {
        if (cooldown > 0) {
            cooldown--;
        }

        // A hungry medic, or one with a zombie close by, looks after itself
        // first, like any other survivor.
        if (getEnergy() <= ON_DUTY_ENERGY || zombieNear(world, this)) {
            super.behave(world);
            return;
        }

        try {
            Human patient = findPatient(world);

            // Does nothing if the medic isn't sheltering.
            leaveBuilding();

            if (distanceTo(patient) > HEAL_DISTANCE) {
                moveTowards(patient.getX(), patient.getY(), world);
            }
            if (distanceTo(patient) <= HEAL_DISTANCE && cooldown == 0) {
                heal(patient);
            }
        } catch (TargetNotFoundException exception) {
            // Nobody nearby needs help, so carry on surviving.
            super.behave(world);
        }
    }

    /**
     * Finds the most urgent patient in range: the visible civilian with the
     * lowest energy, as long as that is below the healing threshold and no
     * zombie is close to them.
     *
     * @throws TargetNotFoundException if nobody nearby needs healing
     */
    private Human findPatient(World world) throws TargetNotFoundException {
        return world.getNearby(this, SEARCH_RADIUS).stream()
                .filter(Human.class::isInstance)
                .map(Human.class::cast)
                .filter(human -> !(human instanceof Military))
                .filter(human -> !human.isHidden())
                .filter(human -> human.getEnergy() < HEAL_THRESHOLD)
                .filter(human -> !zombieNear(world, human))
                .min(Comparator.comparingDouble(Human::getEnergy))
                .orElseThrow(() -> new TargetNotFoundException(
                        "No injured civilian within " + SEARCH_RADIUS
                                + " units of the medic"
                ));
    }

    /** Whether any zombie is within the danger radius of the given entity. */
    private static boolean zombieNear(World world, Entity entity) {
        return world.getNearby(entity, DANGER_RADIUS).stream()
                .anyMatch(Zombie.class::isInstance);
    }

    /** Restores some of the patient's energy and starts the cooldown. */
    private void heal(Human patient) {
        patient.restoreEnergy(HEAL_AMOUNT);
        cooldown = HEAL_COOLDOWN_TICKS;
    }
}
