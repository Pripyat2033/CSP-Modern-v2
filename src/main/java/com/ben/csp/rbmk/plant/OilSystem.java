package com.ben.csp.rbmk.plant;

/**
 * The turbine oil system: the oil pumps that supply the bearings and the
 * governing (control) oil. It is its own system and it matters, because losing
 * the main oil pump on a turbine trip is what the emergency oil pump and the
 * accumulators exist to cover — and losing oil pressure trips the turbine
 * (docs/RBMK-PLANT-SYSTEMS.md §5).
 */
public final class OilSystem {

    /** Main oil pump per turbine, plus an emergency (backup) pump. */
    private boolean mainPumpRunning = true;
    private boolean emergencyPumpRunning = false;
    /** Oil pressure (bar); nominal bearing supply ~ 1.5-2 bar. */
    private double pressureBar = 2.0;

    public boolean mainPumpRunning() {
        return mainPumpRunning;
    }

    public boolean emergencyPumpRunning() {
        return emergencyPumpRunning;
    }

    public double pressureBar() {
        return pressureBar;
    }

    /** The main oil pump is driven off the turbine shaft: it stops with the rotor. */
    public void onTurbineTrip(double speedFraction) {
        if (speedFraction < 0.5) {
            mainPumpRunning = false;
            emergencyPumpRunning = true; // the standby pump starts on low pressure
        }
    }

    public void step(double dt) {
        double target = emergencyPumpRunning ? 1.8 : 2.0;
        if (!mainPumpRunning && !emergencyPumpRunning) {
            target = 0.0;
        }
        pressureBar += (target - pressureBar) * Math.min(1.0, dt / 3.0);
    }

    public boolean lowPressureTrip() {
        return pressureBar < 1.0;
    }
}
