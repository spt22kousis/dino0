import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

enum GameMode {
    DINO,
    WAVE
}
// import java.util.Random;

public class GameScene extends Pane {

    private static final int GROUND_Y = MainApplication.getHEIGHT() - 50;

    private Player player;
    private GameMode gameMode = GameMode.DINO; // Default game mode
    private List<Obstacle> obstacles = new ArrayList<>();
    // private Random random = new Random();
    private long score = 0;
    private boolean gameOver = false;
    private boolean dying = false;
    private long deathAnimationStartTime = 0;
    private static final long DEATH_ANIMATION_DURATION = 500_000_000; // 0.5 seconds in nanoseconds
    private Image explosionImage;
    private ImageView explosionView;
    private boolean levelComplete = false;

    private List<LevelObstacleData> levelSequence = new ArrayList<>();
    private int currentSequenceIndex = 0;
    private double gameWorldDistance = 0;
    private double finalLevelDistance = 0;
    private boolean levelModeActive = true;
    private boolean allLevelObstaclesSpawned = false;
    private String currentLevelFile = "level1.txt"; // Default level file

    private GraphicsContext gc;
    // 控制遊戲迴圈的執行續
    private AnimationTimer gameTimer;
    // 與主執行程式連結
    private MainApplication app;
    // 背景圖片
    private Background background;

    public GameScene(MainApplication app, String levelFile) {
        this.app = app;
        setPrefSize(MainApplication.getWIDTH(), MainApplication.getHEIGHT());

        Canvas canvas = new Canvas(MainApplication.getWIDTH(), MainApplication.getHEIGHT());
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);
        // 2. 讓 GameScene 這個 Pane 可以拿到鍵盤 focus，並在按鍵時呼叫 handleInput
        setFocusTraversable(true);
        setOnKeyPressed(evt -> handleInput(evt.getCode()));
        setOnKeyReleased(evt -> handleKeyReleased(evt.getCode()));
        // 注意：到畫面真正顯示前（Stage.show()）這個 requestFocus 有時候還無效，
        // 但可以先呼叫一次。真正顯示後 GameScene 才能拿到焦點。
        requestFocus();

        // 載入爆炸圖片
        explosionImage = new Image("file:./picture/explosion.png");

        // Initialize player based on game mode
        createPlayer();
        loadLevel(levelFile);

