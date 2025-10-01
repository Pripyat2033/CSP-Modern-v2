package com.ben.csp.entity.ai;

import com.ben.csp.build.BuildTask;
import com.ben.csp.entity.MasterEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

/**
 * AI Goal for a Master to travel to their assigned build site.
 */
public class GoToBuildSiteGoal extends Goal {
    private final MasterEntity master;
    private BlockPos targetPos;

    public GoToBuildSiteGoal(MasterEntity master) {
        this.master = master;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        BuildTask task = this.master.getCurrentTask();
        if (task == null || !"Assigned".equals(task.getStatus())) {
            return false;
        }
        this.targetPos = task.getDestination();
        // Can start if we have a destination and are not already there.
        return this.targetPos != null && !this.master.getBlockPos().isWithinDistance(this.targetPos, 4.0);
    }

    @Override
    public boolean shouldContinue() {
        // Continue as long as we haven't reached the destination.
        return this.targetPos != null && !this.master.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.master.getNavigation().startMovingTo(this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ(), 1.0D);
    }

    @Override
    public void stop() {
        this.targetPos = null;
    }
}