import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;

/**
 * A soldier that patrols the world and shoots zombies on sight.
 *
 * <p>A military unit is still a {@link Human} as far as the outbreak is
 * concerned, so a {@link Zombie} treats it as a person. Unlike an ordinary
 * human it does not starve, flee, or shelter in buildings.
 *
 * <p>The unit is built from three patterns:
 * <ul>
 *   <li><b>State</b> - each tick is delegated to the current
 *       {@link MilitaryState} ({@link PatrolState}, {@link EngageState},
 *       {@link ReloadingState}), which decides its own transitions.</li>
 *   <li><b>Strategy as lambdas</b> - the states move the unit through
 *       {@link MovementBehaviour} lambdas rather than behaviour classes.</li>
 *   <li><b>Decorator</b> - the {@link Weapon} is promoted with
 *       {@link WeaponDecorator}s as the unit racks up kills.</li>
 * </ul>
 * The supplied front, side, and back PNGs are chosen from the unit's current
 * facing.
 */
public class Military extends Human {

    private static final double DETECTION_RADIUS = 260.0;
    private static final double SPEED = 2.4;
    private static final int MUZZLE_FLASH_TICKS = 3;
    private static final int CHARACTER_WIDTH = 38;
    private static final int CHARACTER_HEIGHT = 54;
    private static final int TICKS_PER_WALK_FRAME = 8;

    /** Weapon upgrade awarded when a unit reaches the given number of kills. */
    private static final Map<Integer, UnaryOperator<Weapon>> PROMOTIONS =
            Map.<Integer, UnaryOperator<Weapon>>of(
                    2, ScopeDecorator::new,
                    4, ExtendedMagazineDecorator::new);

    private final PatrolState patrolState;
    private final EngageState engageState;
    private MilitaryState state;
    private Weapon weapon;

    private Zombie target;
    private int ammo;
    private int fireCooldown;
    private int muzzleFlashTicks;
    private int kills;

    private final Image[] frontWalkFrames;
    private final Image[] sideWalkFrames;
    private final Image frontIdleFrame;
    private final Image sideIdleFrame;
    private final Image backFrame;
    private int walkFrame;
    private int ticksOnCurrentFrame;
    private boolean facingLeft;
    private Facing facing = Facing.FRONT;
    private boolean moving;

    /** Creates a unit that patrols the given route with the given weapon. */
    public Military(double x, double y, PatrolRoute route, Weapon weapon) {
        super(x, y);
        Objects.requireNonNull(route, "Route cannot be null");
        this.weapon = Objects.requireNonNull(weapon, "Weapon cannot be null");
        this.ammo = weapon.magazineSize();
        this.patrolState = new PatrolState(MovementBehaviour.patrol(route));
        this.engageState = new EngageState(MovementBehaviour.hunt());
        this.state = patrolState;

        frontWalkFrames = new Image[]{
                loadImage("/MilitaryFrontWalk1.png"),
                loadImage("/MilitaryFrontWalk2.png"),
                loadImage("/MilitaryFrontWalk3.png"),
                loadImage("/MilitaryFrontWalk4.png")
        };
        sideWalkFrames = new Image[]{
                loadImage("/MilitaryWalk1.png"),
                loadImage("/MilitaryWalk2.png"),
                loadImage("/MilitaryWalk3.png")
        };
        frontIdleFrame = loadImage("/MilitaryFrontIdle.png");
        sideIdleFrame = loadImage("/MilitarySideIdle.png");
        backFrame = loadImage("/MilitaryBack.png");
    }

    /** Creates a unit with a standard rifle that patrols the given waypoints. */
    public Military(double x, double y, List<Point2D> patrolRoute) {
        this(x, y, new PatrolRoute(patrolRoute), new Rifle());
    }

    /** Creates a unit with a standard rifle that patrols a diamond around its spawn point. */
    public Military(double x, double y) {
        this(x, y, PatrolRoute.diamond(x, y), new Rifle());
    }

    public Optional<Zombie> getTarget() {
        return Optional.ofNullable(target);
    }

    public int getKills() {
        return kills;
    }

