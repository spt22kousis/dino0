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

        dino = new Dino();
        loadLevel("level1.txt");
    }

    public void startGameLoop() {
        gameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameOver && !levelComplete) {
                    updateGame(now);
                    renderGame();
                } else if (gameOver) {
                    renderGameOver();
                } else if (levelComplete) {
                    renderLevelComplete();
                }
            }
        };
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
            BgmPlayer.getInstance().play();
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
        BgmPlayer.getInstance().stop();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, MainApplication.getWIDTH(), MainApplication.getHEIGHT());

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0, GROUND_Y, MainApplication.getWIDTH(), MainApplication.getHEIGHT() - GROUND_Y);

        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 50));
        gc.fillText("遊戲結束", MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 2.5,
                MainApplication.getHEIGHT() / 2 - 20);

        gc.setFont(new Font("Arial", 20));
        gc.fillText("按 R 重新開始", MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 4,
                MainApplication.getHEIGHT() / 2 + 20);
        gc.fillText("按 M 返回選單", MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 4,
                MainApplication.getHEIGHT() / 2 + 45);

        gc.setFont(new Font("Arial", 16));
        gc.fillText("最終分數: " + score, MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 4,
                MainApplication.getHEIGHT() / 2 + 70);

        String completionText = "";
        if (levelComplete) {
            completionText = "關卡完成度: 100.0%";
        } else {
            double completionPercentage = (gameWorldDistance / finalLevelDistance) * 100;
            if (completionPercentage > 100)
                completionPercentage = 100;
            completionText = String.format("關卡完成度: %.1f%%", completionPercentage);
        }
        gc.fillText(completionText, MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 4,
                MainApplication.getHEIGHT() / 2 + 95);
    }

    private void renderLevelComplete() {
        BgmPlayer.getInstance().stop();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, MainApplication.getWIDTH(), MainApplication.getHEIGHT());

        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0, GROUND_Y, MainApplication.getWIDTH(), MainApplication.getHEIGHT() - GROUND_Y);

        gc.setFill(Color.BLUE);
        gc.setFont(new Font("Arial", 50));
        gc.fillText("關卡完成！", MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 3.5,
                MainApplication.getHEIGHT() / 2 - 20);

        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 20));
        gc.fillText("按 R 重新開始", MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 4,
                MainApplication.getHEIGHT() / 2 + 20);
        gc.fillText("按 M 返回選單", MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 4,
                MainApplication.getHEIGHT() / 2 + 45);

        gc.setFont(new Font("Arial", 16));
        gc.fillText("最終分數: " + score, MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 4,
                MainApplication.getHEIGHT() / 2 + 70);

        gc.fillText("關卡完成度: 100.0%", MainApplication.getWIDTH() / 2 - gc.getFont().getSize() * 4,
                MainApplication.getHEIGHT() / 2 + 95);
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
    }
}