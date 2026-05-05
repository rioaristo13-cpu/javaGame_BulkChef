package com.bulkchef;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class BulkChef extends Game {
    public Skin skin;
    public Music bgm;
    public Sound sfxNavigate;
    public Sound sfxEnter;
    public float sfxVolume = 0.8f;

    @Override
    public void create() {
        skin = new Skin(Gdx.files.internal("ui/skin/comic-ui.json"));

        bgm = Gdx.audio.newMusic(Gdx.files.internal("audio/bgm.mp3"));
        bgm.setLooping(true);
        bgm.setVolume(0.5f);

        sfxNavigate = Gdx.audio.newSound(Gdx.files.internal("audio/sfx_navigate.mp3"));
        sfxEnter = Gdx.audio.newSound(Gdx.files.internal("audio/sfx_enter.mp3"));

        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {

        skin.dispose();
        bgm.dispose();
        sfxNavigate.dispose();
        sfxEnter.dispose();
    }
}
