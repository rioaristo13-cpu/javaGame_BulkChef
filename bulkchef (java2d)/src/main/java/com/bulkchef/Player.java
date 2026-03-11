package com.bulkchef;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

public class Player {
    public float x, y;
    public int width = 40, height = 40;

    private float velocityX = 0;
    private float velocityY = 0;
    private final float SPEED = 4f;
    private final float GRAVITY = 0.5f;
    private final float JUMP_FORCE = -11f;
    private boolean onGround = false;

    // Ground Y position (floor line)
    public static final int GROUND_Y = 300;

    public Player() {
        x = 100;
        y = GROUND_Y - height;
    }

    public void update(InputHandler input) {
        // Horizontal movement
        velocityX = 0;
        if (input.isKeyDown(KeyEvent.VK_LEFT) || input.isKeyDown(KeyEvent.VK_A)) {
            velocityX = -SPEED;
        }
        if (input.isKeyDown(KeyEvent.VK_RIGHT) || input.isKeyDown(KeyEvent.VK_D)) {
            velocityX = SPEED;
        }

        // Jump
        if ((input.isKeyDown(KeyEvent.VK_SPACE) || input.isKeyDown(KeyEvent.VK_UP) || input.isKeyDown(KeyEvent.VK_W)) && onGround) {
            velocityY = JUMP_FORCE;
            onGround = false;
        }

        // Apply gravity
        velocityY += GRAVITY;

        // Apply velocity
        x += velocityX;
        y += velocityY;

        // Ground collision
        if (y >= GROUND_Y - height) {
            y = GROUND_Y - height;
            velocityY = 0;
            onGround = true;
        }

        // Keep player within screen bounds
        if (x < 0) x = 0;
        if (x > Game.WIDTH - width) x = Game.WIDTH - width;
    }

    public void render(Graphics2D g) {
        // Player body
        g.setColor(new Color(70, 180, 255));
        g.fillRect((int) x, (int) y, width, height);

        // Player outline
        g.setColor(new Color(30, 120, 200));
        g.drawRect((int) x, (int) y, width, height);

        // Simple eyes
        g.setColor(Color.WHITE);
        g.fillOval((int) x + 8, (int) y + 10, 8, 8);
        g.fillOval((int) x + 24, (int) y + 10, 8, 8);
        g.setColor(Color.BLACK);
        g.fillOval((int) x + 10, (int) y + 12, 4, 4);
        g.fillOval((int) x + 26, (int) y + 12, 4, 4);
    }
}
