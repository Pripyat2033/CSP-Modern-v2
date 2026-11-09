package com.ben.csp.networking;

import com.ben.csp.networking.packet.SendSkalaRequestC2SPacket;
import com.ben.csp.networking.packet.UpdateSkalaReferenceC2SPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Client-side networking entry point.
 */
public class ModMessages {

    public static final Identifier SKALA_REQUEST_ID = new Identifier("csp-modern", "skala_request");
    public static final Identifier SKALA_UPDATE_ID = new Identifier("csp-modern", "skala_update");

    /** Register handlers for packets sent from the server. */
    public static void registerS2CPackets() {
        // No server-to-client packets are defined yet. Hook points are declared
        // here so the client initialiser has a single, real call to make.
    }

    public static void sendToServer(SendSkalaRequestC2SPacket packet) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        packet.toBuf(buf);
        ClientPlayNetworking.send(SKALA_REQUEST_ID, buf);
    }

    public static void sendToServer(UpdateSkalaReferenceC2SPacket packet) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        packet.toBuf(buf);
        ClientPlayNetworking.send(SKALA_UPDATE_ID, buf);
    }
}
