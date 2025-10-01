package com.ben.csp.entity.ai;

import net.minecraft.util.math.BlockPos;

/**
 * A simple data record representing a Point of Interest defined in a blueprint.
 */
public record PoiDefinition(BlockPos pos, String type) {}