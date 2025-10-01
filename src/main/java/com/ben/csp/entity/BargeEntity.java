package com.ben.csp.entity;

import org.jetbrains.annotations.Nullable;

import com.ben.csp.entity.ai.DeliverToPortGoal;

import net.minecraft.block.BlockPos;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.world.World;

public class BargeEntity extends PathAwareEntity {

    @Nullable
    private BlockPos destination;
    private ItemStack cargo = ItemStack.EMPTY;

    public BargeEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createBargeAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void initGoals() {
        // The primary goal for the barge: move to its designated port.
        this.goalSelector.add(1, new DeliverToPortGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.destination != null) {
            nbt.put("Destination", NbtHelper.fromBlockPos(this.destination));
        }
        if (!this.cargo.isEmpty()) {
            nbt.put("Cargo", this.cargo.writeNbt(new NbtCompound()));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Destination")) {
            this.destination = NbtHelper.toBlockPos(nbt.getCompound("Destination"));
        }
        if (nbt.contains("Cargo")) {
            this.cargo = ItemStack.fromNbt(nbt.getCompound("Cargo"));
        }
    }

    @Nullable
    public BlockPos getDestination() {
        return this.destination;
    }

    public void setDestination(@Nullable BlockPos destination) {
        this.destination = destination;
    }

    public ItemStack getCargo() {
        return this.cargo;
    }

    public void loadCargo(ItemStack cargo) {
        this.cargo = cargo;
    }
}