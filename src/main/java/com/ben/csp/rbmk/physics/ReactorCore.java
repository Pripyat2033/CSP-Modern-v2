package com.ben.csp.rbmk.physics;

import com.ben.csp.rbmk.RbmkGeometry;

/**
 * The coupled, time-dependent RBMK-1000 core: the model that lets the accident
 * sequence be run. It ties together the validated pieces — point kinetics, the
 * xenon chain, the reactivity feedbacks and the rod-worth curve — into one state
 * that evolves in time.
 *
 * <p>The reactivity is the sum of independent contributions measured against a
 * nominal operating point at which the core is critical (rho = 0):
 * <pre>
 *   rho = rhoExcess                      (the clean core's margin, calibrated)
 *       + [ W(insertion) - W(nominal) ]  (control rods, from the axial worth curve)
 *       + [ void term - nominal ]        (the positive void coefficient)
 *       + [ temperature term - nominal ] (Doppler + graphite, negative)
 *       + xenon worth
 * </pre>
 * This is the structure of an operator-training simulator: a reference critical
 * state plus the deltas that drive the power.
 */
public final class ReactorCore implements ReactorModel {

    private final PointKinetics kinetics = new PointKinetics();
    private final Xenon xenon = new Xenon(1.0);
    private final RodWorth rodWorth = RodWorth.bottomPeaked();

    // --- operating point (the reference critical state) ---
    public static final double NOMINAL_INSERTION = 0.80;
    public static final double NOMINAL_VOID = 0.05;
    public static final double NOMINAL_FUEL_C = 640.0;
    public static final double NOMINAL_GRAPHITE_C = 600.0;

    private double insertion = NOMINAL_INSERTION;
    private double insertionTarget = NOMINAL_INSERTION;
    private double voidFraction = NOMINAL_VOID;
    private double fuelTempC = NOMINAL_FUEL_C;
    private double graphiteTempC = NOMINAL_GRAPHITE_C;
    private double power = 1.0;
    /** Fission-product decay heat: the heat that survives the chain reaction. */
    private final DecayHeat decayHeat = new DecayHeat(1.0);
    private double rhoExcess;
    private boolean autoRod = false;
    private double powerSetpoint = 1.0;
    /** Approximate |dW/dinsertion|, for the auto-operator's gain. */
    private static final double ROD_GAIN = 0.035;
    /** Reactivity demanded per unit power error (the operator's authority). */
    private static final double POWER_GAIN = 0.01;
    /** Integral gain: the АР trims out the steady-state power offset. */
    private static final double INTEGRAL_GAIN = 0.002;
    /** Anti-windup clamp on the integral term. */
    private static final double INTEGRAL_LIMIT = 0.4;
    private double rodIntegral = 0;

    /** Rod drive speed in units of insertion per second: 0.4 m/s over 7 m. */
    private static final double ROD_RATE = RbmkGeometry.ROD_SPEED_M_PER_S / RbmkGeometry.CORE_HEIGHT_M;
    /** Void and temperature time constants (s) — representative. TO VERIFY. */
    private static final double TAU_VOID = 3.0;
    private static final double TAU_FUEL = 10.0;
    private static final double TAU_GRAPHITE = 30.0;
    /** Void per unit power above nominal. */
    private static final double VOID_PER_POWER = 0.08;
    /** Fuel temperature rise per unit power above nominal, °C. */
    private static final double FUEL_PER_POWER = 600.0;

    public ReactorCore() {
        calibrate();
    }

    /**
     * Set the excess reactivity so the core is critical (rho = 0) at the nominal
     * operating point. This is the "reference critical state".
     */
    private void calibrate() {
        rhoExcess = 0;
        double r = rawReactivity(NOMINAL_INSERTION, NOMINAL_VOID, NOMINAL_FUEL_C,
            NOMINAL_GRAPHITE_C, 1.0);
        rhoExcess = -r;
    }

    private double rawReactivity(double ins, double vd, double tf, double tg, double xe) {
        return (rodWorth.worth(ins) - rodWorth.worth(NOMINAL_INSERTION))
            + (ReactivityModel.VOID_COEFF * vd - ReactivityModel.VOID_COEFF * NOMINAL_VOID)
            + (ReactivityModel.DOPPLER_COEFF * (tf - NOMINAL_FUEL_C))
            + (ReactivityModel.GRAPHITE_COEFF * (tg - NOMINAL_GRAPHITE_C))
            + xenonTerm(xe);
    }

