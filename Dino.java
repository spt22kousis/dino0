import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
// import javafx.geometry.Rectangle2D;

public class Dino extends Player {
    public static final int DINO_WIDTH = PLAYER_WIDTH;
    public static final int DINO_HEIGHT = PLAYER_HEIGHT;
    private static final int JUMP_STRENGTH = -18;
    private static final double GRAVITY = 1.5;

    private Image dinoImage;

    public Dino() {
        this.dinoImage = new Image("file:./picture/dino.png", DINO_WIDTH, DINO_HEIGHT, false, false);
    }

    public void jump() {
        if (!isJumping) {
            yVelocity = JUMP_STRENGTH;
            isJumping = true;
        }
    }

    @Override
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

    @Override
    public void render(GraphicsContext gc) {
        gc.drawImage(dinoImage, x, y);
    }
}
