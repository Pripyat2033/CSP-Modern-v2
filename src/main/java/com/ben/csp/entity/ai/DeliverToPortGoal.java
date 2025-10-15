package com.ben.csp.entity.ai;

import com.ben.csp.entity.BargeEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.EnumSet;

/**
 * An AI goal for a BargeEntity to travel to a specified destination port.
 */
public class DeliverToPortGoal extends Goal {

    private final BargeEntity barge;
    private BlockPos destination;

    public DeliverToPortGoal(BargeEntity barge) {
        this.barge = barge;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // This goal can start if the barge has a destination set by an external system.
        this.destination = this.barge.getDestination();
        return this.destination != null;
    }

    @Override
    public boolean shouldContinue() {
        // Continue as long as we have a destination and haven't reached it (navigation is not idle).
        return this.destination != null && !this.barge.getNavigation().isIdle();
    }

    @Override
    public void start() {
        // Start moving towards the destination. The speed is controlled by the entity's attribute.
        this.barge.getNavigation().startMovingTo(this.destination.getX(), this.destination.getY(), this.destination.getZ(), 1.0D);
    }

    @Override
    public void tick() {
        // If we've reached the destination (or are very close)
        if (this.destination != null && this.barge.getBlockPos().isWithinDistance(this.destination, 4.0)) {
            // "Unload" cargo and clear the barge's destination to signal completion.
            this.barge.loadCargo(Collections.emptyList()); // Placeholder for unloading logic
            this.barge.setDestination(null);
        }
    }

    @Override
    public void stop() {
        this.barge.getNavigation().stop();
        this.destination = null;
    }
}