    /**
     * Xenon worth relative to the nominal full-power equilibrium. A reference value
     * ({@link PhysicsProvenance} {@code xenon.coeff}, {@code to_verify}).
     */
    private static final double XENON_COEFF = 0.024;

    private double xenonTerm(double xeRatio) {
        return -XENON_COEFF * (xeRatio - 1.0);
    }

    public double reactivity() {
        return rhoExcess + rawReactivity(insertion, voidFraction, fuelTempC, graphiteTempC,
            xenon.xenon() / xenonEquilibrium());
    }

    private double xenonEquilibrium() {
        return new Xenon(1.0).xenon();
    }

    // -------------------------------------------------------------- controls
    public void setRodTarget(double target) {
        this.insertionTarget = clamp01(target);
        this.autoRod = false;
    }

    /**
     * Hand the rods to the automatic regulator (АР), which holds the core critical
     * against the xenon by withdrawing them as the poison builds. This is the
     * operator behaviour that left the core in the low-margin state before AZ-5.
     */
    public void setAutoRegulator(boolean on) {
        this.autoRod = on;
        this.powerSetpoint = power;
    }

    public double insertion() {
        return insertion;
    }

    public double power() {
        // Total thermal power = fission + decay heat. At the reference state this is
        // exactly 1.0 (0.935 fission + 0.065 decay); after a scram the fission term
        // dies but the decay term holds a few percent — the heat that cannot be
        // switched off.
        return DecayHeat.FISSION_SHARE * power + decayHeat.total();
    }

    /** Fission power alone, as a fraction of nominal (the chain reaction). */
    public double fissionPower() {
        return power;
    }

    public double voidFraction() {
        return voidFraction;
    }

    public double fuelTempC() {
        return fuelTempC;
    }

    public double xenonRatio() {
        return xenon.xenon() / xenonEquilibrium();
    }

    // ----------------------------------------------------------------- step
    /** Advance the coupled core by dt seconds. */
    public void step(double dt) {
        if (autoRod) {
            double error = powerSetpoint - power;
            // integral trims the offset: power below setpoint drives rods out
            rodIntegral -= INTEGRAL_GAIN * error * dt;
            rodIntegral = Math.max(-INTEGRAL_LIMIT, Math.min(INTEGRAL_LIMIT, rodIntegral));
            double demand = reactivity() - POWER_GAIN * error + rodIntegral;
            insertionTarget = clamp01(insertion + demand / ROD_GAIN);
        }
        moveRod(dt);

        kinetics.setReactivity(reactivity());
        kinetics.step(dt);
        power = Math.max(0, kinetics.power());

        // The decay heat follows the fission power (it is produced BY the fissions),
        // and does not stop when the chain reaction does.
        decayHeat.step(dt, power);

        // Thermal-hydraulics: void and temperatures follow the TOTAL heat (fission +
        // decay), so a scrammed core still boils and still sits hot.
        double total = DecayHeat.FISSION_SHARE * power + decayHeat.total();
        double voidEq = voidFromPower(total);
        voidFraction += (voidEq - voidFraction) * (dt / TAU_VOID);
        voidFraction = clamp01(voidFraction);
        double fuelEq = tempFromPower(total);
        fuelTempC += (fuelEq - fuelTempC) * (dt / TAU_FUEL);
        graphiteTempC += (fuelEq * 0.95 - graphiteTempC) * (dt / TAU_GRAPHITE);

        xenon.setFlux(Math.max(power, 0));
        xenon.step(dt);
    }

    private void moveRod(double dt) {
        double step = ROD_RATE * dt;
        if (insertion < insertionTarget) {
            insertion = Math.min(insertionTarget, insertion + step);
        } else if (insertion > insertionTarget) {
            insertion = Math.max(insertionTarget, insertion - step);
        }
    }

    /** Steam void as a function of power — more power, more boiling. */
    private double voidFromPower(double p) {
        return clamp01(NOMINAL_VOID + VOID_PER_POWER * (p - 1.0));
    }

    /** Fuel temperature as a function of power. */
    private double tempFromPower(double p) {
        return NOMINAL_FUEL_C + FUEL_PER_POWER * (p - 1.0);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
