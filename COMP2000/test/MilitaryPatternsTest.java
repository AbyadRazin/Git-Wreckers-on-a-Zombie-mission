import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Checks the Military patterns: Decorator, Iterator, State, lambdas, collector. */
public final class MilitaryPatternsTest {

    public static void main(String[] args) throws Exception {
        testDecoratorsStackAndKeepStatsIndependent();
        testDecoratorRejectsNullWeapon();
        testRouteCyclesAndIsDefensivelyCopied();
        testRouteRejectsEmptyWaypoints();
        testPatrolLambdaVisitsWaypointsInOrder();
        testStateMachinePatrolEngageReload();
        testSoldierReturnsToPatrolWhenThreatIsGone();
        testShootThrowsWithoutATargetInRange();
        testThreatPrioritisationProtectsCivilians();
        testKillsPromoteTheWeaponWithDecorators();
        testBattleReportCollectsStatistics();
        testBattleReportIsIdenticalSequentialAndParallel();
        testBattleReportOfEmptyWorldIsZero();
        testInfectedHumanRisesAsZombie();
        System.out.println("MilitaryPatternsTest passed");
    }

    // ---- Decorator --------------------------------------------------------

    private static void testDecoratorsStackAndKeepStatsIndependent() {
        Weapon rifle = new Rifle();
        Weapon scoped = new ScopeDecorator(rifle);
        Weapon both = new ExtendedMagazineDecorator(scoped);

        check(rifle.range() == 140.0 && rifle.magazineSize() == 6, "base rifle stats");
        check(scoped.range() == 210.0, "scope adds range");
        check(scoped.fireIntervalTicks() == 15, "scope slows the fire rate");
        check(scoped.magazineSize() == 6, "scope leaves the magazine alone");
        check(both.range() == 210.0, "outer decorator forwards range");
        check(both.magazineSize() == 12 && both.reloadTicks() == 42,
                "extended magazine adds rounds and reload time");
        check("Rifle + Scope + Extended Magazine".equals(both.describe()),
                "description lists the stack, got " + both.describe());
        check(rifle.range() == 140.0, "decorating never mutates the wrapped weapon");
    }

    private static void testDecoratorRejectsNullWeapon() {
        expectThrows(NullPointerException.class, () -> new ScopeDecorator(null),
                "null weapon rejected");
    }

    // ---- Iterator ---------------------------------------------------------

    private static void testRouteCyclesAndIsDefensivelyCopied() {
        List<Point2D> points = new ArrayList<>(Arrays.asList(
                new Point2D.Double(0, 0), new Point2D.Double(10, 0), new Point2D.Double(10, 10)));
        PatrolRoute route = new PatrolRoute(points);

        points.clear();
        points.add(new Point2D.Double(99, 99));
        check(route.size() == 3, "route is unaffected by later changes to the list");

        Iterator<Point2D> cycle = route.cycle();
        List<Double> xs = IntStream.range(0, 7)
                .mapToObj(i -> cycle.next().getX())
                .collect(Collectors.toList());
        check(xs.equals(Arrays.asList(0.0, 10.0, 10.0, 0.0, 10.0, 10.0, 0.0)),
                "iterator wraps round forever, got " + xs);
        check(route.cycle().next().getX() == 0.0, "each cycle() starts from the first waypoint");
    }

    private static void testRouteRejectsEmptyWaypoints() {
        expectThrows(IllegalArgumentException.class,
                () -> new PatrolRoute(new ArrayList<>()), "empty route rejected");
        expectThrows(IllegalArgumentException.class,
                () -> new PatrolRoute(null), "null route rejected");
    }

    // ---- Lambda strategies ------------------------------------------------

    private static void testPatrolLambdaVisitsWaypointsInOrder() {
        World world = new World(1000, 750);
        PatrolRoute route = new PatrolRoute(Arrays.asList(
                new Point2D.Double(300, 300), new Point2D.Double(300, 330)));
        Military soldier = new Military(300, 300, route, new Rifle());
        MovementBehaviour patrol = MovementBehaviour.patrol(route);

        // Standing on the first waypoint, the lambda immediately heads for the
        // second one (30px further down)...
        int steps = 0;
        while (soldier.getY() < 324 && steps++ < 60) {
            patrol.move(soldier, world);
        }
        check(soldier.getY() >= 324,
                "patrol lambda walks to the second waypoint, y=" + soldier.getY());

        // ...and having arrived there it turns back to the first (wrap-around).
        steps = 0;
        while (soldier.getY() > 306 && steps++ < 60) {
            patrol.move(soldier, world);
        }
        check(soldier.getY() <= 306,
                "patrol lambda wraps back to the first waypoint, y=" + soldier.getY());
    }

