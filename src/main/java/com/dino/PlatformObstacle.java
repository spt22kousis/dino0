package com.dino;

import javafx.scene.canvas.GraphicsContext;

public class PlatformObstacle extends Obstacle {

    private boolean prev = false;
    private boolean colide = false;
    private static boolean isLevel2 = false;

    public static void setIsLevel2(boolean level2) {
        isLevel2 = level2;
    }

    public PlatformObstacle(double width, double height, double yPosition) {
        super(width, height, yPosition, isLevel2 ? "/picture/platform2.png" : "/picture/platform.png");
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
    public boolean getColide(Player player) {
        colide = colideStatus(player);
        if (colide) {
            if (player.getYVelocity() >= 0) {
                player.setY(this.y - Player.PLAYER_HEIGHT);
                player.setJumping(false);
            } else {
                player.setYVelocity(-player.getYVelocity());
            }
        } else if (prev) {
            player.setJumping(true);
        }
        prev = colide;
        return false;
    }

    public boolean colideStatus(Player player) { // 返回值：player是否站在上面
        return player.getX() + Player.PLAYER_WIDTH > this.x &&
                player.getX() <= this.x + width &&
                player.getY() + Player.PLAYER_HEIGHT >= this.y &&
                player.getY() <= this.y + height;
    }
}