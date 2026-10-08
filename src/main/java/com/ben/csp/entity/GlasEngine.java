package com.ben.csp.entity;

import net.minecraft.nbt.NbtCompound;

/**
 * Glas (Глас) Entity - Engineer/Designer Character AI.
 */
public class GlasEngine {

    // AI State Machine
    public enum AIState {
        WAITING_FOR_TASK,
        PERFORMING_CALCULATION,
        REVIEWING_RESULTS,
        PRESENTING_REPORTS,
        INVESTIGATING_ISSUE
    }

    private AIState aiState = AIState.WAITING_FOR_TASK;
    private double currentCalculationComplexity = 0.0;
    private String researchTopic = "Doppler Broadening Analysis";
    private String currentProject = null;

    public GlasEngine() {
    }

    /**
     * Get current AI state.
     */
    public AIState getAiState() {
        return aiState;
    }

    /**
     * Transition to new AI state.
     */
    public void setAiState(AIState newState) {
        aiState = newState;
    }

    /**
     * Request calculation task.
     */
    public boolean requestCalculationTask(String topic, double complexity) {
        this.researchTopic = topic;
        this.currentCalculationComplexity = complexity;
        
        if (complexity <= 10.0) {
            setAiState(AIState.PERFORMING_CALCULATION);
            return true;
        }
        
        return false;
    }

    /**
     * Get research topic.
     */
    public String getResearchTopic() {
        return researchTopic;
    }

    /**
     * Set research topic.
     */
    public void setResearchTopic(String topic) {
        this.researchTopic = topic;
    }

    /**
     * Get current project.
     */
    public String getCurrentProject() {
        return currentProject;
    }

    /**
     * Set current project.
     */
    public void setCurrentProject(String project) {
        this.currentProject = project;
    }

    /**
     * Write AI state to NBT.
     */
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("aiState", aiState.name());
        nbt.putDouble("calculationComplexity", currentCalculationComplexity);
    }

    /**
     * Read AI state from NBT.
     */
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("aiState")) {
            this.aiState = AIState.valueOf(nbt.getString("aiState"));
        }
        if (nbt.contains("calculationComplexity")) {
            this.currentCalculationComplexity = nbt.getDouble("calculationComplexity");
        }
    }

    public void readNbtFromCompound(NbtCompound nbt) {
        readNbt(nbt);
    }
}
