package com.bulkchef;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class MainMenuScreen implements Screen {

    private final BulkChef game;
    private ShapeRenderer shape;
    private BitmapFont font;
    private Texture coverTexture;

    private static final float COVER_W = 453 * 1.15f;
    private static final float COVER_H = 131 * 1.15f;

    private final Rectangle startBtn = new Rectangle(
        (BulkChef.WIDTH - 220) / 2f, 95, 220, 56
    );

    private float animTick = 0f;

    public MainMenuScreen(BulkChef game) {
        this.game    = game;
        shape        = new ShapeRenderer();
        font         = new BitmapFont();
        coverTexture = new Texture(Gdx.files.internal("Cover.png"));
    }

    @Override
    public void render(float delta) {
        animTick += delta;

        float mx = Gdx.input.getX();
        float my = BulkChef.HEIGHT - Gdx.input.getY();
        boolean hover = startBtn.contains(mx, my);

        // Pure black — nothing else
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Button glow + fill
        shape.begin(ShapeRenderer.ShapeType.Filled);
        float pulse = (float)(Math.sin(animTick * 2.5f) * 0.5f + 0.5f);
        shape.setColor(0.9f, 0.55f, 0f, 0.06f + pulse * 0.09f);
        shape.rect(startBtn.x - 16, startBtn.y - 16,
            startBtn.width + 32, startBtn.height + 32);
        shape.setColor(hover ? 0.95f : 0.62f,
            hover ? 0.70f : 0.38f,
            hover ? 0.10f : 0.04f, 1f);
        shape.rect(startBtn.x, startBtn.y, startBtn.width, startBtn.height);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(1f, 0.85f, 0.28f, 1f);
        shape.rect(startBtn.x, startBtn.y, startBtn.width, startBtn.height);
        shape.end();

        game.batch.begin();

        // Cover art
        float coverX = (BulkChef.WIDTH  - COVER_W) / 2f;
        float coverY = BulkChef.HEIGHT - COVER_H - 30;
        game.batch.draw(coverTexture, coverX, coverY, COVER_W, COVER_H);

        /*// Subtitle
        font.getData().setScale(1.05f);
        font.setColor(0.55f, 0.45f, 0.75f, 0.75f);
        GlyphLayout sub = new GlyphLayout(font, "TRAIN  \u2022  COOK  \u2022  COMPETE");
        font.draw(game.batch, sub, (BulkChef.WIDTH - sub.width) / 2f, coverY - 14);*/

        // Button label
        font.getData().setScale(1.5f);
        font.setColor(hover ? Color.WHITE : new Color(1f, 0.92f, 0.72f, 1f));
        GlyphLayout btn = new GlyphLayout(font, "START");
        font.draw(game.batch, btn,
            startBtn.x + (startBtn.width - btn.width) / 2f,
            startBtn.y + startBtn.height - 13);

        /*// Footer
        font.getData().setScale(0.80f);
        font.setColor(0.28f, 0.24f, 0.40f, 1f);
        GlyphLayout hint = new GlyphLayout(font, "A / D  or  \u2190 \u2192  to move");
        font.draw(game.batch, hint, (BulkChef.WIDTH - hint.width) / 2f, 26);
*/
        game.batch.end();

        if (Gdx.input.justTouched() && hover) {
            game.setScreen(new GameScreen(game));
            dispose();
        }
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shape.dispose();
        font.dispose();
        coverTexture.dispose();
    }
}
