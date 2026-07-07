package com.bulkchef;

public class PlayerStats {
    public static final float MAX_CAL = 3000f;
    public static final float MAX_ENERGY = 100f;
    public static final float MAX_UPPER_MUSCLE = 50f;
    public static final float MAX_LOWER_MUSCLE = 50f;


    public void addUpperMuscle(float amount) {
        upperMuscle = Math.max(0, Math.min(upperMuscle + amount, MAX_UPPER_MUSCLE));
    }

    public void addLowerMuscle(float amount) {
        lowerMuscle = Math.max(0, Math.min(lowerMuscle + amount, MAX_LOWER_MUSCLE));
    }

    public float totalMuscle() {
        return upperMuscle + lowerMuscle; // max 100
    }

    public void addCalories(float amount) {
        cal = Math.max(0, Math.min(cal + amount, MAX_CAL));
    }

    public void addEnergy(float amount) {
        energy = Math.max(0, Math.min(energy + amount, MAX_ENERGY));
    }

    public float cal = 600f;
    public float energy = 100f;
    public float upperMuscle = 0f;
    public float lowerMuscle = 0f;
    public int daysRemaining = 7;
}
