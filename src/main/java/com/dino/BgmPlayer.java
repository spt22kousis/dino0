package com.dino;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class BgmPlayer {
    private static BgmPlayer instance; // Singleton 實例
    private MediaPlayer player;
    private String currentPath;

    // 私有建構子，禁止外部直接 new
    private BgmPlayer(String filePath) {
        URL resourceUrl = getClass().getResource(filePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Cannot find resource: " + filePath);
        }
        Media media = new Media(resourceUrl.toString());
        player = new MediaPlayer(media);
        player.setCycleCount(MediaPlayer.INDEFINITE); // 重複播放
        this.currentPath = filePath;
    }

    // 初始化 Singleton（只執行一次）
    public static void init(String filePath) {
        if (instance == null || !instance.currentPath.equals(filePath)) {
            instance = new BgmPlayer(filePath);
        }
    }

    // 取得 Singleton 實例
    public static BgmPlayer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BgmPlayer 尚未初始化。請先呼叫 init()");
        }
        return instance;
    }

    // 控制方法
    public void play() {
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void stop() {
        player.stop();
    }

    public void setVolume(double volume) {
        player.setVolume(volume);
    }
}