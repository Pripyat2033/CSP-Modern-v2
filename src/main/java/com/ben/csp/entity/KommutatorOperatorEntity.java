package com.ben.csp.entity;

import com.ben.csp.entity.ai.Dosye;
import com.ben.csp.entity.ai.DynamicPersonalityEngine;
import com.ben.csp.agent.goals.GarrisonGoal;
import com.ben.csp.entity.ai.Garrisonable;
import com.ben.csp.entity.goals.WorkAtAssignedPostGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * KommutatorOperatorEntity - Switchboard operator entity assigned to enterprise organization.
 * Implements Garrisonable (garrisoned at HQ) and EnterprisePersonnel (assigned to enterprise).
 */
public class KommutatorOperatorEntity extends PathAwareEntity implements Garrisonable, EnterprisePersonnel {
    
    private String enterpriseName = "";
    private Dosye dosye;
    private BlockPos workplacePos = BlockPos.ORIGIN;
    
    // Garrisonable fields
    private BlockPos garrisonPos;
    private boolean sitting;
    
    /**
     * Default constructor for server-side entity creation.
     */
    public KommutatorOperatorEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        if (!world.isClient()) {
            this.dosye = DynamicPersonalityEngine.getInstance().generateDosye();
            this.setCustomName(Text.literal(this.dosye.getFullName()));
            this.setCustomNameVisible(true);
        }
    }
    
    /**
     * Create with pre-initialized Dosye (for NBT loading or special creation).
     */
    public KommutatorOperatorEntity(EntityType<? extends PathAwareEntity> entityType, World world, Dosye dosye) {
        super(entityType, world);
        this.dosye = dosye;
        if (!world.isClient()) {
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
        this.goalSelector.add(1, new WorkAtAssignedPostGoal(this)); // Work at assigned switchboard position
        this.goalSelector.add(2, new GarrisonGoal(this, 1.0f));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8f));
        this.goalSelector.add(4, new LookAtEntityGoal(this, net.minecraft.entity.player.PlayerEntity.class, 8.0f));
    }
    
    // ===========================================
    // Workplace Management
    // ===========================================
    
    public BlockPos getWorkplacePos() {
        return workplacePos;
    }
    
    public void setWorkplacePos(BlockPos workplacePos) {
        this.workplacePos = workplacePos;
    }
    
    public boolean canSave() {
        // Can save if has workplace position (needed for NBT persistence)
        return workplacePos != null && !workplacePos.equals(BlockPos.ORIGIN);
    }
    
    // ===========================================
    // EnterprisePersonnel Implementation
    // ===========================================
    
    @Override
    public String getEnterpriseName() {
        return this.enterpriseName;
    }
    
    @Override
    public void setEnterpriseName(String name) {
        this.enterpriseName = name != null ? name : "";
    }
    
    @Override
    public Enterprise getEnterprise() {
        // Return null if not assigned to an enterprise
        return null;
    }
    
    @Override
    public void setEnterprise(Enterprise enterprise) {
        if (enterprise == null) {
            this.enterpriseName = "";
        } else {
            this.enterpriseName = enterprise.getEnterpriseName();
        }
    }
    
    @Override
    public boolean hasEnterprise() {
        return !enterpriseName.isEmpty();
    }
    
    @Override
    public String getPersonnelRole() {
        // Default role for switchboard operator
        return "Kommutator";
    }
    
    @Override
    public void setPersonnelRole(String role) {
        // Store personnel role (can be used for rendering/name tags later)
        // Implementation can be added when role system is needed
    }
    
    // ===========================================
    // Garrisonable Implementation
    // ===========================================
    
    @Override
    public void setGarrisonPos(BlockPos pos) {
        this.garrisonPos = pos;
    }
    
    @Override
    public BlockPos getGarrisonPos() {
        if (garrisonPos == null) {
            // Fall back to workplace position if no explicit garrison pos
            return workplacePos;
        }
        return garrisonPos;
    }
    
    @Override
    public void setSitting(boolean sitting) {
        this.sitting = sitting;
    }
    
    @Override
    public boolean isSitting() {
        return this.sitting;
    }
    
    // ===========================================
    // NBT Serialization
    // ===========================================
    
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (workplacePos != null && !workplacePos.equals(BlockPos.ORIGIN)) {
            nbt.putString("EnterpriseName", this.enterpriseName);
            nbt.putLong("WorkplacePos", this.workplacePos.asLong());
        }
    }
    
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("EnterpriseName")) {
            this.enterpriseName = nbt.getString("EnterpriseName");
        }
        if (nbt.contains("WorkplacePos")) {
            this.workplacePos = BlockPos.fromLong(nbt.getLong("WorkplacePos"));
        }
    }
    
    // ===========================================
    // Client-Side Custom Name Handling
    // ===========================================
    
    /**
     * Called to update custom name from Dosye.
     */
    public void updateCustomName() {
        if (dosye != null) {
            this.setCustomName(Text.literal(this.dosye.getFullName()));
            this.setCustomNameVisible(true);
        }
    }
}
