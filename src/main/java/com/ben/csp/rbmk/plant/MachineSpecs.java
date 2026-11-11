package com.ben.csp.rbmk.plant;

import com.ben.csp.block.ModBlocks;

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
        124, 11, 11, MachineSpec.Shape.CYLINDER_X);

    /** Turbogenerator: ~39 m long (5 cylinders + the generator). */
    public static final MachineSpec TURBINE = new MachineSpec(
        "turbine", "turbogenerator K-500-65/3000 (ТВВ-500)",
        ModBlocks.TURBINE_BLOCK,
        156, 40, 48, MachineSpec.Shape.BOX_SHELL);

    /** Condenser: under the turbine. */
    public static final MachineSpec CONDENSER = new MachineSpec(
        "condenser", "condenser (конденсатор)",
        ModBlocks.CONDENSER_BLOCK,
        80, 20, 48, MachineSpec.Shape.BOX_SHELL);

    /** Main circulating pump (ГЦН): a pump body on a 6 kV motor. */
    public static final MachineSpec MCP = new MachineSpec(
        "mcp", "main circulating pump (ГЦН)",
        ModBlocks.MCP_BLOCK,
        3, 3, 3, MachineSpec.Shape.BOX_SHELL);

    /** Deaerator: also the feedwater storage tank. */
    public static final MachineSpec DEAERATOR = new MachineSpec(
        "deaerator", "deaerator / feedwater tank (деаэратор)",
        ModBlocks.FEEDWATER_BLOCK,
        24, 40, 24, MachineSpec.Shape.BOX_SHELL);

    /** ECCS accumulator: high-pressure nitrogen-blanketed tank. */
    public static final MachineSpec ECCS_ACCUMULATOR = new MachineSpec(
        "eccs_accumulator", "ECCS accumulator (САОР)",
        ModBlocks.ECCS_ACCUMULATOR_BLOCK,
        3, 6, 3, MachineSpec.Shape.BOX_SHELL);
}
