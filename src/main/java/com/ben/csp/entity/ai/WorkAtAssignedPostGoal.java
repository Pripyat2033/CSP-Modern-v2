package com.ben.csp.entity.ai;

import com.ben.csp.entity.IndustrialWorkerEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI Goal for an IndustrialWorker to go to their assigned post and perform a process.
 */
public class WorkAtAssignedPostGoal extends Goal {
    private final IndustrialWorkerEntity worker;
    private final double speed;
    private int workTicks;

    public WorkAtAssignedPostGoal(IndustrialWorkerEntity worker, double speed) {
        this.worker = worker;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Can start if the worker has a process assigned.
        return this.worker.getProcessId() != null;
    }

    @Override
    public boolean shouldContinue() {
        // Continue until the work is "done" or the process is unassigned.
        return this.worker.getProcessId() != null && this.workTicks > 0;
    }

    @Override
    public void start() {
        this.workTicks = 400; // Simulate 20 seconds of work.
        this.worker.getNavigation().startMovingTo(
            this.worker.getWorkplacePos().getX() + 0.5,
            this.worker.getWorkplacePos().getY(),
            this.worker.getWorkplacePos().getZ() + 0.5,
            this.speed
        );
    }

    @Override
    public void tick() {
        if (this.worker.getNavigation().isIdle()) {
            this.workTicks--;
            if (this.workTicks % 40 == 0) {
                this.worker.getLookControl().lookAt(this.worker.getWorkplacePos().toCenterPos());
            }
        }
    }

    @Override
    public void stop() {
        this.worker.getNavigation().stop();
        this.workTicks = 0;
    }
}