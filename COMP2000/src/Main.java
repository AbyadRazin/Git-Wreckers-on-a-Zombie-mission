import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


public class Main {

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
        world.addEntity(new Building(155, 125, 150, 100));
        world.addEntity(new Building(580, 490, 150, 100));

        // Traps are added before the characters so they are drawn
        // underneath anyone walking over them.
        world.addEntity(new Trap(750, 300, 2));
        world.addEntity(new Trap(350, 300, 2));
        world.addEntity(new Trap(480, 460, 2));

        // Part B: people wander until they find and consume nearby food.
        world.addEntity(new Human(100, 400));
        world.addEntity(new Human(450, 200));
        world.addEntity(new Human(800, 350));

        world.addEntity(new Food(250, 300, 45));
        world.addEntity(new Food(500, 600, 45));
        world.addEntity(new Food(850, 150, 45));

        world.addEntity(new Zombie(700, 250));
        world.addEntity(new Military(500, 375));
        world.addEntity(new Military(200, 620));
        world.publishStatistics();
    }
}
