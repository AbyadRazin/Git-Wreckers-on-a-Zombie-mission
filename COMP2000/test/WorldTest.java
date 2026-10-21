import java.awt.Graphics2D;
import java.util.List;
import java.util.function.Consumer;

public final class WorldTest {

    public static void main(String[] args) {
        testEntityStorageAndReadOnlyView();
        testNearbyBoundariesAndOrdering();
        testTickAndInactiveEntities();
        testEntityDeactivatedBeforeItsTurnIsSkipped();
        testEntityAddedDuringUpdateWaitsForNextTick();
        testPeriodicSpawning();
        testResetRestartsTheSchedule();
        testInvalidArguments();
        System.out.println("WorldTest passed");
    }

    private static void testEntityStorageAndReadOnlyView() {
        World world = new World(100, 80);
        List<Entity> view = world.getEntities();
        CountingEntity entity = new CountingEntity(10, 10);
        world.addEntity(entity);
        check(world.getWidth() == 100 && world.getHeight() == 80,
                "World dimensions should be retained");
        check(view.equals(List.of(entity)), "Entity view should reflect additions");
        expectException(UnsupportedOperationException.class, () -> view.clear());
        check(world.getEntities().contains(entity), "Read-only view must protect storage");
    }

    private static void testNearbyBoundariesAndOrdering() {
        World world = new World(100, 80);
        CountingEntity source = new CountingEntity(10, 10);
        CountingEntity boundary = new CountingEntity(13, 14);
        CountingEntity coincident = new CountingEntity(10, 10);
        CountingEntity far = new CountingEntity(16, 10);
        CountingEntity inactive = new CountingEntity(11, 10);
        inactive.deactivate();
        world.addEntity(source);
        world.addEntity(boundary);
        world.addEntity(inactive);
        world.addEntity(far);
        world.addEntity(coincident);

        check(world.getNearby(source, 5).equals(List.of(boundary, coincident)),
                "Query should include the radius boundary, preserve order and skip source/inactive entities");
        check(world.getNearby(source, 0).equals(List.of(coincident)),
                "Zero radius should include a distinct entity at the same position");
    }

    private static void testTickAndInactiveEntities() {
        World world = new World(100, 80);
        CountingEntity active = new CountingEntity(10, 10);
        CountingEntity inactive = new CountingEntity(20, 20);
        inactive.deactivate();
        world.addEntity(active);
        world.addEntity(inactive);
        world.update();

        check(active.updates == 1 && active.lastObservedTick == 0,
                "Entities should update once before the tick counter advances");
        check(inactive.updates == 0, "Inactive entities should not update");
        check(world.getTick() == 1, "A completed update should advance the tick once");
        check(world.getEntities().equals(List.of(active)),
                "Inactive entities should be removed after updating");
        active.deactivate();
        world.removeInactiveEntities();
        check(world.getEntities().isEmpty(), "Explicit inactive removal should retain no dead entities");
    }

    private static void testEntityDeactivatedBeforeItsTurnIsSkipped() {
        World world = new World(100, 80);
        CountingEntity victim = new CountingEntity(20, 20);
        CountingEntity first = new CountingEntity(10, 10, current -> victim.deactivate());
        world.addEntity(first);
        world.addEntity(victim);
        world.update();

        check(victim.updates == 0, "An entity killed earlier in the tick must not take its turn");
        check(world.getEntities().equals(List.of(first)), "Victim should be removed in the same tick");
    }

    private static void testEntityAddedDuringUpdateWaitsForNextTick() {
        World world = new World(100, 80);
        CountingEntity child = new CountingEntity(20, 20);
        CountingEntity parent = new CountingEntity(10, 10, current -> {
            if (!current.getEntities().contains(child)) {
                current.addEntity(child);
            }
        });
        world.addEntity(parent);
        world.update();
        check(world.getEntities().contains(child), "Updates may add entities safely");
        check(child.updates == 0, "New entities must wait until the next tick");
        world.update();
        check(parent.updates == 2 && child.updates == 1,
                "Each entity in the next tick snapshot should update once");
    }

    private static void testPeriodicSpawning() {
        World world = new World(100, 80);
        advanceTo(world, 24);
        check(world.getEntities().isEmpty(), "Nothing should spawn before tick 25");
        world.update();
        check(count(world, Food.class) == 1 && count(world, Human.class) == 0,
                "Only food should spawn at tick 25");
        advanceTo(world, 99);
        check(count(world, Food.class) == 3 && count(world, Human.class) == 0,
                "Human spawning should wait until tick 100");
        world.update();
        check(count(world, Food.class) == 4 && count(world, Human.class) == 1,
                "Food and one human should spawn at tick 100");
        for (Entity entity : world.getEntities()) {
            check(entity.getX() >= 0 && entity.getX() < world.getWidth()
                            && entity.getY() >= 0 && entity.getY() < world.getHeight(),
                    "Periodic spawns should remain inside the world bounds");
            if (entity instanceof Human) {
                check(((Human) entity).getEnergy() == 100.0,
                        "A newly spawned human should not update in its spawn tick");
            }
        }
    }

    private static void testResetRestartsTheSchedule() {
        World world = new World(100, 80);
        world.addEntity(new CountingEntity(10, 10));
        advanceTo(world, 25);
        world.reset();
        check(world.getTick() == 0 && world.getEntities().isEmpty(),
                "Reset should clear entities and return to tick zero");
        advanceTo(world, 24);
        check(world.getEntities().isEmpty(), "Reset should restart the spawn interval");
        world.update();
        check(count(world, Food.class) == 1, "Food spawning should resume at the new tick 25");
    }

    private static void testInvalidArguments() {
        expectException(IllegalArgumentException.class, () -> new World(0, 80));
        expectException(IllegalArgumentException.class, () -> new World(100, -1));
        World world = new World(100, 80);
        CountingEntity source = new CountingEntity(10, 10);
        expectException(IllegalArgumentException.class, () -> world.getNearby(source, -1));
        expectException(NullPointerException.class, () -> world.getNearby(null, 1));
        expectException(NullPointerException.class, () -> world.addEntity(null));
    }

    private static void advanceTo(World world, long tick) {
        while (world.getTick() < tick) {
            world.update();
        }
    }

    private static long count(World world, Class<? extends Entity> type) {
        return world.getEntities().stream().filter(type::isInstance).count();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectException(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (type.isInstance(exception)) {
                return;
            }
            throw new AssertionError("Unexpected exception type", exception);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static final class CountingEntity extends Entity {
        private final Consumer<World> action;
        private int updates;
        private long lastObservedTick = -1;

        private CountingEntity(double x, double y) {
            this(x, y, world -> { });
        }

        private CountingEntity(double x, double y, Consumer<World> action) {
            super(x, y);
            this.action = action;
        }

        @Override
        public void update(World world) {
            updates++;
            lastObservedTick = world.getTick();
            action.accept(world);
        }

        @Override
        public void draw(Graphics2D graphics) { }
    }
}
