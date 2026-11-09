package com.ben.csp.zone;

import com.ben.csp.enterprise.Enterprise;
import com.ben.csp.enterprise.EnterpriseRoster;
import com.ben.csp.enterprise.Facility;
import com.ben.csp.prombaza.Area;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.List;

/**
 * Server-side persistence for the zone: the prombaza territory and the
 * enterprises standing on it. It keeps the whole build across restarts and
 * crashes, so the site carries on where it left off rather than starting again.
 *
 * <p>The roster itself is not saved — it is rebuilt from code, which is the
 * single source of truth for who the enterprises are. What is saved is the state
 * the enterprises have reached: their HQ, their staff, and how far each facility
 * has been built.
 */
public final class ZoneState extends PersistentState {

    private static final String NAME = "csp_zone";
    private static final int VERSION = 1;

    private long day;
    private int tickAccumulator;

    private PrombazaTerritory territory;
    private ZoneEnterprises zone;

    public long day() { return day; }
    public int tickAccumulator() { return tickAccumulator; }
    public PrombazaTerritory territory() { return territory; }
    public ZoneEnterprises zone() { return zone; }

    public void setTickAccumulator(int value) { this.tickAccumulator = value; }

    /** Lay out the territory and put the whole roster onto it. */
    public void spawn(Area area) {
        this.territory = PrombazaTerritory.standard("pb1", area);
        List<Enterprise> roster = EnterpriseRoster.provisional();
        this.zone = ZoneEnterprises.spawn(territory, roster, day);
    }

    public boolean isSpawned() { return zone != null; }

    /** Advance the zone one work-day. */
    public void advanceDay() {
        if (zone == null) {
            return;
        }
        zone.tick(day);
        day++;
    }

    public static ZoneState get(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(ZoneState::fromNbt, ZoneState::new, NAME);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("version", VERSION);
        nbt.putLong("day", day);
        nbt.putInt("acc", tickAccumulator);
        if (territory != null) {
            NbtCompound t = new NbtCompound();
            t.putString("id", territory.id());
            t.putDouble("minX", territory.boundary().minX());
            t.putDouble("minZ", territory.boundary().minZ());
            t.putDouble("maxX", territory.boundary().maxX());
            t.putDouble("maxZ", territory.boundary().maxZ());
            nbt.put("territory", t);
        }
        if (zone != null) {
            NbtList lifecycles = new NbtList();
            for (EnterpriseLifecycle l : zone.lifecycles()) {
                lifecycles.add(writeLifecycle(l));
            }
            nbt.put("lifecycles", lifecycles);
        }
        return nbt;
    }

    private NbtCompound writeLifecycle(EnterpriseLifecycle l) {
        NbtCompound c = new NbtCompound();
        c.putString("id", l.enterprise().id());
        c.putBoolean("hqComplete", l.setup().hasHq());
        c.putInt("hqProgress", l.setup().hqProgress());
        c.putInt("staffHired", l.setup().staffHired());
        c.putInt("design", l.setup().designContractsCompleted());
        c.putInt("level", l.setup().level());

        NbtList facilities = new NbtList();
        for (Facility f : l.enterprise().facilities()) {
            NbtCompound fc = new NbtCompound();
            fc.putBoolean("sited", f.hasFootprint());
            if (f.hasFootprint()) {
                fc.putDouble("minX", f.footprint().minX());
                fc.putDouble("minZ", f.footprint().minZ());
                fc.putDouble("maxX", f.footprint().maxX());
                fc.putDouble("maxZ", f.footprint().maxZ());
            }
            fc.putBoolean("built", f.isBuilt());
            fc.putBoolean("accepted", f.isAsBuiltAccepted());
            fc.putBoolean("refused", f.isAsBuiltRefused());
            fc.putString("director", f.facilityDirectorId() == null ? "" : f.facilityDirectorId());
            fc.putInt("staff", f.staff());
            facilities.add(fc);
        }
        c.put("facilities", facilities);

        NbtList sites = new NbtList();
        for (ConstructionSite s : l.sites()) {
            NbtCompound sc = new NbtCompound();
            sc.putDouble("manHoursDone", s.manHoursDone());
            sc.putDouble("materialsOnSite", s.materialsOnSite());
            sc.putString("brigade", s.brigadeId() == null ? "" : s.brigadeId());
            sites.add(sc);
        }
        c.put("sites", sites);
        return c;
    }

    public static ZoneState fromNbt(NbtCompound nbt) {
        ZoneState state = new ZoneState();
        state.day = nbt.getLong("day");
        state.tickAccumulator = nbt.getInt("acc");

        if (!nbt.contains("territory")) {
            return state; // never spawned
        }
        NbtCompound t = nbt.getCompound("territory");
        Area area = Area.of(
            (int) Math.floor(t.getDouble("minX")), 60, (int) Math.floor(t.getDouble("minZ")),
            (int) Math.floor(t.getDouble("maxX")), 80, (int) Math.floor(t.getDouble("maxZ")));
        state.territory = PrombazaTerritory.standard(t.getString("id"), area);

        List<Enterprise> roster = EnterpriseRoster.provisional();
        state.zone = ZoneEnterprises.spawn(state.territory, roster, state.day);

        NbtList lifecycles = nbt.getList("lifecycles", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < lifecycles.size(); i++) {
            restoreLifecycle(state.zone, lifecycles.getCompound(i));
        }
        return state;
    }

    private static void restoreLifecycle(ZoneEnterprises zone, NbtCompound c) {
        String id = c.getString("id");
        EnterpriseLifecycle l = null;
        for (EnterpriseLifecycle candidate : zone.lifecycles()) {
            if (candidate.enterprise().id().equals(id)) {
                l = candidate;
                break;
            }
        }
        if (l == null) {
            return;
        }
        l.setup().restore(c.getBoolean("hqComplete"), c.getInt("hqProgress"),
            c.getInt("staffHired"), c.getInt("design"), c.getInt("level"));

        NbtList facilities = c.getList("facilities", NbtElement.COMPOUND_TYPE);
        List<Facility> fs = l.enterprise().facilities();
        for (int i = 0; i < facilities.size() && i < fs.size(); i++) {
            NbtCompound fc = facilities.getCompound(i);
            Facility f = fs.get(i);
            if (fc.getBoolean("sited")) {
                l.claimPlot(f, com.ben.csp.enterprise.Footprint.of(
                    fc.getDouble("minX"), fc.getDouble("minZ"),
                    fc.getDouble("maxX"), fc.getDouble("maxZ")));
            }
            if (fc.getBoolean("built")) {
                f.completeConstruction();
            }
            if (fc.getBoolean("accepted")) {
                f.acceptAsBuilt();
            } else if (fc.getBoolean("refused")) {
                f.refuseAsBuilt();
            }
            String director = fc.getString("director");
            if (!director.isEmpty()) {
                f.assignFacilityDirector(director);
            }
            f.setStaff(fc.getInt("staff"));
        }

        NbtList sites = c.getList("sites", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < sites.size(); i++) {
            NbtCompound sc = sites.getCompound(i);
            String brigade = sc.getString("brigade");
            l.restoreSite(i, sc.getDouble("manHoursDone"), sc.getDouble("materialsOnSite"),
                brigade.isEmpty() ? null : brigade);
        }
        l.recomputePhase();
    }
}
