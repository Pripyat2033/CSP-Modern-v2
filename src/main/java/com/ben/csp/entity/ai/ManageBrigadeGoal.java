package com.ben.csp.entity.ai;

import com.ben.csp.entity.BrigadierEntity;
import com.ben.csp.entity.StroitelEntity;
import com.ben.csp.util.CspEntityUtil;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Science Grade: AI Goal for a Brigadier to manage its brigade members.
 * This involves assigning the correct Master to each Stroitel in the crew.
 */
public class ManageBrigadeGoal extends Goal {
    private final BrigadierEntity brigadier;
    private int checkCooldown;

    public ManageBrigadeGoal(BrigadierEntity brigadier) {
        this.brigadier = brigadier;
        this.setControls(EnumSet.of(Goal.Control.LOOK)); // Doesn't need to move, just think.
    }

    @Override
    public boolean canStart() {
        // This is a background task that should always be running.
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
            this.checkCooldown = 100; // Check every 5 seconds.

            UUID masterUuid = this.brigadier.getMasterUuid();
            if (masterUuid == null) return;

            ServerWorld world = (ServerWorld) this.brigadier.getWorld();

            for (UUID memberUuid : this.brigadier.getBrigadeMemberUuids()) {
                StroitelEntity stroitel = CspEntityUtil.findEntityByUuid(world, memberUuid, StroitelEntity.class);
                if (stroitel != null && (stroitel.getMasterUuid() == null || !stroitel.getMasterUuid().equals(masterUuid))) {
                    // Assign or correct the Master for this Stroitel.
                    stroitel.setMasterUuid(masterUuid);
                }
            }
        }
    }
}