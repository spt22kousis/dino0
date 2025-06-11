import javafx.scene.canvas.GraphicsContext;
// import java.util.Random;

public class LemonObstacle extends Obstacle {

    // public RegularObstacle() {
    // super();
    // Random localRandom = new Random();
    // this.width = OBSTACLE_MIN_WIDTH + localRandom.nextInt(OBSTACLE_MAX_WIDTH -
    // OBSTACLE_MIN_WIDTH + 1);
    // this.height = OBSTACLE_MIN_HEIGHT + localRandom.nextInt(OBSTACLE_MAX_HEIGHT -
    // OBSTACLE_MIN_HEIGHT + 1);
    // this.y = MainApplication.getHEIGHT() - 50 - this.height;
    // this.color = Color.BROWN;
    // }

    public LemonObstacle(double width, double height, double yPosition) {
        super(width, height, yPosition, ".//picture/lemon.png");
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.drawImage(image, x, y);
    }

    @Override
    public void render(GraphicsContext gc, double cameraY) {
        gc.drawImage(image, x, y - cameraY);
    }
}
