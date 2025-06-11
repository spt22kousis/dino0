import javafx.scene.canvas.GraphicsContext;

public abstract class Player {
    public static final int PLAYER_WIDTH = 40;
    public static final int PLAYER_HEIGHT = 60;
    
    protected double y = MainApplication.getHEIGHT() - 50 - PLAYER_HEIGHT;
    protected double x = 50;
    protected double yVelocity = 0;
    protected boolean isJumping = false;
    
    public abstract void update();
    public abstract void render(GraphicsContext gc);
    
    // Common methods for all player types
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
    
    // public boolean isJumping() { return isJumping; }
    
    public void setJumping(boolean jumping) {
        isJumping = jumping;
    }
}