    public int getAmmo() {
        return ammo;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    /** "Patrolling", "Engaging" or "Reloading". */
    public String getStateName() {
        return state.name();
    }

    @Override
    public void update(World world) {
        if (!isActive()) {
            return;
        }

        moving = false;
        if (fireCooldown > 0) {
            fireCooldown--;
        }
        if (muzzleFlashTicks > 0) {
            muzzleFlashTicks--;
        }

        state.update(this, world);
    }

    // ---- hooks used by the MilitaryState implementations ------------------

    PatrolState patrolState() {
        return patrolState;
    }

    EngageState engageState() {
        return engageState;
    }

    void transitionTo(MilitaryState next) {
        this.state = Objects.requireNonNull(next, "State cannot be null");
    }

    void lockTarget(Zombie zombie) {
        this.target = zombie;
    }

    void clearTarget() {
        this.target = null;
    }

    /** Refills the magazine to the current weapon's capacity. */
    void reload() {
        ammo = weapon.magazineSize();
    }

    /**
     * Finds the zombie most worth shooting: the one closest to a visible
     * civilian, so soldiers intercept threats to people rather than shooting
     * whichever zombie happens to be nearest to themselves. Distance to the
     * soldier breaks ties (and decides when there are no civilians around).
     */
    public Optional<Zombie> findThreat(World world) {
        List<Entity> nearby = world.getNearby(this, DETECTION_RADIUS);

        List<Human> civilians = nearby.stream()
                .filter(entity -> entity instanceof Human && !(entity instanceof Military))
                .map(entity -> (Human) entity)
                .filter(human -> !human.isHidden())
                .collect(Collectors.toList());

        Comparator<Zombie> closestToACivilian = Comparator.comparingDouble(
                zombie -> civilians.stream()
                        .mapToDouble(zombie::distanceTo)
                        .min()
                        .orElse(Double.MAX_VALUE));

        return nearby.stream()
                .filter(entity -> entity instanceof Zombie)
                .map(entity -> (Zombie) entity)
                .filter(Entity::isActive)
                .min(closestToACivilian.thenComparingDouble(this::distanceTo));
    }

    /**
     * Fires at the current target when it is alive and inside the weapon's
     * range. A hit deactivates the zombie so {@link World} removes it after
     * the tick. Does nothing while the weapon is still cooling down or the
     * magazine is empty.
     *
     * @throws NoTargetException if there is no live zombie in firing range
     */
    public void shoot() throws NoTargetException {
        if (target == null
                || !target.isActive()
                || distanceTo(target) > weapon.range()) {
            throw new NoTargetException(
                    "No zombie within firing range of the military unit at ("
                            + (int) getX() + ", " + (int) getY() + ")");
        }

        if (fireCooldown > 0 || ammo == 0) {
            return;
        }

        target.deactivate();
        ammo--;
        kills++;
        fireCooldown = weapon.fireIntervalTicks();
        muzzleFlashTicks = MUZZLE_FLASH_TICKS;

        // Decorate the weapon if this kill earns a promotion.
        Optional.ofNullable(PROMOTIONS.get(kills))
                .ifPresent(upgrade -> weapon = upgrade.apply(weapon));
    }

    /**
     * Moves one {@link #SPEED} step towards a point, clamped to the world
     * bounds, and updates the facing used for sprite selection. Called by
     * the {@link MovementBehaviour} lambdas.
     */
    public void moveTowards(double targetX, double targetY, World world) {
        double xDifference = targetX - getX();
        double yDifference = targetY - getY();
        double distance = Math.hypot(xDifference, yDifference);
        if (distance < 1.0e-6) {
            return;
        }

        double directionX = xDifference / distance;
        double directionY = yDifference / distance;

        if (Math.abs(directionY) > Math.abs(directionX)) {
            facing = directionY < 0 ? Facing.BACK : Facing.FRONT;
        } else if (Math.abs(directionX) > 0.01) {
            facing = Facing.SIDE;
            facingLeft = directionX < 0;
        }

        double nextX = getX() + directionX * SPEED;
        double nextY = getY() + directionY * SPEED;
        nextX = Math.max(CHARACTER_WIDTH / 2.0,
                Math.min(world.getWidth() - CHARACTER_WIDTH / 2.0, nextX));
        nextY = Math.max(CHARACTER_HEIGHT / 2.0,
                Math.min(world.getHeight() - CHARACTER_HEIGHT / 2.0, nextY));
        setPosition(nextX, nextY);

        moving = true;
        advanceWalkAnimation();
    }

    private void advanceWalkAnimation() {
        ticksOnCurrentFrame++;
        if (ticksOnCurrentFrame >= TICKS_PER_WALK_FRAME) {
            // Twelve is a shared cycle for four front frames and three side frames.
            walkFrame = (walkFrame + 1) % 12;
            ticksOnCurrentFrame = 0;
        }
    }

    @Override
    public void draw(Graphics2D graphics) {
        if (!isActive()) {
            return;
        }

        Image frame = getCurrentFrame();
        int left = (int) getX() - CHARACTER_WIDTH / 2;
        int top = (int) getY() - CHARACTER_HEIGHT / 2
                - (moving && walkFrame % 2 == 1 ? 2 : 0);

        if (facing == Facing.SIDE && facingLeft) {
            // A negative width mirrors the current frame horizontally.
            graphics.drawImage(frame, left + CHARACTER_WIDTH, top,
                    -CHARACTER_WIDTH, CHARACTER_HEIGHT, null);
        } else {
            graphics.drawImage(frame, left, top,
                    CHARACTER_WIDTH, CHARACTER_HEIGHT, null);
        }

        // Draw the overlay on a copy so colours, strokes and fonts set here
        // cannot leak into the entities drawn after this one.
        Graphics2D overlay = (Graphics2D) graphics.create();
        try {
            drawCombatOverlay(overlay);
            drawStatus(overlay, top);
        } finally {
            overlay.dispose();
        }
    }

    /** Draws the firing-range ring while engaging and a tracer on firing. */
    private void drawCombatOverlay(Graphics2D graphics) {
        int centreX = (int) getX();
        int centreY = (int) getY();

        if (state.isEngaging()) {
            int range = (int) weapon.range();
            graphics.setColor(new Color(200, 40, 40, 110));
            graphics.setStroke(new BasicStroke(1.0f));
            graphics.drawOval(centreX - range, centreY - range, range * 2, range * 2);
        }

        if (muzzleFlashTicks > 0 && target != null) {
            graphics.setColor(new Color(255, 220, 90));
            graphics.setStroke(new BasicStroke(2.0f));
            graphics.drawLine(centreX, centreY - CHARACTER_HEIGHT / 4,
                    (int) target.getX(), (int) target.getY());
        }
    }

    /** Draws one pip per round above the unit, plus the state name. */
    private void drawStatus(Graphics2D graphics, int spriteTop) {
        int pipSize = 4;
        int pipGap = 1;
        int magazine = weapon.magazineSize();
        int barWidth = magazine * (pipSize + pipGap) - pipGap;
        int barLeft = (int) getX() - barWidth / 2;
        int barTop = spriteTop - 8;

        for (int round = 0; round < magazine; round++) {
            graphics.setColor(round < ammo
                    ? new Color(255, 210, 70)
                    : new Color(70, 70, 70, 180));
            graphics.fillRect(barLeft + round * (pipSize + pipGap), barTop, pipSize, pipSize);
        }

        graphics.setFont(new Font("Arial", Font.BOLD, 10));
        String label = state.name();
        int labelX = (int) getX() - graphics.getFontMetrics().stringWidth(label) / 2;
        graphics.setColor(Color.BLACK);
        graphics.drawString(label, labelX + 1, barTop - 2);
        graphics.setColor(Color.WHITE);
        graphics.drawString(label, labelX, barTop - 3);
    }

    private Image getCurrentFrame() {
        if (!moving) {
            if (facing == Facing.SIDE) {
                return sideIdleFrame;
            }
            return facing == Facing.BACK ? backFrame : frontIdleFrame;
        }

        if (facing == Facing.SIDE) {
            return sideWalkFrames[walkFrame % sideWalkFrames.length];
        }
        if (facing == Facing.BACK) {
            return backFrame;
        }
        return frontWalkFrames[walkFrame % frontWalkFrames.length];
    }

    private Image loadImage(String imagePath) {
        URL imageUrl = Military.class.getResource(imagePath);
        if (imageUrl != null) {
            return new ImageIcon(imageUrl).getImage();
        }

        // When Java is run with only the generated out directory on its
        // classpath, image assets remain in src rather than being duplicated.
        String fileName = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
        File sourceAsset = new File("src", fileName);
        if (!sourceAsset.isFile()) {
            sourceAsset = new File("COMP2000/src", fileName);
        }
        if (!sourceAsset.isFile()) {
            throw new IllegalStateException(imagePath + " was not found");
        }
        return new ImageIcon(sourceAsset.getAbsolutePath()).getImage();
    }

    private enum Facing {
        FRONT,
        SIDE,
        BACK
    }
}
