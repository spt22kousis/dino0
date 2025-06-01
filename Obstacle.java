import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public abstract class Obstacle {
    protected static final int OBSTACLE_MIN_WIDTH = 20;
    protected static final int OBSTACLE_MAX_WIDTH = 40;
    protected static final int OBSTACLE_MIN_HEIGHT = 30;
    protected static final int OBSTACLE_MAX_HEIGHT = 60;
    protected static final int OBSTACLE_SPEED = 10;

    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected Color color;
    protected long spawnTime;

    public Obstacle() {
        this.x = MainApplication.getWIDTH();
        this.spawnTime = System.nanoTime();
    }

    public Obstacle(double worldX, double width, double height, double yPosition, Color color) {
        this.x = worldX;
        this.width = width;
        this.height = height;
        this.y = yPosition;
        this.color = color;
    }

    public void update() {
        x -= OBSTACLE_SPEED;
    }

    public abstract void render(GraphicsContext gc);

    public abstract void render(GraphicsContext gc, double cameraX);

    public boolean isOffScreen() {
        return x + width < 0;
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D(x, y, width, height);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public long getSpawnTime() {
        return spawnTime;
    }
}