import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PreviewScene extends Pane {

    private static final int GROUND_Y = MainApplication.getHEIGHT() - 50;
    private static final int DINO_WIDTH = 40;
    private static final int DINO_HEIGHT = 60;
    private static final int DINO_START_X = 50;
    public static final double CAMERA_SPEED = 20;

    private List<Obstacle> obstacles = new ArrayList<>();
    private GraphicsContext gc;
    private double cameraX = 0;
    private AnimationTimer previewTimer;
    private MainApplication app;

    public PreviewScene(MainApplication app) {
        this.app = app;
        setPrefSize(MainApplication.getWIDTH(), MainApplication.getHEIGHT());

        Canvas canvas = new Canvas(MainApplication.getWIDTH(), MainApplication.getHEIGHT());
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        loadLevel("level1.txt");
    }

    public void startPreviewLoop() {
        previewTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderPreview();
            }
        };
        previewTimer.start();
    }

    public void stopPreviewLoop() {
        if (previewTimer != null) {
            previewTimer.stop();
        }
    }

    public void handleInput(KeyCode code) {
        if (code == KeyCode.LEFT) {
            moveCamera(-CAMERA_SPEED);
        } else if (code == KeyCode.RIGHT) {
            moveCamera(CAMERA_SPEED);
        } else if (code == KeyCode.M) {
            stopPreviewLoop();
            app.showStartMenu();
        }
    }

    public void moveCamera(double deltaX) {
        cameraX += deltaX;
        if (cameraX < 0)
            cameraX = 0;
    }

    private void loadLevel(String levelFilePath) {
        obstacles.clear();

        List<LevelObstacleData> tempLevelSequence = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(levelFilePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length == 5) {
                    try {
                        double spawnTriggerX = Double.parseDouble(parts[0].trim());
                        double width = Double.parseDouble(parts[1].trim());
                        double height = Double.parseDouble(parts[2].trim());
                        double yPosition = Double.parseDouble(parts[3].trim());
                        String type = parts[4].trim().toLowerCase();

                        if (!type.equals("regular") && !type.equals("platform")) {
                            System.err.println("關卡檔案中未知障礙物類型 '" + type + "'，行：" + line + "。預設為 'regular'。");
                            type = "regular";
                        }
                        tempLevelSequence.add(new LevelObstacleData(spawnTriggerX, width, height, yPosition, type));
                    } catch (NumberFormatException e) {
                        System.err.println("關卡檔案中數字格式無效，行：" + line + " - " + e.getMessage());
                    }
                } else {
                    System.err
                            .println("關卡檔案中格式無效，行：" + line + "。預期格式為 'spawn_trigger_x,width,height,y_position,type'。");
                }
            }

            for (LevelObstacleData data : tempLevelSequence) {
                Obstacle newObstacle;
                if (data.type.equals("platform")) {
                    newObstacle = new PlatformObstacle(data.width, data.height, data.yPosition);
                } else {
                    newObstacle = new RegularObstacle(data.width, data.height, data.yPosition);
                }
                obstacles.add(newObstacle);
            }

            if (obstacles.isEmpty()) {
                System.out.println("未載入關卡資料。預覽器將為空。");
            }

        } catch (Exception e) {
            System.err.println("載入關卡檔案失敗：" + levelFilePath + " - " + e.getMessage());
            System.out.println("預覽器載入關卡失敗。請檢查檔案路徑和格式。");
        }
    }

    private void renderPreview() {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, MainApplication.getWIDTH(), MainApplication.getHEIGHT());

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0 - cameraX, GROUND_Y, MainApplication.getWIDTH() + cameraX * 2,
                MainApplication.getHEIGHT() - GROUND_Y);

        for (Obstacle obstacle : obstacles) {
            obstacle.render(gc, cameraX);
        }

        gc.setFill(Color.GREEN);
        gc.fillRect(DINO_START_X - cameraX, GROUND_Y - DINO_HEIGHT, DINO_WIDTH, DINO_HEIGHT);
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 12));
        gc.fillText("Dino 起始點", DINO_START_X - cameraX, GROUND_Y - DINO_HEIGHT - 5);

        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 16));
        gc.fillText(String.format("攝影機 X: %.0f", cameraX), 10, 30);
        gc.fillText("按 M 返回選單", 10, 50);
    }
}