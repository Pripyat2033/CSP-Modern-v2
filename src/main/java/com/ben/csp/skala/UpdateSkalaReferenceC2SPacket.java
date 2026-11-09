package com.ben.csp.skala;

// ============================================================================
// SUPERSEDED by com.ben.csp.networking.packet.UpdateSkalaReferenceC2SPacket
//   This duplicate is not referenced anywhere. It is kept only so that
//   existing work is not destroyed. DO NOT USE IT IN NEW CODE.
//   Ask Ben before deleting.
// ============================================================================

import net.minecraft.util.math.BlockPos;

@Deprecated
public record UpdateSkalaReferenceC2SPacket(BlockPos pos, String targetCode, String value) {
}