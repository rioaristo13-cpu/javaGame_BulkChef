package com.bulkchef;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class BulkChef extends Game {
    public Skin skin;

    @Override
    public void create() {
        skin = new Skin(Gdx.files.internal("ui/skin/comic-ui.json"));
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        skin.dispose();
    }
}
