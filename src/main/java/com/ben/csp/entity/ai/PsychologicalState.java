package com.ben.csp.entity.ai;

import net.minecraft.nbt.NbtCompound;

/**
 * A model for tracking the psychological state of an NPC, including stress and morale.
 * This affects their performance and decision-making.
 */
public class PsychologicalState {
    private float stress = 0.0f;    // 0.0 (calm) to 1.0 (overwhelmed)
    private float morale = 0.5f;    // 0.0 (disillusioned) to 1.0 (zealous)
    private float fatigue = 0.0f;   // 0.0 (rested) to 1.0 (exhausted)

    public float getStress() {
        return this.stress;
    }
    public void addStress(float amount) {
        this.stress = Math.min(1.0f, this.stress + amount);
    }

    public float getMorale() {
        return this.morale;
    }
    public void addMorale(float amount) {
        this.morale = Math.max(0.0f, Math.min(1.0f, this.morale + amount));
    }

    public float getFatigue() {
        return this.fatigue;
    }
    public void addFatigue(float amount) {
        this.fatigue = Math.min(1.0f, this.fatigue + amount);
    }
    public void rest(float amount) {
        this.fatigue = Math.max(0.0f, this.fatigue - amount);
    }

    public void tick() {
        // Natural stress decay over time.
        if (this.stress > 0) {
            this.stress = Math.max(0.0f, this.stress - 0.0005f);
        }
        // Fatigue slowly increases during a workday.
        this.fatigue = Math.min(1.0f, this.fatigue + 0.0001f);
    }

    public void writeToNbt(NbtCompound nbt) {
        nbt.putFloat("Stress", this.stress);
        nbt.putFloat("Morale", this.morale);
        nbt.putFloat("Fatigue", this.fatigue);
    }
    public void readFromNbt(NbtCompound nbt) {
        this.stress = nbt.getFloat("Stress");
        this.morale = nbt.getFloat("Morale");
        this.fatigue = nbt.getFloat("Fatigue");
    }
}