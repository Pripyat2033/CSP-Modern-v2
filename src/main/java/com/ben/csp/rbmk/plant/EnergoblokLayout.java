package com.ben.csp.rbmk.plant;

import com.ben.csp.rbmk.ConstructionJob;

import java.util.ArrayList;
import java.util.List;

/**
 * The energoblok of an RBMK-1000 unit as an architecture of <b>sealed,
 * pressure-resistant concrete boxes</b> — the correction the Director made to the
 * "machines scattered around a core" model.
 *
 * <p>An energoblok is not one hall. It is a stack of separate boxes, and those
 * boxes <em>are</em> the containment. The claim that an RBMK has no containment
 * is false. The design-basis accident is not a core melt (there is no steel
 * pressure vessel to melt through) but the rupture of a large-diameter water pipe
 * feeding the core, which flashes to steam and pressurises a box; past the relief
 * capacity that pressure can lift the upper shield ("Схема Е" / "Elena") off its
 * rollers. So the boxes must exist as real, sealed, rated volumes for the
 * accident physics to have somewhere to live.
 *
 * <p>Coordinates are block offsets from the energoblok origin: the reactor
 * centre, at the reactor-hall floor (the bottom of the graphite stack). +x points
 * to the machine (turbine) hall; +/-z are the two sides of the building
 * (left/right) where the pump and separator rooms sit.
 *
 * <p>Every dimension is the real one at 1:4 (1 block = 0.25 m), from
 * docs/RBMK-PLANT-SYSTEMS.md (reactor pit 21.6 m square; drums 2.8 x 31 m;
 * turbogenerator 39 m; compartment design pressures §7). What the doc marks
 * TO VERIFY (the exact room sizes and the vent stack) is approximated and noted.
 */
public final class EnergoblokLayout {

    private EnergoblokLayout() {}

    /** Wall thickness of the sealed boxes (0.5 m of concrete at 1:4). */
    public static final int WALL = 2;

    // ---- reactor cavity: the 21.6 m pit (WP §34) -> 86 blocks across ----
    public static final int CAVITY_RADIUS_BLOCKS = 43;
    public static final int CAVITY_FLOOR_Y = -12;   // below the lower biological shield
    public static final int CAVITY_TOP_Y = 46;      // above Схема Е and its plugs

    // ---- room dimensions (blocks; real metres at 1:4) ----
    public static final int PUMP_ROOM_HALF_LEN = 22;      // 44 blocks = 11 m
    public static final int PUMP_ROOM_DEPTH = 22;         // 5.5 m
    public static final int PUMP_ROOM_HEIGHT = 16;        // 4 m
    public static final int PUMP_ROOM_FLOOR_Y = -8;

    public static final int SEPARATOR_ROOM_HALF_LEN = 66; // 132 blocks = 33 m (holds the 31 m drum)
    public static final int SEPARATOR_ROOM_DEPTH = 30;    // 7.5 m (two drums side by side)
    public static final int SEPARATOR_ROOM_HEIGHT = 18;   // 4.5 m
    public static final int SEPARATOR_FLOOR_Y = 12;       // above the pump rooms

    public static final int HEADER_ROOM_LEN = 24;         // 6 m
    public static final int HEADER_ROOM_HALF_W = 30;      // 15 m

    // ---- machine hall (turbine building), adjacent along +x ----
    public static final int HALL_X0 = 120;
    public static final int HALL_FLOOR_Y = -8;
    public static final int HALL_HEIGHT = 40;
    /** Centre-to-centre spacing of the two turbogenerators (z), ~17 m. */
    public static final int TURBINE_HALL_GAP = 34;

    /**
     * A machine placement: its room, what it is, where its min corner stands, and
     * its size. The {@code part} determines the machine; the builder maps it to the
     * matching {@code MachineSpec}. The record itself carries no Minecraft type, so
     * the layout stays testable with no game on the classpath.
     */
    public record Placement(String room, ConstructionJob.Part part,
                            int sizeX, int sizeY, int sizeZ,
                            int x, int y, int z) {}

    // ------------------------------------------------------------- the boxes

    /** The reactor cavity: the reactor in its own room. */
    public static Compartment reactorCavity() {
        int r = CAVITY_RADIUS_BLOCKS;
        return new Compartment(Compartment.Kind.REACTOR_CAVITY, "reactor_cavity",
            -r, CAVITY_FLOOR_Y, -r, r, CAVITY_TOP_Y, r,
            WALL, Compartment.Kind.REACTOR_CAVITY.designPressurePa());
    }

