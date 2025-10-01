package com.ben.csp.entity;

import com.ben.csp.build.Enterprise;
import com.ben.csp.entity.ai.Dosye;
import com.ben.csp.entity.ai.DynamicPersonalityEngine;
import com.ben.csp.entity.ai.GarrisonGoal;
import com.ben.csp.entity.ai.Garrisonable;
import com.ben.csp.entity.ai.WorkAtAssignedPostGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class KommutatorOperatorEntity extends PathAwareEntity implements Garrisonable, EnterprisePersonnel {

    private String enterpriseName = "";
    private Dosye dosye;
    private BlockPos workplacePos = BlockPos.ORIGIN;

    public KommutatorOperatorEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        if (!world.isClient()) {
            this.dosye = DynamicPersonalityEngine.getInstance().generateDosye();
            this.setCustomName(Text.literal(this.dosye.getFullName()));
            this.setCustomNameVisible(true);
        }
    }

    public static DefaultAttributeContainer.Builder createKommutatorOperatorAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new WorkAtAssignedPostGoal(this, 0.8)); // Work at their assigned switchboard position
        this.goalSelector.add(2, new GarrisonGoal(this, 1.0D));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    // --- Workplace Management ---

    public BlockPos getWorkplacePos() {
        return workplacePos;
    }

    public void setWorkplacePos(BlockPos workplacePos) {
        this.workplacePos = workplacePos;
    }

    // --- EnterprisePersonnel Implementation ---

    @Override
    public String getEnterpriseName() { return this.enterpriseName; }
    @Override
    public void setEnterpriseName(String name) { this.enterpriseName = name; }

    // --- Garrisonable Implementation ---

    @Override
    public BlockPos getGarrisonPos() {
        Enterprise enterprise = getEnterprise();
        return enterprise != null ? enterprise.getHqPos() : null;
    }

    @Override
    public int getGarrisonRadius() { return 15; }

    // --- NBT Serialization ---

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("EnterpriseName", this.enterpriseName);
        nbt.putLong("WorkplacePos", this.workplacePos.asLong());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.enterpriseName = nbt.getString("EnterpriseName");
        if (nbt.contains("WorkplacePos")) {
            this.workplacePos = BlockPos.fromLong(nbt.getLong("WorkplacePos"));
        }
    }
}