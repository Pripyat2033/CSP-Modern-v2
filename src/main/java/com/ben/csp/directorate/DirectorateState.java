package com.ben.csp.directorate;

import com.ben.csp.enterprise.FacilityType;
import com.ben.csp.prombaza.Area;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

/**
 * Server-side persistence for the director's in-tray. The requests the
 * enterprises raise, and the Director's decisions on them, are kept across
 * restarts.
 */
public final class DirectorateState extends PersistentState {

    private static final String NAME = "csp_directorate";
    private static final int VERSION = 1;

    private final DirectorateWorkflow workflow = new DirectorateWorkflow();

    public DirectorateWorkflow workflow() {
        return workflow;
    }

    public static DirectorateState get(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(DirectorateState::fromNbt, DirectorateState::new, NAME);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("version", VERSION);
        NbtList list = new NbtList();
        for (DirectorateRequest r : workflow.all()) {
            NbtCompound c = new NbtCompound();
            c.putString("id", r.id());
            c.putString("type", r.type().name());
            c.putString("subject", r.subject());
            c.putString("enterprise", r.enterpriseId() == null ? "" : r.enterpriseId());
            c.putString("facilityType", r.facilityType() == null ? "" : r.facilityType().name());
            if (r.area() != null) {
                Area a = r.area();
                c.putInt("minX", a.minX()); c.putInt("minY", a.minY()); c.putInt("minZ", a.minZ());
                c.putInt("maxX", a.maxX()); c.putInt("maxY", a.maxY()); c.putInt("maxZ", a.maxZ());
            }
            c.putString("requestedBy", r.requestedBy());
            c.putLong("requestedDay", r.requestedDay());
            c.putString("status", r.status().name());
            c.putString("decidedBy", r.decidedBy() == null ? "" : r.decidedBy());
            c.putLong("decidedDay", r.decidedDay());
            c.putString("reason", r.decisionReason() == null ? "" : r.decisionReason());
            list.add(c);
        }
        nbt.put("requests", list);
        return nbt;
    }

    public static DirectorateState fromNbt(NbtCompound nbt) {
        DirectorateState state = new DirectorateState();
        NbtList list = nbt.getList("requests", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            String ft = c.getString("facilityType");
            Area area = c.contains("minX")
                ? Area.of(c.getInt("minX"), c.getInt("minY"), c.getInt("minZ"),
                          c.getInt("maxX"), c.getInt("maxY"), c.getInt("maxZ"))
                : null;
            String ent = c.getString("enterprise");
            DirectorateRequest r = new DirectorateRequest(
                c.getString("id"),
                RequestType.valueOf(c.getString("type")),
                c.getString("subject"),
                ent.isEmpty() ? null : ent,
                ft.isEmpty() ? null : FacilityType.valueOf(ft),
                area,
                c.getString("requestedBy"),
                c.getLong("requestedDay"));
            r.restore(RequestStatus.valueOf(c.getString("status")),
                c.getString("decidedBy"), c.getLong("decidedDay"), c.getString("reason"));
            state.workflow.submit(r);
        }
        return state;
    }
}
