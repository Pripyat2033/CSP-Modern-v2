package com.ben.csp.skala;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public class SkalaCoreBlockEntity extends BlockEntity {

    public static final BlockEntityType<SkalaCoreBlockEntity> SKALA_CORE_BLOCK_ENTITY_TYPE =
        FabricBlockEntityTypeBuilder.create(SkalaCoreBlockEntity::new, com.ben.csp.block.ModBlocks.SKALA_CORE_BLOCK).build();

    public SkalaCoreBlockEntity(BlockPos pos, BlockState state) {
        super(SKALA_CORE_BLOCK_ENTITY_TYPE, pos, state);
    }

    // GUI state used by SkalaRequestDeviceScreen.
    private String requestCode = "";
    private String valueDisplay = "";
    private String referenceDisplay = "";

    public String getRequestCode() { return requestCode; }
    public String getValueDisplay() { return valueDisplay; }
    public String getReferenceDisplay() { return referenceDisplay; }
    public void setRequestCode(String v) { this.requestCode = v; }
    public void setValueDisplay(String v) { this.valueDisplay = v; }
    public void setReferenceDisplay(String v) { this.referenceDisplay = v; }
}
