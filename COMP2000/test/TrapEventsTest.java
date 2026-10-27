import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class TrapEventsTest {

    public static void main(String[] args) {
        testListenerHearsTheCatch();
        testEveryListenerIsTold();
        testRemovedListenerHearsNothing();
        testSameListenerAddedTwiceIsToldOnce();
        testListenerCanUnsubscribeWhileBeingTold();
        testNoCatchNoEvent();
        testListenToAllCoversEveryTrap();
        testReportKeepsCatchesAfterTrapWearsOut();
        testReportReset();
        testBlastsAppearAndFade();
        testNullListenersRejected();
        System.out.println("TrapEventsTest passed");
    }

    private static void testListenerHearsTheCatch() {
        World world = new World(500, 500);
        Trap trap = new Trap(100, 100, 2);
        Zombie zombie = new Zombie(100, 100);
        world.addEntity(trap);
        world.addEntity(zombie);

        List<Zombie> heard = new ArrayList<>();
        trap.addTrapListener((source, caught) -> {
            check(source == trap, "told which trap went off");
            heard.add(caught);
        });
        trap.update(world);

        check(heard.equals(List.of(zombie)), "told about the zombie it caught");
        check(!zombie.isActive(), "the zombie is already out of play when listeners hear about it");
    }

    private static void testEveryListenerIsTold() {
        World world = worldWithTrapAndZombies(2);
        Trap trap = firstTrap(world);
        TrapReport report = new TrapReport();
        TrapBlasts blasts = new TrapBlasts(() -> 0);
        trap.addTrapListener(report);
        trap.addTrapListener(blasts);

        trap.update(world);

        check(report.getCatches() == 2, "the counter hears both catches");
        check(blasts.getActiveCount() == 2, "the blast effect hears both catches too");
    }

    private static void testRemovedListenerHearsNothing() {
        World world = worldWithTrapAndZombies(1);
        Trap trap = firstTrap(world);
        TrapReport report = new TrapReport();
        trap.addTrapListener(report);
        trap.removeTrapListener(report);

        trap.update(world);

        check(report.getCatches() == 0, "an unsubscribed listener is not told");
    }

    private static void testSameListenerAddedTwiceIsToldOnce() {
        World world = worldWithTrapAndZombies(1);
        Trap trap = firstTrap(world);
        TrapReport report = new TrapReport();
        trap.addTrapListener(report);
        trap.addTrapListener(report);

        trap.update(world);

        check(report.getCatches() == 1, "duplicates are ignored");
    }

    private static void testListenerCanUnsubscribeWhileBeingTold() {
        World world = worldWithTrapAndZombies(2);
        Trap trap = firstTrap(world);
        AtomicLong calls = new AtomicLong();
        TrapListener oneShot = new TrapListener() {
            @Override
            public void onZombieCaught(Trap source, Zombie zombie) {
                calls.incrementAndGet();
                source.removeTrapListener(this);
            }
        };
        TrapReport report = new TrapReport();
        trap.addTrapListener(oneShot);
        trap.addTrapListener(report);

        trap.update(world);

        check(calls.get() == 1, "a listener that leaves after the first catch hears only that one");
        check(report.getCatches() == 2, "the other listener still hears both");
    }

    private static void testNoCatchNoEvent() {
        World world = new World(500, 500);
        Trap trap = new Trap(100, 100, 2);
        world.addEntity(trap);
        world.addEntity(new Zombie(300, 300));
        world.addEntity(new Human(100, 100));
        TrapReport report = new TrapReport();
        trap.addTrapListener(report);

        trap.update(world);

        check(report.getCatches() == 0, "far-away zombies and humans don't set it off");
    }

    private static void testListenToAllCoversEveryTrap() {
        World world = new World(500, 500);
        world.addEntity(new Trap(100, 100, 1));
        world.addEntity(new Trap(300, 300, 1));
        world.addEntity(new Zombie(100, 100));
        world.addEntity(new Zombie(300, 300));
        world.addEntity(new Human(50, 50));
        TrapReport report = new TrapReport();

        Trap.listenToAll(world, report);
        Trap.listenToAll(world, report);
        world.getEntities().stream()
                .filter(Trap.class::isInstance)
                .forEach(trap -> trap.update(world));

        check(report.getCatches() == 2, "both traps report, once each, even after subscribing twice");
    }

    private static void testReportKeepsCatchesAfterTrapWearsOut() {
        // Two zombies use up both of the trap's uses.
        World world = worldWithTrapAndZombies(2);
        Trap trap = firstTrap(world);
        TrapReport report = new TrapReport();
        Trap.listenToAll(world, report);

        trap.update(world);
        world.removeInactiveEntities();

        check(world.getEntities().stream().noneMatch(Trap.class::isInstance),
                "the worn-out trap has left the world");
        check(report.getCatches() == 2, "but its catches are still counted");
    }

    private static void testReportReset() {
        TrapReport report = new TrapReport();
        report.onZombieCaught(new Trap(0, 0, 1), new Zombie(0, 0));
        report.onZombieCaught(new Trap(0, 0, 1), new Zombie(0, 0));
        check(report.getCatches() == 2, "counts each catch");
        report.reset();
        check(report.getCatches() == 0, "reset starts from zero");
    }

    private static void testBlastsAppearAndFade() {
        AtomicLong now = new AtomicLong(1_000);
        TrapBlasts blasts = new TrapBlasts(now::get);
        blasts.onZombieCaught(new Trap(50, 50, 1), new Zombie(50, 50));
        check(blasts.getActiveCount() == 1, "a catch starts a blast");

        now.addAndGet(300);
        check(blasts.getActiveCount() == 1, "still showing part-way through");

        now.addAndGet(300);
        check(blasts.getActiveCount() == 0, "gone once it has faded out");

        blasts.onZombieCaught(new Trap(50, 50, 1), new Zombie(50, 50));
        blasts.clear();
        check(blasts.getActiveCount() == 0, "clear removes every blast");
    }

    private static void testNullListenersRejected() {
        Trap trap = new Trap(0, 0, 1);
        expectNullPointer(() -> trap.addTrapListener(null));
        expectNullPointer(() -> Trap.listenToAll(new World(100, 100), null));
    }

    /** A 2-use trap with {@code zombies} zombies standing on it. */
    private static World worldWithTrapAndZombies(int zombies) {
        World world = new World(500, 500);
        world.addEntity(new Trap(100, 100, 2));
        for (int index = 0; index < zombies; index++) {
            world.addEntity(new Zombie(100 + index * 5, 100));
        }
        return world;
    }

    private static Trap firstTrap(World world) {
        return world.getEntities().stream()
                .filter(Trap.class::isInstance)
                .map(Trap.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static void expectNullPointer(Runnable action) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("Expected NullPointerException");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
