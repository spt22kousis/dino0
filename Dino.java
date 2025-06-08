import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
// import javafx.geometry.Rectangle2D;

public class Dino {
    public static final int DINO_WIDTH = 40;
    public static final int DINO_HEIGHT = 60;
    private static final int JUMP_STRENGTH = -18;
    private static final double GRAVITY = 1.5;

    private double y = MainApplication.getHEIGHT() - 50 - DINO_HEIGHT;
    private double x = 50;
    private double yVelocity = 0;
    public boolean isJumping = false;

    public void jump() {
        if (!isJumping) {
            yVelocity = JUMP_STRENGTH;
            isJumping = true;
        }
    }

    public void update() {
        y += yVelocity;
        yVelocity += GRAVITY;

        if (y >= MainApplication.getHEIGHT() - 50 - DINO_HEIGHT) {
            y = MainApplication.getHEIGHT() - 50 - DINO_HEIGHT;
            isJumping = false;
        }

        if (!isJumping) {
            yVelocity = 0;
        }
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.GREEN);
        gc.fillRect(x, y, DINO_WIDTH, DINO_HEIGHT);
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getYVelocity() {
        return yVelocity;
    }

    public void setYVelocity(double yVelocity) {
        this.yVelocity = yVelocity;
    }

    public double getX() {
        return x;
    }

    public boolean isJumping() {
        return isJumping;
    }

    public void setJumping(boolean jumping) {
        isJumping = jumping;
    }
}