package com.dino;

import javafx.scene.canvas.GraphicsContext;
// import java.util.Random;
import javafx.scene.paint.Color;

public class LemonObstacle extends Obstacle {

    // 新增定時器
    private int timer;
    // public RegularObstacle() {
    // super();
    // Random localRandom = new Random();
    // this.width = OBSTACLE_MIN_WIDTH + localRandom.nextInt(OBSTACLE_MAX_WIDTH -
    // OBSTACLE_MIN_WIDTH + 1);
    // this.height = OBSTACLE_MIN_HEIGHT + localRandom.nextInt(OBSTACLE_MAX_HEIGHT -
    // OBSTACLE_MIN_HEIGHT + 1);
    // this.y = MainApplication.getHEIGHT() - 50 - this.height;
    // this.color = Color.BROWN;
    // }

    public LemonObstacle(double width, double height, double yPosition) {
        super(width, height, yPosition, ".//picture/lemon.png");
        this.timer = 150;
        this.x = 0;
    }

    @Override
    public void update() {
        timer--;
    }

    @Override
    public void render(GraphicsContext gc) {
        if (this.timer <= 0) {
            gc.drawImage(image, x, y);
        } else {
            gc.setStroke(Color.BLUE); // 設定線條顏色
            gc.setLineWidth(6); // 設定線條寬度
            gc.strokeLine(x, y, x + width, y);
            gc.strokeLine(x, y + height, x + width, y + height); // 從 (x1, y1) 畫到 (x2, y2)
            gc.strokeLine(x + width, y, x + width, y + height);
            gc.strokeLine(x, y, x, y + height);

        }
    }

    @Override
    public void render(GraphicsContext gc, double cameraY) {
        gc.drawImage(image, x, y - cameraY);
    }

    @Override
    public boolean isOffScreen() {
        if (timer < -10) {
            return true;
        }
        return false;
    }

    /**
     * 改良後的碰撞/攻擊判定
     * 
     * @param player  玩家
     * @param cameraX 相機水平位移
     * @return 若在高亮結束後玩家仍在此區域則回傳 true
     */
    public boolean getColide(Player player) {
        // 若高亮結束，判定玩家是否仍在區域內
        if (timer == 0) {
            int playerWidth = Player.PLAYER_WIDTH;
            int playerHeight = Player.PLAYER_HEIGHT;
            double px = player.getX();
            double py = player.getY();
            boolean insideX = px + playerWidth >= this.x && px <= this.x + width;
            boolean insideY = py + playerHeight >= this.y && py <= this.y + height;
            return insideX && insideY;
        }
        return false;
    }
}
