import java.util.Arrays;
import java.util.Comparator;

/**
 * How experienced a {@link Military} unit is, earned from its kill count.
 * The same thresholds decide when the unit's weapon is upgraded, so what the
 * rank chevrons show matches what the soldier can do.
 */
public enum Rank {
    RECRUIT(0, "Recruit"),
    VETERAN(2, "Veteran"),
    ELITE(4, "Elite");

    private final int minKills;
    private final String title;

    Rank(int minKills, String title) {
        this.minKills = minKills;
        this.title = title;
    }

    /** Kills needed to hold this rank. */
    public int minKills() {
        return minKills;
    }

    public String title() {
        return title;
    }

    /** The highest rank whose kill threshold has been reached. */
    public static Rank forKills(int kills) {
        return Arrays.stream(values())
                .filter(rank -> kills >= rank.minKills)
                .max(Comparator.comparingInt(rank -> rank.minKills))
                .orElse(RECRUIT);
    }
}
