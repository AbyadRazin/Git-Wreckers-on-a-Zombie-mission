import java.util.Optional;

/** Walks the patrol route until a zombie comes into detection range. */
public class PatrolState implements MilitaryState {

    private final MovementBehaviour patrolMovement;

    public PatrolState(MovementBehaviour patrolMovement) {
        this.patrolMovement = patrolMovement;
    }

    @Override
    public String name() {
        return "Patrolling";
    }

    @Override
    public void update(Military unit, World world) {
        Optional<Zombie> threat = unit.findThreat(world);
        if (threat.isPresent()) {
            unit.lockTarget(threat.get());
            unit.transitionTo(unit.engageState());
            return;
        }
        patrolMovement.move(unit, world);
    }
}
