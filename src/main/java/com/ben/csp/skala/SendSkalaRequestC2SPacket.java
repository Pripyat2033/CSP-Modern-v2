package com.ben.csp.skala;

// ============================================================================
// SUPERSEDED by com.ben.csp.networking.packet.SendSkalaRequestC2SPacket
//   This duplicate is not referenced anywhere. It is kept only so that
//   existing work is not destroyed. DO NOT USE IT IN NEW CODE.
//   Ask Ben before deleting.
// ============================================================================

import net.minecraft.util.math.BlockPos;

@Deprecated
public record SendSkalaRequestC2SPacket(BlockPos pos, String code) {
}