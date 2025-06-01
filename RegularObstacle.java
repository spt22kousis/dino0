import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.Random;

public class RegularObstacle extends Obstacle {

    public RegularObstacle(double width, double height, double yPosition) {
        super();
        this.width = width;
        this.height = height;
        this.y = yPosition;
        this.color = Color.BROWN;
    }

    public RegularObstacle() {
        super();
        Random localRandom = new Random();
        this.width = OBSTACLE_MIN_WIDTH + localRandom.nextInt(OBSTACLE_MAX_WIDTH - OBSTACLE_MIN_WIDTH + 1);
        this.height = OBSTACLE_MIN_HEIGHT + localRandom.nextInt(OBSTACLE_MAX_HEIGHT - OBSTACLE_MIN_HEIGHT + 1);
        this.y = MainApplication.getHEIGHT() - 50 - this.height;
        this.color = Color.BROWN;
    }

    public RegularObstacle(double worldX, double width, double height, double yPosition) {
        super(worldX, width, height, yPosition, Color.BROWN);
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillRect(x, y, width, height);
    }

    @Override
    public void render(GraphicsContext gc, double cameraX) {
        gc.setFill(color);
        gc.fillRect(x - cameraX, y, width, height);
    }
}