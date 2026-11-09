package com.ben.csp.networking.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public record UpdateSkalaReferenceC2SPacket(BlockPos pos, String targetCode, String value) {
    public UpdateSkalaReferenceC2SPacket(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readString(), buf.readString());
    }

    public void toBuf(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(targetCode);
        buf.writeString(value);
    }
}