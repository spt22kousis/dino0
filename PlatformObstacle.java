import javafx.scene.canvas.GraphicsContext;
// import javafx.scene.paint.Color;
// import java.util.Random;

public class PlatformObstacle extends Obstacle {

    // public PlatformObstacle() {
    // super();
    // Random localRandom = new Random();
    // this.width = OBSTACLE_MIN_WIDTH + localRandom.nextInt(OBSTACLE_MAX_WIDTH -
    // OBSTACLE_MIN_WIDTH + 1);
    // this.height = OBSTACLE_MIN_HEIGHT + localRandom.nextInt(OBSTACLE_MAX_HEIGHT -
    // OBSTACLE_MIN_HEIGHT + 1);
    // this.y = MainApplication.getHEIGHT() - 50 - this.height;
    // this.color = Color.ORANGE;
    // }

    public PlatformObstacle(double width, double height, double yPosition) {
        super(width, height, yPosition, ".//picture/platform.png");
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.drawImage(image, x, y);
    }

    @Override
    public void render(GraphicsContext gc, double cameraY) {
        gc.drawImage(image, x, y - cameraY);
    }

    @Override
    // 如果在上面就不算撞到
    public boolean getColide(Dino b) {
        if (b.getX() + Dino.DINO_WIDTH > this.x && b.getX() <= this.x + width) {
            if (b.getY() + Dino.DINO_HEIGHT >= this.y) {
                b.setY(b.getY() - this.y);
            }
        }
        return false;
    }
}