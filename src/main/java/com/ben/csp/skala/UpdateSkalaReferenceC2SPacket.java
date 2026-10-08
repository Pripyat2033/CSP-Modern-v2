package com.ben.csp.networking.packet;

import net.minecraft.util.math.BlockPos;

public record UpdateSkalaReferenceC2SPacket(BlockPos pos, String targetCode, String value) {
}