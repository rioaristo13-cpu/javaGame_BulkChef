package com.bulkchef;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.bulkchef.entities.Player;

public class GameScreen implements Screen {

    private final BulkChef game;
    private ShapeRenderer shape;
    private BitmapFont font;

    // ── Room layout ───────────────────────────────────────────────────────────
    //   Window : 1280 x 720
    //   Room   : 1100 x 360  (half height), centred on screen
    //   RX     : (1280-1100)/2 = 90
    //   RY     : (720-360)/2   = 180
    private static final int RW = 1100;
    private static final int RH = 360;
    private static final int RX = (BulkChef.WIDTH  - RW) / 2;
    private static final int RY = (BulkChef.HEIGHT - RH) / 2;

    private static final Rectangle ROOM = new Rectangle(RX, RY, RW, RH);

    // Border line thickness
    private static final float BORDER = 3f;

    // FLOOR_Y — the one source of truth for where all sprite bottoms sit.
    // = inner bottom of room = RY + border thickness
    // Every sprite's y position is set to FLOOR_Y.  No offsets. No gaps.
    private static final float FLOOR_Y = RY + BORDER;

    // Back wall strip height
    private static final int WALL_H = 70;

    // ── Sprites ───────────────────────────────────────────────────────────────
    private Texture kitchenTexture;
    private Texture bedTexture;

    // kitchen.png is now cropped to exactly 228x152 (zero padding)
    // Scale to fit room height naturally:
    //   kitchen is counter+fridge height — about 40% of room height looks right
    //   40% of 360 = 144px tall, width = 228/152 * 144 = ~216px
    private static final float KITCHEN_W = 278f;
    private static final float KITCHEN_H = 200f;

    // Bed (drawn in code) — right side, base on FLOOR_Y
    private static final float BED_W = 150f;
    private static final float BED_H = 96f;

    private Player player;

    public GameScreen(BulkChef game) {
        this.game = game;

        shape = new ShapeRenderer();
        font  = new BitmapFont();
        font.getData().setScale(0.9f);

        kitchenTexture = new Texture(Gdx.files.internal("kitchen.png"));
        kitchenTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        bedTexture = new Texture(Gdx.files.internal("bed.png"));
        bedTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Spawn player centre of room — y = FLOOR_Y (feet on floor)
        float spawnX = RX + RW / 2f - Player.DISPLAY_W / 2f;
        player = new Player(spawnX, FLOOR_Y);
    }

    @Override
    public void render(float delta) {
        player.update(delta, ROOM);

        // ── 1. Black void everywhere ──────────────────────────────────────
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.begin(ShapeRenderer.ShapeType.Filled);

        /*// ── 2. Back wall — darker gray top strip ──────────────────────────
        shape.setColor(0.42f, 0.42f, 0.42f, 1f);
        shape.rect(RX, RY + RH - WALL_H, RW, WALL_H);*/

        // ── 3. Floor area — flat lighter gray, no grid ────────────────────
        shape.setColor(0.66f, 0.66f, 0.66f, 1f);
        shape.rect(RX, RY, RW, RH - WALL_H);

        /*// ── 4. Bed — right side, base sitting on FLOOR_Y ─────────────────
        float bedX = RX + RW - BED_W - 16f;
        drawBed(shape, bedX, FLOOR_Y);*/

        shape.end();

        // ── 5. Black border around room — no inner lines ──────────────────
        Gdx.gl.glLineWidth(BORDER);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0f, 0f, 0f, 1f);
        shape.rect(RX, RY, RW, RH);
        // Wall / floor divider
        shape.setColor(0.22f, 0.22f, 0.22f, 1f);
        shape.line(RX, RY + RH - WALL_H, RX + RW, RY + RH - WALL_H);
        shape.end();
        Gdx.gl.glLineWidth(1f);

        float xPos = Gdx.graphics.getWidth() - bedTexture.getWidth();
        float yPos = FLOOR_Y;





        // ── 6. Sprites — y = FLOOR_Y for every asset ─────────────────────
        game.batch.begin();

        // Kitchen left side — base on FLOOR_Y
        game.batch.draw(kitchenTexture,
            RX + 40f,    // left margin inside room
            FLOOR_Y,     // bottom of sprite = floor line
            KITCHEN_W,
            KITCHEN_H);

        game.batch.draw(bedTexture, xPos, yPos);

        // Player — base set to FLOOR_Y in constructor, never moves vertically
        player.render(game.batch);

        game.batch.end();

        /*// ── 7. HUD ────────────────────────────────────────────────────────
        game.batch.begin();
        font.setColor(1f, 1f, 1f, 0.28f);
        GlyphLayout hint = new GlyphLayout(font, "A / D  or  \u2190 \u2192  to move");
        font.draw(game.batch, hint, RX + 8f, RY - 8f);
        game.batch.end();*/
    }

    /*// ── Bed: flat 2D side view, base at y ────────────────────────────────────
    private void drawBed(ShapeRenderer s, float x, float y) {
        float bw = BED_W, bh = BED_H;

        // Feet
        s.setColor(0.22f, 0.14f, 0.08f, 1f);
        s.rect(x + 8,       y,  10, 8);
        s.rect(x + bw - 18, y,  10, 8);

        // Frame
        s.setColor(0.32f, 0.20f, 0.11f, 1f);
        s.rect(x, y + 8, bw, bh - 8);

        // Mattress
        s.setColor(0.88f, 0.88f, 0.92f, 1f);
        s.rect(x + 5, y + 12, bw - 10, bh - 24);

        // Duvet lower half
        s.setColor(0.28f, 0.48f, 0.80f, 1f);
        s.rect(x + 5, y + 12, bw - 10, (bh - 24) / 2f);

        // Pillow
        s.setColor(0.95f, 0.95f, 1.0f, 1f);
        s.rect(x + 10, y + bh - 26, bw - 20, 18);

        // Headboard
        s.setColor(0.24f, 0.14f, 0.07f, 1f);
        s.rect(x, y + bh - 10, bw, 10);
    }*/

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shape.dispose();
        font.dispose();
        kitchenTexture.dispose();
        player.dispose();
    }
}
