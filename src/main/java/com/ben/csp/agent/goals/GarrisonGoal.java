package com.ben.csp.agent.goals;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * GarrisonGoal - AI behavior goal that makes entities stay at their garrison position.
 * Used for director, foreman, and other garrisoned entity types.
 * 
 * Note: This goal handles state transitions (entering/leaving work pose) without direct movement.
 * Movement is handled by the AI system's default navigation behavior.
 */
public class GarrisonGoal extends Goal {
    
    private final PathAwareEntity owner;
    private boolean shouldTick = false;
    
    /**
     * Create garrison goal.
     * @param owner The entity to keep at garrison position.
     * @param speedModifier Movement speed modifier for navigation (deprecated - ignored).
     */
    public GarrisonGoal(PathAwareEntity owner, float speedModifier) {
        this.owner = owner;
    }
    
    @Override
    public boolean canStart() {
        // Check if entity has a garrison position set
        if (!(owner instanceof com.ben.csp.entity.ai.Garrisonable garrisonable)) {
            return false;
        }
        
        BlockPos garrisonPos = garrisonable.getGarrisonPos();
        if (garrisonPos == null) {
            return false;
        }
        
        // Don't start if we're very close to workplace (already there)
        int ownerX = MathHelper.floor(this.owner.getX());
        int ownerY = MathHelper.floor(this.owner.getY()) + 1;
        int ownerZ = MathHelper.floor(this.owner.getZ());
        int garrisonX = garrisonPos.getX();
        int garrisonY = garrisonPos.getY() + 1;
        int garrisonZ = garrisonPos.getZ();
        
        // Use squared distance to avoid floating point issues
        return (ownerX - garrisonX) * (ownerX - garrisonX) +
                (ownerY - garrisonY) * (ownerY - garrisonY) +
                (ownerZ - garrisonZ) * (ownerZ - garrisonZ) > 100;
    }
    
    @Override
    public void start() {
        shouldTick = true;
    }
    
    @Override
    public void stop() {
        shouldTick = false;
    }
    
    @Override
    public void tick() {
        if (!shouldTick) {
            return;
        }
        
        World world = this.owner.getWorld();
        if (world == null) {
            shouldTick = false;
            return;
        }
        
        // Let the AI system handle navigation - we just need to ensure entity stays at garrison
        // Enter work/sitting pose if supported when on ground
        BlockPos garrisonPos = getGarrisonPosition(walker());
        if (garrisonPos != null && walker().isOnGround()) {
            enterGarrisonPose(walker());
        }
    }
    
    private PathAwareEntity walker() {
        return owner;
    }
    
    /**
     * Enter garrison (work) pose for this entity.
     */
    private void enterGarrisonPose(PathAwareEntity walker) {
        if (walker instanceof com.ben.csp.entity.ai.Garrisonable garrisonable) {
            garrisonable.setSitting(true);
        }
    }
    
    /**
     * Get garrison position from Garrisonable interface.
     */
    private static BlockPos getGarrisonPosition(PathAwareEntity walker) {
        if (walker instanceof com.ben.csp.entity.ai.Garrisonable garrisonable) {
            BlockPos pos = garrisonable.getGarrisonPos();
            return pos != null ? pos : null;
        }
        return null;
    }
}
