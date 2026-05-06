package com.bulkchef;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
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
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.*;
import com.ray3k.stripe.scenecomposer.SceneComposerStageBuilder;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.objects.EllipseMapObject;

import java.util.Comparator;

public class GameScreen implements Screen {

    // Layar waktu tidur
    private boolean showSleepScreen = false;
    private float sleepAlpha = 0f;
    private enum SleepPhase {FADE_IN, HOLD, FADE_OUT}
    private SleepPhase sleepPhase = SleepPhase.FADE_IN;

    // Zona interaksi di map
    private final java.util.Map<String, Interaction> interactions = new java.util.HashMap<>();

    //Tekstur ikon UI
    private Image iconCalories, iconEnergy, iconUpper, iconLower, iconTotal;

    //UI
    private Table hudRoot;
    private final PlayerStats stats = new PlayerStats();
    private Stage hudStage;
    private Label caloriesLabel, energyLabel, upperLabel, lowerLabel, totalLabel, sleepLabel;

    private TiledMap map;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    private OrthogonalTiledMapRenderer mapRenderer;
    private ShapeRenderer shapeRenderer;
    private TextButton promptLabel;

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

    //Load field tekstur
    private Texture benchTexture, treadmillTexture, icoCal, icoEnergy, icoUpper, icoLower, icoTotal;

    private Body playerBody;
    private SpriteBatch batch ;
    private final BulkChef game;
    private Stage stage;
    private boolean isPaused = false;
    private boolean inOptionMenu = false;

    private Slider musicSlider;
    private Slider sfxSlider;

    private TextButton resumeButton;
    private TextButton quitButton;
    private TextButton optionButton;
    private TextButton backButton;

    private Table menuGroup;
    private Table optionGroup;

    private int selectedIndex = 0;
    private int optionSelectedIndex = 0;

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


    private final SaveData saveData;
    public GameScreen(BulkChef game) { this(game, null); }
    public GameScreen(BulkChef game, SaveData saveData) {
        this.game = game;
        this.saveData = saveData;
    }

    @Override
    public void show() {

        //Load savefile
        if (saveData != null) {
            stats.cal = saveData.cal;
            stats.energy = saveData.energy;
            stats.upperMuscle = saveData.upperMuscle;
            stats.lowerMuscle = saveData.lowerMuscle;
            stats.daysRemaining = saveData.daysRemaining;
        }

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_W, VIRTUAL_H, camera);

        world = new World(new Vector2(0, 0), true);
        debugRenderer = new Box2DDebugRenderer();

