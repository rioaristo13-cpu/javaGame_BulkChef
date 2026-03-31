package com.bulkchef.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Player {

    public float x, y;

    // Cropped sprite: 129x409 (no padding at all)
    // Scale to natural in-room height ~100px tall, width proportional
    // 129/409 * 100 = ~32px wide
    public static final int DISPLAY_W = 65;
    public static final int DISPLAY_H = 205;

    private final float SPEED = 200f;
    private boolean facingLeft = false;

    private final Texture sprite;

    public Player(float startX, float floorY) {
        this.x = startX;
        this.y = floorY;  // bottom of sprite sits exactly on floor — never changes

        sprite = new Texture(Gdx.files.internal("character_prototype.png"));
        sprite.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public void update(float delta, Rectangle roomBounds) {
        // Horizontal ONLY — up/down keys intentionally not handled
        float dx = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)  || Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx -= SPEED;
            facingLeft = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx += SPEED;
            facingLeft = false;
        }

        x += dx * delta;

        // Clamp inside room horizontally
        float wall = 10f;
        x = Math.max(roomBounds.x + wall,
            Math.min(x, roomBounds.x + roomBounds.width - DISPLAY_W - wall));
        // y is NEVER touched here
    }

    public void render(SpriteBatch batch) {
        if (facingLeft) {
            batch.draw(sprite, x + DISPLAY_W, y, -DISPLAY_W, DISPLAY_H);
        } else {
            batch.draw(sprite, x, y, DISPLAY_W, DISPLAY_H);
        }
    }

    public void dispose() {
        sprite.dispose();
    }
}
