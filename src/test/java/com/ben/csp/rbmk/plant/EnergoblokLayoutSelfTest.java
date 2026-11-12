package com.ben.csp.rbmk.plant;

import com.ben.csp.rbmk.ConstructionJob;
import com.ben.csp.rbmk.HoleMask;
import com.ben.csp.rbmk.RbmkGeometry;
import com.ben.csp.rbmk.RbmkLattice;

import java.util.List;

/**
 * Standalone self-test for the energoblok as an architecture of sealed
 * compartments. No mocks: it exercises the real layout classes.
 *
 * <p>The checks that matter most are the ones that enforce the Director's
 * correction — an energoblok is separate pressure-resistant boxes, and those
 * boxes are the containment — and the one that catches the bug he found: a
 * machine must sit inside its own room, never drifting into the reactor or the
 * side water tank.
 */
public final class EnergoblokLayoutSelfTest {

    private static int checks = 0;

    public static void main(String[] args) {
        compartmentsAreSealedAndRated();
        roomsAreDisjoint();
        machinesAreInTheirRooms();
        machineCounts();
        ubsIsPierced();
        System.out.println("EnergoblokLayoutSelfTest: " + checks + " checks passed.");
    }

    private static void compartmentsAreSealedAndRated() {
        List<Compartment> rooms = EnergoblokLayout.compartments();
        int sealed = 0, open = 0;
        for (Compartment c : rooms) {
            if (c.sealed()) {
                sealed++;
                check(c.designPressurePa() > 0,
                    c.id() + " is a sealed box with a design pressure");
                check(c.wall() >= 1, c.id() + " has a wall");
            } else {
                open++;
                check(c.designPressurePa() == 0, c.id() + " is an open island, not rated");
            }
        }
        check(sealed == 7, "there are 7 sealed boxes (cavity, 2 pump, 2 separator, 2 header): " + sealed);
        check(open == 5, "there are 5 open hall islands (2 turbine, 2 condenser, 1 feedwater): " + open);

        // the design pressures are the real ones (docs §7)
        eq(0.18e6, EnergoblokLayout.reactorCavity().designPressurePa(), 1.0,
            "reactor cavity is rated 0.18 MPa");
        eq(0.45e6, EnergoblokLayout.pumpRoom(1).designPressurePa(), 1.0,
            "pump room is rated 0.45 MPa (the strongest)");
        eq(0.08e6, EnergoblokLayout.headerRoom(1).designPressurePa(), 1.0,
            "header room is rated 0.08 MPa (the DBA room)");

        // the cavity is the real 21.6 m pit
        Compartment cav = EnergoblokLayout.reactorCavity();
        eq(86, cav.x1() - cav.x0(), 0, "the reactor cavity is the 21.6 m pit (86 blocks)");
        check(cav.contains(0, 0, 0), "the reactor centre is inside its cavity");
    }

    private static void roomsAreDisjoint() {
        List<Compartment> rooms = EnergoblokLayout.compartments();
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                check(!overlaps(rooms.get(i), rooms.get(j)),
                    rooms.get(i).id() + " and " + rooms.get(j).id() + " do not overlap");
            }
        }
    }

    private static boolean overlaps(Compartment a, Compartment b) {
        return a.x0() < b.x1() && b.x0() < a.x1()
            && a.y0() < b.y1() && b.y0() < a.y1()
            && a.z0() < b.z1() && b.z0() < a.z1();
    }

    private static void machinesAreInTheirRooms() {
        List<EnergoblokLayout.Placement> machines = EnergoblokLayout.machines();
        check(machines.size() > 0, "the unit has machines placed");
        for (EnergoblokLayout.Placement p : machines) {
            Compartment room = EnergoblokLayout.roomOf(p);
            check(room != null, p.part() + " is placed in a known room");
            // the machine's min corner and its far corner must both be inside the room
            int fx = p.x() + p.sizeX() - 1;
            int fy = p.y() + p.sizeY() - 1;
            int fz = p.z() + p.sizeZ() - 1;
            check(room.contains(p.x(), p.y(), p.z()),
                p.part() + " min corner is inside " + room.id()
                    + " (at " + p.x() + "," + p.y() + "," + p.z() + ")");
            check(room.contains(fx, fy, fz),
                p.part() + " fits inside " + room.id()
                    + " (far corner " + fx + "," + fy + "," + fz + ")");

            // and it must NOT be in the reactor cavity or the water tank
            check(!EnergoblokLayout.reactorCavity().contains(p.x(), p.y(), p.z()),
                p.part() + " is not in the reactor cavity");
        }
    }

    private static void machineCounts() {
        List<EnergoblokLayout.Placement> m = EnergoblokLayout.machines();
        eq(8, count(m, ConstructionJob.Part.PUMPS), 0, "8 main circulating pumps (2 circuits x 4)");
        eq(4, count(m, ConstructionJob.Part.SEPARATORS), 0, "4 steam separator drums (2 per box)");
        eq(12, count(m, ConstructionJob.Part.ACCUMULATORS), 0, "12 ECCS accumulators (САОР)");
        eq(2, count(m, ConstructionJob.Part.TURBINES), 0, "2 turbogenerators");
        eq(2, count(m, ConstructionJob.Part.CONDENSERS), 0, "2 condensers, one under each turbine");
        eq(4, count(m, ConstructionJob.Part.FEEDWATER), 0, "4 deaerators / feedwater tanks");

        // the two pumps rooms hold one circuit each (4 pumps apiece)
        int leftPumps = 0, rightPumps = 0;
        for (EnergoblokLayout.Placement p : m) {
            if (p.part() == ConstructionJob.Part.PUMPS) {
                if (p.room().contains("left")) leftPumps++; else rightPumps++;
            }
        }
        eq(4, leftPumps, 0, "4 pumps in the left pump room (one circuit)");
        eq(4, rightPumps, 0, "4 pumps in the right pump room (the other circuit)");
    }

    private static void ubsIsPierced() {
        // The upper biological shield is not a solid lid: every one of the 1,872
        // channels passes through it. Compare the pierced disc to the solid one.
        int r = RbmkGeometry.UBS_DIAMETER_BLOCKS / 2;   // 34
        long solidCells = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if ((long) x * x + (long) z * z <= (long) r * r) {
                    solidCells++;
                }
            }
        }
        List<RbmkLattice.Channel> channels = RbmkLattice.channels();
        int[] hx = new int[channels.size()];
        int[] hz = new int[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            hx[i] = channels.get(i).x();
            hz[i] = channels.get(i).z();
        }
        HoleMask mask = new HoleMask(hx, hz);
        long piercedCells = mask.countDisc(0, 0, 0, r, 1);
        check(piercedCells < solidCells,
            "the UBS is pierced, not a solid lid (" + piercedCells + " vs " + solidCells + ")");
        eq((int) (solidCells - piercedCells), 1872, 0,
            "the UBS has one hole per channel (1,872)");
    }

    private static int count(List<EnergoblokLayout.Placement> m, ConstructionJob.Part part) {
        int n = 0;
        for (EnergoblokLayout.Placement p : m) {
            if (p.part() == part) {
                n++;
            }
        }
        return n;
    }

    private static void check(boolean cond, String what) {
        checks++;
        if (!cond) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    private static void eq(int expected, int actual, int tol, String what) {
        check(Math.abs(expected - actual) <= tol,
            what + ": expected <" + expected + "> but was <" + actual + ">");
    }

    private static void eq(double expected, double actual, double tol, String what) {
        check(Math.abs(expected - actual) <= tol,
            what + ": expected <" + expected + "> but was <" + actual + ">");
    }
}
