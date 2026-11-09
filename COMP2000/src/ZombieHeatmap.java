import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

/**
 * Records where zombies spend their time over many headless runs and draws
 * it as a heatmap, with the traps' starting spots marked on top.
 *
 * <p>This showed that zombies crowd around the building doors (survivors
 * run there to shelter and zombies follow) while the original fixed traps
 * sat in quiet gaps, which is why traps now start near the doors.
 *
 * <p>Runs execute in parallel. Each run fills its own {@link Grid}, so no
 * grid is shared between threads while it is being written, and the grids
 * are added together at the end by a custom {@link Collector}.
 *
 * <p>Run it with {@code java -cp out:src ZombieHeatmap [runs] [ticks] [output.png]}.
 */
public final class ZombieHeatmap {

    static final int WIDTH = 1000;
    static final int HEIGHT = 750;
    static final int CELL = 10;
    private static final double TRAP_RADIUS = 30.0;
    private static final long LAYOUT_SEED = 20261109L;

    private final int runs;
    private final int ticks;

    /**
     * @param runs  how many simulations to record
     * @param ticks how long each simulation runs for
     * @throws IllegalArgumentException if either value is not positive
     */
    public ZombieHeatmap(int runs, int ticks) {
        if (runs <= 0 || ticks <= 0) {
            throw new IllegalArgumentException("Runs and ticks must be positive");
        }
        this.runs = runs;
        this.ticks = ticks;
    }

    /** Runs every simulation in parallel and adds their grids together. */
    public Grid record() {
        return IntStream.range(0, runs)
                .parallel()
                .mapToObj(this::recordRun)
                .collect(Grid.collector());
    }

    /** One run with medics and traps, recording every active zombie each tick. */
    private Grid recordRun(int run) {
        World world = Experiment.Scenario.BOTH.createWorld(LAYOUT_SEED * 1_000_003L + run);
        Grid grid = new Grid();
        world.getEntities().stream()
                .filter(Trap.class::isInstance)
                .forEach(trap -> grid.addTrapSpot(trap.getX(), trap.getY()));
        world.getEntities().stream()
                .filter(Building.class::isInstance)
                .map(Building.class::cast)
                .forEach(building -> grid.addBuilding(building));
        for (int tick = 0; tick < ticks; tick++) {
            world.update();
            world.getEntities().stream()
                    .filter(Zombie.class::isInstance)
                    .filter(Entity::isActive)
                    .forEach(zombie -> grid.record(zombie.getX(), zombie.getY()));
        }
        return grid;
    }

    /**
     * Zombie positions counted per 10x10 cell, plus where traps started.
     * Not thread-safe on its own: each run fills its own grid, and grids are
     * only merged once the runs that filled them have finished.
     */
    static final class Grid {
        private final long[][] counts = new long[WIDTH / CELL][HEIGHT / CELL];
        private final List<double[]> trapSpots = new ArrayList<>();
        // Every run has the same buildings, so only one set is kept.
        private final List<int[]> buildings = new ArrayList<>();
        private long total;

        /** Counts one zombie at this position; positions off the map count at the edge. */
        void record(double x, double y) {
            int column = clamp((int) (x / CELL), counts.length);
            int row = clamp((int) (y / CELL), counts[0].length);
            counts[column][row]++;
            total++;
        }

        void addTrapSpot(double x, double y) {
            trapSpots.add(new double[]{x, y});
        }

        void addBuilding(Building building) {
            buildings.add(new int[]{(int) building.getX(), (int) building.getY(),
                    building.getWidth(), building.getHeight()});
        }

        /** Adds another grid into this one. Order doesn't matter, so merging is associative. */
        Grid merge(Grid other) {
            for (int column = 0; column < counts.length; column++) {
                for (int row = 0; row < counts[column].length; row++) {
                    counts[column][row] += other.counts[column][row];
                }
            }
            trapSpots.addAll(other.trapSpots);
            if (buildings.isEmpty()) {
                buildings.addAll(other.buildings);
            }
            total += other.total;
            return this;
        }

        /** Collects per-run grids into one; safe for parallel streams. */
        static Collector<Grid, Grid, Grid> collector() {
            return Collector.of(Grid::new, Grid::merge, Grid::merge,
                    Collector.Characteristics.IDENTITY_FINISH,
                    Collector.Characteristics.UNORDERED);
        }

