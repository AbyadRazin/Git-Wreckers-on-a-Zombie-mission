import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TrapPlacementTest {

    public static void main(String[] args) {
        testTrapIsNearTheEntrance();
        testTrapIsNeverOnABuilding();
        testTrapStaysInsideTheWorld();
        testPositionsAreRandom();
        testStartingTrapsGuardEveryBuilding();
        testNoFreeSpotIsReported();
        testInvalidArguments();
        System.out.println("TrapPlacementTest passed");
    }

    private static void testTrapIsNearTheEntrance() {
        World world = new World(1000, 750);
        Building building = new Building(155, 125, 150, 100);
        world.addEntity(building);
        Random random = new Random(1);

        for (int index = 0; index < 500; index++) {
            Trap trap = Trap.nearEntrance(world, building, random, 2);
            double distance = Math.hypot(trap.getX() - building.getEntranceX(),
                    trap.getY() - building.getEntranceY());
            check(distance >= 15 && distance <= 55, "trap is 15 to 55 units from the entrance");
            check(trap.getMaxUses() == 2, "trap gets the requested uses");
        }
    }

    private static void testTrapIsNeverOnABuilding() {
        World world = new World(1000, 750);
        Building guarded = new Building(155, 125, 150, 100);
        // A second building right next to the entrance blocks part of the area.
        Building neighbour = new Building(240, 230, 100, 60);
        world.addEntity(guarded);
        world.addEntity(neighbour);
        Random random = new Random(2);

        for (int index = 0; index < 500; index++) {
            Trap trap = Trap.nearEntrance(world, guarded, random, 2);
            check(!guarded.containsPoint(trap.getX(), trap.getY()), "not on the guarded building");
            check(!neighbour.containsPoint(trap.getX(), trap.getY()), "not on any other building");
        }
    }

    private static void testTrapStaysInsideTheWorld() {
        // The building sits against the left edge of the world.
        World world = new World(400, 300);
        Building building = new Building(0, 100, 60, 100);
        world.addEntity(building);
        Random random = new Random(3);

        for (int index = 0; index < 500; index++) {
            Trap trap = Trap.nearEntrance(world, building, random, 2);
            check(trap.getX() >= 14 && trap.getX() <= 386
                            && trap.getY() >= 14 && trap.getY() <= 286,
                    "the whole trap picture stays inside the world");
        }
    }

    private static void testPositionsAreRandom() {
        World world = new World(1000, 750);
        Building building = new Building(155, 125, 150, 100);
        world.addEntity(building);
        Random random = new Random(4);

        long distinct = IntStream.range(0, 20)
                .mapToObj(attempt -> Trap.nearEntrance(world, building, random, 2))
                .map(trap -> Math.round(trap.getX()) + "," + Math.round(trap.getY()))
                .distinct()
                .count();
        check(distinct > 15, "traps land in different spots each time");
    }

    private static void testStartingTrapsGuardEveryBuilding() {
        World world = new World(1000, 750);
        Main.populateWorld(world, new Random(5));
        List<Building> buildings = entitiesOf(world, Building.class);
        List<Trap> traps = entitiesOf(world, Trap.class);

        check(traps.size() == 3, "three starting traps");
        for (Building building : buildings) {
            boolean guarded = traps.stream().anyMatch(trap -> Math.hypot(
                    trap.getX() - building.getEntranceX(),
                    trap.getY() - building.getEntranceY()) <= 55);
            check(guarded, "every building has a trap near its entrance");
        }
        check(traps.stream().noneMatch(trap -> buildings.stream()
                        .anyMatch(building -> building.containsPoint(trap.getX(), trap.getY()))),
                "no starting trap is on a building");
    }

    private static void testNoFreeSpotIsReported() {
        // The building covers the whole world, so there is nowhere to put a trap.
        World world = new World(200, 200);
        Building building = new Building(0, 0, 200, 200);
        world.addEntity(building);
        try {
            Trap.nearEntrance(world, building, new Random(6), 2);
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalStateException when there is no free spot");
    }

    private static void testInvalidArguments() {
        World world = new World(1000, 750);
        Building building = new Building(155, 125, 150, 100);
        world.addEntity(building);
        expectException(IllegalArgumentException.class,
                () -> Trap.nearEntrance(world, building, new Random(7), 0));
        expectException(NullPointerException.class,
                () -> Trap.nearEntrance(world, null, new Random(7), 2));
    }

    private static <T extends Entity> List<T> entitiesOf(World world, Class<T> type) {
        return world.getEntities().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    private static void expectException(Class<? extends RuntimeException> type, Runnable action) {
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
