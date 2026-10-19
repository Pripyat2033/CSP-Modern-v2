package com.ben.csp.entity.goals;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/**
 * WorkAtAssignedPostGoal - AI behavior goal that makes entities work at their assigned workplace.
 * The entity will enter work pose and stay there while not actively moving.
 * 
 * Note: This goal handles state transitions (entering/leaving work pose) without direct movement.
 * Movement is handled by the AI system's default navigation behavior.
 */
public class WorkAtAssignedPostGoal extends Goal {
    
    private final PathAwareEntity owner;
    private boolean shouldTick = false;
    
    /**
     * Create work-at-post goal.
     * @param owner The entity that will work at the assigned post.
     */
    public WorkAtAssignedPostGoal(PathAwareEntity owner) {
        this.owner = owner;
    }
    
    @Override
    public boolean canStart() {
        // Only start if we have a workplace position and are in world
        World world = this.owner.getWorld();
        if (world == null || !world.isClient()) {
            return false;
        }
        
        BlockPos workplacePos = getWorkplacePosition(this.owner);
        if (workplacePos == null) {
            return false;
        }
        
        // Don't start if we're too close to workplace - use integer coordinate distance
        int ownerX = MathHelper.floor(this.owner.getX());
        int ownerY = MathHelper.floor(this.owner.getY()) + 1;
        int ownerZ = MathHelper.floor(this.owner.getZ());
        int workX = workplacePos.getX();
        int workY = workplacePos.getY() + 1;
        int workZ = workplacePos.getZ();
        
        return (ownerX - workX) * (ownerX - workX) +
                (ownerY - workY) * (ownerY - workY) +
                (ownerZ - workZ) * (ownerZ - workZ) > 100;
    }
    
    @Override
    public void start() {
        shouldTick = true;
    }
    
    @Override
    public void stop() {
        shouldTick = false;
        enterWorkPose(this.owner);
    }
    
    /**
     * Get the workplace position from various sources.
     */
    private BlockPos getWorkplacePosition(PathAwareEntity walker) {
        // Try garrison position first (their home/base of operations)
        if (walker instanceof com.ben.csp.entity.ai.Garrisonable garrisonable) {
            BlockPos garrisonPos = garrisonable.getGarrisonPos();
            if (garrisonPos != null) {
                return garrisonPos;
            }
        }
        
        // Fall back to workplace position field via reflection
        try {
            java.lang.reflect.Field field = walker.getClass().getDeclaredField("workplacePos");
            field.setAccessible(true);
            Object workplaceObj = field.get(walker);
            if (workplaceObj instanceof net.minecraft.util.math.BlockPos) {
                return (net.minecraft.util.math.BlockPos) workplaceObj;
            }
        } catch (Exception e) {
            // Reflection failed, continue without it
        }
        
        return null;
    }
    
    /**
     * Enter work pose for this entity.
     */
    private void enterWorkPose(PathAwareEntity walker) {
        if (walker instanceof com.ben.csp.entity.ai.Garrisonable garrisonable) {
            garrisonable.setSitting(true);
        }
        
        // Try to set look at workplace using reflection on LookControl
        BlockPos workplacePos = getWorkplacePosition(walker);
        if (workplacePos != null && walker.getWorld() != null) {
            try {
                Object lookControl = walker.getClass().getMethod("getLookControl").invoke(walker);
                if (lookControl != null) {
                    // Use reflection to call lookAt method on LookControl
                    java.lang.reflect.Method method = lookControl.getClass().getMethod("lookAt", 
                        double.class, double.class, double.class, float.class);
                    method.invoke(lookControl, workplacePos.getX(), workplacePos.getY() + 1.0, 
                        workplacePos.getZ(), 30.0f);
                }
            } catch (Exception e) {
                // Ignore reflection failures for lookAt - not critical
            }
        }
    }
    
    @Override
    public void tick() {
        // Only process if we're ticking (after starting)
        if (!shouldTick) {
            return;
        }
        
        World world = this.owner.getWorld();
        if (world == null || !walker().isOnGround()) {
            shouldTick = false;
            return;
        }
        
        // Let the AI system handle navigation - we just need to ensure entity enters work pose when at ground
        BlockPos workplacePos = getWorkplacePosition(this.owner);
        if (workplacePos == null) {
            stop();
            return;
        }
        
        // At ground, enter work pose
        enterWorkPose(walker());
    }
    
    private PathAwareEntity walker() {
        return owner;
    }
}
