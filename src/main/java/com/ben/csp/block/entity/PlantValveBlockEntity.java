package com.ben.csp.block.entity;

import com.ben.csp.block.ModBlocks;
import com.ben.csp.rbmk.plant.PlantInstruments;
import com.ben.csp.rbmk.plant.PlantSystem;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * A flow-control valve on the МЦК pipework. Bound to a fuel channel, it reads
 * that channel's valve position from the running plant, so a closed or throttled
 * valve shows on the block and the client can draw it.
 */
public class PlantValveBlockEntity extends PlantInstrumentBlockEntity {

    public static final BlockEntityType<PlantValveBlockEntity> TYPE =
        FabricBlockEntityTypeBuilder.create(PlantValveBlockEntity::new,
            ModBlocks.PLANT_VALVE_BLOCK).build();

    public PlantValveBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    protected void read() {
        PlantSystem p = plant();
        if (p == null) {
            live = false;
            reading = 0;
            open = false;
            return;
        }
        live = true;
        reading = PlantInstruments.valvePosition(p, channelZ);
        open = PlantInstruments.valveOpen(p, channelZ);
    }

    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state,
                            PlantValveBlockEntity be) {
        be.tickInstrument();
    }
}
