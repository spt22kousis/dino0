package com.dino;

import javafx.scene.canvas.GraphicsContext;

public class RegularObstacle extends Obstacle {

    private static boolean isLevel2 = false;

    public static void setIsLevel2(boolean level2) {
        isLevel2 = level2;
    }

    public RegularObstacle(double width, double height, double yPosition) {
        super(width, height, yPosition, isLevel2 ? "/picture/regular2.jpg" : "/picture/regular.jpg");
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