package com.dino;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class Wave extends Player {
    public static final int WAVE_WIDTH = PLAYER_WIDTH;
    public static final int WAVE_HEIGHT = PLAYER_HEIGHT;
    private static final double VERTICAL_SPEED = 10; // Constant speed for rising/falling

    private boolean isUpKeyPressed = false;
    private Image waveUpImage;
    private Image waveDownImage;

    public Wave() {
        this.waveUpImage = new Image(getClass().getResourceAsStream("/picture/wave_up.png"), WAVE_WIDTH, WAVE_HEIGHT, false, false);
        this.waveDownImage = new Image(getClass().getResourceAsStream("/picture/wave_down.png"), WAVE_WIDTH, WAVE_HEIGHT, false, false);
    }

    public void setUpKeyPressed(boolean pressed) {
        this.isUpKeyPressed = pressed;
    }

    public boolean isUpKeyPressed() {
        return isUpKeyPressed;
    }

    @Override
    public void update() {
        // If up key is pressed, rise at constant speed
        if (isUpKeyPressed) {
            yVelocity = -VERTICAL_SPEED;
        } else {
            // If up key is not pressed, fall at constant speed
            yVelocity = VERTICAL_SPEED;
        }

        // Update position
        y += yVelocity;

        // Prevent going above the screen
        if (y < 0) {
            y = 0;
        }

        // Prevent going below the ground
        if (y >= MainApplication.getHEIGHT() - 50 - WAVE_HEIGHT) {
            y = MainApplication.getHEIGHT() - 50 - WAVE_HEIGHT;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // Use waveUpImage when rising (negative yVelocity), waveDownImage when falling (positive yVelocity)
        if (yVelocity < 0) {
            gc.drawImage(waveUpImage, x, y);
        } else {
            gc.drawImage(waveDownImage, x, y);
        }
    }
}
