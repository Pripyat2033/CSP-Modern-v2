package com.ben.csp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value = EnvType.CLIENT)
public class ModMessages {
    // Network message handlers for CSP mod
    
    private static final String C2S_PACKET_ID = "c2s_message";
    
    public static String getC2SPacketId() {
        return C2S_PACKET_ID;
    }
}