        map = new TmxMapLoader().load("maps/apartmentmap.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        loadCollisionLayer();
        loadInteractionLayer();
        interactions.put("treadmill",  new Interaction("Run",  Interaction.Type.EX_LOWER, 0, 0, 0,  15f, 20f));
        interactions.put("benchpress", new Interaction("Bench",       Interaction.Type.EX_UPPER, 0, 0, 20f, 0,  25f));
        interactions.put("cycling",    new Interaction("Ride",         Interaction.Type.EX_LOWER, 0, 0, 0,  12f, 18f));
        interactions.put("dumbell",    new Interaction("Lift",    Interaction.Type.EX_UPPER, 0, 0, 18f, 0,  22f));
        interactions.put("kitchen",    new Interaction("Cook",        Interaction.Type.FOOD,    500f, 30f, 0, 0, 0f));
        interactions.put("bed", new Interaction("Sleep", Interaction.Type.REST, 0, 0, 0, 0, 0));
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
        icoCal    = new Texture(Gdx.files.internal("ui/icons/icon_calories.png"));
        icoEnergy = new Texture(Gdx.files.internal("ui/icons/icon_energy.png"));
        icoUpper  = new Texture(Gdx.files.internal("ui/icons/icon_upper.png"));
        icoLower  = new Texture(Gdx.files.internal("ui/icons/icon_lower.png"));
        icoTotal  = new Texture(Gdx.files.internal("ui/icons/icon_total.png"));

        iconCalories = new Image(icoCal);
        iconEnergy   = new Image(icoEnergy);
        iconUpper    = new Image(icoUpper);
        iconLower    = new Image(icoLower);
        iconTotal    = new Image(icoTotal);

        spawnPlayer();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        stage = new Stage(new ScreenViewport());

        SceneComposerStageBuilder builder = new SceneComposerStageBuilder();
        builder.build(stage, game.skin, Gdx.files.internal("ui/pause/pausemenu2.json"));

        Table rootTable = stage.getRoot().findActor("root");
        if (rootTable != null) {
            rootTable.setFillParent(true);
        }

        menuGroup = stage.getRoot().findActor("menugroup");
        optionGroup = stage.getRoot().findActor("optiongroup");

        backButton = stage.getRoot().findActor("backbtn");
        if (backButton    != null) addHoverSound(backButton);

        resumeButton = stage.getRoot().findActor("resumebtn");
        if (resumeButton != null) addHoverSound(resumeButton);

        optionButton = stage.getRoot().findActor("optionbtn");
        if (optionButton  != null) addHoverSound(optionButton);

        quitButton = stage.getRoot().findActor("quitbtn");
        if (quitButton    != null) addHoverSound(quitButton);

        musicSlider = stage.getRoot().findActor("slidermusik");

        if (musicSlider != null) {
            musicSlider.setValue(game.bgm.getVolume() * 100);
            musicSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.bgm.setVolume(musicSlider.getValue() / 100f);
                }
            });
        }

        sfxSlider = stage.getRoot().findActor("sfxslider");

        if (sfxSlider != null) {
            sfxSlider.setValue(game.sfxVolume * 100f);
            sfxSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.sfxVolume = sfxSlider.getValue() / 100f;
                }
            });
        }

        if (menuGroup != null) {
            menuGroup.setVisible(true);
            menuGroup.setTouchable(Touchable.enabled);
            menuGroup.setFillParent(false);
            menuGroup.center();
            menuGroup.pack();

            menuGroup.setPosition(
                (stage.getWidth() - menuGroup.getWidth()) / 2f,
                (stage.getHeight() - menuGroup.getHeight()) / 2f
            );
        }

        if (optionGroup != null) {
            optionGroup.setVisible(false);
            optionGroup.setTouchable(Touchable.disabled);
            optionGroup.setFillParent(false);
            optionGroup.center();
            optionGroup.pack();
            optionGroup.setPosition(
                (stage.getWidth() - optionGroup.getWidth()) / 2f,
                (stage.getHeight() - optionGroup.getHeight()) / 2f
            );
        }


        com.badlogic.gdx.scenes.scene2d.Actor labelmusik = stage.getRoot().findActor("labelmusik");
        com.badlogic.gdx.scenes.scene2d.Actor labelsfx = stage.getRoot().findActor("labelsfx");

        if (labelmusik != null) {
            labelmusik.setColor(Color.WHITE);
        }

        if (labelsfx != null) {
            labelsfx.setColor((Color.WHITE));
        }
        if (musicSlider != null) {
            musicSlider.setTouchable(Touchable.enabled);
        }

        if (sfxSlider != null) {
            sfxSlider.setTouchable(Touchable.enabled);
        }

        if (resumeButton != null) {
            resumeButton.setProgrammaticChangeEvents(true);
            resumeButton.clearActions();
            resumeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    game.sfxEnter.play(game.sfxVolume);
                    isPaused = false;
                    inOptionMenu = false;
                    Gdx.input.setInputProcessor(new InputMultiplexer(hudStage));
                    stage.setKeyboardFocus(null);
                }
            });
        }

        if (optionButton != null) {
            optionButton.setProgrammaticChangeEvents(true);
            optionButton.clearActions();
            optionButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    game.sfxEnter.play(game.sfxVolume);
                    openOptionMenu();
                }
            });
        }

        if (quitButton != null) {
            quitButton.setProgrammaticChangeEvents(true);
            quitButton.clearActions();
            quitButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    game.sfxEnter.play(game.sfxVolume);
                    SaveData.save(stats, playerBody.getPosition().x, playerBody.getPosition().y);
                    game.setScreen(new MainMenuScreen(game));
                }
            });
        }

        if (backButton != null) {
            backButton.setProgrammaticChangeEvents(true);
            backButton.clearActions();
            backButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    game.sfxEnter.play(game.sfxVolume);
                    closeOptionMenu();
                }
            });
        }
        game.bgm.play();
        buildHud();
        InputMultiplexer multiplexer = new InputMultiplexer(hudStage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private void loadInteractionLayer() {
        MapLayer layer = map.getLayers().get("interaction");
        if (layer == null) return;
        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof EllipseMapObject) {
                com.badlogic.gdx.math.Ellipse e = ((EllipseMapObject) obj).getEllipse();
                float cx = (e.x + e.width  / 2f) / PPM;    // center, not top-left
                float cy = (e.y + e.height / 2f) / PPM;
                float r  = (e.width / 2f) / PPM;
                String type = obj.getName();
                interactionZones.add(new InteractionZone(cx, cy, r, type));
            }
        }
    }
    private static class InteractionZone {
        float x, y, radius;
        String type;
        InteractionZone(float x, float y, float r, String type) {
            this.x = x; this.y = y; this.radius = r; this.type = type;
        }
    }
    private final Array<InteractionZone> interactionZones = new Array<>();

    private void buildHud() {
        hudStage = new Stage(new ScreenViewport());
        sleepLabel = new Label("", game.skin, "big");
        sleepLabel.setVisible(false);
        sleepLabel.setFillParent(true);
        sleepLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        hudStage.addActor(sleepLabel);


        // Root table fills screen
        hudRoot = new Table();
        hudRoot.setFillParent(true);
        hudRoot.pad(8f);
        hudStage.addActor(hudRoot);

        // ── Top-left: Calories & Energy ──────────────────────
        Table topLeft = new Table();
        caloriesLabel = new Label("Calories: ", game.skin, "big");
        energyLabel   = new Label("Energy: ",   game.skin, "big");
        topLeft.add(iconCalories).size(32, 32).padRight(4);
        topLeft.add(caloriesLabel).left().row();
        topLeft.add(iconEnergy).size(32, 32).padRight(4);
        topLeft.add(energyLabel).left();


        // ── Top-center: Total Muscle ──────────────────────────
        totalLabel = new Label("Total Muscle: ", game.skin, "big");

        // ── Top-right: Muscle stats ───────────────────────────
        Table topRight = new Table();
        upperLabel = new Label("Upper: ", game.skin, "big");
        lowerLabel = new Label("Lower: ", game.skin, "big");
        topRight.add(iconUpper).size(32, 32).padRight(4);
        topRight.add(upperLabel).right().row();
        topRight.add(iconLower).size(32, 32).padRight(4);
        topRight.add(lowerLabel).right();

        // ── Lay out top row ───────────────────────────────────
        hudRoot.add(topLeft).top().left().expandX();
        Table topCenter = new Table();
        topCenter.add(iconTotal).size(32, 32).padRight(4);
        topCenter.add(totalLabel);
        hudRoot.add(topCenter).top().center().expandX();
        hudRoot.add(topRight).top().right().expandX();
        hudRoot.row();

        // ── Middle spacer ────────────────────────────────────
        hudRoot.add().expand().colspan(3);

        promptLabel = new TextButton("E", game.skin);
        promptLabel.setVisible(false);
        promptLabel.setTouchable(Touchable.disabled);
        hudStage.addActor(promptLabel);
    }

    private void openOptionMenu() {
        inOptionMenu = true;
        optionSelectedIndex = 0;

        if (menuGroup != null) {
            menuGroup.setVisible(false);
            menuGroup.setTouchable(Touchable.disabled);
        }

        if (optionGroup != null) {
            optionGroup.setVisible(true);
            optionGroup.setTouchable(Touchable.enabled);
            optionGroup.invalidate();
            optionGroup.invalidate();
            optionGroup.pack();
            float x = (stage.getWidth() - optionGroup.getHeight()) / 2f;
            float y = (stage.getHeight() - optionGroup.getHeight()) / 2f;
            optionGroup.setBounds(x, y, optionGroup.getWidth(),optionGroup.getHeight());
        }

        if (musicSlider != null) {
            stage.setKeyboardFocus(musicSlider);
        } else if (backButton != null) {
            stage.setKeyboardFocus(backButton);
        }

    }

    private void closeOptionMenu() {
        inOptionMenu = false;
        selectedIndex = 1;

        if (menuGroup != null) {
            menuGroup.setVisible(true);
            menuGroup.setTouchable(Touchable.enabled);
        }

        if (optionGroup != null) {
            optionGroup.setVisible(false);
            optionGroup.setTouchable(Touchable.disabled);
        }

        if (optionGroup != null) {
            stage.setKeyboardFocus(optionButton);
        }
    }

    private void updateOptionFocus() {
        if (optionSelectedIndex == 0) {
            if (musicSlider != null) {
                stage.setKeyboardFocus(musicSlider);
            }
        } else if (optionSelectedIndex == 1) {
            if (sfxSlider != null) {
                stage.setKeyboardFocus(sfxSlider);
            }
        } else {
            if (backButton != null) {
                stage.setKeyboardFocus(backButton);
            }
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
        com.badlogic.gdx.maps.MapGroupLayer group =
            (com.badlogic.gdx.maps.MapGroupLayer) map.getLayers().get("objects");
        if (group == null) return;

        String[] subLayerNames = {"gym", "bedroom", "kitchen", "livingroom"};

        for (String layerName : subLayerNames) {
            MapLayer layer = group.getLayers().get(layerName);
            if (layer == null) continue;

            // getRenderOffsetX/Y() is already cumulative (group + sublayer)
            // offsetX: add normally
            // offsetY: SUBTRACT because it's in Tiled's Y-down space,
            //          but rawY is already flipped to Y-up by LibGDX
            float offsetX = layer.getRenderOffsetX() / PPM;
            float offsetY = layer.getRenderOffsetY() / PPM;

            for (MapObject obj : layer.getObjects()) {
                Integer rawGid = obj.getProperties().get("gid", Integer.class);
                if (rawGid == null) continue;

                int gid = rawGid & ~(FLIP_HORIZONTAL | FLIP_VERTICAL | 0x20000000);
                boolean flipX = (rawGid & FLIP_HORIZONTAL) != 0;
                boolean flipY = (rawGid & FLIP_VERTICAL) != 0;

                TiledMapTile tile = map.getTileSets().getTile(gid);
                if (tile == null) continue;

                float rawX = obj.getProperties().get("x",      Float.class) / PPM;
                float rawY = obj.getProperties().get("y",      Float.class) / PPM;
                float w    = obj.getProperties().get("width",  Float.class) / PPM;
                float h    = obj.getProperties().get("height", Float.class) / PPM;

                float x = rawX + offsetX;
                float y = rawY - offsetY; // subtract! offsetY is Y-down, rawY is Y-up

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
            if (saveData != null) {
                spawnX = saveData.posX;
                spawnY = saveData.posY;
            } else if (spawnObject != null) {
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
            game.sfxNavigate.play(game.sfxVolume);
            if (!isPaused) {
                isPaused = true;
                inOptionMenu = false;
                playerBody.setLinearVelocity(0, 0);
                Gdx.input.setInputProcessor(new InputMultiplexer(stage, hudStage));
                selectedIndex = 0;

                if (menuGroup != null) {
                    menuGroup.setVisible(true);
                    menuGroup.setTouchable(Touchable.enabled);
                }

                if (optionGroup != null) {
                    optionGroup.setVisible(false);
                    optionGroup.setTouchable(Touchable.disabled);
                }

                if (resumeButton != null) {
                    stage.setKeyboardFocus(resumeButton);
                }

            } else {
                if (inOptionMenu) {
                    closeOptionMenu();
                } else {
                    isPaused = false;
                    Gdx.input.setInputProcessor(new  InputMultiplexer(hudStage));
                    stage.setKeyboardFocus(null);
                }
            }
        }

        Vector2 playerPos = playerBody.getPosition();

        if (!isPaused) {
            handleInput();
            world.step(1 / 60f, 6, 2);

            playerPos = playerBody.getPosition();
            camera.position.lerp(new Vector3(playerPos.x, playerPos.y, 0f), 0.1f);

        }
        viewport.apply();

        camera.update();

        mapRenderer.setView(camera);
        mapRenderer.render();

        // Draw interaction zone circles
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 0f, 1f);
        for (InteractionZone zone : interactionZones) {
            shapeRenderer.circle(zone.x, zone.y, zone.radius, 32);
        }
        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawYSorted(playerPos);
        batch.end();

        if (showSleepScreen) {
            float speed = 0.8f;
            if (sleepPhase == SleepPhase.FADE_IN) {
                sleepAlpha = Math.min(1f, sleepAlpha + delta * speed);
                if (sleepAlpha >= 1f) sleepPhase = SleepPhase.HOLD;
            } else if (sleepPhase == SleepPhase.HOLD) {
                sleepAlpha = 1f;
                // Wait for any key to begin fade out
                if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    sleepPhase = SleepPhase.FADE_OUT;
                }
            } else {
                sleepAlpha = Math.max(0f, sleepAlpha - delta * speed);
                if (sleepAlpha <= 0f) {
                    showSleepScreen = false;
                    isPaused = false;
                    sleepLabel.setVisible(false);
                    hudRoot.setVisible(true);   // add this
                }
            }

            // Draw black overlay
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, sleepAlpha);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();

            // Draw text using hudStage's batch
            sleepLabel.setText(stats.daysRemaining + " days remaining");
            sleepLabel.setVisible(true);
            sleepLabel.getColor().a = sleepAlpha;
            hudStage.act(0);
            hudStage.draw();
        }

        //Render debug box kolisi
        //debugRenderer.render(world, camera.combined);

        // E prompt above player
        Vector2 pPos = playerBody.getPosition();
        boolean nearZone = false;
        for (InteractionZone zone : interactionZones) {
            float dx = pPos.x - zone.x;
            float dy = pPos.y - zone.y;
            if (dx*dx + dy*dy <= zone.radius * zone.radius) {
                nearZone = true;
                break;
            }
        }
        promptLabel.setVisible(nearZone && !showSleepScreen);
        if (nearZone) {
            Vector3 screenPos = camera.project(new Vector3(pPos.x, pPos.y + PLAYER_H, 0));
            promptLabel.setPosition(screenPos.x - promptLabel.getWidth() / 2f, screenPos.y);
        }

        if (isPaused  && !showSleepScreen) {
            if (!inOptionMenu) {
                boolean navigateDown =
                    Gdx.input.isKeyJustPressed(Input.Keys.S) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.TAB);

                boolean navigateUp =
                    Gdx.input.isKeyJustPressed(Input.Keys.W) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.UP);

                if (navigateDown) {
                    selectedIndex = (selectedIndex + 1) % 3;
                    game.sfxNavigate.play(game.sfxVolume);
                } else if (navigateUp) {
                    selectedIndex = (selectedIndex - 1 + 3) % 3;
                    game.sfxNavigate.play(game.sfxVolume);
                }

                if (selectedIndex == 0) {
                    stage.setKeyboardFocus(resumeButton);
                } else if (selectedIndex == 1) {
                    stage.setKeyboardFocus(optionButton);
                } else {
                    stage.setKeyboardFocus(quitButton);
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                    Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {

                    game.sfxEnter.play(0.8f);

                    Actor focused = stage.getKeyboardFocus();

                    if (focused == resumeButton) {
                        isPaused = false;
                        Gdx.input.setInputProcessor(null);
                        stage.setKeyboardFocus(null);
                    } else if (focused == optionButton) {
                        openOptionMenu();
                    } else if (focused == quitButton) {
                        SaveData.save(stats, playerBody.getPosition().x, playerBody.getPosition().y);
                        game.setScreen(new MainMenuScreen(game));
                    }
                }
            } else {
                boolean navigateDown =
                    Gdx.input.isKeyJustPressed(Input.Keys.S) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.TAB);

                boolean navigateUp =
                    Gdx.input.isKeyJustPressed(Input.Keys.W) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.UP);

                if (navigateDown) {
                    optionSelectedIndex = (optionSelectedIndex + 1) % 3;
                    game.sfxNavigate.play(game.sfxVolume);
                } else if (navigateUp) {
                    optionSelectedIndex = (optionSelectedIndex - 1 +3) % 3;
                    game.sfxNavigate.play(game.sfxVolume);
                }

                updateOptionFocus();

                if (optionSelectedIndex == 0 && musicSlider != null) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                        musicSlider.setValue((musicSlider.getValue() - musicSlider.getStepSize()));
                    }

                    if  (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                        musicSlider.setValue(musicSlider.getValue() + musicSlider.getStepSize());
                    }
                } else if (optionSelectedIndex == 1 && sfxSlider != null) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                        sfxSlider.setValue(sfxSlider.getValue() - sfxSlider.getStepSize());
                    }

                    if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                        sfxSlider.setValue(sfxSlider.getValue() + sfxSlider.getStepSize());
                    }
                } else if (optionSelectedIndex == 2) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                        closeOptionMenu();
                    }
                }
            }
            stage.act(0f);
            stage.draw();
        }
        if (!isPaused && !showSleepScreen) {
            updateHud();
            hudStage.act(delta);
            hudStage.draw();
        } else if (showSleepScreen) {
            hudStage.act(0);
            hudStage.draw(); // x Days remaining showed only when everything is hidden
        }
    }

    private void updateHud() {
        caloriesLabel.setText("Calories: " + (int) stats.cal);
        energyLabel  .setText("Energy: "   + (int) stats.energy);
        upperLabel   .setText("Upper: "    + (int) stats.upperMuscle);
        lowerLabel   .setText("Lower: "    + (int) stats.lowerMuscle);
        totalLabel   .setText("Total Muscle: " + (int) stats.totalMuscle());
    }

    //suara kursor untuk menu
    private void addHoverSound(Actor actor) {
        actor.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) {
                    game.sfxNavigate.play(game.sfxVolume);
                }
            }
        });
    }

    private void drawYSorted(Vector2 playerPos) {
        drawList.clear();
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
        // Player frame (same as before)
        animTime += Gdx.graphics.getDeltaTime();
        TextureRegion frame = currentAnimation.getKeyFrame(animTime, true);
        float drawW = frame.getRegionWidth() / PPM;
        float drawH = frame.getRegionHeight() / PPM;
        batch.draw(frame,
            playerPos.x - drawW / 2f,
            playerPos.y - drawH / 2f,
            drawW, drawH);
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            Vector2 pos = playerBody.getPosition();
            for (InteractionZone zone : interactionZones) {
                float dx = pos.x - zone.x;
                float dy = pos.y - zone.y;
                if (dx*dx + dy*dy <= zone.radius * zone.radius) {
                    handleInteraction(zone.type);
                    break;
                }
            }
        }
    }

    private void handleInteraction(String type) {
        Interaction action = interactions.get(type);
        if (action == null) return;

        if (action.type == Interaction.Type.FOOD) {
            stats.addCalories(action.calDelta);
            stats.addEnergy(action.energyDelta);
        } else if (action.type == Interaction.Type.EX_UPPER || action.type == Interaction.Type.EX_LOWER) {
            // Exercise zones: require energy, consume calories, build muscle
            if (stats.isTired()) return;
            stats.addEnergy(-action.energyCost);
            stats.addCalories(action.calDelta);
            stats.addUpperMuscle(action.upperDelta);
            stats.addLowerMuscle(action.lowerDelta);
        } else if (action.type == Interaction.Type.REST) {
            stats.daysRemaining--;
            showSleepScreen = true;
            sleepPhase = SleepPhase.FADE_IN;
            sleepAlpha = 0f;
            isPaused = true;
            hudRoot.setVisible(false);      // add this
            promptLabel.setVisible(false);  // add this
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
        game.bgm.stop();
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
        shapeRenderer.dispose();
    }
}
