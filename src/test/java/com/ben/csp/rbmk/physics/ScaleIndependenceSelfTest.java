package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkCore;
import com.ben.csp.rbmk.RbmkGeometry;

/**
 * Guards the mission rule that the physics must never depend on how many blocks
 * the world spends on the core. The simulation runs at true metres, with one
 * neutron node per real 250 mm lattice cell, whether the build is drawn at 1:4
 * today or at 1:1 bit-scale later.
 *
 * <p>This was once only accidentally true. The physics mesh used to be built from
 * the {@code *_BLOCKS} constants, which are derived from
 * {@code METRES_PER_BLOCK} — the <em>drawing</em> scale. That is correct only
 * while {@code METRES_PER_BLOCK == LATTICE_PITCH_M}, which held at 1:4 and would
 * have silently collapsed the mesh 4x the moment the build went to 1:1. The
 * physics now reads dedicated {@code *_CELLS} constants, computed from the real
 * metres divided by the real pitch, so the two are decoupled.
 *
 * <p>What this test pins, and why each check matters:
 * <ul>
 *   <li>the cell counts are the real metres / the real 250 mm pitch;</li>
 *   <li>the axial node count ({@code RbmkCore.AXIAL_NODES}) and the rod-worth
 *       node count are the cell count, not a block count;</li>
 *   <li>the cell counts do NOT move when {@code METRES_PER_BLOCK} moves — proven
 *       by recomputing the same real-metre arithmetic with no reference to the
 *       build scale, so the check would fail if a cell constant were ever
 *       re-expressed in blocks.</li>
 * </ul>
 */
public final class ScaleIndependenceSelfTest {

    private static int checks = 0;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
    }

    public static void main(String[] args) {
        double pitch = RbmkGeometry.LATTICE_PITCH_M;

        // --- the mesh pitch is the real 250 mm --------------------------------
        check(Math.abs(pitch - 0.250) < 1e-12,
            "the lattice pitch is the real 250 mm (" + pitch + ")");

        // --- cell counts are the real metres / pitch --------------------------
        check(RbmkGeometry.CORE_HEIGHT_CELLS == 28,
            "CORE_HEIGHT_CELLS = 28 (7 m / 250 mm), got " + RbmkGeometry.CORE_HEIGHT_CELLS);
        check(RbmkGeometry.CORE_DIAMETER_CELLS == 47,
            "CORE_DIAMETER_CELLS = 47 (11.8 m / 250 mm), got " + RbmkGeometry.CORE_DIAMETER_CELLS);
        check(RbmkGeometry.CHANNEL_REGION_DIAMETER_CELLS == 49,
            "CHANNEL_REGION_DIAMETER_CELLS = 49 (12.25 m / 250 mm), got "
                + RbmkGeometry.CHANNEL_REGION_DIAMETER_CELLS);
        check(RbmkGeometry.STACK_DIAMETER_CELLS == 56,
            "STACK_DIAMETER_CELLS = 56 (14 m / 250 mm), got " + RbmkGeometry.STACK_DIAMETER_CELLS);

        // --- the cell counts span the real metres exactly ---------------------
        check(Math.abs(RbmkGeometry.CORE_HEIGHT_CELLS * pitch - RbmkGeometry.CORE_HEIGHT_M) < 1e-9,
            "CORE_HEIGHT_CELLS x pitch spans the real 7 m core height");
        check(Math.abs(RbmkGeometry.CHANNEL_REGION_DIAMETER_CELLS * pitch
                - RbmkGeometry.CHANNEL_REGION_DIAMETER_M) < 1e-9,
            "CHANNEL_REGION_DIAMETER_CELLS x pitch spans the real 12.25 m region");
        check(Math.abs(RbmkGeometry.STACK_DIAMETER_CELLS * pitch
                - RbmkGeometry.STACK_DIAMETER_M) < 1e-9,
            "STACK_DIAMETER_CELLS x pitch spans the real 14 m stack");

        // --- the physics uses the CELL counts, not the BLOCK counts -----------
        // RbmkCore's axial node count and RodWorth's node count must equal the
        // cell count. If either were ever re-expressed in blocks, this fails.
        check(RbmkCore.AXIAL_NODES == RbmkGeometry.CORE_HEIGHT_CELLS,
            "RbmkCore.AXIAL_NODES (" + RbmkCore.AXIAL_NODES
                + ") is the cell count, not a block count");
        check(RodWorth.nodes() == RbmkGeometry.CORE_HEIGHT_CELLS,
            "RodWorth.nodes() (" + RodWorth.nodes()
                + ") is the cell count, not a block count");

        // --- scale independence, the whole point ------------------------------
        // Recompute the mesh from the real metres. This expression cannot move
        // when METRES_PER_BLOCK moves, because it never reads it. The cell
        // constants must equal it.
        int axialFromMetres = (int) Math.round(RbmkGeometry.CORE_HEIGHT_M / pitch);
        check(RbmkGeometry.CORE_HEIGHT_CELLS == axialFromMetres,
            "CORE_HEIGHT_CELLS equals the metre-derived node count ("
                + axialFromMetres + ") with no reference to the build scale");

        System.out.println("ScaleIndependenceSelfTest: " + checks + " checks passed");
    }
}
