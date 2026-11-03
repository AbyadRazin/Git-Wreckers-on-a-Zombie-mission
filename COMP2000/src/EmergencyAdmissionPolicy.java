/** Reserves the last places for survivors whose energy is at or below a threshold. */
public final class EmergencyAdmissionPolicy implements AdmissionPolicy {
    private final int reservedPlaces;
    private final double energyThreshold;

    public EmergencyAdmissionPolicy(int reservedPlaces, double energyThreshold) {
        if (reservedPlaces <= 0) {
            throw new IllegalArgumentException("Reserved places must be positive");
        }
        if (!Double.isFinite(energyThreshold) || energyThreshold <= 35 || energyThreshold > 100) {
            throw new IllegalArgumentException("Threshold must be above shelter-leaving energy (35) and at most 100");
        }
        this.reservedPlaces = reservedPlaces;
        this.energyThreshold = energyThreshold;
    }

    @Override
    public boolean allows(Human person, Building building) {
        // Anonymous entry cannot bypass a person-specific policy.
        if (person == null) return false;
        int available = building.getCapacity() - building.getOccupantCount();
        return available > reservedPlaces || person.getEnergy() <= energyThreshold;
    }

    @Override
    public String describe() {
        return "Emergency (" + reservedPlaces + " reserved)";
    }
}