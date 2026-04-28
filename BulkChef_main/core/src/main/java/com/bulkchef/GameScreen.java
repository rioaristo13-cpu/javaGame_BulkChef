package com.bulkchef;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.ray3k.stripe.scenecomposer.SceneComposerStageBuilder;

import java.util.Comparator;

public class GameScreen implements Screen {
    private TiledMap map;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private OrthogonalTiledMapRenderer mapRenderer;

    //Field field animasi
    private Texture idleSheet;
    private Texture runSheet;

    private Animation<TextureRegion> idleDown, idleUp, idleLeft, idleRight;
    private Animation<TextureRegion> runDown, runUp, runLeft, runRight;

    private Animation<TextureRegion> currentAnimation;
    private float animTime = 0f;

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
    private Direction facing = Direction.DOWN;

    private Texture benchTexture;
    private Texture treadmillTexture;
    private Body playerBody;
    private SpriteBatch batch ;
    private final BulkChef game;
    private Stage stage;
    private boolean isPaused = false;
    private TextButton resumeButton;
    private TextButton quitButton;
    private int selectedIndex = 0;

    // Tiled uses pixels, Box2D uses meters — scale down
    private static final float PPM = 16f; // pixels per meter

    // Untuk menaruh hitbox di kaki player
    private static final float PLAYER_H = 32f / PPM;

    // Besar hitbox
    private static final float HITBOX_W = 16f / PPM;
    private static final float HITBOX_H = 20f / PPM;

    // Untuk merender objek sesuai di tiled (flipped atau tidak)
    private static final int FLIP_HORIZONTAL = 0x80000000;
    private static final int FLIP_VERTICAL   = 0x40000000;

    private static final float VIRTUAL_W = 320f / PPM;
    private static final float VIRTUAL_H = 180f / PPM;

    private static class PropEntry {
        float x, y, w, h;
        TextureRegion region;
        boolean flipX, flipY;
    }
    private final Array<PropEntry> props = new Array<>();

    // --- Y-sort ---
    private static class DrawEntry {
        float x, y, w, h, footY;
        Texture texture;
        TextureRegion region; //Animasi player
        boolean flipX, flipY;

        DrawEntry(float x, float y, float w, float h, TextureRegion region) {
            this.x      = x;
            this.y      = y;
            this.w      = w;
            this.h      = h;
            this.footY  = y;
            this.region = region;
        }
    }

    // Dipake setiap frame selanjutnya
    private final Array<DrawEntry> drawList = new Array<>();

    private static final Comparator<DrawEntry> Y_SORT_DESC =
        (a, b) -> Float.compare(b.footY, a.footY);


    public GameScreen(BulkChef game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_W, VIRTUAL_H, camera);

        world = new World(new Vector2(0, 0), true);
        debugRenderer = new Box2DDebugRenderer();

