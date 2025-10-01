package com.ben.csp.radiology;

import net.minecraft.nbt.NbtCompound;

import java.util.HashSet;
import java.util.Set;

/**
 * Science Grade: A data object that tracks the psychological state of an NPC,
 * primarily their stress level. This state can be influenced by various in-game events.
 */
public class PsychologicalState {
    private float stress = 0.0f; // A value from 0.0 (calm) to 1.0 (highly stressed)
    private final Set<String> recentTopics = new HashSet<>();

    public float getStress() { return stress; }

    public void addStress(float amount) {
        this.stress = Math.min(1.0f, this.stress + amount);
    }

    public void relieveStress(float amount) {
        this.stress = Math.max(0.0f, this.stress - amount);
    }

    public void logTopic(String topicId) {
        recentTopics.add(topicId);
    }

    public boolean hasDiscussedTopic(String topicId) {
        return recentTopics.contains(topicId);
    }

    public void writeToNbt(NbtCompound nbt) {
        nbt.putFloat("Stress", this.stress);
    }

    public void readFromNbt(NbtCompound nbt) {
        if (nbt.contains("Stress")) {
            this.stress = nbt.getFloat("Stress");
        }
        // Note: recentTopics are not persisted. They are session-based.
    }
}