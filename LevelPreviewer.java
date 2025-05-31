import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random; // 雖然預覽器中沒有隨機生成，但為了 Obstacle 類別的預設建構子，保留此導入

public class LevelPreviewer extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 300;
    private static final int GROUND_Y = HEIGHT - 50; // 地面線的 Y 座標

    // Dino 的固定尺寸，用於在預覽器中顯示其起始位置
    private static final int DINO_WIDTH = 40;
    private static final int DINO_HEIGHT = 60;
    private static final int DINO_START_X = 50; // Dino 在遊戲中的固定起始 X 座標

    private List<Obstacle> obstacles = new ArrayList<>();
    private GraphicsContext gc;

    private double cameraX = 0; // 攝影機的 X 偏移量，控制視圖的滾動
    private final double CAMERA_SPEED = 20; // 攝影機移動的速度

    /**
     * 內部類別，用於儲存關卡檔案中定義的障礙物資料。
     * 每個實例代表一個障礙物的屬性、其生成觸發 X 座標和其類型。
     */
    private static class LevelObstacleData {
        double spawnTriggerX; // 障礙物生成的觸發 X 座標 (遊戲世界滾動距離)
        double width;
        double height;
        double yPosition; // 障礙物的 Y 座標 (頂部)
        String type; // "regular" 或 "platform"

        public LevelObstacleData(double spawnTriggerX, double width, double height, double yPosition, String type) {
            this.spawnTriggerX = spawnTriggerX;
            this.width = width;
            this.height = height;
            this.yPosition = yPosition;
            this.type = type;
        }
    }

    /**
     * 從純文字檔案載入自訂關卡。
     * 檔案中的每一行應為以下格式：spawn_trigger_x,width,height,y_position,type
     *
     * @param levelFilePath 關卡檔案的路徑 (例如："level1.txt")。
     * 此檔案應放置在資源資料夾中 (例如：src/main/resources)
     * 或編譯後的 .class 檔案所在的相同目錄中。
     */
    private void loadLevel(String levelFilePath) {
        obstacles.clear(); // 清除任何先前的障礙物

        List<LevelObstacleData> tempLevelSequence = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(levelFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 忽略註解行 (以 # 開頭)
                if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length == 5) { // 預期 5 個部分：spawn_trigger_x, width, height, y_position, type
                    try {
                        double spawnTriggerX = Double.parseDouble(parts[0].trim());
                        double width = Double.parseDouble(parts[1].trim());
                        double height = Double.parseDouble(parts[2].trim());
                        double yPosition = Double.parseDouble(parts[3].trim());
                        String type = parts[4].trim().toLowerCase();

                        if (!type.equals("regular") && !type.equals("platform")) {
                            System.err.println("關卡檔案中未知障礙物類型 '" + type + "'，行：" + line
                                    + "。預設為 'regular'。");
                            type = "regular";
                        }
                        tempLevelSequence.add(new LevelObstacleData(spawnTriggerX, width, height, yPosition, type));
                    } catch (NumberFormatException e) {
                        System.err.println("關卡檔案中數字格式無效，行：" + line + " - " + e.getMessage());
                    }
                } else {
                    System.err.println(
                            "關卡檔案中格式無效，行：" + line + "。預期格式為 'spawn_trigger_x,width,height,y_position,type'。");
                }
            }

            // 根據載入的 LevelObstacleData 建立實際的 Obstacle 物件
            for (LevelObstacleData data : tempLevelSequence) {
                Obstacle newObstacle;
                if (data.type.equals("platform")) {
                    // 將 spawnTriggerX 加上視窗寬度，使其從螢幕右側開始
                    newObstacle = new PlatformObstacle(data.spawnTriggerX + WIDTH, data.width, data.height, data.yPosition);
                } else {
                    // 將 spawnTriggerX 加上視窗寬度，使其從螢幕右側開始
                    newObstacle = new RegularObstacle(data.spawnTriggerX + WIDTH, data.width, data.height, data.yPosition);
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

    /**
     * 建立預覽器的主內容面板，包括畫布和渲染循環。
     *
     * @return 包含預覽器元素的根 Pane。
     */
    private Parent createContent() {
        Pane root = new Pane();
        root.setPrefSize(WIDTH, HEIGHT);

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        // 載入預設關卡。使用者可以更改此檔案。
        loadLevel("level1.txt");

        // 渲染循環
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                renderPreview();
            }
        };
        timer.start();

        return root;
    }

    /**
     * 渲染所有遊戲元素到畫布上，並考慮攝影機的偏移。
     */
    private void renderPreview() {
        // 用淺灰色背景清除畫布
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // 繪製地面，根據攝影機偏移
        gc.setFill(Color.DARKGRAY);
        // 繪製一個足夠寬的地面，以覆蓋攝影機移動的範圍
        gc.fillRect(0 - cameraX, GROUND_Y, WIDTH + cameraX * 2, HEIGHT - GROUND_Y);

        // 繪製所有障礙物，根據攝影機偏移
        for (Obstacle obstacle : obstacles) {
            obstacle.render(gc, cameraX);
        }

        // 繪製 Dino 的起始位置指示器
        gc.setFill(Color.GREEN);
        gc.fillRect(DINO_START_X - cameraX, GROUND_Y - DINO_HEIGHT, DINO_WIDTH, DINO_HEIGHT);
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 12));
        gc.fillText("Dino 起始點", DINO_START_X - cameraX, GROUND_Y - DINO_HEIGHT - 5);

        // 繪製攝影機當前 X 座標的指示
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 16));
        gc.fillText(String.format("攝影機 X: %.0f", cameraX), 10, 30);
    }

    /**
     * JavaFX 應用程式的進入點。
     *
     * @param primaryStage 此應用程式的主要舞台。
     */
    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(createContent());

        // 處理鍵盤輸入以移動攝影機
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                cameraX -= CAMERA_SPEED;
                // 防止 cameraX 變成負值，除非您希望可以向左無限滾動
                if (cameraX < 0) cameraX = 0;
            } else if (event.getCode() == KeyCode.RIGHT) {
                cameraX += CAMERA_SPEED;
                // 可以設定一個最大值，例如關卡中最遠障礙物的 X 加上螢幕寬度
                // 這裡暫不設定最大值，允許無限向右滾動
            }
        });

        primaryStage.setTitle("關卡預覽器 (Level Previewer)");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // 為求簡潔，保持視窗固定大小
        primaryStage.show();
    }

    /**
     * 啟動 JavaFX 應用程式的主方法。
     *
     * @param args 命令列參數。
     */
    public static void main(String[] args) {
        launch(args);
    }

    // 所有障礙物的抽象基類
    // 在預覽器中，障礙物的 x 座標是其在遊戲世界中的絕對位置
    abstract static class Obstacle { // 設為 static 以便在 LevelPreviewer 中作為內部類別使用
        protected double x; // 這將是它在遊戲世界中的絕對 X 座標
        protected double y; // Y 座標 (頂部)
        protected double width; // 障礙物寬度
        protected double height; // 障礙物高度
        protected Color color; // 障礙物顏色

        public Obstacle(double worldX, double width, double height, double yPosition, Color color) {
            this.x = worldX;
            this.width = width;
            this.height = height;
            this.y = yPosition;
            this.color = color;
        }

        // 渲染方法需要攝影機的偏移量
        public abstract void render(GraphicsContext gc, double cameraX);

        // 在預覽器中，我們不需要 getBounds() 或 isOffScreen()，因為沒有碰撞和動態移除
    }

    // 常規障礙物的具體類別
    static class RegularObstacle extends Obstacle { // 設為 static
        public RegularObstacle(double worldX, double width, double height, double yPosition) {
            super(worldX, width, height, yPosition, Color.BROWN);
        }

        @Override
        public void render(GraphicsContext gc, double cameraX) {
            gc.setFill(color);
            // 繪製時減去攝影機的 X 偏移量
            gc.fillRect(x - cameraX, y, width, height);
        }
    }

    // 平台障礙物的具體類別
    static class PlatformObstacle extends Obstacle { // 設為 static
        public PlatformObstacle(double worldX, double width, double height, double yPosition) {
            super(worldX, width, height, yPosition, Color.ORANGE);
        }

        @Override
        public void render(GraphicsContext gc, double cameraX) {
            gc.setFill(color);
            // 繪製時減去攝影機的 X 偏移量
            gc.fillRect(x - cameraX, y, width, height);
        }
    }
}
