import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Shows a short, fading blast wherever a trap catches a zombie.
 *
 * <p>Catches are reported by the update timer and blasts are drawn by the
 * render timer. Both are Swing timers, so they run on the same thread and no
 * locking is needed.
 */
public final class TrapBlasts implements TrapListener {

    private static final long DURATION_MILLIS = 600;
    private static final double START_RADIUS = 10;
    private static final double EXTRA_RADIUS = 30;

    private final LongSupplier clock;
    private final List<Blast> blasts = new ArrayList<>();

    /** Times blasts with the real clock. */
    public TrapBlasts() {
        this(System::currentTimeMillis);
    }

    /** Times blasts with the given clock in milliseconds, so tests can control time. */
    TrapBlasts(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    @Override
    public void onZombieCaught(Trap trap, Zombie zombie) {
        blasts.add(new Blast(trap.getX(), trap.getY(), clock.getAsLong()));
    }

    /** How many blasts are still showing. */
    public int getActiveCount() {
        dropFinished(clock.getAsLong());
        return blasts.size();
    }

    /** Removes every blast, for a fresh run. */
    public void clear() {
        blasts.clear();
    }

    /** Draws each blast as a ring that grows and fades out. */
    public void draw(Graphics2D graphics) {
        long now = clock.getAsLong();
        dropFinished(now);

        Color previousColor = graphics.getColor();
        Stroke previousStroke = graphics.getStroke();

        for (Blast blast : blasts) {
            double progress = (double) (now - blast.startedAt()) / DURATION_MILLIS;
            int radius = (int) (START_RADIUS + EXTRA_RADIUS * progress);
            int alpha = (int) (220 * (1 - progress));
            int left = (int) blast.x() - radius;
            int top = (int) blast.y() - radius;

            graphics.setColor(new Color(255, 170, 40, alpha / 2));
            graphics.fillOval(left, top, radius * 2, radius * 2);
            graphics.setColor(new Color(230, 60, 20, alpha));
            graphics.setStroke(new BasicStroke(3f));
            graphics.drawOval(left, top, radius * 2, radius * 2);
        }

        graphics.setColor(previousColor);
        graphics.setStroke(previousStroke);
    }

    private void dropFinished(long now) {
        blasts.removeIf(blast -> now - blast.startedAt() >= DURATION_MILLIS);
    }

    /** Where and when one trap went off. */
    private record Blast(double x, double y, long startedAt) {
    }
}
