package com.ben.csp.entity.ai;

import com.ben.csp.entity.ai.PsychologicalState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;

import java.util.EnumSet;

/**
 * A low-priority AI goal for an entity to recover from fatigue when idle.
 * This represents an NPC taking a break, sitting down, or otherwise resting.
 */
public class RestGoal extends Goal {
    private final PathAwareEntity entity;
    private final PsychologicalState psychologicalState;
    private final float recoveryRate;

    public RestGoal(PathAwareEntity entity, PsychologicalState psychologicalState, float recoveryRate) {
        this.entity = entity;
        this.psychologicalState = psychologicalState;
        this.recoveryRate = recoveryRate;
        // This goal doesn't require movement, but it should be interruptible.
        this.setControls(EnumSet.noneOf(Goal.Control.class));
    }

    @Override
    public boolean canStart() {
        // Can start if the entity is idle and has some fatigue to recover.
        return this.entity.getNavigation().isIdle() && this.psychologicalState.getFatigue() > 0;
    }

    @Override
    public boolean shouldContinue() {
        // Continue as long as the entity remains idle and is still fatigued.
        return this.entity.getNavigation().isIdle() && this.psychologicalState.getFatigue() > 0;
    }

    @Override
    public void tick() {
        // Recover from fatigue.
        this.psychologicalState.rest(this.recoveryRate);
    }
}