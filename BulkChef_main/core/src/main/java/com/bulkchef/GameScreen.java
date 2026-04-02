package com.bulkchef;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.Comparator;

public class GameScreen implements Screen {
    private TiledMap map;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera camera;
    private OrthogonalTiledMapRenderer mapRenderer;
    private Texture playerTexture;
    private Texture benchTexture;
    private Texture treadmillTexture;
    private Body playerBody;
    private SpriteBatch batch ;
    private final BulkChef game;
    private boolean isPaused = false;

    // Tiled uses pixels, Box2D uses meters — scale down
    private static final float PPM = 16f; // pixels per meter

    // Besar objek di dunia / PPM
    private static final float BENCH_W = 32f / PPM;
    private static final float BENCH_H = 28f / PPM;
    private static final float TREADMILL_W = 32f / PPM;
    private static final float TREADMILL_H = 25f / PPM; // LIAT DARI TILED/ASEPRITE

    // Besar karakter
    private static final float PLAYER_W = 16f / PPM;
    private static final float PLAYER_H = 31f / PPM;

    // Besar hitbox
    private static final float HITBOX_W = 16f / PPM;
    private static final float HITBOX_H = 20f / PPM;

    // Untuk merender objek sesuai di tiled (flipped atau tidak)
    private static final int FLIP_HORIZONTAL = 0x80000000;
    private static final int FLIP_VERTICAL   = 0x40000000;

    // --- Y-sort ---
    private static class DrawEntry {
        float x, y, w, h, footY;
        Texture texture;
        boolean flipX, flipY;

        DrawEntry(float x, float y, float w, float h, Texture texture, boolean flipX, boolean flipY) {
            this.x          = x;
            this.y          = y;
            this.w          = w;
            this.h          = h;
            this.footY      = y;
            this.texture    = texture;
            this.flipX      = flipX;
            this.flipY      = flipY;

        }
    }

    // Dipake setiap frame selanjutnya
    private final Array<DrawEntry> drawList = new Array<>();

    private static final Comparator<DrawEntry> Y_SORT_DESC =
        (a, b) -> Float.compare(b.footY, a.footY);

    // Prop positions and flip flags — loaded from TMX
    private float   benchX,     benchY;
    private boolean benchFlipX, benchFlipY;
    private float   treadmillX,     treadmillY;
    private boolean treadmillFlipX, treadmillFlipY;

    public GameScreen(BulkChef game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() / PPM, Gdx.graphics.getHeight() / PPM);

        world = new World(new Vector2(0, 0), true);
        debugRenderer = new Box2DDebugRenderer();

        map = new TmxMapLoader().load("maps/apartmentmap.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        loadCollisionLayer();
        loadPropsLayer();

        playerTexture = new Texture(Gdx.files.internal("objects/player.png"));
        benchTexture = new Texture(Gdx.files.internal("objects/bench.png"));
        treadmillTexture = new Texture(Gdx.files.internal("objects/treadmill.png"));

        spawnPlayer();

        camera.zoom = 0.25f;
        batch = new SpriteBatch();
    }

    private void loadPropsLayer() {

        MapLayer propsLayer = map.getLayers().get("objects");
        if (propsLayer == null) {
            Gdx.app.error("Map","No objects layers found");
            return;
        }

        for (MapObject object : propsLayer.getObjects()) {
            Integer rawGid = object.getProperties().get("gid", Integer.class);
            if (rawGid == null) continue;

            String name = object.getName();
            if (name == null || name.isEmpty()) continue;

            // Extract flip flags from the raw GID bits
            boolean flipX = (rawGid & FLIP_HORIZONTAL) != 0;
            boolean flipY = (rawGid & FLIP_VERTICAL)   != 0;

            float worldX = object.getProperties().get("x", Float.class) / PPM;
            float worldY = object.getProperties().get("y", Float.class) / PPM;

            switch (name) {
                case "bench":
                    benchX     = worldX;
                    benchY     = worldY;
                    benchFlipX = flipX;
                    benchFlipY = flipY;
                    break;
                case "treadmill":
                    treadmillX     = worldX;
                    treadmillY     = worldY;
                    treadmillFlipX = flipX;
                    treadmillFlipY = flipY;
                    break;
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
                (rect.x / PPM) + size.x,
                (rect.y / PPM) + size.y
            ), 0f
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
            }
        }
        Vector2 playerPos= playerBody.getPosition();

        if (!isPaused) {
            handleInput();
            world.step(1/60f, 6, 2);

            playerPos = playerBody.getPosition();
            camera.position.lerp(new Vector3(playerPos.x,  playerPos.y, 0f), 0.1f);

        }
        camera.update();

        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawYSorted(playerPos);
        batch.end();

        //Viewing the collision shapes for debugging.
        debugRenderer.render(world, camera.combined);

        if (isPaused && Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    private void drawYSorted(Vector2 playerPos) {
        drawList.clear();

        // Karakter
        drawList.add(new DrawEntry(
            playerPos.x - PLAYER_W / 2f,
            playerPos.y - PLAYER_H / 2f,
            PLAYER_W, PLAYER_H,
            playerTexture, false, false
        ));

        // Benchpress
        drawList.add(new DrawEntry(
            benchX, benchY,
            BENCH_W, BENCH_H,
            benchTexture, benchFlipX, benchFlipY
        ));

        // Treadmill
        drawList.add(new DrawEntry(
            treadmillX, treadmillY,
            TREADMILL_W, TREADMILL_H,
            treadmillTexture, treadmillFlipX, treadmillFlipY
        ));

        // Disort
        drawList.sort(Y_SORT_DESC);

        for (int i = 0; i < drawList.size; i++) {
            DrawEntry e = drawList.get(i);
            batch.draw(
                e.texture,
                e.x, e.y,                           // position
                0, 0,                               // origin
                e.w, e.h,                           // size
                1f, 1f,                             // scale
                0f,                                 // rotation
                0, 0,                               // source x, y
                e.texture.getWidth(), e.texture.getHeight(), // source w, h
                e.flipX, e.flipY                    // flip flags
            );
        }
    }

    private void handleInput() {
//        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
//            game.setScreen(new MainMenuScreen(game));
//            return; // stop processing input after switching screen
//        }

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
    }

    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.setToOrtho(false, width / PPM, height / PPM);
        }
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
        playerTexture.dispose();
        benchTexture.dispose();
        treadmillTexture.dispose();
        batch.dispose();
        mapRenderer.dispose();
    }
}
