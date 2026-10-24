public final class MedicTest {

    private static final double TOLERANCE = 1e-9;

    public static void main(String[] args) {
        testHealsTheMostUrgentPatient();
        testCooldownSpacesOutHeals();
        testIgnoresSheltered();
        testLeavesShelterToHelp();
        testStillGetsHungryWhileHealing();
        testTiredMedicStopsHealing();
        testRestoreEnergyIsCappedAndValidated();
        System.out.println("MedicTest passed");
    }

    private static void testHealsTheMostUrgentPatient() {
        World world = new World(1000, 750);
        Medic medic = new Medic(100, 100);
        Human hungry = injured(130, 100, 50);
        Human starving = injured(130, 110, 30);
        world.addEntity(medic);
        world.addEntity(hungry);
        world.addEntity(starving);
        double hungryBefore = hungry.getEnergy();
        double starvingBefore = starving.getEnergy();

        for (int tick = 0; tick < 20 && medic.getCooldown() == 0; tick++) {
            medic.update(world);
        }

        check(Math.abs(starving.getEnergy() - (starvingBefore + 30)) < TOLERANCE,
                "the lowest-energy patient should be healed first");
        check(hungry.getEnergy() == hungryBefore, "only one patient is healed at a time");
    }

    private static void testCooldownSpacesOutHeals() {
        World world = new World(1000, 750);
        Medic medic = new Medic(100, 100);
        Human patient = injured(110, 100, 20);
        world.addEntity(medic);
        world.addEntity(patient);
        double before = patient.getEnergy();

        medic.update(world);
        check(Math.abs(patient.getEnergy() - (before + 30)) < TOLERANCE, "first heal is immediate");
        for (int tick = 0; tick < 14; tick++) {
            medic.update(world);
        }
        check(Math.abs(patient.getEnergy() - (before + 30)) < TOLERANCE,
                "no second heal during the cooldown");
        medic.update(world);
        check(Math.abs(patient.getEnergy() - (before + 60)) < TOLERANCE,
                "heals again once the cooldown has passed");
    }

    private static void testIgnoresSheltered() {
        World world = new World(1000, 750);
        Building building = new Building(300, 300, 150, 100);
        Medic medic = new Medic(100, 100);
        Human sheltered = injured(110, 100, 40);
        world.addEntity(building);
        world.addEntity(medic);
        world.addEntity(sheltered);
        sheltered.enterBuilding(building);
        double before = sheltered.getEnergy();

        for (int tick = 0; tick < 30; tick++) {
            medic.update(world);
        }

        check(sheltered.getEnergy() == before, "sheltered civilians are safe and left alone");
    }

    private static void testLeavesShelterToHelp() {
        World world = new World(1000, 750);
        Building building = new Building(300, 300, 150, 100);
        Medic medic = new Medic(375, 420);
        Human patient = injured(375, 440, 30);
        world.addEntity(building);
        world.addEntity(medic);
        world.addEntity(patient);
        medic.enterBuilding(building);
        check(medic.isHidden() && building.getOccupantCount() == 1, "medic starts sheltered");

        medic.update(world);

        check(!medic.isHidden(), "medic comes out of shelter to help");
        check(building.getOccupantCount() == 0, "medic gives up its place in the building");
    }

    private static void testStillGetsHungryWhileHealing() {
        World world = new World(1000, 750);
        Medic medic = new Medic(100, 100);
        Human patient = injured(110, 100, 20);
        world.addEntity(medic);
        world.addEntity(patient);
        double before = medic.getEnergy();

        medic.update(world);

        check(Math.abs(medic.getEnergy() - (before - 0.2)) < TOLERANCE,
                "a medic on duty still loses energy every tick");
    }

    private static void testTiredMedicStopsHealing() {
        World world = new World(1000, 750);
        Medic medic = new Medic(100, 100);
        drainTo(medic, 55);
        medic.setPosition(100, 100);
        Human patient = injured(110, 100, 20);
        world.addEntity(medic);
        world.addEntity(patient);
        double before = patient.getEnergy();

        for (int tick = 0; tick < 5; tick++) {
            medic.update(world);
        }

        check(patient.getEnergy() == before, "a tired medic looks after itself instead");
    }

    private static void testRestoreEnergyIsCappedAndValidated() {
        Human human = injured(0, 0, 90);
        human.restoreEnergy(50);
        check(human.getEnergy() == 100, "energy never goes above the maximum");
        expectIllegalArgument(() -> human.restoreEnergy(0));
        expectIllegalArgument(() -> human.restoreEnergy(-5));
    }

    /** A civilian whose energy has drained to just below the given level. */
    private static Human injured(double x, double y, double energy) {
        Human human = new Human(x, y);
        drainTo(human, energy);
        human.setPosition(x, y);
        return human;
    }

    /** Ticks a human alone in an empty world until its energy drops below the target. */
    private static void drainTo(Human human, double energy) {
        World empty = new World(1000, 750);
        while (human.getEnergy() > energy) {
            human.update(empty);
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
