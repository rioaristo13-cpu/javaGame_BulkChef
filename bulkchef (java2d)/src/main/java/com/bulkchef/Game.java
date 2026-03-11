package com.bulkchef;

import javax.swing.JFrame;

public class Game {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 400;
    public static final String TITLE = "Side Scroller";

    public static void main(String[] args) {
        JFrame frame = new JFrame(TITLE);
        GamePanel panel = new GamePanel();

        frame.add(panel);
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.startGameLoop();
    }
}
