import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MainSpawnTest {
    public static void main(String[] args) throws Exception {
        testSeededInitialLayoutsRepeatAndVary();
        testStaticSceneAndPopulationsArePreserved();
        testSpawnBoundsAndMilitaryPatrolProgress();
        testResetPublishesOneCompleteFreshLayout();
        System.out.println("MainSpawnTest passed");
    }

    private static void testSeededInitialLayoutsRepeatAndVary() throws Exception {
        World first = populated(new Random(13));
        World repeat = populated(new Random(13));
        World different = populated(new Random(29));
        check(positions(first).equals(positions(repeat)),
                "A controlled random input should reproduce the initial layout");
        List<String> firstPositions = positions(first);
        List<String> differentPositions = positions(different);
        check(firstPositions.size() == 8 && differentPositions.size() == 8,
                "The initial mobile population should stay unchanged");
        for (int index = 0; index < firstPositions.size(); index++) {
            check(!firstPositions.get(index).equals(differentPositions.get(index)),
                    "Every mobile agent should use the selected random input");
        }
    }

    private static void testStaticSceneAndPopulationsArePreserved() throws Exception {
        World world = populated(new Random(7));
        checkPopulation(world.getStatistics());
        List<String> stationary = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Building) {
                Building building = (Building) entity;
                stationary.add("Building:" + building.getX() + ":" + building.getY());
                check(building.getWidth() == 150 && building.getHeight() == 100,
                        "Existing shelter dimensions should be preserved");
            } else if (entity instanceof Food) {
                stationary.add("Food:" + entity.getX() + ":" + entity.getY());
                check(((Food) entity).getEnergyValue() == 45,
                        "Initial food should retain its energy value");
            }
        }
        check(stationary.equals(List.of("Building:155.0:125.0", "Building:580.0:490.0",
                        "Food:250.0:300.0", "Food:500.0:600.0", "Food:850.0:150.0")),
                "Random mobile starts should not alter existing buildings or food");
    }

    private static void testSpawnBoundsAndMilitaryPatrolProgress() throws Exception {
        for (double value : new double[]{0.0, Math.nextDown(1.0)}) {
            World world = populated(new FixedRandom(value));
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Human || entity instanceof Zombie) {
                    check(entity.getX() >= 80 && entity.getX() <= world.getWidth() - 80
                                    && entity.getY() >= 80 && entity.getY() <= world.getHeight() - 80,
                            "Mobile starts should retain a safe margin from world edges");
                }
                if (entity instanceof Military) {
                    Military military = (Military) entity;
                    double initialY = military.getY();
                    World patrolWorld = new World(1000, 750);
                    patrolWorld.addEntity(military);
                    for (int tick = 0; tick < 130; tick++) {
                        military.update(patrolWorld);
                    }
                    check(Math.abs(military.getY() - initialY) > 10,
                            "Extreme random starts must leave the first patrol waypoint reachable");
                }
            }
        }
    }

    private static void testResetPublishesOneCompleteFreshLayout() throws Exception {
        World world = new World(1000, 750);
        List<WorldStatistics> samples = new ArrayList<>();
        world.addObserver(samples::add);
        populate(world, new Random(13));
        List<String> first = positions(world);
        check(samples.size() == 1, "Initial setup should publish once");
        checkPopulation(samples.get(0));
        world.reset();
        check(samples.size() == 1, "Reset must not publish an empty scenario");
        populate(world, new Random(29));
        check(samples.size() == 2, "Repopulation should publish once after reset");
        checkPopulation(samples.get(1));
        check(!first.equals(positions(world)), "Reset should use the new random layout");
    }

    private static World populated(Random random) throws Exception {
        World world = new World(1000, 750);
        populate(world, random);
        return world;
    }

    private static void populate(World world, Random random) throws Exception {
        Method populate = Main.class.getDeclaredMethod("populateWorld", World.class, Random.class);
        populate.setAccessible(true);
        populate.invoke(null, world, random);
    }

    private static List<String> positions(World world) {
        List<String> result = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Human || entity instanceof Zombie) {
                result.add(entity.getClass().getSimpleName() + ":" + entity.getX() + ":" + entity.getY());
            }
        }
        return result;
    }

    private static void checkPopulation(WorldStatistics statistics) {
        check(statistics.getTick() == 0 && statistics.getHumanCount() == 3
                        && statistics.getMilitaryCount() == 2 && statistics.getZombieCount() == 1
                        && statistics.getFoodCount() == 3,
                "Setup and reset should retain the original complete population");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FixedRandom extends Random {
        private final double value;

        private FixedRandom(double value) {
            super(0);
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }
}
