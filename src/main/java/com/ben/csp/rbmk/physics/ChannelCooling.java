package com.ben.csp.rbmk.physics;

/**
 * The coolant path through one fuel channel: heat in, enthalpy up, boiling, void out.
 *
 * <p>This is the piece that removes the last fitted map from the core. The core used
 * to be TOLD its void: {@code void = 0.05 + 0.08*(power-1)}, a straight line with a
 * slope someone chose. Here the void is DERIVED from the energy balance the plant
 * actually runs:
 *
 * <pre>
 *   heat deposited per node (from the flux)  ->  coolant enthalpy rise
 *   enthalpy  ->  boiling onset  ->  steam quality x
 *   quality + slip  ->  volume void fraction alpha
 * </pre>
 *
 * <p>Every number is sourced from {@code docs/RBMK-PLANT-SYSTEMS.md} (WP §83-89):
 * 6 x 8,000 m3/h at 750 kg/m3 is 10,000 kg/s; 3,200 MW over that flow is a 320 kJ/kg
 * rise; the outlet quality comes out at 0.156, which matches the documented "~15%".
 * The void that follows from that balance is ~0.35 core-average at nominal power —
 * not the 0.05 the fitted map assumed.
 *
 * <p>The axial shape is supplied by the caller (from the flux), so the void profile
 * and the flux profile are the SAME field seen twice: the flux decides where the heat
 * goes, the heat decides where the steam is, and the steam decides the flux. That
 * loop is the accident, and it is now closed with no fitted map in it.
 *
 * <p>Units: SI (W, kg, s, J, K) except temperatures in degrees C where labelled.
 */
public final class ChannelCooling {

    // ------------------------------------------------- sourced coolant state
    /** System pressure, 6.9 MPa (WP §88). */
    public static final double P_SYSTEM_PA = 6.9e6;
    /** Saturation temperature at 6.9 MPa (WP §88). */
    public static final double T_SAT_C = 284.0;
    /** Core inlet temperature, 265-270 C (WP §88). */
    public static final double T_INLET_C = 267.0;
    /** Liquid specific heat near 280 C (J/kg/K). */
    public static final double CP_COOLANT = 5.0e3;
    /** Latent heat of vaporisation at 6.9 MPa (J/kg). */
    public static final double H_FG = 1505e3;
    /** Saturated-liquid enthalpy at 6.9 MPa (J/kg), from H_STEAM - H_FG. */
    public static final double H_F = 2772e3 - H_FG;
    /** Coolant inlet enthalpy (J/kg) at the inlet temperature. */
    public static final double H_IN = H_F - CP_COOLANT * (T_SAT_C - T_INLET_C);
    /** Liquid density near saturation (kg/m3). */
    public static final double RHO_LIQUID = 750.0;
    /** Steam density at 6.9 MPa (kg/m3). */
    public static final double RHO_STEAM = 36.0;
    /**
     * Slip ratio S in the void-quality relation (steam moves faster than water).
     * S = 1 is the homogeneous model; real channel flow gives 1.2-2.0. S = 1.5 is a
     * representative value, marked {@code to_verify} in the provenance — it is the
     * one number here not taken straight from a source, and it is stated, not hidden.
     */
    public static final double SLIP_RATIO = 1.5;

    /** Nominal total mass flow, 2 circuits x 3 pumps x 8,000 m3/h x 750 kg/m3. */
    public static final double NOMINAL_FLOW_KGS = 2.0 * 3.0 * (8000.0 / 3600.0) * RHO_LIQUID;

    /** Fuel-channel inside diameter (m), for the channel velocity. */
    private static final double CHANNEL_ID_M = 0.088;

    private final int na;
    private final double[] shape;      // axial power fraction per node, sum = 1
    private final double[] enthalpy;   // J/kg
    private final double[] quality;    // steam quality x
    private final double[] voidFrac;   // volume void fraction alpha
    private final double[] coolantC;   // coolant temperature, deg C

    /**
     * @param axialNodes number of axial nodes (the core's 28)
     */
    public ChannelCooling(int axialNodes) {
        this.na = axialNodes;
        this.shape = new double[axialNodes];
        this.enthalpy = new double[axialNodes];
        this.quality = new double[axialNodes];
        this.voidFrac = new double[axialNodes];
        this.coolantC = new double[axialNodes];
        // Start from a cosine, the shape of a bare reactor; the caller replaces it
        // with the live flux shape every step.
        setCosineShape();
    }

