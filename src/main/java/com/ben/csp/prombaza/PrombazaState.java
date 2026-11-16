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

    /** One входной контроль desk per prombaza, created on first use. */
    private final java.util.Map<String, PrombazaControl> controlDesks = new java.util.HashMap<>();

    /** One rail siding (goods yard) per prombaza, created on first use. */
    private final java.util.Map<String, GoodsYard> yards = new java.util.HashMap<>();

    public PrombazaRegistry registry() {
        return registry;
    }

    /** The входной контроль desk for a prombaza, created on first use. */
    public PrombazaControl control(String prombazaId) {
        return controlDesks.computeIfAbsent(prombazaId, PrombazaControl::new);
    }

    /** The rail siding for a prombaza, created on first use. */
    public GoodsYard yard(String prombazaId) {
        return yards.computeIfAbsent(prombazaId, GoodsYard::new);
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

        // The sidings and the входной контроль desks: the standing wagons and the
        // checking already done must survive a restart, or the yard is emptied and
        // the man-hours are forgotten every time the world reloads.
        NbtList yardsNbt = new NbtList();
        for (GoodsYard y : yards.values()) {
            yardsNbt.add(y.toNbt());
        }
        nbt.put("yards", yardsNbt);

        NbtList desksNbt = new NbtList();
        for (PrombazaControl d : controlDesks.values()) {
            desksNbt.add(d.toNbt());
        }
        nbt.put("desks", desksNbt);
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
        NbtList yardsNbt = nbt.getList("yards", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < yardsNbt.size(); i++) {
            GoodsYard y = GoodsYard.fromNbt(yardsNbt.getCompound(i));
            state.yards.put(y.prombazaId(), y);
        }
        NbtList desksNbt = nbt.getList("desks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < desksNbt.size(); i++) {
            PrombazaControl d = PrombazaControl.fromNbt(desksNbt.getCompound(i));
            state.controlDesks.put(d.prombazaId(), d);
        }
        return state;
    }
}
