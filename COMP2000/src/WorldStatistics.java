import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Population values captured at one point in a simulation run. */
public final class WorldStatistics {
    private final long tick;
    private final long humanCount;
    private final long militaryCount;
    private final long zombieCount;
    private final long foodCount;

    private WorldStatistics(long tick, Map<Class<?>, Long> counts) {
        this.tick = tick;
        this.humanCount = counts.getOrDefault(Human.class, 0L);
        this.militaryCount = counts.getOrDefault(Military.class, 0L);
        this.zombieCount = counts.getOrDefault(Zombie.class, 0L);
        this.foodCount = counts.getOrDefault(Food.class, 0L);
    }

    static WorldStatistics capture(long tick, List<Entity> entities) {
        // Exact classes keep Military separate from its Human superclass.
        Map<Class<?>, Long> counts = entities.stream()
                .filter(Entity::isActive)
                .collect(Collectors.groupingBy(Entity::getClass, Collectors.counting()));
        return new WorldStatistics(tick, counts);
    }

    public long getTick() {
        return tick;
    }

    public long getHumanCount() {
        return humanCount;
    }

    public long getMilitaryCount() {
        return militaryCount;
    }

    public long getZombieCount() {
        return zombieCount;
    }

    public long getFoodCount() {
        return foodCount;
    }
}
