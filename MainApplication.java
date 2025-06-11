import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.io.IOException;

public class MainApplication extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Dino 遊戲與關卡預覽");
        primaryStage.setResizable(false);

        showStartMenu();
    }

    public void showStartMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainMenu.fxml"));
            Parent root = loader.load();

            // Get the controller and set the main application reference
            MainMenuController controller = loader.getController();
            controller.setMainApp(this);

            Scene scene = new Scene(root, WIDTH, HEIGHT);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to the old menu if FXML loading fails
            showFallbackMenu();
        }
    }

    private void showFallbackMenu() {
        VBox menuRoot = new VBox(20);
        menuRoot.setAlignment(Pos.CENTER);
        menuRoot.setPrefSize(WIDTH, HEIGHT);
        menuRoot.setStyle("-fx-background-color: lightblue;");

        Font titleFont = new Font("Arial", 40);
        javafx.scene.text.Text title = new javafx.scene.text.Text("Dino 遊戲");
        title.setFont(titleFont);
        title.setFill(Color.DARKBLUE);

        Button level1Button = new Button("關卡 1");
        level1Button.setFont(new Font("Arial", 24));
        level1Button.setOnAction(e -> startGame("level1.txt"));

        Button level2Button = new Button("關卡 2");
        level2Button.setFont(new Font("Arial", 24));
        level2Button.setOnAction(e -> startGame("level2.txt"));

        menuRoot.getChildren().addAll(title, level1Button, level2Button);

        Scene menuScene = new Scene(menuRoot, WIDTH, HEIGHT);
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    public void startGame(String levelFile) {
        GameScene game = new GameScene(this, levelFile);
        Scene gameScene = new Scene(game, WIDTH, HEIGHT);

        gameScene.setOnKeyPressed(event -> game.handleInput(event.getCode()));

        // 再次搶回焦點，確保 GameScene 可以立刻拿到鍵盤輸入
        game.requestFocus();

        primaryStage.setScene(gameScene);
        game.startGameLoop();
    }

    private void showPreviewScene() {
        PreviewScene preview = new PreviewScene(this);
        Scene previewScene = new Scene(preview, WIDTH, HEIGHT);

        previewScene.setOnKeyPressed(event -> preview.handleInput(event.getCode()));

        primaryStage.setScene(previewScene);
        preview.startPreviewLoop();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static int getWIDTH() {
        return WIDTH;
    }

    public static int getHEIGHT() {
        return HEIGHT;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
