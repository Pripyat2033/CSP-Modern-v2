package com.ben.csp.rbmk.module;

import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RbmkLattice;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Standalone self-test for the swappable plant modules. No mocks and no Minecraft:
 * it exercises the real {@link PipeNetwork} topology and the real
 * {@link ModuleRegistry} audit.
 *
 * <p>The checks that matter: the module is <b>disjoint</b> from everything it must
 * not disturb (that is what "remove and replace without breaking anything else"
 * means, and the audit must agree), it carries the documented valve+meter pair per
 * channel, and it is deterministic (so a rebuild is identical).
 */
public final class PlantModuleSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        moduleIsDisjoint();
        carriesValvesAndMetersPerChannel();
        isDeterministic();
        detailLevelScales();
        System.out.println("PlantModuleSelfTest: " + checks + " checks passed.");
    }

    private static void moduleIsDisjoint() {
        PipeNetwork net = new PipeNetwork();
        ModuleRegistry.Audit a = ModuleRegistry.audit(net);
        check(a.ok(), "the pipework is disjoint from core, boxes and machines: " + a.describe());
        check(a.cellCount() == net.cells().size(), "the audit saw every cell (" + a.cellCount() + ")");

        // no two cells may share a coordinate (the map dedups; prove it)
        Set<Long> seen = new HashSet<>();
        boolean unique = true;
        for (PlantCell c : net.cells()) {
            if (!seen.add(key(c))) {
                unique = false;
            }
        }
        check(unique, "every cell is at a unique position (" + seen.size() + " cells)");
    }

    private static void carriesValvesAndMetersPerChannel() {
        PipeNetwork net = new PipeNetwork(22, 1);
        int channels = RbmkLattice.channels().size();
        check(net.count(PlantCell.Role.VALVE) == 2L * channels,
            "two flow-control valves per channel (lower + upper): "
                + net.count(PlantCell.Role.VALVE) + " = 2 x " + channels);
        check(net.count(PlantCell.Role.METER) == 2L * channels,
            "two ball flow meters per channel: " + net.count(PlantCell.Role.METER));
        check(net.count(PlantCell.Role.PIPE) > 0,
            "the pipework itself is present: " + net.count(PlantCell.Role.PIPE) + " pipe blocks");
    }

    private static void isDeterministic() {
        List<PlantCell> a = new PipeNetwork().cells();
        List<PlantCell> b = new PipeNetwork().cells();
        check(a.equals(b), "two plans are identical (" + a.size() + " cells)");
    }

    private static void detailLevelScales() {
        PipeNetwork one = new PipeNetwork(22, 1);
        PipeNetwork two = new PipeNetwork(22, 2);
        check(two.count(PlantCell.Role.VALVE) < one.count(PlantCell.Role.VALVE),
            "step=2 uses fewer valves than step=1 ("
                + two.count(PlantCell.Role.VALVE) + " < " + one.count(PlantCell.Role.VALVE) + ")");
        check(ModuleRegistry.audit(two).ok(), "the coarser detail level is still disjoint");
    }

    private static long key(PlantCell c) {
        return ((long) (c.x() & 0x3FF) << 20) | ((long) (c.y() & 0x3FF) << 10) | (c.z() & 0x3FF);
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
        System.out.println("  ok  " + what);
    }
}
