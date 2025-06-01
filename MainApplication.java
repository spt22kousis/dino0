import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.geometry.Pos;

public class MainApplication extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 300;

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Dino 遊戲與關卡預覽");
        primaryStage.setResizable(false);

        showStartMenu();
    }

    public void showStartMenu() {
        VBox menuRoot = new VBox(20);
        menuRoot.setAlignment(Pos.CENTER);
        menuRoot.setPrefSize(WIDTH, HEIGHT);
        menuRoot.setStyle("-fx-background-color: lightblue;");

        Font titleFont = new Font("Arial", 40);
        javafx.scene.text.Text title = new javafx.scene.text.Text("Dino 遊戲");
        title.setFont(titleFont);
        title.setFill(Color.DARKBLUE);

        Button playButton = new Button("遊玩關卡");
        playButton.setFont(new Font("Arial", 24));
        playButton.setOnAction(e -> showGameScene());

        Button previewButton = new Button("預覽關卡");
        previewButton.setFont(new Font("Arial", 24));
        previewButton.setOnAction(e -> showPreviewScene());

        menuRoot.getChildren().addAll(title, playButton, previewButton);

        Scene menuScene = new Scene(menuRoot, WIDTH, HEIGHT);
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    private void showGameScene() {
        GameScene game = new GameScene(this);
        Scene gameScene = new Scene(game, WIDTH, HEIGHT);

        gameScene.setOnKeyPressed(event -> game.handleInput(event.getCode()));

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