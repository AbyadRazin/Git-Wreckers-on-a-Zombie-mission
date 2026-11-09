import java.awt.image.BufferedImage;
import java.util.List;

public final class ZombieHeatmapTest {

    public static void main(String[] args) {
        testRecordCountsTheRightCell();
        testPositionsOffTheMapCountAtTheEdge();
        testMergeAddsEverything();
        testMergeOrderDoesNotMatter();
        testShareNear();
        testSmallRecordingAndRender();
        testInvalidArguments();
        System.out.println("ZombieHeatmapTest passed");
    }

    private static void testRecordCountsTheRightCell() {
        ZombieHeatmap.Grid grid = new ZombieHeatmap.Grid();
        grid.record(15, 27);
        grid.record(19, 21);
        check(grid.getCount(1, 2) == 2 && grid.getTotal() == 2, "both land in cell (1, 2)");
    }

    private static void testPositionsOffTheMapCountAtTheEdge() {
        ZombieHeatmap.Grid grid = new ZombieHeatmap.Grid();
        grid.record(-50, -50);
        grid.record(5000, 5000);
        check(grid.getCount(0, 0) == 1, "off the top-left counts in the corner cell");
        check(grid.getCount(ZombieHeatmap.WIDTH / ZombieHeatmap.CELL - 1,
                ZombieHeatmap.HEIGHT / ZombieHeatmap.CELL - 1) == 1, "off the bottom-right too");
    }

    private static void testMergeAddsEverything() {
        ZombieHeatmap.Grid first = new ZombieHeatmap.Grid();
        first.record(5, 5);
        first.addTrapSpot(100, 100);
        ZombieHeatmap.Grid second = new ZombieHeatmap.Grid();
        second.record(5, 5);
        second.record(500, 500);
        second.addTrapSpot(200, 200);

        first.merge(second);

        check(first.getCount(0, 0) == 2 && first.getCount(50, 50) == 1, "counts are added cell by cell");
        check(first.getTotal() == 3 && first.getTrapSpotCount() == 2, "totals and trap spots are kept");
    }

    private static void testMergeOrderDoesNotMatter() {
        List<ZombieHeatmap.Grid> grids = List.of(gridAt(5, 5), gridAt(105, 5), gridAt(5, 105), gridAt(5, 5));
        ZombieHeatmap.Grid sequential = grids.stream().collect(ZombieHeatmap.Grid.collector());
        ZombieHeatmap.Grid parallel = List.of(gridAt(5, 5), gridAt(105, 5), gridAt(5, 105), gridAt(5, 5))
                .parallelStream().collect(ZombieHeatmap.Grid.collector());
        check(sequential.getTotal() == 4 && parallel.getTotal() == 4, "every grid is counted once");
        check(sequential.getCount(0, 0) == 2 && parallel.getCount(0, 0) == 2,
                "parallel and sequential collection agree");
    }

    private static void testShareNear() {
        ZombieHeatmap.Grid grid = new ZombieHeatmap.Grid();
        grid.record(105, 105);
        grid.record(105, 105);
        grid.record(905, 705);
        grid.record(905, 705);
        check(Math.abs(grid.shareNear(105, 105, 15) - 0.5) < 1e-9, "half the zombie time was near (105, 105)");
        check(new ZombieHeatmap.Grid().shareNear(0, 0, 10) == 0, "an empty grid has no share anywhere");
    }

    private static void testSmallRecordingAndRender() {
        ZombieHeatmap.Grid grid = new ZombieHeatmap(2, 20).record();
        check(grid.getTotal() > 0, "zombies were recorded");
        check(grid.getTrapSpotCount() == 2 * 3, "three trap spots per run");
        BufferedImage image = ZombieHeatmap.render(grid, "test");
        check(image.getWidth() == ZombieHeatmap.WIDTH && image.getHeight() == ZombieHeatmap.HEIGHT,
                "the picture is the size of the map");
    }

    private static void testInvalidArguments() {
        try {
            new ZombieHeatmap(0, 10);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static ZombieHeatmap.Grid gridAt(double x, double y) {
        ZombieHeatmap.Grid grid = new ZombieHeatmap.Grid();
        grid.record(x, y);
        return grid;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
