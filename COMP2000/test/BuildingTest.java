public class BuildingTest {

    private static int passedTests = 0;

    private static void check(
            boolean condition,
            String testName
    ) {
        if (!condition) {
            throw new AssertionError(
                    "FAILED: " + testName
            );
        }

        passedTests++;
        System.out.println("PASSED: " + testName);
    }

    public static void main(String[] args) {

        Building building =
                new Building(
                        100,
                        200,
                        150,
                        100,
                        2
                );

        // Test initial values
        check(
                building.getOccupantCount() == 0,
                "Building starts empty"
        );

        check(
                building.getCapacity() == 2,
                "Capacity is stored correctly"
        );

        check(
                !building.isFull(),
                "New building is not full"
        );

        check(
                building.canEnter(),
                "Human can enter an empty building"
        );

        // Test entering
        check(
                building.enter(),
                "First human enters"
        );

        check(
                building.getOccupantCount() == 1,
                "Occupant count becomes one"
        );

        check(
                building.enter(),
                "Second human enters"
        );

        check(
                building.isFull(),
                "Building becomes full"
        );

        check(
                !building.canEnter(),
                "Full building refuses entry"
        );

        check(
                !building.enter(),
                "Third human cannot enter"
        );

        check(
                building.getOccupantCount() == 2,
                "Failed entry does not change count"
        );

        // Test leaving
        check(
                building.leave(),
                "First human leaves"
        );

        check(
                building.leave(),
                "Second human leaves"
        );

        check(
                building.getOccupantCount() == 0,
                "Building becomes empty"
        );

        check(
                !building.leave(),
                "Nobody can leave an empty building"
        );

        check(
                building.getOccupantCount() == 0,
                "Occupant count cannot become negative"
        );

        // Test building boundaries
        check(
                building.containsPoint(100, 200),
                "Top-left point is inside"
        );

        check(
                building.containsPoint(175, 250),
                "Centre point is inside"
        );

        check(
                !building.containsPoint(99, 250),
                "Point left of building is outside"
        );

        check(
                !building.containsPoint(250, 250),
                "Point beyond right edge is outside"
        );

        check(
                !building.containsPoint(175, 300),
                "Point beyond bottom edge is outside"
        );

        // Test entrance coordinates
        check(
                building.getEntranceX() == 175.0,
                "Entrance X coordinate is correct"
        );

        check(
                building.getEntranceY() == 300.0,
                "Entrance Y coordinate is correct"
        );

        // Test constructor exceptions
        try {
            new Building(0, 0, 0, 100, 5);

            throw new AssertionError(
                    "FAILED: Zero width should be rejected"
            );

        } catch (IllegalArgumentException exception) {
            passedTests++;
            System.out.println(
                    "PASSED: Zero width is rejected"
            );
        }

        try {
            new Building(0, 0, 100, 100, 0);

            throw new AssertionError(
                    "FAILED: Zero capacity should be rejected"
            );

        } catch (IllegalArgumentException exception) {
            passedTests++;
            System.out.println(
                    "PASSED: Zero capacity is rejected"
            );
        }

        System.out.println(
                "\nAll " + passedTests + " tests passed."
        );
    }
}
