package com.bulkchef;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.ray3k.stripe.scenecomposer.SceneComposerStageBuilder;

public class MainMenuScreen implements Screen {
    private final BulkChef game;
    private Stage stage;
    private boolean isStarting = false;
    private TextButton startButton;
    private TextButton quitButton;
    private int selectedIndex = -1;
    private float quitTimer = -1f;

    public MainMenuScreen(BulkChef game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        SceneComposerStageBuilder builder = new SceneComposerStageBuilder();
        builder.build(stage, game.skin, Gdx.files.internal("ui/homescreen/startmenu.json"));

        // Assign to class fields only — no redundant local TextButton declarations
        startButton = stage.getRoot().findActor("newgame");
        quitButton  = stage.getRoot().findActor("quit");

        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isStarting) return;
                game.sfxEnter.play(game.sfxVolume);
                isStarting = true;
                startButton.setDisabled(true);
                startButton.setTouchable(Touchable.disabled);
                game.setScreen(new GameScreen(game));
            }
        });

        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.sfxEnter.play(game.sfxVolume);
                quitTimer = 0f;
            }
        });

        // Hover sounds
        addHoverSound(startButton);
        addHoverSound(quitButton);
    }

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

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        if (quitTimer >= 0f) {
            quitTimer += delta;
            if (quitTimer >= 0.6f) {
                Gdx.app.exit();
            }
        }

        boolean navDown = Gdx.input.isKeyJustPressed(Input.Keys.S)
            || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
            || Gdx.input.isKeyJustPressed(Input.Keys.TAB);
        boolean navUp = Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP);

        if (navDown) {
            selectedIndex = (selectedIndex + 1) % 2;
            stage.setKeyboardFocus(selectedIndex == 0 ? startButton : quitButton);
            game.sfxNavigate.play(game.sfxVolume);
        } else if (navUp) {
            selectedIndex = (selectedIndex - 1 + 2) % 2;
            stage.setKeyboardFocus(selectedIndex == 0 ? startButton : quitButton);
            game.sfxNavigate.play(game.sfxVolume);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {

            game.sfxEnter.play(game.sfxVolume);

            Actor focused = stage.getKeyboardFocus();

            if (focused == startButton) {
                game.setScreen(new GameScreen(game));
            } else if (focused == quitButton) {
                game.sfxEnter.play(game.sfxVolume);
                quitTimer = 0f;
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
