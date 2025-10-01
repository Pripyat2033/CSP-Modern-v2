package com.ben.csp.geology;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockBox;

import java.util.Map;

public record GeologicalReport(
    BlockBox surveyedArea,
    int averageWaterTableY,
    Map<Block, Double> composition // Percentage of each block type found
) {
}