    // ---- State ------------------------------------------------------------

    private static void testStateMachinePatrolEngageReload() {
        World world = new World(1000, 750);
        Military soldier = new Military(300, 300);
        Zombie zombie = new Zombie(300, 380);
        world.addEntity(soldier);
        world.addEntity(zombie);

        check("Patrolling".equals(soldier.getStateName()), "starts patrolling");

        soldier.update(world);
        check("Engaging".equals(soldier.getStateName()), "zombie in range triggers Engaging");
        check(soldier.getTarget().isPresent() && soldier.getTarget().get() == zombie,
                "target is locked");

        // Run the magazine empty on a stream of fresh zombies in range.
        int shots = 0;
        while (soldier.getAmmo() > 0 && shots < 100) {
            if (!zombie.isActive()) {
                zombie = new Zombie(300, 380);
                world.addEntity(zombie);
            }
            soldier.update(world);
            shots++;
        }
        check(soldier.getAmmo() == 0, "magazine eventually runs dry");
        if (!zombie.isActive()) {
            zombie = new Zombie(300, 380);
            world.addEntity(zombie);
        }
        soldier.update(world);
        check("Reloading".equals(soldier.getStateName()), "empty magazine triggers Reloading");

        // The reload takes exactly reloadTicks ticks, then the unit is back
        // in the fight with a full magazine (it fires on the following tick).
        for (int i = 0; i < soldier.getWeapon().reloadTicks(); i++) {
            check("Reloading".equals(soldier.getStateName()), "still reloading at tick " + i);
            soldier.update(world);
        }
        check(soldier.getAmmo() == soldier.getWeapon().magazineSize(),
                "reload refills the magazine");
        check("Engaging".equals(soldier.getStateName()), "reload finishes -> Engaging");
    }

    private static void testSoldierReturnsToPatrolWhenThreatIsGone() {
        World world = new World(1000, 750);
        Military soldier = new Military(300, 300);
        Zombie zombie = new Zombie(300, 380);
        world.addEntity(soldier);
        world.addEntity(zombie);

        soldier.update(world);
        check("Engaging".equals(soldier.getStateName()), "engaged");
        soldier.update(world);
        check(!zombie.isActive(), "zombie in range was shot");
        world.removeInactiveEntities();
        soldier.update(world);
        check("Patrolling".equals(soldier.getStateName()), "no threat left -> back to Patrolling");
        check(!soldier.getTarget().isPresent(), "target cleared");
    }

    private static void testShootThrowsWithoutATargetInRange() {
        World world = new World(1000, 750);
        Military soldier = new Military(300, 300);
        world.addEntity(soldier);

        expectThrows(NoTargetException.class, soldier::shoot, "no target -> NoTargetException");

        Zombie far = new Zombie(900, 700);
        world.addEntity(far);
        soldier.lockTarget(far);
        expectThrows(NoTargetException.class, soldier::shoot, "out of range -> NoTargetException");
        check(far.isActive(), "out-of-range zombie untouched");
    }

    // ---- Streams: threat prioritisation ----------------------------------

    private static void testThreatPrioritisationProtectsCivilians() {
        World world = new World(1000, 750);
        Military soldier = new Military(300, 300);
        Zombie nearToSoldier = new Zombie(300, 350);
        Zombie nearToCivilian = new Zombie(450, 300);
        Human civilian = new Human(470, 300);
        world.addEntity(soldier);
        world.addEntity(nearToSoldier);
        world.addEntity(nearToCivilian);
        world.addEntity(civilian);

        check(soldier.findThreat(world).get() == nearToCivilian,
                "soldier picks the zombie closest to the civilian, not to itself");

        world.reset();
        Military lone = new Military(300, 300);
        world.addEntity(lone);
        world.addEntity(nearToSoldier);
        world.addEntity(nearToCivilian);
        check(lone.findThreat(world).get() == nearToSoldier,
                "with no civilians it falls back to the nearest zombie");

        world.reset();
        world.addEntity(lone);
        check(!lone.findThreat(world).isPresent(), "no zombies -> empty Optional");
    }

    // ---- Decorator at work: promotions -----------------------------------

    private static void testKillsPromoteTheWeaponWithDecorators() throws Exception {
        World world = new World(1000, 750);
        Military soldier = new Military(300, 300);
        world.addEntity(soldier);
        check("Rifle".equals(soldier.getWeapon().describe()), "starts with a plain rifle");

        killOne(world, soldier);
        check("Rifle".equals(soldier.getWeapon().describe()), "no promotion after 1 kill");
        killOne(world, soldier);
        check("Rifle + Scope".equals(soldier.getWeapon().describe()),
                "second kill adds a scope, got " + soldier.getWeapon().describe());
        check(soldier.getWeapon().range() == 210.0, "scope range is live");
        killOne(world, soldier);
        killOne(world, soldier);
        check("Rifle + Scope + Extended Magazine".equals(soldier.getWeapon().describe()),
                "fourth kill adds the magazine, got " + soldier.getWeapon().describe());
        check(soldier.getKills() == 4, "kill counter");
    }

