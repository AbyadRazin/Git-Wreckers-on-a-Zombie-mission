import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class Building extends Entity {
    private final Image image;
    private final int width;
    private final int height;
    private final int capacity;
    private int occupantCount;
    private final AdmissionPolicy admissionPolicy;

    public Building(double x, double y, int width, int height) {
        this(x, y, width, height, 10);
    }

    public Building(double x, double y, int width, int height, int capacity) {
        this(x, y, width, height, capacity, new OpenAdmissionPolicy());
    }

    /** Compose a building with an interchangeable admission strategy. */
    public Building(double x, double y, int width, int height, int capacity,
                    AdmissionPolicy admissionPolicy) {
        super(x, y);
        this.admissionPolicy = Objects.requireNonNull(admissionPolicy, "Admission policy cannot be null");
        if (width > 0 && height > 0) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Building capacity must be positive");
            } else {
                this.width = width;
                this.height = height;
                this.capacity = capacity;
                this.occupantCount = 0;

                Image loadedImage;
                try {
                    URL imageURL = Building.class.getResource("/Building.png");
                    if (imageURL != null) {
                        loadedImage = ImageIO.read(imageURL);
                    } else {
                        File sourceAsset = new File("src", "Building.png");
                        if (!sourceAsset.isFile()) {
                            sourceAsset = new File("COMP2000/src", "Building.png");
                        }
                        if (!sourceAsset.isFile()) {
                            throw new IOException("Building.png could not be found");
                        }
                        loadedImage = new ImageIcon(sourceAsset.getAbsolutePath()).getImage();
                    }
                    if (loadedImage == null) {
                        throw new IOException("Building.png is corrupted or unsupported");
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to load the building image", exception);
                }

                this.image = loadedImage;
            }
        } else {
            throw new IllegalArgumentException("Building dimensions must be positive");
        }
    }

    public boolean isFull() {
        return this.occupantCount >= this.capacity;
    }

    /** Capacity query retained for callers that do not yet have a person. */
    public boolean canEnter() {
        return isActive() && !isFull();
    }

    public boolean canEnter(Human person) {
        return person != null && person.isActive() && canEnter()
                && admissionPolicy.allows(person, this);
    }

    /** Legacy anonymous admission is available only when the strategy permits it. */
    public boolean enter() {
        if (!canEnter() || !admissionPolicy.allows(null, this)) return false;
        occupantCount++;
        return true;
    }

    public boolean enter(Human person) {
        if (!canEnter(person)) return false;
        occupantCount++;
        return true;
    }

    public String getAdmissionDescription() {
        return admissionPolicy.describe();
    }

    public boolean leave() {
        if (this.occupantCount == 0) {
            return false;
        } else {
            --this.occupantCount;
            return true;
        }
    }

    public boolean containsPoint(double pointX, double pointY) {
        return pointX >= this.getX() && pointX < this.getX() + (double)this.width && pointY >= this.getY() && pointY < this.getY() + (double)this.height;
    }

    public double getEntranceX() {
        return this.getX() + (double)this.width / (double)2.0F;
    }

    public double getEntranceY() {
        return this.getY() + (double)this.height;
    }

    public int getOccupantCount() {
        return this.occupantCount;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void update(World world) {
    }

    public void draw(Graphics2D graphics) {
        graphics.drawImage(this.image, (int)this.getX(), (int)this.getY(), this.width, this.height, (ImageObserver)null);
    }
}