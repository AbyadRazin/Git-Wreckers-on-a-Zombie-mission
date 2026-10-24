import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.Random;


public class Main {

    private static final double AGENT_SPAWN_MARGIN = 80;
    // Reserve the default patrol's 160/110-unit excursions and sprite clearance.
    private static final double MILITARY_SPAWN_MARGIN_X = 180;
    private static final double MILITARY_SPAWN_MARGIN_Y = 140;

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

        SimPanel simPanel = new SimPanel(world, () -> {
            world.reset();
            populateWorld(world);
        });

        JFrame frame =
                new JFrame("Zombie Simulation");

        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        frame.add(simPanel);
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

    private static void populateWorld(World world) {
        populateWorld(world, new Random());
    }

    private static void populateWorld(World world, Random random) {
        world.addEntity(new Building(155, 125, 150, 100));
        world.addEntity(new Building(580, 490, 150, 100));

        // Vary initial encounters without changing the population sizes.
        for (int index = 0; index < 3; index++) {
            world.addEntity(new Human(
                    randomCoordinate(random, world.getWidth(), AGENT_SPAWN_MARGIN),
                    randomCoordinate(random, world.getHeight(), AGENT_SPAWN_MARGIN)));
        }

        world.addEntity(new Food(250, 300, 45));
        world.addEntity(new Food(500, 600, 45));
        world.addEntity(new Food(850, 150, 45));

        world.addEntity(new Zombie(
                randomCoordinate(random, world.getWidth(), AGENT_SPAWN_MARGIN),
                randomCoordinate(random, world.getHeight(), AGENT_SPAWN_MARGIN)));
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
