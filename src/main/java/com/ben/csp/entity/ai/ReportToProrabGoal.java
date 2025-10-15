package com.ben.csp.entity.ai;

import com.ben.csp.build.BuildTask;
import com.ben.csp.entity.MasterEntity;
import com.ben.csp.entity.ProrabEntity;
import com.ben.csp.util.CspEntityUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

/**
 * AI Goal for a Master to find their assigned Prorab and report task completion.
 */
public class ReportToProrabGoal extends Goal {
    private final MasterEntity master;
    private ProrabEntity targetProrab;

    public ReportToProrabGoal(MasterEntity master) {
        this.master = master;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        BuildTask task = this.master.getCurrentTask();
        // Can start if work is complete and we have a Prorab assigned.
        return task != null && "WorkComplete".equals(task.getStatus()) && this.master.getProrabUuid() != null;
    }

    @Override
    public boolean shouldContinue() {
        // Continue as long as we have a target and haven't reached them.
        return this.targetProrab != null && !this.master.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.targetProrab = CspEntityUtil.findEntityByUuid((ServerWorld) this.master.getWorld(), this.master.getProrabUuid(), ProrabEntity.class);
        if (this.targetProrab != null) {
            this.master.getNavigation().startMovingTo(this.targetProrab, 1.0D);
        }
    }

    @Override
    public void tick() {
        if (this.targetProrab != null && this.master.distanceTo(this.targetProrab) < 3.0) {
            // We've reached the Prorab.
            this.targetProrab.acceptReportFromMaster(this.master, "Task '" + this.master.getCurrentTask().getDescription() + "' complete.");
            this.master.getCurrentTask().setStatus("Reported");
            this.master.resetTask(); // Master is now free for a new task.
            this.master.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.targetProrab = null;
    }
}