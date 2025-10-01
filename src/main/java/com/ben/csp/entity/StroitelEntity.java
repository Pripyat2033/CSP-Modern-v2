package com.ben.csp.entity;

import com.ben.csp.entity.ai.FollowMasterToBuildSiteGoal;
import com.ben.csp.entity.ai.PerformConstructionGoal;
import com.ben.csp.entity.ai.ReportToMasterGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Science Grade: A "Stroitel" (Строитель) - a builder or construction worker.
 * This is the lowest-level "hands-on" NPC that performs basic construction tasks under a Master's supervision.
 */
public class StroitelEntity extends PathAwareEntity {

    @Nullable
    private UUID masterUuid;

    public StroitelEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createStroitelAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FollowMasterToBuildSiteGoal(this));
        this.goalSelector.add(2, new PerformConstructionGoal(this));
        this.goalSelector.add(3, new ReportToMasterGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    @Nullable public UUID getMasterUuid() { return masterUuid; }
    public void setMasterUuid(@Nullable UUID uuid) { this.masterUuid = uuid; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (masterUuid != null) { nbt.putUuid("MasterUUID", masterUuid); }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("MasterUUID")) { this.masterUuid = nbt.getUuid("MasterUUID"); }
    }
}