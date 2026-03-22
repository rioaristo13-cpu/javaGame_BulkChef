package com.bulkchef;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class BulkChef extends Game {
    @Override
    public void create() {
        setScreen(new FirstScreen());
    }
}