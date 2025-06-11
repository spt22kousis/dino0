package com.dino;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

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
    protected Image image;
    protected long spawnTime;

    public Obstacle(double width, double height, double yPosition, String imagePath) {
        this.x = MainApplication.getWIDTH();
        this.width = width;
        this.height = height;
        this.y = yPosition;
        this.image = new Image(getClass().getResourceAsStream(imagePath), width, height, false, false);
    }

    public void update() {
        x -= OBSTACLE_SPEED;
    }

    public abstract void render(GraphicsContext gc);

    public abstract void render(GraphicsContext gc, double cameraY);

    public boolean isOffScreen() {
        return x + width < 0;
    }

    // 撞到return true
    public boolean getColide(Player player) {
        int playerWidth = Player.PLAYER_WIDTH;
        int playerHeight = Player.PLAYER_HEIGHT;

        if (player.getX() + playerWidth >= this.x && player.getX() <= this.x + width) {
            if (player.getY() + playerHeight - 10 >= this.y) {
                return true;
            }
        }
        return false;
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