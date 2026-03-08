package com.bulkchef;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Game extends JPanel {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Side Scroller");
        Game game = new Game();
        frame.add(game);
        frame.setSize(800, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.CYAN);
        g.fillRect(100, 150, 50, 50); // placeholder "player"
    }
}