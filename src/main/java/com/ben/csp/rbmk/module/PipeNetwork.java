package com.ben.csp.rbmk.module;

import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RbmkLattice;
import com.ben.csp.rbmk.plant.Compartment;
import com.ben.csp.rbmk.plant.EnergoblokLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The multiple forced circulation pipework (МЦК) as a swappable module — the
 * pipework, valves and flow meters that run through the energoblok, planned as
 * pure data so it can be built and removed on its own.
 *
 * <p>The topology is the documented one (docs/RBMK-PLANT-SYSTEMS.md §2):
 * <pre>
 *   lower water line (per column, through the pump rooms)
 *     -> group distribution header (GDH, one per column)
 *       -> per-channel lower connection: flow-control valve + ball flow meter
 *         -> the pressure channel (ТК) through the core
 *           -> per-channel upper connection (valve + meter)
 *             -> steam collector -> separator room (steam drum)
 * </pre>
 *
 * <p>Two parameters set the detail level so the module can be re-scaled without
 * touching anything else: {@code columns} (the number of group distribution
 * headers; the real RBMK has 22 per circuit) and {@code step} (how many channels
 * share one valve+meter pair; 1 = one pair per channel, the real arrangement).
 *
 * <p>The module is deliberately kept clear of the core, the compartment walls and
 * the machines — see {@link ModuleRegistry#audit} — so swapping it in or out
 * cannot disturb anything else. The one documented exception is the lower
 * biological shield, which the pressure channels pass through: that section is
 * excluded from the keep-out for exactly that reason.
 */
public final class PipeNetwork implements PlantModule {

    public static final String ID = "mfc_pipework";

    // ---- origin-relative levels (the reactor centre at the reactor-hall floor) ----
    /** Lower water lines, below the shields. */
    static final int WATER_Y = -12;
    /** The group distribution headers, just above the water lines. */
    static final int GDH_Y = -11;
    /** The lower per-channel connections rise to just under the core. */
    static final int LOWER_TOP = -1;
    /** Top of the graphite stack — where the upper connections begin. */
    static final int CORE_TOP = RbmkGeometry.STACK_HEIGHT_BLOCKS; // 32
    /** Upper flow-control valve and meter, in the shield penetration above the stack. */
    static final int UPPER_VALVE_Y = CORE_TOP + 1;
    static final int UPPER_METER_Y = CORE_TOP + 2;
    /** Steam collector, above the reactor cavity. */
    static final int STEAM_Y = 48;

    private final int columns;
    private final int step;
    private final List<PlantCell> cells;

    /** The real arrangement: 22 headers per circuit, a valve+meter on every channel. */
    public PipeNetwork() {
        this(22, 1);
    }

    public PipeNetwork(int columns, int step) {
        this.columns = Math.max(1, columns);
        this.step = Math.max(1, step);
        this.cells = List.copyOf(build());
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String label() {
        if (step == 1) {
            return String.format("МЦК pipework (%d headers, 1 valve+meter per channel)",
                columns);
        }
        return String.format("МЦК pipework (%d headers, 1 valve+meter per %d channels)",
            columns, step);
    }

    @Override
    public List<PlantCell> cells() {
        return cells;
    }

    public int columns() {
        return columns;
    }

    public int step() {
        return step;
    }

    // ------------------------------------------------------------------ build

    private List<PlantCell> build() {
        Map<Long, PlantCell.Role> acc = new LinkedHashMap<>();

        List<RbmkLattice.Channel> chs = new ArrayList<>(RbmkLattice.channels());
        chs.sort(Comparator.comparingInt(RbmkLattice.Channel::x)
            .thenComparingInt(RbmkLattice.Channel::z));

        int per = (chs.size() + columns - 1) / columns;
        int paxZ = pumpRoomCentreZ();

        for (int g = 0; g < columns; g++) {
            int a = g * per;
            int b = Math.min(chs.size(), a + per);
            if (a >= b) {
                break;
            }
            List<RbmkLattice.Channel> grp = chs.subList(a, b);

            // the header sits on the group's channel nearest the core centre
            RbmkLattice.Channel rep = grp.get(0);
            for (RbmkLattice.Channel c : grp) {
                if (Math.abs(c.x()) < Math.abs(rep.x())) {
                    rep = c;
                }
            }
            int gx = rep.x();
            int gz = rep.z();
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (RbmkLattice.Channel c : grp) {
                minZ = Math.min(minZ, c.z());
                maxZ = Math.max(maxZ, c.z());
            }

            // lower water line: a run through both pump rooms at this column
            zLine(acc, -paxZ, paxZ, gx, WATER_Y);
            // riser up to the group distribution header
            yLine(acc, WATER_Y, GDH_Y, gx, gz);
            // the group distribution header itself
            zLine(acc, minZ, maxZ, gx, GDH_Y);

            // per-channel lower connection (valve + meter), then the channel,
            // then the upper connection (valve + meter)
            for (int i = 0; i < grp.size(); i += step) {
                RbmkLattice.Channel c = grp.get(i);
                xLine(acc, gx, c.x(), GDH_Y, c.z());
                yLine(acc, GDH_Y, LOWER_TOP, c.x(), c.z());
                put(acc, c.x(), LOWER_TOP, c.z(), PlantCell.Role.VALVE);
                put(acc, c.x(), LOWER_TOP - 1, c.z(), PlantCell.Role.METER);

                yLine(acc, CORE_TOP, STEAM_Y, c.x(), c.z());
                put(acc, c.x(), UPPER_VALVE_Y, c.z(), PlantCell.Role.VALVE);
                put(acc, c.x(), UPPER_METER_Y, c.z(), PlantCell.Role.METER);
                xLine(acc, gx, c.x(), STEAM_Y, c.z());
            }

            // steam collector out to the separator room, then a drop to its roof
            int side = gz >= 0 ? 1 : -1;
            Compartment room = EnergoblokLayout.separatorRoom(side);
            int roomCentreZ = (room.z0() + room.z1()) / 2;
            zLine(acc, gz, roomCentreZ, gx, STEAM_Y);
            yLine(acc, room.outerBox()[4], STEAM_Y, gx, roomCentreZ);
        }

        List<PlantCell> out = new ArrayList<>(acc.size());
        for (Map.Entry<Long, PlantCell.Role> e : acc.entrySet()) {
            long k = e.getKey();
            int x = (int) (k >> 20) & 0x3FF;
            int y = (int) (k >> 10) & 0x3FF;
            int z = (int) k & 0x3FF;
            if (x >= 512) x -= 1024;
            if (y >= 512) y -= 1024;
            if (z >= 512) z -= 1024;
            out.add(new PlantCell(x, y, z, e.getValue()));
        }
        return out;
    }

    // -------------------------------------------------------------- primitives

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FF) << 20) | ((long) (y & 0x3FF) << 10) | (z & 0x3FF);
    }

    private static void put(Map<Long, PlantCell.Role> acc, int x, int y, int z,
                            PlantCell.Role role) {
        long k = key(x, y, z);
        PlantCell.Role cur = acc.get(k);
        if (cur == null || (cur == PlantCell.Role.PIPE && role != PlantCell.Role.PIPE)) {
            acc.put(k, role);
        }
    }

    private static void xLine(Map<Long, PlantCell.Role> acc, int x0, int x1, int y, int z) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            put(acc, x, y, z, PlantCell.Role.PIPE);
        }
    }

    private static void yLine(Map<Long, PlantCell.Role> acc, int y0, int y1, int x, int z) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
            put(acc, x, y, z, PlantCell.Role.PIPE);
        }
    }

    private static void zLine(Map<Long, PlantCell.Role> acc, int z0, int z1, int x, int y) {
        for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
            put(acc, x, y, z, PlantCell.Role.PIPE);
        }
    }

    private static int pumpRoomCentreZ() {
        Compartment r = EnergoblokLayout.pumpRoom(1);
        return (r.z0() + r.z1()) / 2;
    }
}
