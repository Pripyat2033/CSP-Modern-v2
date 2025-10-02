package com.ben.csp.entity;

import com.ben.csp.entity.ai.ManageSupplyChainGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * Science Grade: A "Nachalnik Snabzheniya" (Начальник снабжения) - Head of Supply.
 * This NPC is responsible for managing the overall logistics and supply chain for an Enterprise.
 */
public class NachalnikSnabzheniyaEntity extends PathAwareEntity implements EnterprisePersonnel {

    private String enterpriseName = "";

    public NachalnikSnabzheniyaEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createNachalnikSnabzheniyaAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ManageSupplyChainGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    @Override
    public String getEnterpriseName() { return this.enterpriseName; }

    @Override
    public void setEnterpriseName(String name) { this.enterpriseName = name; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("EnterpriseName", this.enterpriseName);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.enterpriseName = nbt.getString("EnterpriseName");
    }
}