    /** Puts a zombie in range and lets the soldier's state machine deal with it. */
    private static void killOne(World world, Military soldier) {
        Zombie zombie = new Zombie(soldier.getX(), soldier.getY() + 60);
        world.addEntity(zombie);
        // Generous limit: covers the fire cooldown and a full reload.
        for (int tick = 0; tick < 200 && zombie.isActive(); tick++) {
            soldier.update(world);
        }
        check(!zombie.isActive(), "zombie was shot");
    }

    // ---- Custom collector -------------------------------------------------

    private static void testBattleReportCollectsStatistics() throws Exception {
        World world = new World(1000, 750);
        Military veteran = new Military(100, 100);
        Military rookie = new Military(500, 500);
        Military engaged = new Military(700, 200);
        world.addEntity(veteran);
        world.addEntity(rookie);
        world.addEntity(engaged);
        world.addEntity(new Human(300, 300));
        killOne(world, veteran);
        killOne(world, veteran);
        killOne(world, veteran);
        killOne(world, rookie);
        // One more tick lets both notice the threat is gone and resume patrol.
        veteran.update(world);
        rookie.update(world);
        world.addEntity(new Zombie(700, 260));
        engaged.update(world);

        BattleReport report = BattleReport.of(world.getEntities());
        check(report.getSoldiers() == 3, "three soldiers");
        check(report.getTotalKills() == 4, "total kills, got " + report.getTotalKills());
        check(report.getBestKills() == 3, "best kills");
        check(Math.abs(report.getAverageKills() - 4.0 / 3.0) < 1e-9, "average kills");
        check(report.getStateCounts().get("Engaging") == 1L
                        && report.getStateCounts().get("Patrolling") == 2L,
                "state head-count, got " + report.getStateCounts());
    }

    private static void testBattleReportIsIdenticalSequentialAndParallel() throws Exception {
        World world = new World(2000, 2000);
        List<Military> soldiers = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            Military soldier = new Military(100 + i * 20, 100);
            world.addEntity(soldier);
            soldiers.add(soldier);
        }
        for (int i = 0; i < soldiers.size(); i += 3) {
            for (int kills = 0; kills < i % 5; kills++) {
                killOne(world, soldiers.get(i));
            }
        }

        BattleReport sequential = soldiers.stream().collect(BattleReport.collector());
        BattleReport parallel = soldiers.parallelStream().collect(BattleReport.collector());
        check(sequential.getSoldiers() == parallel.getSoldiers()
                        && sequential.getTotalKills() == parallel.getTotalKills()
                        && sequential.getBestKills() == parallel.getBestKills()
                        && sequential.getStateCounts().equals(parallel.getStateCounts())
                        && Math.abs(sequential.getAverageKills() - parallel.getAverageKills()) < 1e-9,
                "parallel collection matches sequential");
    }

    private static void testBattleReportOfEmptyWorldIsZero() {
        BattleReport report = BattleReport.of(new World(100, 100).getEntities());
        check(report.getSoldiers() == 0 && report.getTotalKills() == 0
                        && report.getBestKills() == 0 && report.getAverageKills() == 0.0
                        && report.getStateCounts().isEmpty(),
                "empty report is all zeros");
    }

    // ---- Outbreak spreading ----------------------------------------------

    private static void testInfectedHumanRisesAsZombie() {
        World world = new World(1000, 750);
        world.addEntity(new Human(500, 500));
        world.addEntity(new Zombie(505, 500));

        long zombiesBefore = world.getStatistics().getZombieCount();
        for (Entity entity : new ArrayList<>(world.getEntities())) {
            entity.update(world);
        }
        world.removeInactiveEntities();
        check(world.getStatistics().getHumanCount() == 0, "human was infected");
        check(world.getStatistics().getZombieCount() == zombiesBefore + 1,
                "the victim rose as a new zombie");
    }

    // ---- helpers ----------------------------------------------------------

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void expectThrows(Class<? extends Exception> expected,
                                     ThrowingRunnable action, String message) {
        try {
            action.run();
        } catch (Exception actual) {
            check(expected.isInstance(actual),
                    message + " (wrong exception " + actual.getClass().getSimpleName() + ")");
            return;
        }
        throw new AssertionError(message + " (nothing thrown)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
