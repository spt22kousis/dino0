import javafx.scene.canvas.GraphicsContext;
// import javafx.scene.paint.Color;
// import java.util.Random;

public class PlatformObstacle extends Obstacle {

    private boolean prev = false;
    private boolean colide = false;

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
        colide = colideStatus(b);
        if (colide) {
            if (b.getYVelocity() >= 0) {
                b.setY(this.y - Dino.DINO_HEIGHT);
                b.isJumping = false;
            } else {
                b.setYVelocity(-b.getYVelocity());
            }
        } else if (prev) {
            b.isJumping = true;
        }
        prev = colide;
        return false;
    }

    public boolean colideStatus(Dino b) { // 返回值：b是否站在上面
        return b.getX() + Dino.DINO_WIDTH > this.x &&
                b.getX() <= this.x + width &&
                b.getY() + Dino.DINO_HEIGHT >= this.y &&
                b.getY() <= this.y + height;
    }
}