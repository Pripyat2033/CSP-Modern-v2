package com.ben.csp.skala;

import net.minecraft.world.World;
import java.util.function.DoubleSupplier;

public class SkalaChannel {
    private final String code;
    private DoubleSupplier valueSupplier;
    private double lastValue;

    public enum ScanType {
        DREG,
        DIIS
    }

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