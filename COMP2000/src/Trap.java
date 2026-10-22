import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A stationary hazard that catches zombies that walk over it.
 *
 * <p>Each zombie caught uses up one of the trap's limited uses. When
 * no uses are left the trap breaks and is removed from the world.
 * Humans and military units can walk over a trap safely.
 */
public class Trap extends Entity {

    private static final double TRIGGER_RADIUS = 18.0;
    private static final int SIZE = 24;

    private final int maxUses;
    private int remainingUses;

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

    public int getMaxUses() {
        return maxUses;
    }

    public int getRemainingUses() {
        return remainingUses;
    }

    /**
     * Catches every zombie standing on the trap, up to the number of
     * uses it has left, and breaks the trap once it is used up.
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

        caughtZombies.forEach(Zombie::deactivate);
        remainingUses -= caughtZombies.size();

        if (remainingUses == 0) {
            deactivate();
        }
    }

    /** Draws a red ring with a cross that fades as the trap wears out. */
    @Override
    public void draw(Graphics2D graphics) {
        Color previousColor = graphics.getColor();
        Stroke previousStroke = graphics.getStroke();

        int alpha = 90 + 165 * remainingUses / maxUses;
        int left = (int) getX() - SIZE / 2;
        int top = (int) getY() - SIZE / 2;
        int inset = SIZE / 4;

        graphics.setColor(new Color(170, 40, 40, alpha));
        graphics.setStroke(new BasicStroke(2.0f));
        graphics.drawOval(left, top, SIZE, SIZE);
        graphics.drawLine(left + inset, top + inset,
                left + SIZE - inset, top + SIZE - inset);
        graphics.drawLine(left + SIZE - inset, top + inset,
                left + inset, top + SIZE - inset);

        // Put the shared graphics state back for whatever draws next.
        graphics.setColor(previousColor);
        graphics.setStroke(previousStroke);
    }
}
