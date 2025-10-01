package com.ben.csp.entity.ai;

import com.ben.csp.entity.MasterEntity;
import com.ben.csp.entity.StroitelEntity;
import com.ben.csp.util.CspEntityUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

/**
 * AI Goal for a Stroitel to perform basic construction work near its Master.
 */
public class PerformConstructionGoal extends Goal {
    private final StroitelEntity stroitel;
    private MasterEntity master;
    private int workTicks;

    public PerformConstructionGoal(StroitelEntity stroitel) {
        this.stroitel = stroitel;
        this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (this.stroitel.getMasterUuid() == null) return false;
        this.master = CspEntityUtil.findEntityByUuid((ServerWorld) this.stroitel.getWorld(), this.stroitel.getMasterUuid(), MasterEntity.class);

        // Can start if the master is nearby and has an active task.
        return this.master != null && this.master.getCurrentTask() != null && this.stroitel.distanceTo(this.master) < 5.0;
    }

    @Override
    public boolean shouldContinue() {
        return this.workTicks > 0 && this.master != null && this.master.getCurrentTask() != null;
    }

    @Override
    public void start() {
        this.workTicks = 200; // Simulate 10 seconds of work.
        this.stroitel.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.workTicks--;
        if (this.workTicks % 20 == 0) {
            // Look towards the master to simulate coordination.
            this.stroitel.getLookControl().lookAt(this.master);
        }
    }

    @Override
    public void stop() {
        this.workTicks = 0;
    }
}