        // 初始化背景 - 根據關卡選擇不同背景
        if (levelFile.equals("level2.txt")) {
            background = new Background("./picture/bg2.jpg", MainApplication.getWIDTH(), MainApplication.getHEIGHT());
        } else {
            background = new Background("./picture/bg.jpg", MainApplication.getWIDTH(), MainApplication.getHEIGHT());
        }
    }

    public void startGameLoop() {
        gameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 不管要不要顯示「遊戲結束/通關畫面」，都先繪製一次遊戲畫面或結算畫面
                if (!gameOver && !levelComplete) {
                    if (dying) {
                        // 處理死亡動畫
                        renderGame();
                        renderDeathAnimation(now);

                        // 檢查死亡動畫是否完成
                        if (now - deathAnimationStartTime >= DEATH_ANIMATION_DURATION) {
                            gameOver = true;
                        }
                    } else {
                        updateGame(now);
                        renderGame();
                    }
                } else if (gameOver) {
                    renderGameOver();
                    stopGameLoop();
                } else {
                    renderLevelComplete();
                    stopGameLoop();
                }
            }
        };
        BgmPlayer.getInstance().play();
        gameTimer.start();
    }

    public void stopGameLoop() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }

    // Create player based on game mode
    private void createPlayer() {
        if (gameMode == GameMode.DINO) {
            player = new Dino();
        } else if (gameMode == GameMode.WAVE) {
            player = new Wave();
        }
    }

    public Player getPlayer() {
        return player;
    }

    // For backward compatibility
    public Dino getDino() {
        if (player instanceof Dino) {
            return (Dino) player;
        }
        return null;
    }

    // Set game mode and create appropriate player
    public void setGameMode(GameMode mode) {
        this.gameMode = mode;
        createPlayer();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isLevelComplete() {
        return levelComplete;
    }

    // 偵測鍵盤輸入
    public void handleInput(KeyCode code) {
        if (!gameOver && !levelComplete) {
            if (code == KeyCode.SPACE || code == KeyCode.UP) {
                if (gameMode == GameMode.DINO && player instanceof Dino) {
                    ((Dino) player).jump();
                } else if (gameMode == GameMode.WAVE && player instanceof Wave) {
                    ((Wave) player).setUpKeyPressed(true);
                }
            } else if (code == KeyCode.DOWN) {
                if (gameMode == GameMode.WAVE && player instanceof Wave) {
                    ((Wave) player).setUpKeyPressed(false);
                }
            }
        }

        if (code == KeyCode.R && (gameOver || levelComplete)) {
            resetGame();
        }
        if (code == KeyCode.M) {
            stopGameLoop();
            app.showStartMenu();
        }
    }

    // Key release handler for Wave mode
    public void handleKeyReleased(KeyCode code) {
        if (gameMode == GameMode.WAVE && player instanceof Wave) {
            if (code == KeyCode.SPACE || code == KeyCode.UP) {
                ((Wave) player).setUpKeyPressed(false);
            }
        }
    }

    private void loadLevel(String levelFilePath) {
        levelSequence.clear();
        currentSequenceIndex = 0;
        gameWorldDistance = 0;
        levelModeActive = true;
        allLevelObstaclesSpawned = false;
        levelComplete = false;
        finalLevelDistance = 0;

        // Store the current level file
        currentLevelFile = levelFilePath;

        // Set game mode based on level file
        boolean isLevel2 = levelFilePath.equals("level2.txt");
        if (levelFilePath.equals("level1.txt")) {
            setGameMode(GameMode.DINO);
            PlatformObstacle.setIsLevel2(false);
            RegularObstacle.setIsLevel2(false);
        } else if (isLevel2) {
            setGameMode(GameMode.WAVE);
            PlatformObstacle.setIsLevel2(true);
            RegularObstacle.setIsLevel2(true);
        }

        try (InputStream is = getClass().getResourceAsStream(levelFilePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line, songpath;
            // 載入音樂
            songpath = reader.readLine();
            BgmPlayer.init(songpath);
            // 載入障礙物
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

                        if (!type.equals("regular") && !type.equals("platform") && !type.equals("lemon")) {
                            System.err.println("關卡檔案中未知障礙物類型 '" + type + "'，行：" + line + "。預設為 'regular'。");
                            type = "regular";
                        }
                        levelSequence.add(new LevelObstacleData(spawnTriggerX, width, height, yPosition, type));
                    } catch (NumberFormatException e) {
                        System.err.println("關卡檔案中數字格式無效，行：" + line + " - " + e.getMessage());
                    }
                } else {
                    System.err
                            .println("關卡檔案中格式無效，行：" + line + "。預期格式為 'spawn_trigger_x,width,height,y_position,type'。");
                }
            }
            if (!levelSequence.isEmpty()) {
                finalLevelDistance = levelSequence.get(levelSequence.size() - 1).spawnTriggerX
                        + MainApplication.getWIDTH();
            } else {
                finalLevelDistance = 0;
                levelModeActive = false;
                allLevelObstaclesSpawned = true;
                System.out.println("未載入關卡資料。切換到隨機障礙物生成。");
            }

        } catch (Exception e) {
            System.err.println("載入關卡檔案失敗：" + levelFilePath + " - " + e.getMessage());
            levelModeActive = false;
            allLevelObstaclesSpawned = true;
            System.out.println("由於關卡載入錯誤，回退到隨機障礙物生成。");
        }
    }

    private void updateGame(long now) {
        if (!dying) {
            player.update();
            gameWorldDistance += Obstacle.OBSTACLE_SPEED;
            background.update(gameWorldDistance);
        }

        if (levelModeActive) {
            while (currentSequenceIndex < levelSequence.size()) {
                LevelObstacleData data = levelSequence.get(currentSequenceIndex);
                if (gameWorldDistance >= data.spawnTriggerX) {
                    Obstacle newObstacle;
                    if (data.type.equals("platform")) {
                        newObstacle = new PlatformObstacle(data.width, data.height, data.yPosition);
                    } else if (data.type.equals("regular")) {
                        newObstacle = new RegularObstacle(data.width, data.height, data.yPosition);
                    } else {
                        newObstacle = new LemonObstacle(data.width, data.height, data.yPosition);
                    }
                    obstacles.add(newObstacle);
                    currentSequenceIndex++;
                    // 判斷是否還有障礙物未生成
                    if (currentSequenceIndex >= levelSequence.size()) {
                        allLevelObstaclesSpawned = true;
                        levelModeActive = false;
                        System.out.println("所有關卡障礙物已排程。等待它們通過。");
                        break; // 沒有更多障礙物，跳出迴圈
                    }
                    // 下一個障礙物也是同一個生成位置
                    if (levelSequence.get(currentSequenceIndex).spawnTriggerX == data.spawnTriggerX) {
                        // 等待迴圈再次執行，物件就會被加入世界中
                        continue;
                    } else {
                        // 下個障礙物生成位置不同，跳出迴圈
                        break;
                    }
                } else {
                    break;
                }
            }
        } else if (!allLevelObstaclesSpawned) {
            // if (obstacles.isEmpty()
            // || (now - (obstacles.get(obstacles.size() - 1).getSpawnTime())) > (1500 +
            // random.nextInt(1000))
            // * 1_000_000L) {
            // RegularObstacle newRandomObstacle = new RegularObstacle();
            // obstacles.add(newRandomObstacle);
            // }
            System.exit(-1);
        }

        if (!dying) {
            obstacles.removeIf(obstacle -> {
                obstacle.update();
                return obstacle.isOffScreen();
            });
        }

        if (allLevelObstaclesSpawned && obstacles.isEmpty()) {
            levelComplete = true;
            System.out.println("關卡完成！");
        }

        for (Obstacle obstacle : obstacles) {
            if (obstacle.getColide(player)) {
                if (!dying) {
                    dying = true;
                    deathAnimationStartTime = now;
                    // 清除爆炸視圖，以便在renderDeathAnimation中重新創建
                    explosionView = null;
                }
                break;
            }
        }

        if (!gameOver && !levelComplete) {
            score++;
        }
    }

    private void renderGame() {
        // 繪製背景
        background.render(gc);

        // 繪製地面
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0, GROUND_Y, MainApplication.getWIDTH(), MainApplication.getHEIGHT() - GROUND_Y);

        // 只有在非死亡狀態才繪製玩家
        if (!dying) {
            player.render(gc);
        }

        for (Obstacle obstacle : obstacles) {
            obstacle.render(gc);
        }

        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 20));
        gc.fillText("分數: " + score, 10, 30);

        if (levelModeActive || allLevelObstaclesSpawned) {
            double completionPercentage = (gameWorldDistance / finalLevelDistance) * 100;
            if (completionPercentage > 100)
                completionPercentage = 100;
            String completionText = String.format("進度: %.1f%%", completionPercentage);
            gc.fillText(completionText, MainApplication.getWIDTH() - 120, 30);
        }
    }

    private void renderDeathAnimation(long now) {
        if (explosionView == null) {
            // 第一次呼叫時，創建爆炸效果
            explosionView = new ImageView(explosionImage);
            explosionView.setFitWidth(Player.PLAYER_WIDTH * 1.5);
            explosionView.setFitHeight(Player.PLAYER_HEIGHT * 1.5);
            explosionView.setX(player.getX() - (explosionView.getFitWidth() - Player.PLAYER_WIDTH) / 2);
            explosionView.setY(player.getY() - (explosionView.getFitHeight() - Player.PLAYER_HEIGHT) / 2);

            // 添加爆炸圖片到場景
            this.getChildren().add(explosionView);

            // 創建淡出動畫
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), explosionView);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.play();
        }
    }

    private void renderGameOver() {
        // 1. 停止背景音樂
        BgmPlayer.getInstance().stop();

        // 2. 載入 GameOver.fxml
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("./GameOver.fxml"));
            Parent gameOverRoot = loader.load();

            // 3. 取得 Controller，把分數與完成度傳進去
            GameOverController controller = loader.getController();
            controller.setScore(score);

            String completionText;
            double completionPercentage = (gameWorldDistance / finalLevelDistance) * 100;
            if (completionPercentage > 100)
                completionPercentage = 100;
            completionText = String.format("關卡完成度: %.1f%%", completionPercentage);
            controller.setCompletionText(completionText);

            // 4. 把原本畫布清空，改把 FXML root 加進來
            this.getChildren().clear();
            this.getChildren().add(gameOverRoot);

            // ★ 請特別注意：FXML 加進來後，要再把 focus 要回給 GameScene ★
            this.requestFocus();

        } catch (IOException e) {
            e.printStackTrace();
            // 若載入失敗，可以 fallback 到 Canvas 原本的繪製方式，或者只是印錯誤訊息
        }
    }

    private void renderLevelComplete() {
        // 1. 停止背景音樂
        BgmPlayer.getInstance().stop();

        // 2. 載入 LevelComplete.fxml
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("./LevelComplete.fxml"));
            Parent levelCompleteRoot = loader.load();

            // 3. 取得 Controller，把分數與完成度傳進去
            LevelCompleteController controller = loader.getController();
            controller.setScore(score);

            // 4. 清掉原本的畫布，改以 FXML 版面
            this.getChildren().clear();
            this.getChildren().add(levelCompleteRoot);

            // ★ 請特別注意：FXML 加進來後，要再把 focus 要回給 GameScene ★
            this.requestFocus();

        } catch (IOException e) {
            e.printStackTrace();
            // 如果失敗，就 fallback 或印錯誤
        }
    }

    private void resetGame() {
        createPlayer();
        obstacles.clear();
        score = 0;
        gameOver = false;
        dying = false;
        explosionView = null;
        levelComplete = false;
        allLevelObstaclesSpawned = false;
        gameWorldDistance = 0;
        finalLevelDistance = 0;
        background.reset();
        loadLevel(currentLevelFile);
        this.getChildren().clear();
        Canvas canvas = new Canvas(MainApplication.getWIDTH(), MainApplication.getHEIGHT());
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);
        BgmPlayer.getInstance().play();
        gameTimer.start();
    }
}
