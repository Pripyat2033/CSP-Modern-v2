package com.ben.csp.entity;

import net.minecraft.nbt.NbtCompound;

import java.util.HashSet;
import java.util.Set;

public class PsychologicalState {
    private float stress = 0.0f; // 0.0 to 1.0
    private final Set<String> recentTopics = new HashSet<>();

    public float getStress() { return stress; }
    public void addStress(float amount) { this.stress = Math.min(1.0f, this.stress + amount); }
    public void relieveStress(float amount) { this.stress = Math.max(0.0f, this.stress - amount); }

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