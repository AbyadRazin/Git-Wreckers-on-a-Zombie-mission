import java.util.Optional;

/**
 * Closes on the most dangerous zombie and shoots it once it is in range.
 * Returns to patrol when no zombie is left (reloading first if the magazine
 * is low) and drops into {@link ReloadingState} when it runs dry.
 */
public class EngageState implements MilitaryState {

    private final MovementBehaviour approachMovement;

    public EngageState(MovementBehaviour approachMovement) {
        this.approachMovement = approachMovement;
    }

    @Override
    public String name() {
        return "Engaging";
    }

    @Override
    public boolean isEngaging() {
        return true;
    }

    @Override
    public void update(Military unit, World world) {
        Optional<Zombie> threat = unit.findThreat(world);
        if (!threat.isPresent()) {
            unit.clearTarget();
            // Nothing left to shoot: top up a low magazine before moving on,
            // so the next contact does not start with an empty rifle.
            unit.transitionTo(unit.needsReload()
                    ? new ReloadingState(unit.getWeapon().reloadTicks())
                    : unit.patrolState());
            return;
        }

        Zombie target = threat.get();
        unit.lockTarget(target);

        if (unit.getAmmo() == 0) {
            unit.transitionTo(new ReloadingState(unit.getWeapon().reloadTicks()));
            return;
        }

        if (unit.distanceTo(target) > unit.getWeapon().range()) {
            approachMovement.move(unit, world);
            return;
        }

        // In range: hold position and fire.
        try {
            unit.shoot();
        } catch (NoTargetException noTarget) {
            // The zombie went inactive since the lookup; the next tick
            // finds a new threat or returns to patrol.
        }
    }
}
