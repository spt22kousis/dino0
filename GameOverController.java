import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * 當 GameScene 判斷為遊戲結束時，會載入這個 FXML.
 */
public class GameOverController {

    @FXML
    private Label scoreLabel;
    @FXML
    private Label completionLabel;

    // 必須由外部呼叫去設定分數與完成度
    public void setScore(long score) {
        scoreLabel.setText("最終分數: " + score);
    }

    public void setCompletionText(String text) {
        completionLabel.setText(text);
    }
}