package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;

/**
 * Fuel temperature from a heat-transfer balance, not a fitted line.
 *
 * <p>The core used to be TOLD its fuel temperature: {@code T = 640 + 600*(P-1)}, a
 * straight line in total power with two hand-set numbers. That is wrong in the three
 * ways the audit flagged: it is a single scalar (the real fuel is hottest where the
 * power is), it ignores the coolant (so a pump trip cannot change it), and neither
 * number was in the provenance.
 *
 * <p>Here the fuel temperature is DERIVED. Each axial node's fuel sits a conduction
 * rise above its OWN coolant temperature:
 *
 * <pre>
 *   T_fuel(a) = T_coolant(a) + R_eff * q'(a)
 * </pre>
 *
 * <p>{@code T_coolant(a)} comes from {@link ChannelCooling}, so it carries the real
 * coolant flow and inlet temperature: throttle the pumps and the fuel cools with the
 * water. {@code q'(a)} is the local linear power (W per metre of fuel rod), so the
 * fuel is hottest where the flux is.
 *
 * <p>{@code R_eff} is the effective thermal resistance of the fuel-to-coolant path
 * (UO2 conduction + gap + clad + film), in K per (W/m). It is not guessed: it is
 * fixed so that the model's HOTTEST node reaches the documented maximum fuel
 * temperature (600 degC — {@code docs/RBMK-PLANT-SYSTEMS.md} §1, WP §57) at nominal
 * power. One sourced number, and the whole per-node field follows from it.
 */
public final class FuelTemperature {

    /** Documented maximum fuel temperature, degC (WP §57). */
    public static final double MAX_FUEL_C = 600.0;

    /** Fuel rods per channel: 2 sub-assemblies x 18 rods. */
    public static final int RODS_PER_CHANNEL = 2 * RbmkGeometry.TVS_RODS_PER_SUBASSEMBLY;

    private final double risePerWPerM;

    private FuelTemperature(double risePerWPerM) {
        this.risePerWPerM = risePerWPerM;
    }

    /**
     * Fix R_eff so the hottest node reaches {@link #MAX_FUEL_C} at the given peak.
     *
     * @param peakLinearWPerM peak per-rod linear power at the nominal state (W/m)
     * @param peakCoolantC    coolant temperature at that same node (degC)
     */
    public static FuelTemperature anchoredToPeak(double peakLinearWPerM, double peakCoolantC) {
        return new FuelTemperature(
            (MAX_FUEL_C - peakCoolantC) / Math.max(1e-9, peakLinearWPerM));
    }

    /** Build with an explicit effective resistance (K per (W/m)). */
    public static FuelTemperature withRise(double risePerWPerM) {
        return new FuelTemperature(risePerWPerM);
    }

    /** Effective fuel-to-coolant thermal resistance, K per (W/m). */
    public double risePerWPerM() {
        return risePerWPerM;
    }

    /** Per-node equilibrium fuel temperature (degC) from coolant and local power. */
    public double[] equilibrium(double[] coolantC, double[] linearPowerWPerM) {
        int n = Math.min(coolantC.length, linearPowerWPerM.length);
        double[] out = new double[coolantC.length];
        for (int a = 0; a < n; a++) {
            out[a] = coolantC[a] + risePerWPerM * Math.max(0.0, linearPowerWPerM[a]);
        }
        return out;
    }

    /**
     * Per-rod linear power (W/m) per axial node, from the whole-core node power the
     * coolant balance deposits. Divided by the number of fuel channels to reach one
     * channel, then by the rod length in one node: 36 rods x the node height.
     */
    public static double[] linearPowerPerRod(double[] coreNodePowerW, double nodeLengthM,
                                             int fuelChannels) {
        double[] out = new double[coreNodePowerW.length];
        double denom = Math.max(1e-9, (double) fuelChannels * RODS_PER_CHANNEL * nodeLengthM);
        for (int a = 0; a < out.length; a++) {
            out[a] = coreNodePowerW[a] / denom;
        }
        return out;
    }
}
