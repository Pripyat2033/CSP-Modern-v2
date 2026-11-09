package com.ben.csp.block.entity;

import com.ben.csp.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public class PlanshetBlockEntity extends BlockEntity {

    public static final BlockEntityType<PlanshetBlockEntity> PLANSHET_BLOCK_ENTITY_TYPE =
        FabricBlockEntityTypeBuilder.create(PlanshetBlockEntity::new, ModBlocks.PLANSHET_BLOCK).build();

    public PlanshetBlockEntity(BlockPos pos, BlockState state) {
        super(PLANSHET_BLOCK_ENTITY_TYPE, pos, state);
    }
}
