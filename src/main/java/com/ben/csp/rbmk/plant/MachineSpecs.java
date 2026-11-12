package com.ben.csp.rbmk.plant;

import com.ben.csp.block.ModBlocks;

import java.util.List;

/**
 * The machines of the RBMK-1000 plant, as multiblocks. Each one is a real machine
 * of a real size, quoted in {@code docs/RBMK-MACHINE-GEOMETRY.md} and converted
 * here at 1:4 (1 block = 0.25 m). A machine is not a single decorated block: it
 * is a shell of casing with a controller, and it is "formed" only when the shell
 * is really present (see {@link MachineValidator}).
 */
public final class MachineSpecs {

    private MachineSpecs() {}

    /** Steam separator drum: 2.8 m dia x 31 m, a horizontal pressure vessel. */
    public static final MachineSpec STEAM_SEPARATOR = new MachineSpec(
        "steam_separator", "steam separator drum (барабан-сепаратор)",
        ModBlocks.STEAM_SEPARATOR_BLOCK,
        MachineSizes.SEPARATOR_X, MachineSizes.SEPARATOR_Y, MachineSizes.SEPARATOR_Z,
        MachineSpec.Shape.CYLINDER_X);

    // ---- turbogenerator K-500-65/3000 + TVV-500 generator (one shaft) ----
    // Real figures (docs/RBMK-MACHINE-GEOMETRY.md): 39 m overall, one HP + four LP
    // cylinders. In the machine hall only the five white circular covers show —
    // the rotors, diaphragms and blade rings are inside. The cylinder lengths and
    // the cover diameter are a block approximation (the datasheet gives the 39 m
    // overall and the 5-cylinder count, not each cylinder's length).
    private static final int TURB_COVER_DIA = 19;   // ~4.75 m (approx, 1:4)
    private static final int TURB_COVER_LEN = 25;   // ~6.25 m each (approx, 1:4)
    private static final int TURB_FOUNDATION_H = 8; // concrete block under the machine
    private static final int TURB_GEN_LEN = 30;     // TVV-500 body (~7.5 m, approx)
    private static final int TURB_SIZE_X = 5 * TURB_COVER_LEN + 2 + TURB_GEN_LEN; // 157

    public static final MachineSpec TURBINE = MachineSpec.composite(
        "turbine", "turbogenerator K-500-65/3000 + ТВВ-500",
        MachineSizes.TURBINE_X, MachineSizes.TURBINE_Y, MachineSizes.TURBINE_Z,
        turbineParts());

    private static List<MachineSpec.Part> turbineParts() {
        List<MachineSpec.Part> p = new java.util.ArrayList<>();
        // the concrete foundation the whole machine sits on
        p.add(new MachineSpec.Part("foundation", ModBlocks.TURBINE_BLOCK,
            0, 0, 0, TURB_SIZE_X, TURB_FOUNDATION_H, TURB_COVER_DIA,
            MachineSpec.Shape.BOX_SHELL));
        // the five cylinder covers: one HP, then four LP, in a row along x
        String[] names = {"ЦВД (HP cover)", "ЦНД-1 (LP cover)", "ЦНД-2 (LP cover)",
                          "ЦНД-3 (LP cover)", "ЦНД-4 (LP cover)"};
        for (int i = 0; i < 5; i++) {
            p.add(new MachineSpec.Part(names[i], ModBlocks.TURBINE_COVER_BLOCK,
                i * TURB_COVER_LEN, TURB_FOUNDATION_H, 0,
                TURB_COVER_LEN, TURB_COVER_DIA, TURB_COVER_DIA,
                MachineSpec.Shape.CYLINDER_X));
        }
        // the shaft between the last cover and the generator
        p.add(new MachineSpec.Part("shaft", ModBlocks.TURBINE_BLOCK,
            5 * TURB_COVER_LEN, TURB_FOUNDATION_H + TURB_COVER_DIA / 2 - 1, TURB_COVER_DIA / 2 - 2,
            2, 3, 5, MachineSpec.Shape.BOX_SHELL));
        // the TVV-500 generator body, on the same shaft
        p.add(new MachineSpec.Part("generator ТВВ-500", ModBlocks.GENERATOR_BLOCK,
            5 * TURB_COVER_LEN + 2, TURB_FOUNDATION_H, 0,
            TURB_GEN_LEN, TURB_COVER_DIA - 2, TURB_COVER_DIA,
            MachineSpec.Shape.BOX_SHELL));
        return p;
    }

    /** Condenser: under the turbine. */
    public static final MachineSpec CONDENSER = new MachineSpec(
        "condenser", "condenser (конденсатор)",
        ModBlocks.CONDENSER_BLOCK,
        MachineSizes.CONDENSER_X, MachineSizes.CONDENSER_Y, MachineSizes.CONDENSER_Z,
        MachineSpec.Shape.BOX_SHELL);

    /**
     * Main circulating pump (ГЦН): the pump body with its 6 kV drive motor mounted
     * above it on the same shaft. 5,500–12,000 m3/h, 6 kV motor (WP §"ГЦН").
     */
    public static final MachineSpec MCP = MachineSpec.composite(
        "mcp", "main circulating pump (ГЦН) + 6 kV motor",
        MachineSizes.MCP_X, MachineSizes.MCP_Y, MachineSizes.MCP_Z, List.of(
            new MachineSpec.Part("pump body", ModBlocks.MCP_BLOCK,
                0, 0, 0, 3, 3, 3, MachineSpec.Shape.BOX_SHELL),
            new MachineSpec.Part("6 kV motor", ModBlocks.MCP_MOTOR_BLOCK,
                0, 3, 0, 3, 3, 3, MachineSpec.Shape.BOX_SHELL)));

    /** Deaerator: also the feedwater storage tank. */
    public static final MachineSpec DEAERATOR = new MachineSpec(
        "deaerator", "deaerator / feedwater tank (деаэратор)",
        ModBlocks.FEEDWATER_BLOCK,
        MachineSizes.DEAERATOR_X, MachineSizes.DEAERATOR_Y, MachineSizes.DEAERATOR_Z,
        MachineSpec.Shape.BOX_SHELL);

    /** ECCS accumulator: high-pressure nitrogen-blanketed tank. */
    public static final MachineSpec ECCS_ACCUMULATOR = new MachineSpec(
        "eccs_accumulator", "ECCS accumulator (САОР)",
        ModBlocks.ECCS_ACCUMULATOR_BLOCK,
        MachineSizes.ECCS_X, MachineSizes.ECCS_Y, MachineSizes.ECCS_Z,
        MachineSpec.Shape.BOX_SHELL);
}
