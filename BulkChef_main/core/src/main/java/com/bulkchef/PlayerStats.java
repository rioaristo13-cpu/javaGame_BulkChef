package com.bulkchef;

public class PlayerStats {
    public static final float MAX_CAL = 3000f;
    public static final float MAX_ENERGY = 100f;

    public void addCalories(float amount) {
        cal = Math.max(0, Math.min(cal + amount, MAX_CAL));
    }

    public void addEnergy(float amount) {
        energy = Math.max(0, Math.min(energy + amount, MAX_ENERGY));
    }

    public void addUpperMuscle(float amount) {
        upperMuscle = Math.max(0, upperMuscle + amount);
    }

    public void addLowerMuscle(float amount) {
        lowerMuscle = Math.max(0, lowerMuscle + amount);
    }

    public boolean isHungry()  { return cal <= 0; }
    public boolean isTired() { return energy <= 0; }

    public float cal = 600f;
    public float energy = 100f;
    public float upperMuscle = 0f;
    public float lowerMuscle = 0f;

    public float totalMuscle() {
        return lowerMuscle+upperMuscle;
    };
}
