package com.ben.csp.entity;

import com.ben.csp.entity.ai.FileDocumentsGoal;
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

/**
 * Science Grade: An "Uchetchik" (Учётчик) - an accountant, clerk, or record-keeper.
 * This NPC is responsible for filing documents and maintaining records.
 */
public class UchetchikEntity extends PathAwareEntity implements EnterprisePersonnel {

    private String enterpriseName = "";
    @Nullable
    private NbtCompound carriedDocument;

    public UchetchikEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createUchetchikAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new FileDocumentsGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    @Override
    public String getEnterpriseName() { return this.enterpriseName; }

    @Override
    public void setEnterpriseName(String name) { this.enterpriseName = name; }

    public boolean isCarryingDocuments() {
        return this.carriedDocument != null;
    }

    @Nullable public NbtCompound getCarriedDocument() { return carriedDocument; }
    public void setCarriedDocument(@Nullable NbtCompound document) { this.carriedDocument = document; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("EnterpriseName", this.enterpriseName);
        if (carriedDocument != null) {
            nbt.put("CarriedDocument", carriedDocument);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.enterpriseName = nbt.getString("EnterpriseName");
        if (nbt.contains("CarriedDocument")) {
            this.carriedDocument = nbt.getCompound("CarriedDocument");
        }
    }
}