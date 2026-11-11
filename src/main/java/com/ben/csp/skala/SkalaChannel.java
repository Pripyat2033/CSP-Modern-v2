package com.ben.csp.skala;

import net.minecraft.world.World;
import java.util.function.DoubleSupplier;

/**
 * One input channel of the SKALA process computer: a named value the computer
 * samples. Pre-1986 SKALA fed every channel through a single registration loop,
 * the DREG program; there was no separate fast loop. The DIIS auxiliary system
 * (later DIIS-2000), which added video monitors and a faster display path, was a
 * 1990s upgrade fitted at Chernobyl well after Unit 4 was destroyed, so it is not
 * modelled here (Chornobyl Family SKALA restoration).
 */
public class SkalaChannel {
    private final String code;
    private DoubleSupplier valueSupplier;
    private double lastValue;

    public SkalaChannel(String code, DoubleSupplier valueSupplier) {
        this.code = code;
        this.valueSupplier = valueSupplier;
    }

    public SkalaChannel(String code, double constantValue) {
        this(code, () -> constantValue);
    }

    public String getCode() {
        return code;
    }

    public double getValue() {
        return lastValue;
    }

    public void update(World world) {
        if (valueSupplier != null) {
            this.lastValue = valueSupplier.getAsDouble();
        }
    }
}