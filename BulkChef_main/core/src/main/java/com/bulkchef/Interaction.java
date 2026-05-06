package com.bulkchef;

public class Interaction {
    public enum Type { FOOD, EX_UPPER, EX_LOWER, REST }

    public final String label;
    public final Type   type;
    public final float  calDelta;
    public final float  energyDelta;
    public final float  upperDelta;
    public final float  lowerDelta;
    public final float  energyCost;

    public Interaction(String label,
                       Type type,
                       float calDelta,
                       float energyDelta,
                       float upperDelta,
                       float lowerDelta,
                       float energyCost) {
        this.label       = label;
        this.type        = type;
        this.calDelta    = calDelta;
        this.energyDelta = energyDelta;
        this.upperDelta  = upperDelta;
        this.lowerDelta  = lowerDelta;
        this.energyCost  = energyCost;
    }

}