        map = new TmxMapLoader().load("maps/apartmentmap.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        loadCollisionLayer();
        loadPropsLayer();

        //Loading animasi player
        idleSheet = new Texture(Gdx.files.internal("objects/character/idle_16x16.png"));
        runSheet = new Texture(Gdx.files.internal("objects/character/run_16x16.png"));

        int FRAME_W = 16, FRAME_H = 32, FRAMES = 6;
        float IDLE_DUR = 0.15f, RUN_DUR = 0.1f;

        idleRight   = createAnim(idleSheet, 0, FRAMES, FRAME_W, FRAME_H, IDLE_DUR);
        idleUp      = createAnim(idleSheet, 6, FRAMES, FRAME_W, FRAME_H, IDLE_DUR);
        idleLeft    = createAnim(idleSheet, 12, FRAMES, FRAME_W, FRAME_H, IDLE_DUR);
        idleDown    = createAnim(idleSheet, 18, FRAMES, FRAME_W, FRAME_H, IDLE_DUR);

        runRight    = createAnim(runSheet, 0, FRAMES, FRAME_W, FRAME_H, RUN_DUR);
        runUp       = createAnim(runSheet, 6, FRAMES, FRAME_W, FRAME_H, RUN_DUR);
        runLeft     = createAnim(runSheet, 12, FRAMES, FRAME_W, FRAME_H, RUN_DUR);
        runDown     = createAnim(runSheet, 18, FRAMES, FRAME_W, FRAME_H, RUN_DUR);

        currentAnimation = idleDown;

        benchTexture = new Texture(Gdx.files.internal("objects/bench.png"));
        treadmillTexture = new Texture(Gdx.files.internal("objects/treadmill.png"));

        spawnPlayer();

        batch = new SpriteBatch();

        stage = new Stage(new ScreenViewport());

        SceneComposerStageBuilder builder = new SceneComposerStageBuilder();
        builder.build(stage, game.skin, Gdx.files.internal("ui/pause/pausemenu.json"));


        resumeButton = stage.getRoot().findActor("resume");
        quitButton = stage.getRoot().findActor("quit");

        if (resumeButton != null) {
            resumeButton.setProgrammaticChangeEvents(true);
            resumeButton.clearActions();
        }

        if (quitButton != null) {
            quitButton.setProgrammaticChangeEvents(true);
            quitButton.clearActions();
        }

        if (resumeButton != null) {
            resumeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    isPaused = false;
                    Gdx.input.setInputProcessor(null);
                }
            });
        }

        if (quitButton != null) {
            quitButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new MainMenuScreen(game));
                }
            });
        }
    }

    private Animation<TextureRegion> createAnim(Texture sheet, int colOffset, int frameCount, int frameW, int frameH, float frameDuration) {
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new TextureRegion(sheet, (colOffset + i) * frameW, 0, frameW, frameH);
        }
        return new Animation<>(frameDuration, frames);
    }

    private void loadPropsLayer() {
        // The group layer
        com.badlogic.gdx.maps.MapGroupLayer group =
            (com.badlogic.gdx.maps.MapGroupLayer) map.getLayers().get("objects");
        if (group == null) return;

        // Sub-layers with their offsets
        String[] subLayerNames = {"gym", "bedroom", "kitchen", "livingroom"};

        for (String layerName : subLayerNames) {
            MapLayer layer = group.getLayers().get(layerName);
            if (layer == null) continue;

            // Read the layer-level offset (gym has one!)
            float layerOffsetX = layer.getRenderOffsetX() / PPM;
            float layerOffsetY = layer.getRenderOffsetY() / PPM;

            for (MapObject obj : layer.getObjects()) {
                Integer rawGid = obj.getProperties().get("gid", Integer.class);
                if (rawGid == null) continue;

                int gid = rawGid & ~(FLIP_HORIZONTAL | FLIP_VERTICAL | 0x20000000);
                boolean flipX = (rawGid & FLIP_HORIZONTAL) != 0;
                boolean flipY = (rawGid & FLIP_VERTICAL) != 0;

                // Get the tile region from the map's tilesets
                TiledMapTile tile = map.getTileSets().getTile(gid);
                if (tile == null) continue;

                float x = obj.getProperties().get("x", Float.class) / PPM + layerOffsetX;
                float w = obj.getProperties().get("width",  Float.class) / PPM;
                float h = obj.getProperties().get("height", Float.class) / PPM;
                float y = obj.getProperties().get("y",      Float.class) / PPM + layerOffsetY;

                PropEntry e = new PropEntry();
                e.x = x; e.y = y; e.w = w; e.h = h;
                e.region = tile.getTextureRegion();
                e.flipX = flipX;
                e.flipY = flipY;
                props.add(e);
            }
        }
    }

    private void spawnPlayer() {
        MapLayer spawnLayer = map.getLayers().get("player");
        float spawnX = 5f;
        float spawnY = 5f;

        if (spawnLayer != null) {
            MapObject spawnObject = spawnLayer.getObjects().get("playerSpawn");
            if (spawnObject != null) {
                spawnX = spawnObject.getProperties().get("x", Float.class) / PPM;
                spawnY = spawnObject.getProperties().get("y", Float.class) / PPM;
            }
        }

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(spawnX, spawnY);
        bodyDef.fixedRotation = true;

        playerBody = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        float offsetY = -(PLAYER_H / 2f) + (HITBOX_H / 2f);

        shape.setAsBox(
            HITBOX_W / 2f,
            HITBOX_H / 2f,
            new Vector2(0, offsetY),
            0f
        );

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0f;
        fixtureDef.restitution = 0f;

        playerBody.createFixture(fixtureDef);
        shape.dispose();
    }

    private void loadCollisionLayer() {
        // Get the object layer by name
        MapLayer layer = map.getLayers().get("collision");
        if (layer == null) {
            Gdx.app.error("Map", "No 'collision' layer found!");
            return;
        }

        for (MapObject object : layer.getObjects()) {
            Shape shape = null;

            if (object instanceof RectangleMapObject) {
                shape = getRectangle((RectangleMapObject) object);

            } else if (object instanceof PolygonMapObject) {
                shape = getPolygon((PolygonMapObject) object);

            } else if (object instanceof PolylineMapObject) {
                shape = getPolyline((PolylineMapObject) object);

            } else if (object instanceof CircleMapObject) {
                shape = getCircle((CircleMapObject) object);
            }

            if (shape == null) continue;

            // Create static body
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.StaticBody;

            Body body = world.createBody(bodyDef);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.friction = 0.4f;
            fixtureDef.restitution = 0f; // no bounce

            body.createFixture(fixtureDef);
            shape.dispose(); // always dispose shapes after use
        }
    }

    // --- Shape helpers ---

    private Shape getRectangle(RectangleMapObject object) {
        Rectangle rect = object.getRectangle();
        PolygonShape polygon = new PolygonShape();

        // Box2D setAsBox takes HALF-widths/heights, centered on body position
        Vector2 size = new Vector2(
            (rect.width  / 2f) / PPM,
            (rect.height / 2f) / PPM
        );
        polygon.setAsBox(
            size.x, size.y,
            new Vector2(
                (rect.x / PPM) + size.x, (rect.y / PPM) + size.y), 0f
        );
        return polygon;
    }

    private Shape getPolygon(PolygonMapObject object) {
        float[] vertices = object.getPolygon().getTransformedVertices();
        float[] worldVerts = new float[vertices.length];

        for (int i = 0; i < vertices.length; i++) {
            worldVerts[i] = vertices[i] / PPM;
        }

        PolygonShape polygon = new PolygonShape();
        polygon.set(worldVerts);
        return polygon;
    }

    private Shape getPolyline(PolylineMapObject object) {
        float[] vertices = object.getPolyline().getTransformedVertices();
        Vector2[] worldVerts = new Vector2[vertices.length / 2];

        for (int i = 0; i < worldVerts.length; i++) {
            worldVerts[i] = new Vector2(
                vertices[i * 2]     / PPM,
                vertices[i * 2 + 1] / PPM
            );
        }

        ChainShape chain = new ChainShape();
        chain.createChain(worldVerts);
        return chain;
    }

    private Shape getCircle(CircleMapObject object) {
        Circle circle = object.getCircle();
        CircleShape circleShape = new CircleShape();
        circleShape.setRadius(circle.radius / PPM);
        circleShape.setPosition(new Vector2(
            circle.x / PPM,
            circle.y / PPM
        ));
        return circleShape;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            isPaused = !isPaused;

            if (isPaused) {
                playerBody.setLinearVelocity(0, 0);
                Gdx.input.setInputProcessor(stage);
                selectedIndex = -1;
                stage.setKeyboardFocus(null);
            } else {
                Gdx.input.setInputProcessor(null);
                stage.setKeyboardFocus(null);
            }
        }
        Vector2 playerPos = playerBody.getPosition();

        if (!isPaused) {
            handleInput();
            world.step(1 / 60f, 6, 2);

            playerPos = playerBody.getPosition();
            camera.position.lerp(new Vector3(playerPos.x, playerPos.y, 0f), 0.1f);

            viewport.apply();
        }

        camera.update();

        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawYSorted(playerPos);
        batch.end();

        //Render debug box kolisi
        debugRenderer.render(world, camera.combined);

        if (isPaused) {
            boolean navigateDown = Gdx.input.isKeyJustPressed(Input.Keys.S) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.TAB);
            boolean navigateUp = Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP);

            if (navigateDown) {
                selectedIndex = (selectedIndex + 1) % 2;
                stage.setKeyboardFocus(selectedIndex == 0 ? resumeButton : quitButton);
            } else if (navigateUp) {
                selectedIndex = (selectedIndex - 1 + 2) % 2;
                stage.setKeyboardFocus(selectedIndex == 0 ? resumeButton : quitButton);
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)|| Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                Actor focused = stage.getKeyboardFocus();

                if (focused == resumeButton) {
                    isPaused = false;
                    Gdx.input.setInputProcessor(null);
                } else if (focused == quitButton) {
                    game.setScreen(new MainMenuScreen(game));
                }
            }

            stage.act(0f);
            stage.draw();
        }
    }

    private void drawYSorted(Vector2 playerPos) {
        drawList.clear();

        // Player frame (same as before)
        animTime += Gdx.graphics.getDeltaTime();
        TextureRegion frame = currentAnimation.getKeyFrame(animTime, true);
        float drawW = frame.getRegionWidth() / PPM;
        float drawH = frame.getRegionHeight() / PPM;
        drawList.add(new DrawEntry(
            playerPos.x - drawW / 2f,
            playerPos.y - drawH / 2f,
            drawW, drawH, frame
        ));

        // All props from all rooms
        for (PropEntry p : props) {
            TextureRegion r = new TextureRegion(p.region);
            r.flip(p.flipX, p.flipY);
            drawList.add(new DrawEntry(p.x, p.y, p.w, p.h, r));
        }

        drawList.sort(Y_SORT_DESC);

        for (DrawEntry e : drawList) {
            if (e.region != null) {
                batch.draw(e.region, e.x, e.y, e.w, e.h);
            } else {
                batch.draw(e.texture, e.x, e.y, 0, 0, e.w, e.h,
                    1f, 1f, 0f, 0, 0,
                    e.texture.getWidth(), e.texture.getHeight(),
                    e.flipX, e.flipY);
            }
        }
    }

    private void handleInput() {
        float speed = 4.2f;
        float vx = 0;
        float vy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            vy = speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            vy = -speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            vx = -speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            vx = speed;
        }

        // Kunci cepat jalan diagonal
        if (vx != 0 && vy != 0) {
            vx *= 0.7071f; // 1/sqrt(2)
            vy *= 0.7071f;
        }

        playerBody.setLinearVelocity(vx, vy);

        boolean isMoving = (vx != 0 || vy != 0);

        if (vy > 0) facing = Direction.UP;
        else if (vy < 0) facing = Direction.DOWN;
        else if (vx > 0) facing = Direction.RIGHT;
        else if (vx < 0) facing = Direction.LEFT;

        switch(facing) {
            case UP     : currentAnimation = isMoving ? runUp : idleUp; break;
            case DOWN   : currentAnimation = isMoving ? runDown : idleDown; break;
            case LEFT   : currentAnimation = isMoving ? runLeft : idleLeft; break;
            case RIGHT  : currentAnimation = isMoving ? runRight : idleRight; break;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        isPaused = true;
        if (playerBody != null) {
            playerBody.setLinearVelocity(0, 0);
        }

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        map.dispose();
        world.dispose();
        debugRenderer.dispose();
        idleSheet.dispose();
        runSheet.dispose();
        benchTexture.dispose();
        treadmillTexture.dispose();
        batch.dispose();
        mapRenderer.dispose();
    }
}
