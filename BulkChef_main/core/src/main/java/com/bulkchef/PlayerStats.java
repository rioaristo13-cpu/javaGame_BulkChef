package com.bulkchef;

public class PlayerStats {
    public float cal = 2000f;
    public float energy = 100f;
    public float upperMuscle = 0f;
    public float lowerMuscle = 0f;

    public float totalMuscle() {
        return lowerMuscle+upperMuscle;
    };
}
