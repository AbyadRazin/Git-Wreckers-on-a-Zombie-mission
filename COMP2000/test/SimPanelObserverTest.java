import javax.swing.SwingUtilities;

public final class SimPanelObserverTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            World world = new World(1000, 750);
            world.addEntity(new Food(100, 100, 45));
            SimPanel panel = new SimPanel(world);
            panel.addNotify();
            check(panel.getDisplayedStatistics().getFoodCount() == 1, "Startup snapshot");
            world.addEntity(new Food(200, 100, 45));
            world.publishStatistics();
            check(panel.getDisplayedStatistics().getFoodCount() == 2, "Published changes");
            world.update();
            check(panel.getDisplayedStatistics().getTick() == 1, "Completed tick");
            WorldStatistics snapshot = panel.getDisplayedStatistics();
            panel.removeNotify();
            world.reset();
            world.publishStatistics();
            check(panel.getDisplayedStatistics() == snapshot, "Removed panel unsubscribes");
            panel.addNotify();
            check(panel.getDisplayedStatistics().getFoodCount() == 0, "Reattached panel refreshes");
            world.addEntity(new Food(300, 100, 45));
            world.publishStatistics();
            check(panel.getDisplayedStatistics().getFoodCount() == 1, "Reset population arrives");
            panel.removeNotify();
        });
        System.out.println("SimPanelObserverTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