        long getTotal() {
            return total;
        }

        long getCount(int column, int row) {
            return counts[column][row];
        }

        int getTrapSpotCount() {
            return trapSpots.size();
        }

        /** Share (0 to 1) of all zombie time spent in cells within the radius of the point. */
        double shareNear(double x, double y, double radius) {
            if (total == 0) {
                return 0;
            }
            long near = 0;
            for (int column = 0; column < counts.length; column++) {
                for (int row = 0; row < counts[column].length; row++) {
                    double centreX = column * CELL + CELL / 2.0;
                    double centreY = row * CELL + CELL / 2.0;
                    if (Math.hypot(centreX - x, centreY - y) <= radius) {
                        near += counts[column][row];
                    }
                }
            }
            return (double) near / total;
        }

        private long max() {
            long max = 0;
            for (long[] column : counts) {
                for (long count : column) {
                    max = Math.max(max, count);
                }
            }
            return max;
        }

        private static int clamp(int index, int size) {
            return Math.max(0, Math.min(size - 1, index));
        }
    }

    /** Draws the grid over the map: brighter red means more zombie time. */
    static BufferedImage render(Grid grid, String title) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            BufferedImage background = loadBackground();
            if (background != null) {
                graphics.drawImage(background, 0, 0, WIDTH, HEIGHT, null);
            } else {
                graphics.setColor(new Color(40, 40, 40));
                graphics.fillRect(0, 0, WIDTH, HEIGHT);
            }
            graphics.setColor(new Color(0, 0, 0, 120));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            long max = Math.max(1, grid.max());
            for (int column = 0; column < WIDTH / CELL; column++) {
                for (int row = 0; row < HEIGHT / CELL; row++) {
                    double heat = Math.sqrt((double) grid.getCount(column, row) / max);
                    if (heat < 0.03) {
                        continue;
                    }
                    graphics.setComposite(AlphaComposite.SrcOver);
                    graphics.setColor(new Color(255, (int) (200 * (1 - heat)), 0, (int) (40 + 200 * heat)));
                    graphics.fillRect(column * CELL, row * CELL, CELL, CELL);
                }
            }

            graphics.setColor(new Color(0, 230, 255, 170));
            for (double[] spot : grid.trapSpots) {
                graphics.fillOval((int) spot[0] - 3, (int) spot[1] - 3, 6, 6);
            }

            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke(2));
            for (int[] building : grid.buildings) {
                graphics.drawRect(building[0], building[1], building[2], building[3]);
            }
            graphics.setFont(new Font("SansSerif", Font.BOLD, 18));
            graphics.drawString(title, 20, 30);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 15));
            graphics.drawString("Red = where zombies spent their time. Cyan dots = where traps started. "
                    + "White = buildings.", 20, 52);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static BufferedImage loadBackground() {
        try {
            URL resource = ZombieHeatmap.class.getResource("/assets/world/Background.png");
            if (resource != null) {
                return ImageIO.read(resource);
            }
            for (String folder : new String[]{"src", "COMP2000/src"}) {
                File file = new File(folder, "assets/world/Background.png");
                if (file.isFile()) {
                    return ImageIO.read(file);
                }
            }
        } catch (IOException exception) {
            // Fall through: the heatmap is still readable on a plain background.
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : 80;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : 1500;
        File output = new File(args.length > 2 ? args[2] : "heatmap.png");

        Grid grid = new ZombieHeatmap(runs, ticks).record();
        String title = "Where zombies spent their time (" + runs + " runs, medics and traps)";
        ImageIO.write(render(grid, title), "png", Objects.requireNonNull(output));

        double nearTraps = grid.trapSpots.stream()
                .mapToDouble(spot -> grid.shareNear(spot[0], spot[1], TRAP_RADIUS))
                .average()
                .orElse(0);
        double areaShare = Math.PI * TRAP_RADIUS * TRAP_RADIUS / (WIDTH * HEIGHT);
        System.out.printf("Wrote %s from %d runs of %d ticks.%n", output, runs, ticks);
        System.out.printf("Zombie time within %.0f px of a trap: %.2f%% per trap (that area is %.2f%% of the map).%n",
                TRAP_RADIUS, nearTraps * 100, areaShare * 100);
    }
}
