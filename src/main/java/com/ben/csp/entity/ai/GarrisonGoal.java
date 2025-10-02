package com.ben.csp.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;

import java.util.EnumSet;

/**
 * A goal that makes a Garrisonable entity return to its post when idle.
 */
public class GarrisonGoal extends Goal {
    private final PathAwareEntity entity;
    private final Garrisonable garrisonable;
    private final double speed;

    public GarrisonGoal(PathAwareEntity entity, double speed) {
        this.entity = entity;
        if (!(entity instanceof Garrisonable)) {
            throw new IllegalArgumentException("GarrisonGoal requires a Garrisonable entity.");
        }
        this.garrisonable = (Garrisonable) entity;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // Can start if the entity is outside its garrison radius.
        return !this.entity.getBlockPos().isWithinDistance(this.garrisonable.getGarrisonPos(), this.garrisonable.getGarrisonRadius());
    }

    @Override
    public boolean shouldContinue() {
        // Continue until we are back inside the radius.
        return !this.entity.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.entity.getNavigation().startMovingTo(
            this.garrisonable.getGarrisonPos().getX(),
            this.garrisonable.getGarrisonPos().getY(),
            this.garrisonable.getGarrisonPos().getZ(),
            this.speed
        );
    }
}