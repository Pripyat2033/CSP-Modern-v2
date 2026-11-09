package com.ben.csp.block.entity;

import com.ben.csp.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public class ArchiveFilingCabinetBlockEntity extends BlockEntity {

    public static final BlockEntityType<ArchiveFilingCabinetBlockEntity> ARCHIVE_FILING_CABINET_BLOCK_ENTITY_TYPE =
        FabricBlockEntityTypeBuilder.create(ArchiveFilingCabinetBlockEntity::new, ModBlocks.ARCHIVE_FILING_CABINET_BLOCK).build();

    public ArchiveFilingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ARCHIVE_FILING_CABINET_BLOCK_ENTITY_TYPE, pos, state);
    }
}
