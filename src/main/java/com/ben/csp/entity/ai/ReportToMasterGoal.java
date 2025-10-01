package com.ben.csp.entity.ai;

import com.ben.csp.entity.MasterEntity;
import com.ben.csp.entity.StroitelEntity;
import com.ben.csp.util.CspEntityUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

/**
 * AI Goal for a Stroitel to report to its Master after a task is complete.
 */
public class ReportToMasterGoal extends Goal {
    private final StroitelEntity stroitel;
    private MasterEntity master;

    public ReportToMasterGoal(StroitelEntity stroitel) {
        this.stroitel = stroitel;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.stroitel.getMasterUuid() == null) return false;
        this.master = CspEntityUtil.findEntityByUuid((ServerWorld) this.stroitel.getWorld(), this.stroitel.getMasterUuid(), MasterEntity.class);

        // Can start if the master exists but no longer has a task (meaning it's complete).
        return this.master != null && this.master.getCurrentTask() == null;
    }

    @Override
    public boolean shouldContinue() {
        return false; // This is a one-off action.
    }

    @Override
    public void start() {
        if (this.master != null) {
            if (this.stroitel.distanceTo(this.master) > 3.0) {
                this.stroitel.getNavigation().startMovingTo(this.master, 1.0D);
            } else {
                // If already close, just report.
                this.master.acceptReportFromStroitel(this.stroitel);
            }
        }
    }

    @Override
    public void tick() {
        if (this.master != null && this.stroitel.distanceTo(this.master) < 3.0) {
            this.master.acceptReportFromStroitel(this.stroitel);
            this.stroitel.getNavigation().stop();
        }
    }
}