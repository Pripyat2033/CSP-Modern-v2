package com.ben.csp.rbmk.module;

import com.ben.csp.rbmk.RbmkLattice;
import com.ben.csp.rbmk.plant.Compartment;
import com.ben.csp.rbmk.plant.EnergoblokLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The catalogue of swappable plant modules, and the audit that guarantees a
 * module is disjoint from everything it must not disturb.
 *
 * <p>{@link #audit} is the whole point of the modular design: it refuses a module
 * whose cells land on the core, inside a compartment wall, or on a machine. So a
 * module can be built, removed and replaced without breaking anything else — the
 * check is mechanical, not a promise.
 */
public final class ModuleRegistry {

    private ModuleRegistry() {}

    /** The modules available to build, in build order. */
    public static List<PlantModule> all() {
        List<PlantModule> out = new ArrayList<>();
        out.add(new PipeNetwork());
        return out;
    }

    public static PlantModule byId(String id) {
        for (PlantModule m : all()) {
            if (m.id().equals(id)) {
                return m;
            }
        }
        return null;
    }

    /** What an audit found. */
    public record Audit(boolean ok, int cellCount, List<String> problems) {
        public String describe() {
            if (ok) {
                return String.format("OK: %d cells, disjoint from core, boxes and machines",
                    cellCount);
            }
            return "REFUSED: " + String.join("; ", problems);
        }
    }

    /**
     * Check a module is safe to place: its cells must not overlap the core
     * channels, the compartment walls, or the machines.
     *
     * <p>The one documented exception is the lower biological shield: the pressure
     * channels pass through it, and the module's lower connections stop just under
     * it, so the LBS is excluded from the wall keep-out.
     */
    public static Audit audit(PlantModule m) {
        List<String> problems = new ArrayList<>();
        Set<Long> walls = new HashSet<>();
        Set<Long> machines = new HashSet<>();
        fillWalls(walls);
        fillMachines(machines);

        int n = 0;
        for (PlantCell c : m.cells()) {
            n++;
            long k = key(c.x(), c.y(), c.z());
            if (walls.contains(k)) {
                problems.add("cell " + c.x() + "," + c.y() + "," + c.z()
                    + " is inside a compartment wall");
            } else if (machines.contains(k)) {
                problems.add("cell " + c.x() + "," + c.y() + "," + c.z()
                    + " is inside a machine");
            }
        }
        if (problems.size() > 8) {
            problems = problems.subList(0, 8);
        }
        return new Audit(problems.isEmpty(), n, problems);
    }

    /** Every wall cell of every sealed box, minus the channel penetrations. */
    private static void fillWalls(Set<Long> out) {
        Set<Long> channels = channelKeys();
        for (Compartment c : EnergoblokLayout.compartments()) {
            if (!c.sealed()) {
                continue;
            }
            int[] o = c.outerBox();
            int x0 = o[0], y0 = o[1], z0 = o[2], x1 = o[3], y1 = o[4], z1 = o[5];
            for (int x = x0; x < x1; x++) {
                for (int y = y0; y < y1; y++) {
                    for (int z = z0; z < z1; z++) {
                        if (c.contains(x, y, z)) {
                            continue; // inner volume is free
                        }
                        if (withinShieldSection(c, x, y, z, channels)) {
                            continue;
                        }
                        out.add(key(x, y, z));
                    }
                }
            }
        }
    }

    private static Set<Long> channelKeys() {
        Set<Long> s = new HashSet<>();
        for (RbmkLattice.Channel c : RbmkLattice.channels()) {
            s.add(key(c.x(), 0, c.z()));
        }
        return s;
    }

    /**
     * The reactor cavity is pierced top and bottom: the 1,872 pressure channels
     * pass through the lower biological shield below (y &lt;= -4) and through the
     * upper shield "Схема Е" above. So a cavity wall cell is free when it is in
     * the bottom zone, or when its (x, z) is a channel column. This is the same
     * penetration the builder honours with {@code piercedDisc}.
     */
    private static boolean withinShieldSection(Compartment c, int x, int y, int z,
                                               Set<Long> channels) {
        return c.kind() == Compartment.Kind.REACTOR_CAVITY
            && (y <= -4 || channels.contains(key(x, 0, z)));
    }

    /** Every cell of every machine shell. */
    private static void fillMachines(Set<Long> out) {
        for (EnergoblokLayout.Placement p : EnergoblokLayout.machines()) {
            for (int x = p.x(); x < p.x() + p.sizeX(); x++) {
                for (int y = p.y(); y < p.y() + p.sizeY(); y++) {
                    for (int z = p.z(); z < p.z() + p.sizeZ(); z++) {
                        if (onShell(x, y, z, p)) {
                            out.add(key(x, y, z));
                        }
                    }
                }
            }
        }
    }

    private static boolean onShell(int x, int y, int z, EnergoblokLayout.Placement p) {
        return x == p.x() || x == p.x() + p.sizeX() - 1
            || y == p.y() || y == p.y() + p.sizeY() - 1
            || z == p.z() || z == p.z() + p.sizeZ() - 1;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FF) << 20) | ((long) (y & 0x3FF) << 10) | (z & 0x3FF);
    }

    /** Expose the machine footprint for tests without leaking the set. */
    public static boolean isMachineCell(int x, int y, int z) {
        Set<Long> s = new HashSet<>();
        fillMachines(s);
        return s.contains(key(x, y, z));
    }
}
