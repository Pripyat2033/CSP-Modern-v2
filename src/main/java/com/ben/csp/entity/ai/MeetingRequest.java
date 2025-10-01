package com.ben.csp.entity.ai;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public record MeetingRequest(UUID requesterUuid, String topic) {
    // This record holds the essential information for a pending meeting request.

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("RequesterUuid", requesterUuid);
        nbt.putString("Topic", topic);
        return nbt;
    }

    public static MeetingRequest fromNbt(NbtCompound nbt) {
        UUID uuid = nbt.getUuid("RequesterUuid");
        String topic = nbt.getString("Topic");
        return new MeetingRequest(uuid, topic);
    }
}