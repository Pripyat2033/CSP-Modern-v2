package com.ben.csp.skala;

import net.minecraft.util.math.BlockPos;

public record SendSkalaRequestC2SPacket(BlockPos pos, String code) {
}