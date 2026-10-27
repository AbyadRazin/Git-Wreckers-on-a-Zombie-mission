import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;

/**
 * A stationary hazard that catches zombies that walk over it.
 *
 * <p>Each zombie caught uses up one of the trap's limited uses. When
 * no uses are left the trap breaks and is removed from the world.
 * Humans and military units can walk over a trap safely.
 *
 * <p>A trap announces every catch to its {@link TrapListener}s (the
 * Observer pattern), so other parts of the program can react without the
 * trap knowing about them.
 *
 * <p>Traps are drawn as a landmine. The picture is loaded once and shared by
 * every trap; if it can't be found, traps fall back to a drawn red ring so
 * the simulation still runs.
 */
public class Trap extends Entity {

    private static final double TRIGGER_RADIUS = 18.0;
    private static final double MIN_ENTRANCE_DISTANCE = 15.0;
    private static final double MAX_ENTRANCE_DISTANCE = 55.0;
    private static final int MAX_PLACEMENT_ATTEMPTS = 1000;
    private static final int SIZE = 28;
    private static final Image SPRITE = loadSprite();

    private final int maxUses;
    private int remainingUses;
    private final List<TrapListener> listeners = new ArrayList<>();

    /**
     * Creates a trap at the given position.
     *
     * @param x       horizontal position of the trap's centre
     * @param y       vertical position of the trap's centre
     * @param maxUses how many zombies the trap can catch before it breaks
     * @throws IllegalArgumentException if {@code maxUses} is not positive
     */
    public Trap(double x, double y, int maxUses) {
        super(x, y);

        if (maxUses <= 0) {
            throw new IllegalArgumentException(
                    "A trap needs at least one use"
            );
        }

        this.maxUses = maxUses;
        this.remainingUses = maxUses;
    }

    /**
     * Creates a trap at a random spot near a building's entrance, outside
     * every building and inside the world.
     *
     * <p>Survivors run to building entrances to shelter and zombies follow
     * them, so that is where zombies spend most of their time and where a
     * trap is most likely to be stepped on. The trap goes in front of the
     * door, 15 to 55 units away.
     *
     * @param world    the world the trap is for, used to avoid buildings and edges
     * @param building the building whose entrance the trap guards
     * @param random   where the random position comes from
     * @param maxUses  how many zombies the trap can catch before it breaks
     * @throws IllegalStateException if no free spot near the entrance can be found
     */
    public static Trap nearEntrance(World world, Building building, Random random, int maxUses) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(building, "Building cannot be null");
        Objects.requireNonNull(random, "Random cannot be null");

        // Entrances are on the bottom edge, so try the half-circle in front
        // of the door; the building itself is behind it.
        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI;
            double distance = MIN_ENTRANCE_DISTANCE
                    + random.nextDouble() * (MAX_ENTRANCE_DISTANCE - MIN_ENTRANCE_DISTANCE);
            double x = building.getEntranceX() + Math.cos(angle) * distance;
            double y = building.getEntranceY() + Math.sin(angle) * distance;

            if (isFreeSpot(world, x, y)) {
                return new Trap(x, y, maxUses);
            }
        }
        throw new IllegalStateException("No free spot for a trap near the building entrance");
    }

    /** Inside the world (with room for the picture) and not on any building. */
    private static boolean isFreeSpot(World world, double x, double y) {
        double margin = SIZE / 2.0;
        boolean insideWorld = x >= margin && x <= world.getWidth() - margin
                && y >= margin && y <= world.getHeight() - margin;
        return insideWorld && world.getEntities().stream()
                .filter(Building.class::isInstance)
                .map(Building.class::cast)
                .noneMatch(building -> building.containsPoint(x, y));
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getRemainingUses() {
        return remainingUses;
    }

    /**
     * Subscribes a listener to this trap's catches. Adding the same listener
     * twice has no effect, so it is never told about a catch twice.
     *
     * @throws NullPointerException if {@code listener} is null
     */
    public void addTrapListener(TrapListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Stops telling a listener about this trap's catches. */
    public void removeTrapListener(TrapListener listener) {
        listeners.remove(listener);
    }

    /**
     * Subscribes a listener to every trap currently in the world. Safe to call
     * again after more traps are added: traps already listened to are skipped.
     */
    public static void listenToAll(World world, TrapListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        world.getEntities().stream()
                .filter(Trap.class::isInstance)
                .map(Trap.class::cast)
                .forEach(trap -> trap.addTrapListener(listener));
    }

    /**
     * Catches every zombie standing on the trap, up to the number of
     * uses it has left, tells the listeners about each one, and breaks the
     * trap once it is used up.
     */
    @Override
    public void update(World world) {
        // getNearby already skips inactive entities, and limit() stops
        // the trap from catching more zombies than it has uses left.
        List<Zombie> caughtZombies = world.getNearby(this, TRIGGER_RADIUS)
                .stream()
                .filter(Zombie.class::isInstance)
                .map(Zombie.class::cast)
                .limit(remainingUses)
                .collect(Collectors.toList());

        for (Zombie zombie : caughtZombies) {
            zombie.deactivate();
            remainingUses--;
            announceCatch(zombie);
        }

        if (remainingUses == 0) {
            deactivate();
        }
    }

    private void announceCatch(Zombie zombie) {
        // Notify a copy, so a listener can unsubscribe while it is being told.
        for (TrapListener listener : List.copyOf(listeners)) {
            listener.onZombieCaught(this, zombie);
        }
    }

    /**
     * Draws the landmine with a faint red ring around it. Both fade as the
     * trap wears out.
     */
    @Override
    public void draw(Graphics2D graphics) {
        Color previousColor = graphics.getColor();
        Stroke previousStroke = graphics.getStroke();
        Composite previousComposite = graphics.getComposite();

        int alpha = 90 + 165 * remainingUses / maxUses;
        int left = (int) getX() - SIZE / 2;
        int top = (int) getY() - SIZE / 2;

        if (SPRITE != null) {
            graphics.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha / 255f));
            graphics.drawImage(SPRITE, left, top, SIZE, SIZE, null);
            graphics.setComposite(previousComposite);

            graphics.setColor(new Color(170, 40, 40, alpha / 2));
            graphics.setStroke(new BasicStroke(1.5f));
            graphics.drawOval(left - 3, top - 3, SIZE + 6, SIZE + 6);
        } else {
            // No picture available: a red ring with a cross.
            int inset = SIZE / 4;
            graphics.setColor(new Color(170, 40, 40, alpha));
            graphics.setStroke(new BasicStroke(2.0f));
            graphics.drawOval(left, top, SIZE, SIZE);
            graphics.drawLine(left + inset, top + inset,
                    left + SIZE - inset, top + SIZE - inset);
            graphics.drawLine(left + SIZE - inset, top + inset,
                    left + inset, top + SIZE - inset);
        }

        // Put the shared graphics state back for whatever draws next.
        graphics.setColor(previousColor);
        graphics.setStroke(previousStroke);
        graphics.setComposite(previousComposite);
    }

    /**
     * Loads the landmine picture once for all traps, from the classpath or
     * the source folder. Returns {@code null} if it can't be found, so traps
     * draw a ring instead of crashing the simulation.
     */
    private static Image loadSprite() {
        URL resource = Trap.class.getResource("/Trap.png");
        if (resource != null) {
            return new ImageIcon(resource).getImage();
        }

        for (String folder : new String[]{"src", "COMP2000/src"}) {
            File file = new File(folder, "Trap.png");
            if (file.isFile()) {
                return new ImageIcon(file.getAbsolutePath()).getImage();
            }
        }
        return null;
    }
}
