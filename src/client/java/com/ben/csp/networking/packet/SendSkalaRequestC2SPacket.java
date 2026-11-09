package com.ben.csp.networking.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public record SendSkalaRequestC2SPacket(BlockPos pos, String code) {
    public SendSkalaRequestC2SPacket(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readString());
    }

    public void toBuf(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(code);
    }
}