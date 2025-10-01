package com.ben.csp.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Science Grade: A simple, non-collidable marker block placed by a Geodezist to lay out a construction site.
 */
public class GeodeticMarkerBlock extends Block {
    public GeodeticMarkerBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0.4, 0.0, 0.4, 0.6, 1.0, 0.6); // A simple post shape
    }
}