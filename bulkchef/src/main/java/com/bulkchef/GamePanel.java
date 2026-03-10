package com.bulkchef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel {

    private GameState gameState = GameState.MAIN_MENU;
    private InputHandler input = new InputHandler();
    private Player player;

    // Main menu button
    private Rectangle startButton = new Rectangle(300, 220, 200, 55);
    private boolean hoverStart = false;

    // Double buffering image
    private BufferedImage buffer;
    private Graphics2D bufferG;

    // Starfield for menu background
    private int[][] stars;

    // Scrolling ground offset
    private float groundOffset = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(Game.WIDTH, Game.HEIGHT));
        setFocusable(true);
        addKeyListener(input);

        // Generate random stars for menu background
        stars = new int[80][2];
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = (int) (Math.random() * Game.WIDTH);
            stars[i][1] = (int) (Math.random() * Game.HEIGHT);
        }

        // Mouse listener for menu button
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameState == GameState.MAIN_MENU) {
                    if (startButton.contains(e.getPoint())) {
                        startGame();
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverStart = startButton.contains(e.getPoint());
            }
        });
    }

    private void startGame() {
        player = new Player();
        gameState = GameState.PLAYING;
        requestFocusInWindow();
    }

    public void startGameLoop() {
        requestFocusInWindow();
        // Create buffer
        buffer = new BufferedImage(Game.WIDTH, Game.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        bufferG = buffer.createGraphics();
        bufferG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bufferG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Thread gameThread = new Thread(() -> {
            final int TARGET_FPS = 60;
            final long FRAME_TIME = 1000000000L / TARGET_FPS;
            long lastTime = System.nanoTime();

            while (true) {
                long now = System.nanoTime();
                long delta = now - lastTime;

                if (delta >= FRAME_TIME) {
                    lastTime = now;
                    update();
                    repaint();
                }

                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void update() {
        if (gameState == GameState.PLAYING && player != null) {
            player.update(input);
            groundOffset = (groundOffset + 2) % 40; // scrolling ground pattern
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (buffer == null) return;

        if (gameState == GameState.MAIN_MENU) {
            renderMainMenu(bufferG);
        } else if (gameState == GameState.PLAYING) {
            renderGame(bufferG);
        }

        g.drawImage(buffer, 0, 0, null);
    }

    // ─── MAIN MENU ────────────────────────────────────────────────────────────

    private void renderMainMenu(Graphics2D g) {
        // Deep space background
        g.setColor(new Color(5, 5, 20));
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        // Stars
        g.setColor(new Color(200, 200, 255, 180));
        for (int[] star : stars) {
            g.fillOval(star[0], star[1], 2, 2);
        }

        // Gradient overlay at bottom
        GradientPaint groundGrad = new GradientPaint(0, 260, new Color(10, 30, 60, 0), 0, Game.HEIGHT, new Color(10, 30, 80, 200));
        g.setPaint(groundGrad);
        g.fillRect(0, 260, Game.WIDTH, Game.HEIGHT - 260);

        // Title shadow
        g.setFont(new Font("Monospaced", Font.BOLD, 52));
        g.setColor(new Color(0, 150, 255, 60));
        g.drawString("SIDE SCROLLER", 102, 142);

        // Title text
        GradientPaint titleGrad = new GradientPaint(0, 90, new Color(100, 200, 255), 0, 145, new Color(30, 100, 255));
        g.setPaint(titleGrad);
        g.drawString("SIDE SCROLLER", 100, 140);

        // Subtitle
        g.setFont(new Font("Monospaced", Font.PLAIN, 16));
        g.setColor(new Color(120, 180, 255, 200));
        g.drawString("A  2 D  A D V E N T U R E", 270, 175);

        // Divider line
        g.setColor(new Color(60, 120, 255, 150));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(200, 192, 600, 192);

        // Start button
        drawStartButton(g);

        // Controls hint
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(80, 120, 180, 180));
        g.drawString("ARROW KEYS / WASD to move   |   SPACE to jump", 200, 360);
    }

    private void drawStartButton(Graphics2D g) {
        int bx = startButton.x, by = startButton.y;
        int bw = startButton.width, bh = startButton.height;

        // Button glow when hovered
        if (hoverStart) {
            g.setColor(new Color(50, 150, 255, 40));
            g.fillRoundRect(bx - 6, by - 6, bw + 12, bh + 12, 20, 20);
        }

        // Button background
        GradientPaint btnGrad = hoverStart
            ? new GradientPaint(bx, by, new Color(60, 160, 255), bx, by + bh, new Color(20, 80, 200))
            : new GradientPaint(bx, by, new Color(30, 100, 200), bx, by + bh, new Color(10, 50, 140));
        g.setPaint(btnGrad);
        g.fillRoundRect(bx, by, bw, bh, 14, 14);

        // Button border
        g.setColor(hoverStart ? new Color(100, 200, 255) : new Color(60, 130, 220));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(bx, by, bw, bh, 14, 14);

        // Button text
        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        String text = hoverStart ? "> START <" : "START";
        int tx = bx + (bw - fm.stringWidth(text)) / 2;
        int ty = by + (bh + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
    }

    // ─── GAME SCREEN ──────────────────────────────────────────────────────────

    private void renderGame(Graphics2D g) {
        // Sky background
        GradientPaint sky = new GradientPaint(0, 0, new Color(30, 80, 160), 0, Game.HEIGHT, new Color(100, 160, 220));
        g.setPaint(sky);
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);

        // Background hills (parallax layer)
        g.setColor(new Color(50, 110, 80, 180));
        int[] hillX = {0, 80, 160, 260, 360, 460, 560, 660, 760, 820};
        int[] hillH = {60, 90, 70, 110, 85, 95, 75, 105, 65, 80};
        for (int i = 0; i < hillX.length; i++) {
            g.fillOval(hillX[i] - 60, Player.GROUND_Y - hillH[i], 160, hillH[i] * 2);
        }

        // Ground
        g.setColor(new Color(60, 40, 20));
        g.fillRect(0, Player.GROUND_Y, Game.WIDTH, Game.HEIGHT - Player.GROUND_Y);

        // Ground top grass strip
        g.setColor(new Color(60, 160, 60));
        g.fillRect(0, Player.GROUND_Y, Game.WIDTH, 8);

        // Scrolling ground detail (dashes)
        g.setColor(new Color(80, 55, 30));
        g.setStroke(new BasicStroke(2f));
        for (int x = -(int) groundOffset; x < Game.WIDTH; x += 40) {
            g.drawLine(x, Player.GROUND_Y + 18, x + 20, Player.GROUND_Y + 18);
        }

        // Player
        if (player != null) {
            player.render(g);
        }

        // HUD
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("← → / A D : Move     SPACE / W : Jump", 10, 20);
    }
}
