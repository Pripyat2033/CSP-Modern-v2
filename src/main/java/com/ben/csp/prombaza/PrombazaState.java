package com.ben.csp.prombaza;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

/**
 * Server-side persistence for the zone's prombazas. Keeps the designated areas
 * across restarts and crashes by storing them in the overworld's persistent
 * state.
 */
public final class PrombazaState extends PersistentState {

    private static final String NAME = "csp_prombaza";
    private static final int VERSION = 1;

    private final PrombazaRegistry registry = new PrombazaRegistry();

    public PrombazaRegistry registry() {
        return registry;
    }

    public static PrombazaState get(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(PrombazaState::fromNbt, PrombazaState::new, NAME);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("version", VERSION);
        NbtList list = new NbtList();
        for (Prombaza p : registry.all()) {
            NbtCompound c = new NbtCompound();
            c.putString("id", p.id());
            c.putString("name", p.name());
            c.putInt("minX", p.area().minX());
            c.putInt("minY", p.area().minY());
            c.putInt("minZ", p.area().minZ());
            c.putInt("maxX", p.area().maxX());
            c.putInt("maxY", p.area().maxY());
            c.putInt("maxZ", p.area().maxZ());
            c.putLong("day", p.createdDay());
            c.putString("by", p.createdBy());
            list.add(c);
        }
        nbt.put("prombazas", list);
        return nbt;
    }

    public static PrombazaState fromNbt(NbtCompound nbt) {
        PrombazaState state = new PrombazaState();
        NbtList list = nbt.getList("prombazas", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            Area area = new Area(
                c.getInt("minX"), c.getInt("minY"), c.getInt("minZ"),
                c.getInt("maxX"), c.getInt("maxY"), c.getInt("maxZ"));
            state.registry.add(new Prombaza(
                c.getString("id"), c.getString("name"), area,
                c.getLong("day"), c.getString("by")));
        }
        return state;
    }
}
