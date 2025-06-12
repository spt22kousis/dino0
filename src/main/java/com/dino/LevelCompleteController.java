package com.dino;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class LevelCompleteController {

    @FXML
    private Label scoreLabel;
    @FXML
    private Label completionLabel;

    public void setScore(long score) {
        scoreLabel.setText("最終分數: " + score);
    }
}