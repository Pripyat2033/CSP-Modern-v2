package com.ben.csp.entity.ai;

import com.ben.csp.build.BuildTask;
import com.ben.csp.entity.MasterEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI Goal for a Master to perform the work of their assigned BuildTask.
 */
public class PerformWorkPackageGoal extends Goal {
    private final MasterEntity master;
    private int workTicks;

    public PerformWorkPackageGoal(MasterEntity master) {
        this.master = master;
        this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.JUMP));
    }

    @Override
    public boolean canStart() {
        BuildTask task = this.master.getCurrentTask();
        if (task == null || !"Assigned".equals(task.getStatus())) {
            return false;
        }
        // Can only start if we are at the build site.
        return task.getDestination() != null && this.master.getBlockPos().isWithinDistance(task.getDestination(), 4.0);
    }

    @Override
    public boolean shouldContinue() {
        return this.workTicks > 0 && this.master.getCurrentTask() != null;
    }

    @Override
    public void start() {
        this.workTicks = 200; // Simulate 10 seconds of work.
        this.master.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.workTicks--;
        if (this.workTicks % 20 == 0) {
            // Look around, swing arm, etc. to simulate work.
            this.master.getLookControl().lookAt(this.master.getCurrentTask().getDestination().toCenterPos());
        }
    }

    @Override
    public void stop() {
        BuildTask task = this.master.getCurrentTask();
        if (task != null) {
            task.setStatus("WorkComplete");
        }
        this.workTicks = 0;
    }
}