import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Comparator;
import java.util.function.BiConsumer;

/**
 * A civilian with first-aid training who keeps other survivors alive.
 *
 * <p>Each tick a medic is in one of four {@link Mode}s (the State pattern):
 * <ul>
 *   <li><b>Healing</b>: it has energy to spare and someone nearby needs help,
 *       so it walks over (leaving shelter if it has to) and heals the most
 *       urgent patient, the visible civilian with the lowest energy.</li>
 *   <li><b>Standing by</b>: nobody nearby needs help.</li>
 *   <li><b>Hungry</b>: its own energy is too low to help anyone.</li>
 *   <li><b>Avoiding zombie</b>: a zombie is close, so it stops helping.</li>
 * </ul>
 * In the last three modes it behaves like any other {@link Human}:
 * sheltering, eating and wandering. Patients with a zombie close by are
 * skipped, so a medic never walks into danger to heal someone.
 *
 * <p>The medic itself decides which mode it is in, in one place
 * ({@link #decideMode(World)}), and each mode carries its own behaviour as a
 * lambda. The rules are checked in priority order, so "hungry" always wins
 * over "avoiding zombie", which wins over "healing".
 *
 * <p>Medics get hungry like everyone else, because {@link Human#update(World)}
 * drains energy before calling {@link #behave(World)}. Zombies can still
 * catch and infect them.
 *
 * <p>Medics reuse the normal human animations with their own sprites from
 * {@code assets/medic}: a white jacket and trousers, a red shirt and a red
 * first-aid backpack. The current mode is shown above the medic.
 */
public class Medic extends Human {

    /** What the medic does this tick. Each mode carries its own behaviour. */
    enum Mode {
        HEALING("Healing", Medic::treatPatient),
        STANDING_BY("Standing by", Medic::surviveLikeACivilian),
        HUNGRY("Hungry", Medic::surviveLikeACivilian),
        AVOIDING_ZOMBIE("Avoiding zombie", Medic::surviveLikeACivilian);

        private final String label;
        private final BiConsumer<Medic, World> action;

        Mode(String label, BiConsumer<Medic, World> action) {
            this.label = label;
            this.action = action;
        }

        /** Text shown above the medic, e.g. {@code "Healing"}. */
        String getLabel() {
            return label;
        }

        void act(Medic medic, World world) {
            action.accept(medic, world);
        }
    }

    private static final double HEAL_DISTANCE = 20.0;
    private static final double HEAL_AMOUNT = 30.0;
    private static final double HEAL_THRESHOLD = 60.0;
    // Same point at which civilians leave shelter to find food: above it the
    // medic can afford to help others, at or below it the medic must eat.
    private static final double ON_DUTY_ENERGY = 35.0;
    private static final double DANGER_RADIUS = 100.0;
    private static final double SEARCH_RADIUS = 300.0;
    private static final int HEAL_COOLDOWN_TICKS = 15;
    private static final int LABEL_OFFSET = 31;

    private int cooldown;
    private Mode mode = Mode.STANDING_BY;
    // Only set while healing: the patient chosen this tick.
    private Human patient;

    public Medic(double x, double y) {
        super(x, y, "/assets/medic/");
    }

    /** Ticks left before this medic can heal again (0 means ready). */
    public int getCooldown() {
        return cooldown;
    }

    /** The mode the medic was in on its last tick. */
    Mode getMode() {
        return mode;
    }

    /** Picks this tick's mode, then lets that mode act. */
    @Override
    protected void behave(World world) {
        if (cooldown > 0) {
            cooldown--;
        }
        mode = decideMode(world);
        mode.act(this, world);
    }

    /**
     * The only place the medic's mode changes. The rules are checked in
     * priority order: a hungry medic looks after itself first, then a medic
     * with a zombie close by, and only then does it look for a patient.
     */
    private Mode decideMode(World world) {
        patient = null;
        if (getEnergy() <= ON_DUTY_ENERGY) {
            return Mode.HUNGRY;
        }
        if (zombieNear(world, this)) {
            return Mode.AVOIDING_ZOMBIE;
        }
        try {
            patient = findPatient(world);
            return Mode.HEALING;
        } catch (TargetNotFoundException exception) {
            return Mode.STANDING_BY;
        }
    }

    /** Healing mode: go to the chosen patient and heal them when close enough. */
    private void treatPatient(World world) {
        // Does nothing if the medic isn't sheltering.
        leaveBuilding();

        if (distanceTo(patient) > HEAL_DISTANCE) {
            moveTowards(patient.getX(), patient.getY(), world);
        }
        if (distanceTo(patient) <= HEAL_DISTANCE && cooldown == 0) {
            heal(patient);
        }
    }

    /** Every other mode: shelter, eat and wander like an ordinary civilian. */
    private void surviveLikeACivilian(World world) {
        super.behave(world);
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

    /** Draws the medic, then its current mode above it, like the soldiers' state labels. */
    @Override
    public void draw(Graphics2D graphics) {
        super.draw(graphics);
        if (isHidden()) {
            return;
        }

        Font previousFont = graphics.getFont();
        Color previousColor = graphics.getColor();

        graphics.setFont(new Font("Arial", Font.BOLD, 10));
        String label = mode.getLabel();
        int labelX = (int) getX() - graphics.getFontMetrics().stringWidth(label) / 2;
        int labelY = (int) getY() - LABEL_OFFSET;
        graphics.setColor(Color.BLACK);
        graphics.drawString(label, labelX + 1, labelY + 1);
        graphics.setColor(new Color(255, 120, 120));
        graphics.drawString(label, labelX, labelY);

        graphics.setFont(previousFont);
        graphics.setColor(previousColor);
    }
}
