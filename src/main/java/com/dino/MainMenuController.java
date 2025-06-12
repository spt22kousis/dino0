package com.dino;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainMenuController {

    @FXML
    private Button level1Button;
    
    @FXML
    private Button level2Button;
    
    private MainApplication mainApp;
    
    public void setMainApp(MainApplication mainApp) {
        this.mainApp = mainApp;
    }
    
    @FXML
    private void playLevel1() {
        if (mainApp != null) {
            mainApp.startGame("/level1.txt");
        }
    }
    
    @FXML
    private void playLevel2() {
        if (mainApp != null) {
            mainApp.startGame("/level2.txt");
        }
    }
}