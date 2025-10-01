package com.ben.csp.radiology;

import net.minecraft.nbt.NbtCompound;

/**
 * ARL: Replaces the simple dose accumulator. This model simulates the ongoing
 * battle between radiation-induced damage and biological repair.
 */
public class TissueDamageModel {
    private double accumulatedDamage = 0.0; // An arbitrary unit of cellular damage
    private static final double REPAIR_RATE_PER_TICK = 0.0005; // Natural biological repair rate

    public void addEnergyDeposition(double energyMeV, RadiationType type) {
        // Damage is energy deposited multiplied by biological effectiveness.
        this.accumulatedDamage += energyMeV * type.biologicalEffectiveness;
    }

    public void tick() {
        // Simulate the body's constant attempt to repair itself.
        if (this.accumulatedDamage > 0) {
            this.accumulatedDamage = Math.max(0, this.accumulatedDamage - REPAIR_RATE_PER_TICK);
        }
    }

    public double getAccumulatedDamage() {
        return this.accumulatedDamage;
    }

    public void writeToNbt(NbtCompound nbt) {
        nbt.putDouble("AccumulatedDamage", this.accumulatedDamage);
    }

    public void readFromNbt(NbtCompound nbt) {
        if (nbt.contains("AccumulatedDamage")) {
            this.accumulatedDamage = nbt.getDouble("AccumulatedDamage");
        }
    }
}