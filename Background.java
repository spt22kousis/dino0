import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Background class that handles the scrolling background of the game.
 * It loads the background image and renders it with a parallax scrolling effect.
 */
public class Background {
    private Image backgroundImage;
    private double scrollPosition = 0;
    private final double scrollSpeed = 0.5; // Background scrolls at half the speed of obstacles for parallax effect
    
    /**
     * Constructor that loads the background image.
     * 
     * @param imagePath The path to the background image
     * @param width The width of the background image
     * @param height The height of the background image
     */
    public Background(String imagePath, double width, double height) {
        // Load background image
        backgroundImage = new Image(imagePath, width, height, false, false);
    }
    
    /**
     * Updates the background scroll position based on the game world distance.
     * 
     * @param gameWorldDistance The current distance traveled in the game world
     */
    public void update(double gameWorldDistance) {
        // Calculate background position based on gameWorldDistance for parallax effect
        // Use a slower scroll rate for the background to create depth
        scrollPosition = (gameWorldDistance * scrollSpeed) % MainApplication.getWIDTH();
    }
    
    /**
     * Renders the background with a parallax scrolling effect.
     * 
     * @param gc The GraphicsContext to render on
     */
    public void render(GraphicsContext gc) {
        // Draw the background image twice to create a seamless scrolling effect
        gc.drawImage(backgroundImage, -scrollPosition, 0);
        gc.drawImage(backgroundImage, MainApplication.getWIDTH() - scrollPosition, 0);
    }
    
    /**
     * Resets the background scroll position.
     */
    public void reset() {
        scrollPosition = 0;
    }
}