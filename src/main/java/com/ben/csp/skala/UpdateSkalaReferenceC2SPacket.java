package com.ben.csp.skala;

import net.minecraft.util.math.BlockPos;

public record UpdateSkalaReferenceC2SPacket(BlockPos pos, String targetCode, String value) {
}