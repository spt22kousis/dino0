import javafx.animation.AnimationTimer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameScene extends Pane {

    private static final int GROUND_Y = MainApplication.getHEIGHT() - 50;

    private Dino dino;
    private List<Obstacle> obstacles = new ArrayList<>();
    private Random random = new Random();
    private long score = 0;
    private boolean gameOver = false;
    private boolean levelComplete = false;

    private List<LevelObstacleData> levelSequence = new ArrayList<>();
    private int currentSequenceIndex = 0;
    private double gameWorldDistance = 0;
    private double finalLevelDistance = 0;
    private boolean levelModeActive = true;
    private boolean allLevelObstaclesSpawned = false;

    private GraphicsContext gc;
    private AnimationTimer gameTimer;
    private MainApplication app;

    public GameScene(MainApplication app) {
        this.app = app;
        setPrefSize(MainApplication.getWIDTH(), MainApplication.getHEIGHT());

        Canvas canvas = new Canvas(MainApplication.getWIDTH(), MainApplication.getHEIGHT());
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);
        // 2. 讓 GameScene 這個 Pane 可以拿到鍵盤 focus，並在按鍵時呼叫 handleInput
        setFocusTraversable(true);
        setOnKeyPressed(evt -> handleInput(evt.getCode()));
        // 注意：到畫面真正顯示前（Stage.show()）這個 requestFocus 有時候還無效，
        // 但可以先呼叫一次。真正顯示後 GameScene 才能拿到焦點。
        requestFocus();
        dino = new Dino();
        loadLevel("level1.txt");
    }

    public void startGameLoop() {
        // 定義 60 FPS 的幀間隔：1 秒 = 1_000_000_000 納秒
        final long intervalNanos = 1_000_000_000L / 60;
        gameTimer = new AnimationTimer() {
            private long lastUpdate = 0; // 上一次實際 update/render 時的時間戳
            private long accumulator = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    // 第一幀，先初始化 lastUpdate
                    lastUpdate = now;
                    return;
                }

                // 本幀與上一幀之間經過的時間
                long delta = now - lastUpdate;
                accumulator += delta;

                // 如果還沒累積到一個 interval，就不做任何更新
                if (accumulator < intervalNanos) {
                    return;
                }

                // 至少要跑一次 update/render
                // （若 accumulator 很大，則可以跑多次 update 減少「跳幀」）
                while (accumulator >= intervalNanos) {
                    if (!gameOver && !levelComplete) {
                        updateGame(now);
                    }
                    accumulator -= intervalNanos;
                }

                // 不管要不要顯示「遊戲結束/通關畫面」，都先繪製一次遊戲畫面或結算畫面
                if (!gameOver && !levelComplete) {
                    renderGame();
                } else if (gameOver) {
                    renderGameOver();
                    stopGameLoop();
                } else {
                    renderLevelComplete();
                    stopGameLoop();
                }

                // 把 lastUpdate 推到「剛剛跑 update 時的時間」
                lastUpdate = now;
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

    public Dino getDino() {
        return dino;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isLevelComplete() {
        return levelComplete;
    }

    // 偵測鍵盤輸入
    public void handleInput(KeyCode code) {
        if (code == KeyCode.SPACE || code == KeyCode.UP) {
            if (!gameOver && !levelComplete) {
                dino.jump();
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

    private void loadLevel(String levelFilePath) {
        levelSequence.clear();
        currentSequenceIndex = 0;
        gameWorldDistance = 0;
        levelModeActive = true;
        allLevelObstaclesSpawned = false;
        levelComplete = false;
        finalLevelDistance = 0;

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

                        if (!type.equals("regular") && !type.equals("platform")) {
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
        dino.update();
        gameWorldDistance += Obstacle.OBSTACLE_SPEED;

        if (levelModeActive) {
            if (currentSequenceIndex < levelSequence.size()) {
                LevelObstacleData data = levelSequence.get(currentSequenceIndex);
                if (gameWorldDistance >= data.spawnTriggerX) {
                    Obstacle newObstacle;
                    if (data.type.equals("platform")) {
                        newObstacle = new PlatformObstacle(data.width, data.height, data.yPosition);
                    } else {
                        newObstacle = new RegularObstacle(data.width, data.height, data.yPosition);
                    }
                    obstacles.add(newObstacle);
                    currentSequenceIndex++;

                    if (currentSequenceIndex >= levelSequence.size()) {
                        allLevelObstaclesSpawned = true;
                        levelModeActive = false;
                        System.out.println("所有關卡障礙物已排程。等待它們通過。");
                    }
                }
            }
        } else if (!allLevelObstaclesSpawned) {
            if (obstacles.isEmpty()
                    || (now - (obstacles.get(obstacles.size() - 1).getSpawnTime())) > (1500 + random.nextInt(1000))
                            * 1_000_000L) {
                RegularObstacle newRandomObstacle = new RegularObstacle();
                obstacles.add(newRandomObstacle);
            }
        }

        obstacles.removeIf(obstacle -> {
            obstacle.update();
            return obstacle.isOffScreen();
        });

        if (allLevelObstaclesSpawned && obstacles.isEmpty()) {
            levelComplete = true;
            System.out.println("關卡完成！");
        }

        for (Obstacle obstacle : obstacles) {
            if (dino.getBounds().intersects(obstacle.getBounds())) {
                if (obstacle instanceof PlatformObstacle) {
                    double dinoPreviousBottom = dino.getY() - dino.getYVelocity() + Dino.DINO_HEIGHT;
                    double platformTop = obstacle.getY();

                    if (dino.getYVelocity() > 0 && dinoPreviousBottom <= platformTop &&
                            dino.getX() + Dino.DINO_WIDTH > obstacle.getX()
                            && dino.getX() < obstacle.getX() + obstacle.getWidth()) {
                        dino.setY(platformTop - Dino.DINO_HEIGHT);
                        dino.setYVelocity(0);
                        dino.setJumping(false);
                    } else {
                        gameOver = true;
                        break;
                    }
                } else {
                    gameOver = true;
                    break;
                }
            }
        }

        if (!gameOver && !levelComplete) {
            score++;
        }
    }

    private void renderGame() {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, MainApplication.getWIDTH(), MainApplication.getHEIGHT());

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0, GROUND_Y, MainApplication.getWIDTH(), MainApplication.getHEIGHT() - GROUND_Y);

        dino.render(gc);

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
        dino = new Dino();
        obstacles.clear();
        score = 0;
        gameOver = false;
        levelComplete = false;
        allLevelObstaclesSpawned = false;
        gameWorldDistance = 0;
        finalLevelDistance = 0;
        loadLevel("level1.txt");
        this.getChildren().clear();
        Canvas canvas = new Canvas(MainApplication.getWIDTH(), MainApplication.getHEIGHT());
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);
        BgmPlayer.getInstance().play();
        gameTimer.start();
    }
}