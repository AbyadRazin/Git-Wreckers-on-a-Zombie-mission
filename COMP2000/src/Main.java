import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.Random;


public class Main {

    private static final double AGENT_SPAWN_MARGIN = 80;
    // Reserve the default patrol's 160/110-unit excursions and sprite clearance.
    private static final double MILITARY_SPAWN_MARGIN_X = 180;
    private static final double MILITARY_SPAWN_MARGIN_Y = 140;
    // A few zombies from the start so traps and infection have an early impact.
    private static final int STARTING_ZOMBIES = 3;
    // Extra traps the viewer can place by hand each run.
    private static final int PLACEABLE_TRAPS = 3;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                startSimulation();
            } catch (IllegalStateException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "The simulation could not start:\n"
                                + exception.getMessage(),
                        "Simulation startup error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private static void startSimulation() {
        World world = new World(1000,750);
        populateWorld(world);
        TrapPlacer trapPlacer = new TrapPlacer(world, PLACEABLE_TRAPS);

        SimPanel simPanel = new SimPanel(world, () -> {
            world.reset();
            populateWorld(world);
            trapPlacer.reset();
        });

        // Place a trap by pressing the button, then clicking on the map.
        JButton trapButton = new JButton();
        // Leave keyboard focus with the simulation so R, P and +/- keep working.
        trapButton.setFocusable(false);
        trapButton.addActionListener(event -> trapPlacer.toggleArmed());
        simPanel.setMapClickListener(
                point -> trapPlacer.placeAt(point.getX(), point.getY()));
        trapPlacer.setOnChange(() -> showTrapState(trapButton, simPanel, trapPlacer));
        showTrapState(trapButton, simPanel, trapPlacer);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(trapButton);

        JFrame frame =
                new JFrame("Zombie Simulation");

        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        frame.add(simPanel, BorderLayout.CENTER);
        frame.add(controls, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // Keep game updates at the existing speed.
        Timer updateTimer = new Timer(simPanel.getTimerDelay(), null);
        updateTimer.addActionListener(event -> {
            int delay = simPanel.getTimerDelay();
            if (updateTimer.getDelay() != delay) {
                updateTimer.setDelay(delay);
            }

            if (simPanel.isRunning()) {
                world.update();
            }
        });

        // Draw independently at approximately 60 FPS.
        Timer renderTimer = new Timer(17, event -> simPanel.repaint());

        updateTimer.start();
        renderTimer.start();
    }

    /** Updates the trap button's label and the map cursor to match the placer. */
    private static void showTrapState(JButton button, SimPanel map, TrapPlacer placer) {
        if (placer.isArmed()) {
            button.setText("Click the map to place a trap (click here to cancel)");
        } else if (placer.getTrapsLeft() > 0) {
            button.setText("Place trap (" + placer.getTrapsLeft() + " left)");
        } else {
            button.setText("No traps left");
        }
        button.setEnabled(placer.isArmed() || placer.getTrapsLeft() > 0);
        map.setCursor(Cursor.getPredefinedCursor(
                placer.isArmed() ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private static void populateWorld(World world) {
        populateWorld(world, new Random());
    }

    private static void populateWorld(World world, Random random) {
        world.addEntity(new Building(155, 125, 150, 100));
        world.addEntity(new Building(580, 490, 150, 100));

        // Traps are added before the characters so they are drawn
        // underneath anyone walking over them.
        world.addEntity(new Trap(750, 300, 2));
        world.addEntity(new Trap(350, 300, 2));
        world.addEntity(new Trap(480, 460, 2));

        // Vary initial encounters without changing the population sizes.
        for (int index = 0; index < 3; index++) {
            world.addEntity(new Human(
                    randomCoordinate(random, world.getWidth(), AGENT_SPAWN_MARGIN),
                    randomCoordinate(random, world.getHeight(), AGENT_SPAWN_MARGIN)));
        }

        world.addEntity(new Food(250, 300, 45));
        world.addEntity(new Food(500, 600, 45));
        world.addEntity(new Food(850, 150, 45));

        for (int index = 0; index < STARTING_ZOMBIES; index++) {
            world.addEntity(new Zombie(
                    randomCoordinate(random, world.getWidth(), AGENT_SPAWN_MARGIN),
                    randomCoordinate(random, world.getHeight(), AGENT_SPAWN_MARGIN)));
        }
        for (int index = 0; index < 2; index++) {
            world.addEntity(new Military(
                    randomCoordinate(random, world.getWidth(), MILITARY_SPAWN_MARGIN_X),
                    randomCoordinate(random, world.getHeight(), MILITARY_SPAWN_MARGIN_Y)));
        }
        // Medics spawn like other civilians and look after the injured.
        for (int index = 0; index < 2; index++) {
            world.addEntity(new Medic(
                    randomCoordinate(random, world.getWidth(), AGENT_SPAWN_MARGIN),
                    randomCoordinate(random, world.getHeight(), AGENT_SPAWN_MARGIN)));
        }
        world.publishStatistics();
    }

    private static double randomCoordinate(Random random, int dimension, double margin) {
        return margin + random.nextDouble() * (dimension - 2 * margin);
    }
}
