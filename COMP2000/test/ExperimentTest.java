import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ExperimentTest {

    private static final double TOLERANCE = 1e-9;

    public static void main(String[] args) {
        testSummaryAverages();
        testParallelMatchesSequential();
        testEmptySummary();
        testOutcomeCountsOnlyOrdinaryCivilians();
        testScenariosKeepTheRightEntities();
        testSmallExperimentRuns();
        testInvalidArguments();
        System.out.println("ExperimentTest passed");
    }

    private static void testSummaryAverages() {
        ExperimentSummary summary = List.of(
                        new RunOutcome(2, 0, 1), new RunOutcome(4, 3, 0), new RunOutcome(6, 0, 5))
                .stream()
                .collect(ExperimentSummary.collector());
        check(summary.getRuns() == 3, "counts every run");
        check(close(summary.getAverageSurvivors(), 4), "average survivors");
        check(close(summary.getAverageZombies(), 1), "average zombies");
        check(close(summary.getAverageTrapCatches(), 2), "average trap catches");
        check(close(summary.getContainedRate(), 2.0 / 3), "two of three runs zombie-free");
        // Sample standard deviation of 2, 4, 6 is 2, so the standard error is 2 / sqrt(3).
        check(close(summary.getSurvivorsStandardError(), 2 / Math.sqrt(3)), "standard error");
    }

    private static void testParallelMatchesSequential() {
        List<RunOutcome> outcomes = IntStream.range(0, 10_000)
                .mapToObj(index -> new RunOutcome(index % 13, index % 5, index % 7))
                .collect(Collectors.toList());
        ExperimentSummary sequential = outcomes.stream().collect(ExperimentSummary.collector());
        ExperimentSummary parallel = outcomes.parallelStream().collect(ExperimentSummary.collector());
        check(parallel.getRuns() == sequential.getRuns(), "same number of runs");
        check(close(parallel.getAverageSurvivors(), sequential.getAverageSurvivors()), "same survivors");
        check(close(parallel.getAverageZombies(), sequential.getAverageZombies()), "same zombies");
        check(close(parallel.getContainedRate(), sequential.getContainedRate()), "same contained rate");
        check(close(parallel.getAverageTrapCatches(), sequential.getAverageTrapCatches()),
                "same trap catches");
        check(close(parallel.getSurvivorsStandardError(), sequential.getSurvivorsStandardError()),
                "same standard error");
    }

    private static void testEmptySummary() {
        ExperimentSummary summary = List.<RunOutcome>of().stream().collect(ExperimentSummary.collector());
        check(summary.getRuns() == 0 && summary.getAverageSurvivors() == 0, "an empty experiment is all zeros");
    }

    private static void testOutcomeCountsOnlyOrdinaryCivilians() {
        World world = new World(500, 500);
        world.addEntity(new Human(10, 10));
        world.addEntity(new Human(20, 20));
        world.addEntity(new Medic(30, 30));
        world.addEntity(new Military(250, 250));
        world.addEntity(new Zombie(400, 400));
        Human gone = new Human(40, 40);
        gone.deactivate();
        world.addEntity(gone);

        RunOutcome outcome = RunOutcome.of(world, 4);
        check(outcome.getSurvivors() == 2, "medics, soldiers and inactive humans are not survivors");
        check(outcome.getZombies() == 1 && !outcome.outbreakContained(), "one zombie left");
        check(outcome.getTrapCatches() == 4, "keeps the trap catches it was given");
    }

    private static void testScenariosKeepTheRightEntities() {
        World neither = Experiment.Scenario.NEITHER.createWorld();
        check(neither.getEntities().stream().noneMatch(e -> e instanceof Medic || e instanceof Trap),
                "baseline has no medics or traps");
        World both = Experiment.Scenario.BOTH.createWorld();
        check(both.getEntities().stream().anyMatch(Medic.class::isInstance)
                        && both.getEntities().stream().anyMatch(Trap.class::isInstance),
                "full scenario keeps medics and traps");
        World trapsOnly = Experiment.Scenario.TRAPS_ONLY.createWorld();
        check(trapsOnly.getEntities().stream().noneMatch(Medic.class::isInstance)
                        && trapsOnly.getEntities().stream().anyMatch(Trap.class::isInstance),
                "traps-only scenario");
    }

    private static void testSmallExperimentRuns() {
        ExperimentSummary summary = new Experiment(4, 20).run(Experiment.Scenario.BOTH);
        check(summary.getRuns() == 4, "every run is collected");
    }

    private static void testInvalidArguments() {
        expectIllegalArgument(() -> new Experiment(0, 10));
        expectIllegalArgument(() -> new Experiment(10, 0));
        expectIllegalArgument(() -> new RunOutcome(-1, 0, 0));
        expectIllegalArgument(() -> new RunOutcome(0, 0, -1));
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < TOLERANCE;
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
