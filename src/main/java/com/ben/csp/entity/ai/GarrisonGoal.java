package com.ben.csp.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class GarrisonGoal extends Goal {
    private final PathAwareEntity entity;
    private final Garrisonable garrisonable;
    private BlockPos garrisonPos;
    private final double speed;
    private int checkCooldown;

    public GarrisonGoal(PathAwareEntity entity, double speed) {
        if (!(entity instanceof Garrisonable)) {
            throw new IllegalArgumentException("Entity must implement Garrisonable to use GarrisonGoal");
        }
        this.entity = entity;
        this.garrisonable = (Garrisonable) entity;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // This check prevents the goal from running its logic on every single tick.
        if (this.checkCooldown > 0) {
            this.checkCooldown--;
            return false;
        }
        // Reset the cooldown. Check every 1-2 seconds, with randomness to prevent all entities checking at once.
        this.checkCooldown = 20 + this.entity.getRandom().nextInt(20);

        // This is a low-priority goal. It can only start if the entity is idle.
        if (!this.entity.getNavigation().isIdle()) {
            return false;
        }

        this.garrisonPos = this.garrisonable.getGarrisonPos();
        if (this.garrisonPos == null) {
            return false;
        }

        // Start if we are outside our garrison radius.
        int radius = this.garrisonable.getGarrisonRadius();
        return this.entity.getPos().squaredDistanceTo(this.garrisonPos.toCenterPos()) > (radius * radius);
    }

    @Override
    public boolean shouldContinue() {
        // Continue as long as we are not at the destination.
        return this.garrisonPos != null && !this.entity.getNavigation().isIdle();
    }

    @Override
    public void start() {
        if (this.garrisonPos != null) {
            this.entity.getNavigation().startMovingTo(this.garrisonPos.getX(), this.garrisonPos.getY(), this.garrisonPos.getZ(), this.speed);
        }
    }

    @Override
    public void stop() {
        this.garrisonPos = null;
        // The entity has arrived and will now be idle, allowing other goals (like Wander) to take over within a small area.
    }
}