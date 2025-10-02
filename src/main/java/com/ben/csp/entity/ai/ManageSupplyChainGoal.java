package com.ben.csp.entity.ai;

import com.ben.csp.build.BuildTask;
import com.ben.csp.build.Enterprise;
import com.ben.csp.entity.NachalnikSnabzheniyaEntity;
import com.ben.csp.logistics.LogisticsManager;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;

/**
 * Science Grade: AI Goal for the Head of Supply to manage the enterprise's logistics.
 * This involves monitoring build tasks and dispatching resources as needed.
 */
public class ManageSupplyChainGoal extends Goal {
    private final NachalnikSnabzheniyaEntity nachalnik;
    private int checkCooldown;

    public ManageSupplyChainGoal(NachalnikSnabzheniyaEntity nachalnik) {
        this.nachalnik = nachalnik;
        this.setControls(EnumSet.of(Goal.Control.LOOK)); // This is a "thinking" goal.
    }

    @Override
    public boolean canStart() {
        // This is a background management task that should always be running.
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return true;
    }

    @Override
    public void tick() {
        this.checkCooldown--;
        if (this.checkCooldown <= 0) {
            this.checkCooldown = 200; // Check for new tasks every 10 seconds.

            Enterprise enterprise = this.nachalnik.getEnterprise();
            if (enterprise == null) {
                return; // Not assigned to an enterprise yet.
            }

            ServerWorld world = (ServerWorld) this.nachalnik.getWorld();

            for (BuildTask task : enterprise.getTasks()) {
                // Find tasks that are pending and require resources.
                if ("Pending".equals(task.getStatus()) && task.getDestination() != null) {
                    // Fulfill the request and update the task status.
                    LogisticsManager.getInstance().fulfillResourceRequest(task, world);
                    task.setStatus("ResourcesDispatched");
                }
            }
        }
    }
}