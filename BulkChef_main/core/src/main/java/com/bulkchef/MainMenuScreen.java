package com.bulkchef;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
    private int selectedIndex = 0;

    public MainMenuScreen(BulkChef game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        SceneComposerStageBuilder builder = new SceneComposerStageBuilder();
        builder.build(stage, game.skin, Gdx.files.internal("ui/homescreen/startmenu.json"));

        startButton = stage.getRoot().findActor("newgame");
        quitButton = stage.getRoot().findActor("quit");

        stage.setKeyboardFocus(startButton);
        selectedIndex = 0;

        if (startButton != null) {
            startButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new GameScreen(game));
                }
            });
        }

        if (quitButton != null) {
            quitButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Gdx.app.exit();
                }
            });
        }

        TextButton startButton = stage.getRoot().findActor("newgame");
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (isStarting) return;

                isStarting = true;
                startButton.setDisabled(true);
                startButton.setTouchable(Touchable.disabled);

                System.out.println("Started");
                game.setScreen(new GameScreen(game));
            }
        });

        TextButton quitButton = stage.getRoot().findActor("quit");
        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("Quitting");
                Gdx.app.exit();
            }
        });
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            selectedIndex = (selectedIndex + 1) % 2;

            if (selectedIndex == 0) {
            stage.setKeyboardFocus(startButton);
        } else {
            stage.setKeyboardFocus(quitButton);
        }
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
        Actor focused = stage.getKeyboardFocus();

        if (focused == startButton) {
            game.setScreen(new GameScreen(game));
        } else if (focused == quitButton) {
            Gdx.app.exit();
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
