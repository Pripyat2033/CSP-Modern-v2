package com.ben.csp.block.entity;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.rbmk.plant.PlantInstruments;
import com.ben.csp.rbmk.plant.PlantSystem;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * A ball flow meter on the МЦК pipework. Bound to a fuel channel, it reads that
 * channel's live flow from the running plant's own rotameter — the same number
 * the operator would have read — so a starving channel shows on the block.
 */
public class PlantMeterBlockEntity extends PlantInstrumentBlockEntity {

    public static final BlockEntityType<PlantMeterBlockEntity> TYPE =
        FabricBlockEntityTypeBuilder.create(PlantMeterBlockEntity::new,
            ModBlocks.PLANT_METER_BLOCK).build();

    public PlantMeterBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    protected void read() {
        PlantSystem p = plant();
        if (p == null) {
            live = false;
            reading = 0;
            flowKgS = 0;
            starving = false;
            return;
        }
        int idx = PlantInstruments.channelIndexOf(channelX, channelZ);
        if (idx < 0) {
            live = false;
            reading = 0;
            flowKgS = 0;
            starving = false;
            return;
        }
        live = true;
        flowKgS = PlantInstruments.channelFlowKgS(p, channelX, channelZ);
        reading = PlantInstruments.channelReading(p, channelX, channelZ);
        starving = PlantInstruments.channelStarving(p, channelX, channelZ);
    }

    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state,
                            PlantMeterBlockEntity be) {
        be.tickInstrument();
    }
}