    /** Fallback axial shape: a bare-reactor cosine, normalised to sum 1. */
    public void setCosineShape() {
        double sum = 0;
        for (int a = 0; a < na; a++) {
            double z = (a + 0.5) / na;
            shape[a] = Math.cos((z - 0.5) * Math.PI);
            sum += shape[a];
        }
        for (int a = 0; a < na; a++) {
            shape[a] /= sum;
        }
    }

    /**
     * Set the axial power shape (fractions of the total, need not be pre-normalised;
     * this normalises). A non-positive total falls back to the cosine.
     */
    public void setShape(double[] fractions) {
        double sum = 0;
        for (int a = 0; a < na && a < fractions.length; a++) {
            shape[a] = Math.max(0, fractions[a]);
            sum += shape[a];
        }
        if (sum <= 1e-12) {
            setCosineShape();
            return;
        }
        for (int a = 0; a < na; a++) {
            shape[a] /= sum;
        }
    }

    /**
     * Solve the channel for a total power and a mass flow.
     *
     * @param powerFraction core power as a fraction of nominal (1.0 = 3,200 MWt)
     * @param massFlowKgS   coolant mass flow through the core (kg/s)
     */
    public void solve(double powerFraction, double massFlowKgS) {
        double powerW = Math.max(0, powerFraction) * 3200e6;
        double mdot = Math.max(1.0, massFlowKgS);

        // March the enthalpy up the channel. The node's value is the mean over the
        // node, i.e. the enthalpy at the node centre = (energy up to the node centre).
        double cumulative = 0;
        for (int a = 0; a < na; a++) {
            double qNode = shape[a] * powerW;             // W deposited in this node
            double hMean = H_IN + (cumulative + 0.5 * qNode) / mdot;
            cumulative += qNode;
            enthalpy[a] = hMean;

            double x = (hMean - H_F) / H_FG;              // steam quality
            quality[a] = Math.max(0, x);
            voidFrac[a] = voidFromQuality(quality[a]);
            coolantC[a] = hMean < H_F
                ? T_INLET_C + (hMean - H_IN) / CP_COOLANT // subcooled: heats up
                : T_SAT_C;                                // boiling: pinned at Tsat
        }
    }

    /** Void from quality with the slip relation: alpha = 1/(1+((1-x)/x)(rho_v/rho_l)S). */
    public static double voidFromQuality(double x) {
        if (x <= 0) {
            return 0.0;
        }
        if (x >= 1) {
            return 1.0;
        }
        return 1.0 / (1.0 + ((1.0 - x) / x) * (RHO_STEAM / RHO_LIQUID) * SLIP_RATIO);
    }

    // ------------------------------------------------------------- readouts

    /** Volume void fraction per axial node, bottom (0) to top (na-1). */
    public double[] voidFraction() {
        return voidFrac;
    }

    /** Coolant temperature per axial node, degrees C. */
    public double[] coolantTempC() {
        return coolantC;
    }

    /** Core-average void fraction (the number the neutronics feels). */
    public double meanVoid() {
        double s = 0;
        for (int a = 0; a < na; a++) {
            s += voidFrac[a];
        }
        return s / na;
    }

    /** Mean coolant temperature, degrees C. */
    public double meanCoolantC() {
        double s = 0;
        for (int a = 0; a < na; a++) {
            s += coolantC[a];
        }
        return s / na;
    }

    /** Steam quality leaving the channel (top node). */
    public double outletQuality() {
        return quality[na - 1];
    }

    /** Axial-node index where boiling starts (first node with any void), or -1. */
    public int boilingOnsetNode() {
        for (int a = 0; a < na; a++) {
            if (voidFrac[a] > 1e-4) {
                return a;
            }
        }
        return -1;
    }

    /**
     * The channel transit time (s): core height over coolant velocity at the given
     * flow. This is the physical time constant on which the void follows a power
     * change — derived, not chosen.
     *
     * @param massFlowKgS coolant mass flow through the core (kg/s)
     * @param coreHeightM core height (m)
     * @param channels    number of coolant channels
     */
    public static double transitTimeS(double massFlowKgS, double coreHeightM, int channels) {
        double perChannel = massFlowKgS / Math.max(1, channels);
        double area = Math.PI * CHANNEL_ID_M * CHANNEL_ID_M / 4.0;
        double velocity = perChannel / (RHO_LIQUID * area);
        return velocity > 1e-6 ? coreHeightM / velocity : 0.0;
    }
}
