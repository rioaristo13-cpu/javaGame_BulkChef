package com.bulkchef.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.bulkchef.BulkChef;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("BulkChef");
        config.setWindowedMode(BulkChef.WIDTH, BulkChef.HEIGHT);
        config.setResizable(false);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new BulkChef(), config);
    }
}
