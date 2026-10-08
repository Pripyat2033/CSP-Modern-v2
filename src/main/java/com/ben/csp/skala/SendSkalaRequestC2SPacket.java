package com.ben.csp.networking.packet;

import net.minecraft.util.math.BlockPos;

public record SendSkalaRequestC2SPacket(BlockPos pos, String code) {
}