import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Pos;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainApplication extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 300;
    private static final int GROUND_Y = HEIGHT - 50; // 地面線的 Y 座標

    private Stage primaryStage;

    /**
     * JavaFX 應用程式的進入點。
     *
     * @param primaryStage 此應用程式的主要舞台。
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Dino 遊戲與關卡預覽");
        primaryStage.setResizable(false); // 為求簡潔，保持視窗固定大小

        showStartMenu(); // 顯示開始選單
    }

    /**
     * 顯示主選單場景。
     */
    private void showStartMenu() {
        VBox menuRoot = new VBox(20); // 垂直佈局，間距 20 像素
        menuRoot.setAlignment(Pos.CENTER); // 內容居中
        menuRoot.setPrefSize(WIDTH, HEIGHT);
        menuRoot.setStyle("-fx-background-color: lightblue;"); // 設定背景顏色

        // 標題
        Font titleFont = new Font("Arial", 40);
        javafx.scene.text.Text title = new javafx.scene.text.Text("Dino 遊戲");
        title.setFont(titleFont);
        title.setFill(Color.DARKBLUE);

        // 遊玩關卡按鈕
        Button playButton = new Button("遊玩關卡");
        playButton.setFont(new Font("Arial", 24));
        playButton.setOnAction(e -> showGameScene()); // 點擊後切換到遊戲場景

        // 預覽關卡按鈕
        Button previewButton = new Button("預覽關卡");
        previewButton.setFont(new Font("Arial", 24));
        previewButton.setOnAction(e -> showPreviewScene()); // 點擊後切換到預覽場景

        menuRoot.getChildren().addAll(title, playButton, previewButton);

        Scene menuScene = new Scene(menuRoot, WIDTH, HEIGHT);
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    /**
     * 顯示遊戲場景。
     */
    private void showGameScene() {
        GameScene game = new GameScene(this); // 傳遞 MainApplication 實例以便返回選單
        Scene gameScene = new Scene(game, WIDTH, HEIGHT);

        // 處理跳躍和重新開始的鍵盤輸入
        gameScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.UP) {
                if (!game.isGameOver() && !game.isLevelComplete()) { // 僅在遊戲活動時跳躍
                    game.getDino().jump();
                }
            }
            if (event.getCode() == KeyCode.R && (game.isGameOver() || game.isLevelComplete())) { // 遊戲結束或關卡完成時重新開始
                game.resetGame();
            }
            if (event.getCode() == KeyCode.M) { // 按 M 返回主選單
                game.stopGameLoop(); // 停止遊戲循環
                showStartMenu();
            }
        });

        primaryStage.setScene(gameScene);
        game.startGameLoop(); // 開始遊戲循環
    }

    /**
     * 顯示關卡預覽場景。
     */
    private void showPreviewScene() {
        PreviewScene preview = new PreviewScene(this); // 傳遞 MainApplication 實例以便返回選單
        Scene previewScene = new Scene(preview, WIDTH, HEIGHT);

        // 處理鍵盤輸入以移動攝影機和返回選單
        previewScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                preview.moveCamera(-PreviewScene.CAMERA_SPEED);
            } else if (event.getCode() == KeyCode.RIGHT) {
                preview.moveCamera(PreviewScene.CAMERA_SPEED);
            } else if (event.getCode() == KeyCode.M) { // 按 M 返回主選單
                preview.stopPreviewLoop(); // 停止預覽循環
                showStartMenu();
            }
        });

        primaryStage.setScene(previewScene);
        preview.startPreviewLoop(); // 開始預覽循環
    }

    /**
     * 啟動 JavaFX 應用程式的主方法。
     *
     * @param args 命令列參數。
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * 內部靜態類別，用於儲存關卡檔案中定義的障礙物資料。
     * 每個實例代表一個障礙物的屬性、其生成觸發 X 座標和其類型。
     */
    static class LevelObstacleData {
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
     * 所有障礙物的抽象基類。
     * 在遊戲中，障礙物的 x 座標是其在螢幕上的位置。
     * 在預覽器中，障礙物的 x 座標是其在遊戲世界中的絕對位置。
     */
    abstract static class Obstacle {
        protected static final int OBSTACLE_MIN_WIDTH = 20;
        protected static final int OBSTACLE_MAX_WIDTH = 40;
        protected static final int OBSTACLE_MIN_HEIGHT = 30;
        protected static final int OBSTACLE_MAX_HEIGHT = 60;
        protected static final int OBSTACLE_SPEED = 10; // 障礙物向左移動的速度

        protected double x; // 初始 X 座標 (螢幕右側之外) 或世界座標
        protected double y; // Y 座標 (在地面或平台上)
        protected double width; // 障礙物寬度
        protected double height; // 障礙物高度
        protected Color color; // 障礙物顏色
        protected long spawnTime; // 障礙物生成的時間 (奈秒)

        // 遊戲模式下的建構子
        public Obstacle() {
            this.x = WIDTH; // 預設從螢幕右側生成
            this.spawnTime = System.nanoTime(); // 記錄生成時間
        }

        // 預覽模式下的建構子 (帶世界座標)
        public Obstacle(double worldX, double width, double height, double yPosition, Color color) {
            this.x = worldX; // 這是它在遊戲世界中的絕對 X 座標
            this.width = width;
            this.height = height;
            this.y = yPosition; // Y 座標 (頂部)
            this.color = color;
        }

        /**
         * 更新障礙物的水平位置 (僅用於遊戲模式)。
         */
        public void update() {
            x -= OBSTACLE_SPEED;
        }

        /**
         * 在給定的 GraphicsContext 上渲染障礙物 (用於遊戲模式)。
         *
         * @param gc 要繪製的 GraphicsContext。
         */
        public abstract void render(GraphicsContext gc);

        /**
         * 在給定的 GraphicsContext 上渲染障礙物，並考慮攝影機的偏移 (用於預覽模式)。
         *
         * @param gc 要繪製的 GraphicsContext。
         * @param cameraX 攝影機的 X 偏移量。
         */
        public abstract void render(GraphicsContext gc, double cameraX);

        /**
         * 檢查障礙物是否已完全移出螢幕左側 (僅用於遊戲模式)。
         *
         * @return 如果障礙物超出螢幕，則為 true，否則為 false。
         */
        public boolean isOffScreen() {
            return x + width < 0;
        }

        /**
         * 返回障礙物的邊界框以進行碰撞偵測 (僅用於遊戲模式)。
         *
         * @return 表示障礙物當前邊界的 Rectangle2D。
         */
        public Rectangle2D getBounds() {
            return new Rectangle2D(x, y, width, height);
        }

        /**
         * 返回障礙物的 X 座標 (僅用於遊戲模式)。
         * @return 障礙物的 X 座標。
         */
        public double getX() {
            return x;
        }

        /**
         * 返回障礙物的生成時間 (僅用於遊戲模式)。
         * @return 障礙物的生成時間 (奈秒)。
         */
        public long getSpawnTime() {
            return spawnTime;
        }
    }

    /**
     * 常規障礙物的具體類別。
     */
    static class RegularObstacle extends Obstacle {
        // 遊戲模式下的建構子 (自訂尺寸和 Y 座標)
        public RegularObstacle(double width, double height, double yPosition) {
            super();
            this.width = width;
            this.height = height;
            this.y = yPosition;
            this.color = Color.BROWN;
        }

        // 遊戲模式下的預設建構子 (隨機尺寸，貼地)
        public RegularObstacle() {
            super();
            Random localRandom = new Random();
            this.width = OBSTACLE_MIN_WIDTH + localRandom.nextInt(OBSTACLE_MAX_WIDTH - OBSTACLE_MIN_WIDTH + 1);
            this.height = OBSTACLE_MIN_HEIGHT + localRandom.nextInt(OBSTACLE_MAX_HEIGHT - OBSTACLE_MIN_HEIGHT + 1);
            this.y = GROUND_Y - this.height;
            this.color = Color.BROWN;
        }

        // 預覽模式下的建構子 (帶世界座標)
        public RegularObstacle(double worldX, double width, double height, double yPosition) {
            super(worldX, width, height, yPosition, Color.BROWN);
        }

        @Override
        public void render(GraphicsContext gc) { // 遊戲模式渲染
            gc.setFill(color);
            gc.fillRect(x, y, width, height);
        }

        @Override
        public void render(GraphicsContext gc, double cameraX) { // 預覽模式渲染
            gc.setFill(color);
            gc.fillRect(x - cameraX, y, width, height);
        }
    }

    /**
     * 平台障礙物的具體類別。
     */
    static class PlatformObstacle extends Obstacle {
        // 遊戲模式下的建構子 (自訂尺寸和 Y 座標)
        public PlatformObstacle(double width, double height, double yPosition) {
            super();
            this.width = width;
            this.height = height;
            this.y = yPosition;
            this.color = Color.ORANGE;
        }

        // 遊戲模式下的預設建構子 (隨機尺寸，貼地) - 儘管平台通常在關卡中定義，但為了隨機生成時的一致性，此建構子仍然存在
        public PlatformObstacle() {
            super();
            Random localRandom = new Random();
            this.width = OBSTACLE_MIN_WIDTH + localRandom.nextInt(OBSTACLE_MAX_WIDTH - OBSTACLE_MIN_WIDTH + 1);
            this.height = OBSTACLE_MIN_HEIGHT + localRandom.nextInt(OBSTACLE_MAX_HEIGHT - OBSTACLE_MIN_HEIGHT + 1);
            this.y = GROUND_Y - this.height;
            this.color = Color.ORANGE;
        }

        // 預覽模式下的建構子 (帶世界座標)
        public PlatformObstacle(double worldX, double width, double height, double yPosition) {
            super(worldX, width, height, yPosition, Color.ORANGE);
        }

        @Override
        public void render(GraphicsContext gc) { // 遊戲模式渲染
            gc.setFill(color);
            gc.fillRect(x, y, width, height);
        }

        @Override
        public void render(GraphicsContext gc, double cameraX) { // 預覽模式渲染
            gc.setFill(color);
            gc.fillRect(x - cameraX, y, width, height);
        }
    }

    /**
     * 遊戲場景類別，包含 Dino 遊戲的所有邏輯。
     */
    static class GameScene extends Pane {
        private Dino dino;
        private List<Obstacle> obstacles = new ArrayList<>();
        private Random random = new Random();
        private long score = 0;
        private boolean gameOver = false;
        private boolean levelComplete = false;

        // 關卡自訂變數
        private List<LevelObstacleData> levelSequence = new ArrayList<>();
        private int currentSequenceIndex = 0;
        private double gameWorldDistance = 0; // 遊戲世界總共滾動的距離
        private double finalLevelDistance = 0; // 關卡的總距離，用於計算完成百分比
        private boolean levelModeActive = true; // 預設以關卡模式開始
        private boolean allLevelObstaclesSpawned = false; // 當所有關卡障礙物都已排程生成時為 true

        private GraphicsContext gc;
        private AnimationTimer gameTimer; // 遊戲循環計時器

        private MainApplication app; // 引用主應用程式以進行場景切換

        public GameScene(MainApplication app) {
            this.app = app;
            setPrefSize(WIDTH, HEIGHT);

            Canvas canvas = new Canvas(WIDTH, HEIGHT);
            gc = canvas.getGraphicsContext2D();
            getChildren().add(canvas);

            dino = new Dino();

            // 載入預設關卡。使用者可以更改此檔案。
            // 確保 "level1.txt" 在資源資料夾中 (例如：src/main/resources)
            loadLevel("level1.txt");
        }

        /**
         * 開始遊戲循環。
         */
        public void startGameLoop() {
            gameTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (!gameOver && !levelComplete) { // 僅在遊戲未結束且關卡未完成時更新
                        updateGame(now);
                        renderGame();
                    } else if (gameOver) {
                        renderGameOver();
                    } else if (levelComplete) {
                        renderLevelComplete(); // 渲染關卡完成畫面
                    }
                }
            };
            gameTimer.start();
        }

        /**
         * 停止遊戲循環。
         */
        public void stopGameLoop() {
            if (gameTimer != null) {
                gameTimer.stop();
            }
        }

        public Dino getDino() {
            return dino;
        }

        public boolean isGameOver() {
            return gameOver;
        }

        public boolean isLevelComplete() {
            return levelComplete;
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
            levelSequence.clear(); // 清除任何先前的關卡資料
            currentSequenceIndex = 0;
            gameWorldDistance = 0; // 重置遊戲世界距離
            levelModeActive = true;
            allLevelObstaclesSpawned = false; // 載入新關卡時重置此旗標
            levelComplete = false; // 重置關卡完成旗標
            finalLevelDistance = 0; // 重置關卡總距離

            try (InputStream is = getClass().getResourceAsStream(levelFilePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    // 忽略註解行 (以 # 開頭)
                    if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                        continue;
                    }

                    String[] parts = line.split(",");
                    if (parts.length == 5) { // 現在預期 5 個部分：spawn_trigger_x, width, height, y_position, type
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
                            levelSequence.add(new LevelObstacleData(spawnTriggerX, width, height, yPosition, type));
                        } catch (NumberFormatException e) {
                            System.err.println("關卡檔案中數字格式無效，行：" + line + " - " + e.getMessage());
                        }
                    } else {
                        System.err.println(
                                "關卡檔案中格式無效，行：" + line + "。預期格式為 'spawn_trigger_x,width,height,y_position,type'。");
                    }
                }
                if (!levelSequence.isEmpty()) {
                    // 設定關卡總距離為最後一個障礙物的 spawnTriggerX
                    finalLevelDistance = levelSequence.get(levelSequence.size() - 1).spawnTriggerX + WIDTH;
                } else {
                    // 如果沒有關卡資料，則總距離為 0
                    finalLevelDistance = 0;
                    levelModeActive = false; // 如果關卡檔案為空或無效，立即切換到隨機模式
                    allLevelObstaclesSpawned = true; // 沒有關卡障礙物可生成，因此視為已「生成」
                    System.out.println("未載入關卡資料。切換到隨機障礙物生成。");
                }

            } catch (Exception e) {
                System.err.println("載入關卡檔案失敗：" + levelFilePath + " - " + e.getMessage());
                levelModeActive = false; // 如果檔案未找到/不可讀，則回退到隨機生成
                allLevelObstaclesSpawned = true; // 沒有關卡障礙物可生成
                System.out.println("由於關卡載入錯誤，回退到隨機障礙物生成。");
            }
        }

        /**
         * 更新遊戲狀態，包括 Dino 的移動、障礙物的生成、移動和碰撞偵測。
         *
         * @param now 從 AnimationTimer 傳入的當前時間 (奈秒)。
         */
        private void updateGame(long now) {
            dino.update();

            // 遊戲世界距離隨著障礙物速度增加
            gameWorldDistance += Obstacle.OBSTACLE_SPEED;

            // 障礙物生成邏輯 (來自關卡序列或隨機生成)
            if (levelModeActive) {
                // 檢查關卡序列中是否有更多障礙物要生成
                if (currentSequenceIndex < levelSequence.size()) {
                    LevelObstacleData data = levelSequence.get(currentSequenceIndex);

                    // 檢查遊戲世界距離是否達到觸發點
                    if (gameWorldDistance >= data.spawnTriggerX) {
                        Obstacle newObstacle;
                        // 根據其類型建立新的障礙物
                        if (data.type.equals("platform")) {
                            newObstacle = new PlatformObstacle(data.width, data.height, data.yPosition);
                        } else { // 如果類型未知或為 "regular"，則預設為 regular
                            newObstacle = new RegularObstacle(data.width, data.height, data.yPosition);
                        }
                        obstacles.add(newObstacle);
                        currentSequenceIndex++; // 移動到序列中的下一個障礙物

                        if (currentSequenceIndex >= levelSequence.size()) {
                            // 所有關卡障礙物都已排程，在當前關卡障礙物通過後切換到隨機模式
                            allLevelObstaclesSpawned = true;
                            levelModeActive = false; // 不再從序列中生成關卡障礙物
                            System.out.println("所有關卡障礙物已排程。等待它們通過。");
                        }
                    }
                }
            } else if (allLevelObstaclesSpawned) {
                // 如果所有關卡障礙物都已生成，且我們不在關卡模式中，
                // 我們等待關卡完成 (障礙物列表為空)
                // 在此狀態下，在 levelComplete 為 true 之前不會生成新的障礙物。
            } else {
                // 隨機障礙物生成邏輯 (如果關卡載入失敗或關卡完成後啟用)
                // 這裡我們仍然使用時間延遲來生成隨機障礙物
                // 為了避免過於頻繁地生成隨機障礙物，我們需要一個計時器
                if (obstacles.isEmpty() || (now - (obstacles.get(obstacles.size() - 1).getSpawnTime())) > (1500 + random.nextInt(1000)) * 1_000_000L) {
                    // 如果沒有障礙物，或者距離上一個隨機障礙物生成已經足夠長時間
                    RegularObstacle newRandomObstacle = new RegularObstacle();
                    obstacles.add(newRandomObstacle);
                }
            }


            // 移動障礙物並移除那些超出螢幕的障礙物
            obstacles.removeIf(obstacle -> {
                obstacle.update();
                return obstacle.isOffScreen();
            });

            // 檢查關卡完成：所有關卡障礙物都已生成且螢幕上沒有障礙物
            if (allLevelObstaclesSpawned && obstacles.isEmpty()) {
                levelComplete = true;
                System.out.println("關卡完成！");
            }

            // Dino 和障礙物之間的碰撞偵測
            for (Obstacle obstacle : obstacles) {
                if (dino.getBounds().intersects(obstacle.getBounds())) {
                    if (obstacle instanceof PlatformObstacle) {
                        // 檢查 Dino 是否正在下落並落在平台上
                        // Dino 在這次更新的 Y 軸移動之前的底部位置
                        double dinoPreviousBottom = dino.y - dino.yVelocity + Dino.DINO_HEIGHT;
                        double platformTop = obstacle.y;

                        // 如果 Dino 正在下落 AND 其先前底部位置在平台頂部或之上
                        // 並且 Dino 的左邊緣在平台的右邊緣之前，右邊緣在平台的左邊緣之後
                        if (dino.yVelocity > 0 && dinoPreviousBottom <= platformTop &&
                                dino.x + Dino.DINO_WIDTH > obstacle.x && dino.x < obstacle.x + obstacle.width) {
                            dino.y = platformTop - Dino.DINO_HEIGHT; // 將 Dino 吸附到平台頂部
                            dino.yVelocity = 0; // 停止垂直移動
                            dino.isJumping = false; // 允許再次跳躍
                        } else {
                            // 側面碰撞或從底部撞擊
                            gameOver = true;
                            break;
                        }
                    } else { // 常規障礙物碰撞
                        gameOver = true;
                        break;
                    }
                }
            }

            // 如果遊戲仍在運行且未完成，則增加分數
            if (!gameOver && !levelComplete) {
                score++;
            }
        }

        /**
         * 將所有遊戲元素渲染到畫布上。
         */
        private void renderGame() {
            // 用淺灰色背景清除畫布
            gc.setFill(Color.LIGHTGRAY);
            gc.fillRect(0, 0, WIDTH, HEIGHT);

            // 繪製地面
            gc.setFill(Color.DARKGRAY);
            gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

            // 繪製 Dino
            dino.render(gc);

            // 繪製所有活動障礙物
            for (Obstacle obstacle : obstacles) {
                obstacle.render(gc);
            }

            // 繪製當前分數
            gc.setFill(Color.BLACK);
            gc.setFont(new Font("Arial", 20));
            gc.fillText("分數: " + score, 10, 30);

            // 繪製關卡進度
            if (levelModeActive || allLevelObstaclesSpawned) {
                double completionPercentage = (gameWorldDistance / finalLevelDistance) * 100;
                if (completionPercentage > 100) completionPercentage = 100; // 防止超過 100%
                String completionText = String.format("進度: %.1f%%", completionPercentage);
                gc.fillText(completionText, WIDTH - 120, 30);
            }
        }

        /**
         * 渲染遊戲結束畫面，並提供重新開始的選項。
         */
        private void renderGameOver() {
            // 在繪製遊戲結束畫面之前，用淺灰色背景清除畫布
            gc.setFill(Color.LIGHTGRAY);
            gc.fillRect(0, 0, WIDTH, HEIGHT);

            // 繪製地面 (可選，但保持一致性)
            gc.setFill(Color.DARKGRAY);
            gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

            // 顯示 "Game Over" 訊息
            gc.setFill(Color.BLACK);
            gc.setFont(new Font("Arial", 50));
            gc.fillText("遊戲結束", WIDTH / 2 - gc.getFont().getSize() * 2.5, HEIGHT / 2 - 20); // 大致居中文字

            // 顯示重新開始說明
            gc.setFont(new Font("Arial", 20));
            gc.fillText("按 R 重新開始", WIDTH / 2 - gc.getFont().getSize() * 4, HEIGHT / 2 + 20);
            gc.fillText("按 M 返回選單", WIDTH / 2 - gc.getFont().getSize() * 4, HEIGHT / 2 + 45);


            // 顯示最終分數
            gc.setFont(new Font("Arial", 16));
            gc.fillText("最終分數: " + score, WIDTH / 2 - gc.getFont().getSize() * 4, HEIGHT / 2 + 70);

            // 顯示關卡完成百分比
            String completionText;
            if (levelComplete) { // 如果關卡已經完成 (玩家贏了關卡)
                completionText = "關卡完成度: 100.0%";
            } else /* if (levelModeActive && finalLevelDistance > 0) */ { // 如果在關卡模式中結束，且有關卡總距離
                double completionPercentage = (gameWorldDistance / finalLevelDistance) * 100;
                if (completionPercentage > 100) completionPercentage = 100; // 防止超過 100%
                completionText = String.format("關卡完成度: %.1f%%", completionPercentage);
            }
            gc.fillText(completionText, WIDTH / 2 - gc.getFont().getSize() * 4, HEIGHT / 2 + 95);
        }

        /**
         * 渲染關卡完成畫面。
         */
        private void renderLevelComplete() {
            // 在繪製關卡完成畫面之前，用淺灰色背景清除畫布
            gc.setFill(Color.LIGHTGRAY);
            gc.fillRect(0, 0, WIDTH, HEIGHT);

            // 繪製地面 (可選，但保持一致性)
            gc.setFill(Color.DARKGRAY);
            gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

            // 顯示 "Level Complete!" 訊息
            gc.setFill(Color.BLUE); // 關卡完成使用不同的顏色
            gc.setFont(new Font("Arial", 50));
            gc.fillText("關卡完成！", WIDTH / 2 - gc.getFont().getSize() * 3.5, HEIGHT / 2 - 20); // 大致居中文字

            // 顯示重新開始說明
            gc.setFill(Color.BLACK);
            gc.setFont(new Font("Arial", 20));
            gc.fillText("按 R 重新開始", WIDTH / 2 - gc.getFont().getSize() * 4, HEIGHT / 2 + 20);
            gc.fillText("按 M 返回選單", WIDTH / 2 - gc.getFont().getSize() * 4, HEIGHT / 2 + 45);

            // 顯示最終分數
            gc.setFont(new Font("Arial", 16));
            gc.fillText("最終分數: " + score, WIDTH / 2 - gc.getFont().getSize() * 4, HEIGHT / 2 + 70);

            // 關卡完成時，直接顯示 100%
            gc.fillText("關卡完成度: 100.0%", WIDTH / 2 - gc.getFont().getSize() * 4, HEIGHT / 2 + 95);
        }

        /**
         * 將遊戲重置為初始狀態。
         */
        private void resetGame() {
            dino = new Dino(); // 建立一個新的 Dino
            obstacles.clear(); // 清除所有現有障礙物
            score = 0; // 重置分數
            gameOver = false; // 將遊戲結束旗標設為 false
            levelComplete = false; // 重置關卡完成旗標
            allLevelObstaclesSpawned = false; // 重置此旗標
            gameWorldDistance = 0; // 重置遊戲世界距離
            finalLevelDistance = 0; // 重置關卡總距離

            // 重新載入關卡以重新播放關卡序列
            loadLevel("level1.txt"); // 重新載入相同的關卡檔案
        }

        /**
         * 代表 Dino 角色的內部類別。
         */
        class Dino {
            private static final int DINO_WIDTH = 40;
            private static final int DINO_HEIGHT = 60;
            private static final int JUMP_STRENGTH = -18; // 向上移動為負值
            private static final double GRAVITY = 1.5;

            private double y = GROUND_Y - DINO_HEIGHT; // 地面上的初始 Y 座標
            private double x = 50; // 固定 X 座標
            private double yVelocity = 0; // 當前垂直速度
            private boolean isJumping = false; // 防止空中多次跳躍的旗標

            /**
             * 如果 Dino 不在空中，則使其跳躍。
             */
            public void jump() {
                if (!isJumping) {
                    yVelocity = JUMP_STRENGTH;
                    isJumping = true;
                }
            }

            /**
             * 根據速度和重力更新 Dino 的垂直位置。
             */
            public void update() {
                y += yVelocity;
                yVelocity += GRAVITY;

                // 防止 Dino 掉落穿過地面
                if (y >= GROUND_Y - DINO_HEIGHT) {
                    y = GROUND_Y - DINO_HEIGHT; // 吸附到地面
                    yVelocity = 0; // 停止垂直移動
                    isJumping = false; // 允許再次跳躍
                }
            }

            /**
             * 在給定的 GraphicsContext 上渲染 Dino。
             *
             * @param gc 要繪製的 GraphicsContext。
             */
            public void render(GraphicsContext gc) {
                gc.setFill(Color.GREEN); // 簡單的綠色矩形代表 Dino
                gc.fillRect(x, y, DINO_WIDTH, DINO_HEIGHT);
            }

            /**
             * 返回 Dino 的邊界框以進行碰撞偵測。
             *
             * @return 表示 Dino 當前邊界的 Rectangle2D。
             */
            public Rectangle2D getBounds() {
                return new Rectangle2D(x, y, DINO_WIDTH, DINO_HEIGHT);
            }
        }
    }

    /**
     * 關卡預覽場景類別，包含關卡預覽的所有邏輯。
     */
    static class PreviewScene extends Pane {

        private static final int DINO_WIDTH = 40;
        private static final int DINO_HEIGHT = 60;
        private static final int DINO_START_X = 50; // Dino 在遊戲中的固定起始 X 座標

        private List<Obstacle> obstacles = new ArrayList<>();
        private GraphicsContext gc;

        private double cameraX = 0; // 攝影機的 X 偏移量，控制視圖的滾動
        public static final double CAMERA_SPEED = 20; // 攝影機移動的速度

        private AnimationTimer previewTimer; // 預覽循環計時器
        private MainApplication app; // 引用主應用程式以進行場景切換

        public PreviewScene(MainApplication app) {
            this.app = app;
            setPrefSize(WIDTH, HEIGHT);

            Canvas canvas = new Canvas(WIDTH, HEIGHT);
            gc = canvas.getGraphicsContext2D();
            getChildren().add(canvas);

            // 載入預設關卡。使用者可以更改此檔案。
            loadLevel("level1.txt");
        }

        /**
         * 開始預覽循環。
         */
        public void startPreviewLoop() {
            previewTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    renderPreview();
                }
            };
            previewTimer.start();
        }

        /**
         * 停止預覽循環。
         */
        public void stopPreviewLoop() {
            if (previewTimer != null) {
                previewTimer.stop();
            }
        }

        /**
         * 移動攝影機。
         * @param deltaX 移動量。
         */
        public void moveCamera(double deltaX) {
            cameraX += deltaX;
            // 防止 cameraX 變成負值，除非您希望可以向左無限滾動
            if (cameraX < 0) cameraX = 0;
            // 可以設定一個最大值，例如關卡中最遠障礙物的 X 加上螢幕寬度
            // 這裡暫不設定最大值，允許無限向右滾動
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
                    // 在預覽模式下，我們直接使用 spawnTriggerX 作為障礙物的世界 X 座標
                    if (data.type.equals("platform")) {
                        newObstacle = new PlatformObstacle(data.spawnTriggerX, data.width, data.height, data.yPosition);
                    } else {
                        newObstacle = new RegularObstacle(data.spawnTriggerX, data.width, data.height, data.yPosition);
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
            gc.fillText("按 M 返回選單", 10, 50);
        }
    }
}
