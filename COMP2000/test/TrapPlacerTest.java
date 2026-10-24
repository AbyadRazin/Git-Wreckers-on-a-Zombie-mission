import java.util.List;

public final class TrapPlacerTest {

    public static void main(String[] args) {
        testStartsFullAndNotPlacing();
        testClickWithoutButtonDoesNothing();
        testButtonThenClickPlacesOneTrap();
        testButtonTwiceCancels();
        testSupplyRunsOut();
        testResetRefillsSupply();
        testListenerHearsEveryChange();
        testInvalidArguments();
        System.out.println("TrapPlacerTest passed");
    }

    private static void testStartsFullAndNotPlacing() {
        TrapPlacer placer = new TrapPlacer(new World(500, 500), 3);
        check(placer.getTrapsLeft() == 3, "starts with the full supply");
        check(!placer.isArmed(), "not in placing mode at the start");
    }

    private static void testClickWithoutButtonDoesNothing() {
        World world = new World(500, 500);
        TrapPlacer placer = new TrapPlacer(world, 3);
        check(!placer.placeAt(100, 100), "a click alone places nothing");
        check(world.getEntities().isEmpty(), "no trap added");
        check(placer.getTrapsLeft() == 3, "supply untouched");
    }

    private static void testButtonThenClickPlacesOneTrap() {
        World world = new World(500, 500);
        TrapPlacer placer = new TrapPlacer(world, 3);
        placer.toggleArmed();
        check(placer.isArmed(), "button switches placing mode on");

        check(placer.placeAt(120, 80), "the click places a trap");
        List<Entity> entities = world.getEntities();
        check(entities.size() == 1 && entities.get(0) instanceof Trap, "a trap is in the world");
        check(entities.get(0).getX() == 120 && entities.get(0).getY() == 80, "where the user clicked");
        check(placer.getTrapsLeft() == 2, "one trap used from the supply");
        check(!placer.isArmed(), "placing mode switches off after one trap");
        check(!placer.placeAt(200, 200), "a second click needs the button again");
    }

    private static void testButtonTwiceCancels() {
        TrapPlacer placer = new TrapPlacer(new World(500, 500), 3);
        placer.toggleArmed();
        placer.toggleArmed();
        check(!placer.isArmed(), "pressing the button again cancels");
        check(placer.getTrapsLeft() == 3, "cancelling costs nothing");
    }

    private static void testSupplyRunsOut() {
        World world = new World(500, 500);
        TrapPlacer placer = new TrapPlacer(world, 3);
        for (int index = 0; index < 3; index++) {
            placer.toggleArmed();
            placer.placeAt(50 + index * 50, 50);
        }
        check(placer.getTrapsLeft() == 0 && world.getEntities().size() == 3, "all three placed");
        placer.toggleArmed();
        check(!placer.isArmed(), "no placing mode once the supply is empty");
    }

    private static void testResetRefillsSupply() {
        TrapPlacer placer = new TrapPlacer(new World(500, 500), 3);
        placer.toggleArmed();
        placer.placeAt(10, 10);
        placer.toggleArmed();
        placer.reset();
        check(placer.getTrapsLeft() == 3 && !placer.isArmed(), "reset refills and stops placing");
    }

    private static void testListenerHearsEveryChange() {
        TrapPlacer placer = new TrapPlacer(new World(500, 500), 3);
        int[] calls = {0};
        placer.setOnChange(() -> calls[0]++);
        placer.toggleArmed();
        placer.placeAt(10, 10);
        placer.reset();
        check(calls[0] == 3, "button, placement and reset each notify, got " + calls[0]);
        placer.placeAt(10, 10);
        check(calls[0] == 3, "a click that places nothing does not notify");
    }

    private static void testInvalidArguments() {
        expect(IllegalArgumentException.class, () -> new TrapPlacer(new World(500, 500), 0));
        expect(NullPointerException.class, () -> new TrapPlacer(null, 3));
        expect(NullPointerException.class, () -> new TrapPlacer(new World(500, 500), 3).setOnChange(null));
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (type.isInstance(exception)) {
                return;
            }
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + exception);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