    /** A main-circulating-pump room: one per circuit, left and right of the reactor. */
    public static Compartment pumpRoom(int side) { // side = +1 or -1 (z)
        int zNear = CAVITY_RADIUS_BLOCKS + WALL;      // 45
        int z0 = side > 0 ? zNear : -(zNear + PUMP_ROOM_DEPTH);
        int z1 = side > 0 ? zNear + PUMP_ROOM_DEPTH : -zNear;
        return new Compartment(Compartment.Kind.PUMP_ROOM,
            side > 0 ? "pump_room_left" : "pump_room_right",
            -PUMP_ROOM_HALF_LEN, PUMP_ROOM_FLOOR_Y, z0,
            PUMP_ROOM_HALF_LEN, PUMP_ROOM_FLOOR_Y + PUMP_ROOM_HEIGHT, z1,
            WALL, Compartment.Kind.PUMP_ROOM.designPressurePa());
    }

    /** A separator room: two steam drums per box, left and right of the reactor. */
    public static Compartment separatorRoom(int side) {
        int zNear = CAVITY_RADIUS_BLOCKS + WALL;
        int z0 = side > 0 ? zNear : -(zNear + SEPARATOR_ROOM_DEPTH);
        int z1 = side > 0 ? zNear + SEPARATOR_ROOM_DEPTH : -zNear;
        return new Compartment(Compartment.Kind.SEPARATOR_ROOM,
            side > 0 ? "separator_room_left" : "separator_room_right",
            -SEPARATOR_ROOM_HALF_LEN, SEPARATOR_FLOOR_Y, z0,
            SEPARATOR_ROOM_HALF_LEN, SEPARATOR_FLOOR_Y + SEPARATOR_ROOM_HEIGHT, z1,
            WALL, Compartment.Kind.SEPARATOR_ROOM.designPressurePa());
    }

    /** A header room: the large-bore pipe rooms, east and west of the reactor. */
    public static Compartment headerRoom(int side) { // side = +1 (east) or -1 (west)
        int xNear = CAVITY_RADIUS_BLOCKS + WALL;
        int x0 = side > 0 ? xNear : -(xNear + HEADER_ROOM_LEN);
        int x1 = side > 0 ? xNear + HEADER_ROOM_LEN : -xNear;
        return new Compartment(Compartment.Kind.HEADER_ROOM,
            side > 0 ? "header_room_east" : "header_room_west",
            x0, PUMP_ROOM_FLOOR_Y, -HEADER_ROOM_HALF_W,
            x1, PUMP_ROOM_FLOOR_Y + PUMP_ROOM_HEIGHT, HEADER_ROOM_HALF_W,
            WALL, Compartment.Kind.HEADER_ROOM.designPressurePa());
    }

    public static Compartment turbineIsland(int unit) { // unit = 0 or 1
        int zc = unit == 0 ? -TURBINE_HALL_GAP : TURBINE_HALL_GAP;
        int halfZ = MachineSizes.TURBINE_Z / 2 + 6;
        return new Compartment(Compartment.Kind.TURBINE_ISLAND,
            "turbine_island_" + unit,
            HALL_X0 - 8, HALL_FLOOR_Y, zc - halfZ,
            HALL_X0 + MachineSizes.TURBINE_X + 8, HALL_FLOOR_Y + HALL_HEIGHT, zc + halfZ,
            1, 0.0);
    }

    public static Compartment condenserIsland(int unit) {
        int zc = unit == 0 ? -TURBINE_HALL_GAP : TURBINE_HALL_GAP;
        int halfZ = MachineSizes.CONDENSER_Z / 2 + 4;
        return new Compartment(Compartment.Kind.CONDENSER_ISLAND,
            "condenser_island_" + unit,
            HALL_X0 + 20, HALL_FLOOR_Y - 24, zc - halfZ,
            HALL_X0 + 20 + MachineSizes.CONDENSER_X, HALL_FLOOR_Y, zc + halfZ,
            1, 0.0);
    }

    public static Compartment feedwaterIsland() {
        int halfZ = MachineSizes.DEAERATOR_Z / 2;
        return new Compartment(Compartment.Kind.FEEDWATER_ISLAND, "feedwater_island",
            HALL_X0 + 30, HALL_FLOOR_Y, -halfZ,
            HALL_X0 + 30 + PlantPhysics.DEAERATOR_COUNT * (MachineSizes.DEAERATOR_X + 4),
            HALL_FLOOR_Y + HALL_HEIGHT, halfZ,
            1, 0.0);
    }

    // ------------------------------------------------------------- the whole unit

    /** Every room of the unit, in build order. */
    public static List<Compartment> compartments() {
        List<Compartment> out = new ArrayList<>();
        out.add(reactorCavity());
        for (int side = -1; side <= 1; side += 2) {
            out.add(pumpRoom(side));
            out.add(separatorRoom(side));
            out.add(headerRoom(side));
        }
        out.add(turbineIsland(0));
        out.add(turbineIsland(1));
        out.add(condenserIsland(0));
        out.add(condenserIsland(1));
        out.add(feedwaterIsland());
        return out;
    }

