package com.ben.csp.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import com.ben.csp.block.ModBlocks;

public class IcmsBlockEntity extends BlockEntity {
    public static final String ID = "icms_block_entity";
    
    public static final net.minecraft.block.entity.BlockEntityType<IcmsBlockEntity> ICMS_BLOCK_ENTITY_TYPE = 
        FabricBlockEntityTypeBuilder.create(
            new FabricBlockEntityTypeBuilder.Factory<IcmsBlockEntity>() {
                @Override
                public IcmsBlockEntity create(BlockPos pos, BlockState state) {
                    return new IcmsBlockEntity(pos, state);
                }
            }, ModBlocks.CHISELED_BLOCK).build();

    public IcmsBlockEntity(BlockPos pos, BlockState state) {
        super(ICMS_BLOCK_ENTITY_TYPE, pos, state);
    }
}
