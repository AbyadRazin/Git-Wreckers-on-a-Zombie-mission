import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.net.URL;
import javax.swing.ImageIcon;

/** A stationary food supply that can restore a human's energy. */
public class Food extends Entity {

    public static final double EAT_DISTANCE = 16.0;
    private static final int FOOD_SIZE = 28;
    // Food expires after 300 simulation updates.
    private static final int LIFETIME_TICKS = 300;
    private final double energyValue;
    private final Image image;
    private int ageInTicks;

    public Food(double x, double y, double energyValue) {
        super(x, y);
        if (energyValue <= 0) {
            throw new IllegalArgumentException("Food energy must be positive");
        }
        this.energyValue = energyValue;
        this.image = loadImage(selectImagePath(x, y));
    }

    public double getEnergyValue() {
        return energyValue;
    }

    /** Returns true once a human has consumed this food item. */
    public boolean isConsumed() {
        return !isActive();
    }

    /** Marks this food as consumed so World removes it after the tick. */
    public void consume() {
        deactivate();
    }

    @Override
    public void update(World world) {
        ageInTicks++;
        if (ageInTicks >= LIFETIME_TICKS) {
            deactivate();
        }
    }

    @Override
    public void draw(Graphics2D graphics) {
        int left = (int) getX() - FOOD_SIZE / 2;
        int top = (int) getY() - FOOD_SIZE / 2;
        graphics.drawImage(image, left, top, FOOD_SIZE, FOOD_SIZE, null);
    }

    private String selectImagePath(double x, double y) {
        int selection = Math.floorMod((int) Math.round(x * 31 + y * 17), 3);
        if (selection == 0) {
            return "/assets/food/FoodApple.png";
        }
        if (selection == 1) {
            return "/assets/food/FoodPizza.png";
        }
        return "/assets/food/FoodBurger.png";
    }

    private Image loadImage(String imagePath) {
        URL imageUrl = Food.class.getResource(imagePath);
        if (imageUrl != null) {
            return new ImageIcon(imageUrl).getImage();
        }

        String fileName = imagePath.substring(1);
        File sourceAsset = new File("src", fileName);
        if (!sourceAsset.isFile()) {
            sourceAsset = new File("COMP2000/src", fileName);
        }
        if (!sourceAsset.isFile()) {
            throw new IllegalStateException(imagePath + " was not found");
        }
        return new ImageIcon(sourceAsset.getAbsolutePath()).getImage();
    }
}