    // ------------------------------------------------------------- the machines

    /**
     * Where each machine stands, inside its room. The pumps and the separators are
     * placed from the room's own bounds, so they can never drift into the reactor
     * or the side water tank again — that is the bug the Director caught.
     */
    public static List<Placement> machines() {
        List<Placement> out = new ArrayList<>();

        // --- 4 pumps per circuit, in a row in each pump room ---
        for (int side = -1; side <= 1; side += 2) {
            Compartment room = pumpRoom(side);
            int n = PlantPhysics.MCP_PER_CIRCUIT;
            int step = MachineSizes.MCP_X + 4;
            int span = n * step - 4;
            int startX = -(span / 2);
            int pz = room.z0() + (room.z1() - room.z0()) / 2 - MachineSizes.MCP_Z / 2;
            for (int p = 0; p < n; p++) {
                out.add(new Placement(room.id(), ConstructionJob.Part.PUMPS,
                    MachineSizes.MCP_X, MachineSizes.MCP_Y, MachineSizes.MCP_Z,
                    startX + p * step, room.y0() + 2, pz));
            }
        }

        // --- 2 steam drums per separator room, side by side in z ---
        for (int side = -1; side <= 1; side += 2) {
            Compartment room = separatorRoom(side);
            int drumLen = MachineSizes.SEPARATOR_X;   // 124
            int drumZ = MachineSizes.SEPARATOR_Z;     // 11
            int x0 = -drumLen / 2;
            int y0 = room.y0() + (room.y1() - room.y0()) / 2 - drumZ / 2;
            for (int d = 0; d < 2; d++) {
                int z0 = room.z0() + 2 + d * (drumZ + 4);
                out.add(new Placement(room.id(), ConstructionJob.Part.SEPARATORS,
                    MachineSizes.SEPARATOR_X, MachineSizes.SEPARATOR_Y, MachineSizes.SEPARATOR_Z,
                    x0, y0, z0));
            }
        }

        // --- 12 ECCS accumulators (САОР), six in each header room ---
        int placed = 0;
        for (int side = -1; side <= 1 && placed < 12; side += 2) {
            Compartment room = headerRoom(side);
            int step = MachineSizes.ECCS_Z + 3;
            int start = room.z0() + 2;
            for (int i = 0; i < 6 && placed < 12; i++, placed++) {
                out.add(new Placement(room.id(), ConstructionJob.Part.ACCUMULATORS,
                    MachineSizes.ECCS_X, MachineSizes.ECCS_Y, MachineSizes.ECCS_Z,
                    (room.x0() + room.x1()) / 2 - 1, room.y0() + 2, start + i * step));
            }
        }

        // --- the two turbogenerators on their islands, condensers beneath ---
        for (int unit = 0; unit < 2; unit++) {
            Compartment island = turbineIsland(unit);
            int zc = unit == 0 ? -TURBINE_HALL_GAP : TURBINE_HALL_GAP;
            out.add(new Placement(island.id(), ConstructionJob.Part.TURBINES,
                MachineSizes.TURBINE_X, MachineSizes.TURBINE_Y, MachineSizes.TURBINE_Z,
                island.x0() + 8, HALL_FLOOR_Y, zc - MachineSizes.TURBINE_Z / 2));

            Compartment cond = condenserIsland(unit);
            out.add(new Placement(cond.id(), ConstructionJob.Part.CONDENSERS,
                MachineSizes.CONDENSER_X, MachineSizes.CONDENSER_Y, MachineSizes.CONDENSER_Z,
                cond.x0(), HALL_FLOOR_Y - MachineSizes.CONDENSER_Y,
                zc - MachineSizes.CONDENSER_Z / 2));
        }

        // --- deaerators / feedwater tanks, in a row on the feedwater island ---
        Compartment fw = feedwaterIsland();
        for (int i = 0; i < PlantPhysics.DEAERATOR_COUNT; i++) {
            out.add(new Placement(fw.id(), ConstructionJob.Part.FEEDWATER,
                MachineSizes.DEAERATOR_X, MachineSizes.DEAERATOR_Y, MachineSizes.DEAERATOR_Z,
                fw.x0() + i * (MachineSizes.DEAERATOR_X + 4), fw.y0(), fw.z0()));
        }
        return out;
    }

    /** The room that holds the machine, or null (used by the self-test). */
    public static Compartment roomOf(Placement p) {
        for (Compartment c : compartments()) {
            if (c.id().equals(p.room())) {
                return c;
            }
        }
        return null;
    }
}
