import java.awt.Graphics2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class WorldObserverTest {

    public static void main(String[] args) throws Exception {
        testStatisticsAreActiveTypedValues();
        testCompletedTickIsPublishedToAllObservers();
        testSubscriptionsChangeOnNextPublication();
        testMainPublishesPopulatedStartupAndReset();
        testFailingObserverDoesNotHideTheCompletedTick();
        testObserversCannotMutateTheWorldLifecycle();
        testNullObserversAreRejected();
        System.out.println("WorldObserverTest passed");
    }

    private static void testStatisticsAreActiveTypedValues() {
        World world = new World(1000, 750);
        checkCounts(world.getStatistics(), 0, 0, 0, 0, 0);
        world.addEntity(new Human(100, 100));
        world.addEntity(new Military(300, 300));
        world.addEntity(new Zombie(600, 600));
        world.addEntity(new Food(200, 200, 45));
        world.addEntity(new Building(100, 400, 150, 100));
        Human inactive = new Human(500, 500);
        inactive.deactivate();
        world.addEntity(inactive);

        WorldStatistics captured = world.getStatistics();
        checkCounts(captured, 0, 1, 1, 1, 1);
        world.reset();
        checkCounts(world.getStatistics(), 0, 0, 0, 0, 0);
        checkCounts(captured, 0, 1, 1, 1, 1);
    }

    private static void testCompletedTickIsPublishedToAllObservers() {
        World world = new World(1000, 750);
        Food oldFood = new Food(100, 100, 45);
        world.addEntity(oldFood);
        world.addEntity(new TestEntity(current -> {
            if (current.getTick() == 24) {
                oldFood.deactivate();
            }
        }));
        List<WorldStatistics> first = new ArrayList<>();
        List<WorldStatistics> second = new ArrayList<>();
        world.addObserver(first::add);
        world.addObserver(second::add);
        check(first.isEmpty(), "Registration should wait for a publication");

        for (int index = 0; index < 25; index++) {
            world.update();
        }
        check(first.size() == 25 && second.size() == 25,
                "Each completed tick should be published once");
        for (int index = 0; index < first.size(); index++) {
            check(first.get(index) == second.get(index),
                    "Observers should receive the same immutable snapshot");
            check(first.get(index).getTick() == index + 1,
                    "Published tick numbers should match completed updates");
        }
        checkCounts(first.get(24), 25, 0, 0, 0, 1);
        check(!world.getEntities().contains(oldFood),
                "Notification should follow removal and periodic spawning");
    }

    private static void testSubscriptionsChangeOnNextPublication() {
        World world = new World(1000, 750);
        List<String> calls = new ArrayList<>();
        WorldObserver second = statistics -> calls.add("second");
        WorldObserver third = statistics -> calls.add("third");
        WorldObserver[] first = new WorldObserver[1];
        first[0] = statistics -> {
            calls.add("first");
            world.removeObserver(first[0]);
            world.removeObserver(second);
            world.addObserver(third);
        };
        world.addObserver(first[0]);
        world.addObserver(first[0]);
        world.addObserver(second);
        world.publishStatistics();
        check(calls.equals(List.of("first", "second")),
                "Duplicates should be ignored and publication should use a subscription snapshot");
        calls.clear();
        world.publishStatistics();
        check(calls.equals(List.of("third")),
                "Subscription changes should take effect on the next publication");
        world.removeObserver(third);
        world.removeObserver(third);
        calls.clear();
        world.update();
        check(calls.isEmpty(), "Unsubscribed observers should receive no more updates");
    }

    private static void testMainPublishesPopulatedStartupAndReset() throws Exception {
        World world = new World(1000, 750);
        List<WorldStatistics> samples = new ArrayList<>();
        world.addObserver(samples::add);
        Method populate = Main.class.getDeclaredMethod("populateWorld", World.class);
        populate.setAccessible(true);
        populate.invoke(null, world);
        check(samples.size() == 1, "Setup should publish one complete scenario");
        checkCounts(samples.get(0), 0, 3, 2, 1, 3);
        world.update();
        world.reset();
        check(samples.size() == 2, "Clearing before repopulation should not publish an empty scenario");
        populate.invoke(null, world);
        check(samples.size() == 3, "Subscriptions should survive reset without duplication");
        checkCounts(samples.get(2), 0, 3, 2, 1, 3);
    }

    private static void testFailingObserverDoesNotHideTheCompletedTick() {
        World world = new World(1000, 750);
        RuntimeException cause = new IllegalArgumentException("Observer failed");
        WorldObserver failing = statistics -> { throw cause; };
        List<WorldStatistics> delivered = new ArrayList<>();
        world.addObserver(failing);
        world.addObserver(delivered::add);
        IllegalStateException failure = expectException(
                IllegalStateException.class, world::update);
        check(failure.getCause() == cause, "Notification failure should preserve its cause");
        check(world.getTick() == 1 && delivered.size() == 1,
                "A callback failure must not roll back the tick or skip later observers");
        world.removeObserver(failing);
        world.update();
        check(world.getTick() == 2 && delivered.size() == 2,
                "Notification failure should not leave the world locked");
    }

    private static void testObserversCannotMutateTheWorldLifecycle() {
        World world = new World(1000, 750);
        world.addEntity(new TestEntity(current -> { }));
        List<Runnable> mutations = List.of(world::update, world::reset,
                world::publishStatistics, world::removeInactiveEntities,
                () -> world.addEntity(new TestEntity(current -> { })));
        for (Runnable mutation : mutations) {
            WorldObserver mutating = statistics -> mutation.run();
            List<WorldStatistics> delivered = new ArrayList<>();
            WorldObserver recorder = delivered::add;
            world.addObserver(mutating);
            world.addObserver(recorder);
            long tickBefore = world.getTick();
            IllegalStateException failure = expectException(
                    IllegalStateException.class, world::update);
            check(failure.getCause() instanceof IllegalStateException,
                    "Reentrant world mutation should fail explicitly");
            check(world.getTick() == tickBefore + 1 && world.getEntities().size() == 1
                            && delivered.size() == 1,
                    "Callbacks must not reset, advance or structurally change the committed world");
            world.removeObserver(mutating);
            world.removeObserver(recorder);
        }
    }

    private static void testNullObserversAreRejected() {
        World world = new World(1000, 750);
        expectException(NullPointerException.class, () -> world.addObserver(null));
        expectException(NullPointerException.class, () -> world.removeObserver(null));
    }

    private static void checkCounts(WorldStatistics statistics, long tick,
                                    long humans, long military, long zombies, long food) {
        check(statistics.getTick() == tick && statistics.getHumanCount() == humans
                        && statistics.getMilitaryCount() == military
                        && statistics.getZombieCount() == zombies && statistics.getFoodCount() == food,
                "Statistics should retain the active population values for that tick");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static <T extends RuntimeException> T expectException(Class<T> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (type.isInstance(exception)) {
                return type.cast(exception);
            }
            throw new AssertionError("Unexpected exception type", exception);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static final class TestEntity extends Entity {
        private final Consumer<World> action;

        private TestEntity(Consumer<World> action) {
            super(10, 10);
            this.action = action;
        }

        @Override
        public void update(World world) {
            action.accept(world);
        }

        @Override
        public void draw(Graphics2D graphics) { }
    }
}
