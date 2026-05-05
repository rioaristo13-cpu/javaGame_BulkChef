package com.bulkchef;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

public class SaveData {
    public float cal, energy, upperMuscle, lowerMuscle;
    public float posX, posY;

    public static void save(PlayerStats stats, float posX, float posY) {
        SaveData d = new SaveData();
        d.cal = stats.cal; d.energy = stats.energy;
        d.upperMuscle = stats.upperMuscle; d.lowerMuscle = stats.lowerMuscle;
        d.posX = posX; d.posY = posY;
        Gdx.files.local("save.json").writeString(new Json().toJson(d), false);
    }

    public static SaveData load() {
        var f = Gdx.files.local("save.json");
        if (!f.exists()) return null;
        return new Json().fromJson(SaveData.class, f.readString());
    }
}
