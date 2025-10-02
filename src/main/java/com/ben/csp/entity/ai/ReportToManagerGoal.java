package com.ben.csp.entity.ai;

import com.ben.csp.entity.IndustrialWorkerEntity;
import com.ben.csp.entity.ProrabEntity;
import com.ben.csp.util.CspEntityUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

/**
 * AI Goal for a worker to report to their manager, typically when stressed.
 */
public class ReportToManagerGoal extends Goal {
    private final IndustrialWorkerEntity worker;
    private ProrabEntity manager;

    public ReportToManagerGoal(IndustrialWorkerEntity worker) {
        this.worker = worker;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // Can start if stress is high and a manager is assigned.
        if (this.worker.getPsychologicalState().getStress() < 0.75 || this.worker.getManagerUuid() == null) {
            return false;
        }
        this.manager = CspEntityUtil.findEntityByUuid((ServerWorld) this.worker.getWorld(), this.worker.getManagerUuid(), ProrabEntity.class);
        return this.manager != null;
    }

    @Override
    public boolean shouldContinue() {
        // Continue as long as we have a manager and haven't reached them.
        return this.manager != null && !this.worker.getNavigation().isIdle();
    }

    @Override
    public void start() {
        if (this.manager != null) {
            this.worker.getNavigation().startMovingTo(this.manager, 1.0D);
        }
    }

    @Override
    public void tick() {
        if (this.manager != null && this.worker.distanceTo(this.manager) < 3.0) {
            // We've reached the manager. "Report" and relieve stress.
            this.worker.getPsychologicalState().relieveStress(0.5f);
            this.worker.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.manager = null;
        this.worker.getNavigation().stop();
    }
}