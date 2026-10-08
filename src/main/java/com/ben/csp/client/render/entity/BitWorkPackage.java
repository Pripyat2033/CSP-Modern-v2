package com.ben.csp.client.render.entity;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * BitWorkPackage - Work package for chiseling bit blocks.
 */
public record BitWorkPackage(BlockPos chiseledBlockPos, int materialId) {

    public static BitWorkPackage fromNbt(NbtCompound nbt) {
        return new BitWorkPackage(
            BlockPos.fromLong(nbt.getLong("chiseledBlockPos")),
            nbt.getInt("materialId")
        );
    }

    @Override
    public String toString() {
        return "BitWorkPackage{" +
            "chiseledBlockPos=" + chiseledBlockPos +
            ", materialId=" + materialId +
            '}';
    }
}
