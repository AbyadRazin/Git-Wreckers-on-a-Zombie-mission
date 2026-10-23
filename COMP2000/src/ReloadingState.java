/** Stands still for the weapon's reload time, then goes back to the fight. */
public class ReloadingState implements MilitaryState {

    private int ticksRemaining;

    public ReloadingState(int reloadTicks) {
        this.ticksRemaining = Math.max(1, reloadTicks);
    }

    @Override
    public String name() {
        return "Reloading";
    }

    @Override
    public void update(Military unit, World world) {
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            unit.reload();
            unit.transitionTo(unit.engageState());
        }
    }
}
