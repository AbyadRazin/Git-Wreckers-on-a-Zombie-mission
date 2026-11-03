public final class AdmissionPolicyTest {
    public static void main(String[] args) {
        Building emergency = new Building(100, 100, 150, 100, 3,
                new EmergencyAdmissionPolicy(2, 60));
        Human healthy = new Human(175, 200);
        check(healthy.enterBuilding(emergency), "First healthy person uses general place");
        Human rejected = new Human(175, 200);
        check(!rejected.enterBuilding(emergency), "Healthy person cannot take reserved place");
        check(!rejected.isHidden() && emergency.getOccupantCount() == 1,
                "Rejected admission changes neither person nor occupancy");
        check(!emergency.enter(), "Anonymous entry cannot bypass emergency policy");

        Human vulnerable = new FixedEnergyHuman(60);
        check(vulnerable.enterBuilding(emergency), "Threshold energy gets reserved place");
        Human second = new FixedEnergyHuman(50);
        check(second.enterBuilding(emergency), "Another vulnerable survivor gets last place");
        check(!new FixedEnergyHuman(40).enterBuilding(emergency), "Full means full under every policy");
        vulnerable.leaveBuilding();
        check(emergency.getOccupantCount() == 2, "Leaving frees exactly one place");
        check(!rejected.enterBuilding(emergency), "Freed reserved place stays protected");

        Building open = new Building(400, 100, 150, 100, 1);
        check(rejected.enterBuilding(open), "Same healthy person is accepted by open strategy");
        check(!new FixedEnergyHuman(50).enterBuilding(open), "Open policy also enforces capacity");
        check(!rejected.enterBuilding(emergency) && rejected.getShelter() == open,
                "Failed transfer preserves previous shelter");

        // Integration: autonomous humans skip a closer shelter that rejects them.
        World world = new World(1000, 750);
        Building closedToHealthy = new Building(100, 100, 150, 100, 1,
                new EmergencyAdmissionPolicy(1, 60));
        Building alternative = new Building(300, 100, 150, 100);
        Human seeker = new Human(175, 200);
        world.addEntity(closedToHealthy);
        world.addEntity(alternative);
        world.addEntity(seeker);
        for (int tick = 0; tick < 120; tick++) world.update();
        check(seeker.getShelter() == alternative, "Autonomous selection respects policy");

        World emergencyWorld = new World(1000, 750);
        Human tiredSeeker = new FixedEnergyHuman(50);
        emergencyWorld.addEntity(closedToHealthy);
        emergencyWorld.addEntity(tiredSeeker);
        emergencyWorld.update();
        check(tiredSeeker.getShelter() == closedToHealthy,
                "Vulnerable autonomous survivor enters reserved shelter");
        emergencyWorld.reset();
        check(!new Building(0, 0, 100, 100, 3, new EmergencyAdmissionPolicy(2, 60)).isFull(),
                "Fresh reset shelter starts empty");
        System.out.println("AdmissionPolicyTest passed");
    }

    private static final class FixedEnergyHuman extends Human {
        private final double displayedEnergy;
        FixedEnergyHuman(double energy) { super(175, 200); displayedEnergy = energy; }
        @Override public double getEnergy() { return displayedEnergy; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}