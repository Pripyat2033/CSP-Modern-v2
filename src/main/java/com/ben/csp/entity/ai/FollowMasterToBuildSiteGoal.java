package com.ben.csp.entity.ai;

import com.ben.csp.entity.MasterEntity;
import com.ben.csp.entity.StroitelEntity;
import com.ben.csp.util.CspEntityUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

/**
 * AI Goal for a Stroitel to follow its assigned Master to the build site.
 */
public class FollowMasterToBuildSiteGoal extends Goal {
    private final StroitelEntity stroitel;
    private MasterEntity master;

    public FollowMasterToBuildSiteGoal(StroitelEntity stroitel) {
        this.stroitel = stroitel;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.stroitel.getMasterUuid() == null) return false;

        this.master = CspEntityUtil.findEntityByUuid((ServerWorld) this.stroitel.getWorld(), this.stroitel.getMasterUuid(), MasterEntity.class);

        // Can start if the master exists, has a task, and the stroitel is too far away.
        return this.master != null && this.master.getCurrentTask() != null && this.stroitel.distanceTo(this.master) > 5.0;
    }

    @Override
    public boolean shouldContinue() {
        // Continue until the stroitel is close to the master.
        return this.master != null && this.master.getCurrentTask() != null && this.stroitel.distanceTo(this.master) > 3.0;
    }

    @Override
    public void start() {
        this.stroitel.getNavigation().startMovingTo(this.master, 1.0D);
    }

    @Override
    public void stop() {
        this.master = null;
        if (!this.stroitel.getNavigation().isIdle()) {
            this.stroitel.getNavigation().stop();
        }
    }
}