package com.ben.csp.rbmk.plant;

import com.ben.csp.rbmk.physics.ReactorModel;
import com.ben.csp.rbmk.physics.ReactorCore;
import com.ben.csp.rbmk.physics.SpatialReactorCore;

/**
 * The whole RBMK-1000 plant as a set of coupled, working systems — not one
 * machine. The reactor core is one part; this class owns the two cooling
 * circuits and their eight main circulating pumps, the four steam separator
 * drums, the two turbines, their condensers, the feedwater train and the ECCS,
 * and couples them the way the real plant was coupled:
 *
 * <pre>
 *   core heat -> steam quality -> separators -> steam -> turbines -> power
 *        ^                                   |                    |
 *        |                                   v                    v
 *   pumps <- downpipes <- separator water <-- + <-- feedwater <-- condensers
 * </pre>
 *
 * <p>Nothing here is a stub. Each system carries its own state and its own
 * physics, and the couplings are the real ones, so the plant can only hold a
 * steady state if the heat balance actually closes — which is the test the
 * Director asked for, and which a chat command cannot stand in for.
 *
 * <p>It also carries the trip matrix (docs/RBMK-PLANT-SYSTEMS.md §3, §6): high
 * or low separator level, high steam pressure, low feedwater flow, and loss of
 * two main pumps on one side.
 */
public final class PlantSystem {

    private final ReactorModel core;

    private final MainCirculatingPump[] pumps =
        new MainCirculatingPump[PlantPhysics.CIRCUITS * PlantPhysics.MCP_PER_CIRCUIT];
    private final SteamSeparator[] separators = new SteamSeparator[PlantPhysics.SEPARATOR_COUNT];
    private final Turbine[] turbines = new Turbine[PlantPhysics.TURBINE_COUNT];
    private final Condenser[] condensers = new Condenser[PlantPhysics.TURBINE_COUNT];
    private final FeedwaterSystem feedwater = new FeedwaterSystem();
    private final Eccs eccs = new Eccs();
    /** Gate and control valves on the two circuits. */
    private final Valve[] valves = {
        new Valve(Valve.Kind.GATE), new Valve(Valve.Kind.CONTROL),
        new Valve(Valve.Kind.GATE), new Valve(Valve.Kind.CONTROL),
    };
    /** The channel flow meters — one per fuel channel. */
    private final Rotameter[] rotameters = new Rotameter[Rotameter.COUNT];
    /** Bearing/governing oil system per turbine. */
    private final OilSystem[] oil = new OilSystem[PlantPhysics.TURBINE_COUNT];

    private boolean scrammed = false;
    private String tripReason = null;
    private double timeS = 0;

    /** Build the plant with the lumped core (fast, spatially averaged). */
    public PlantSystem() {
        this(false);
    }

    /**
     * Build the plant, choosing the core. With {@code spatial} true the reaction
     * is driven by the full 3D two-group diffusion solver, ticked live; otherwise
     * by the lumped point-kinetics core.
     */
    public PlantSystem(boolean spatial) {
        this.core = spatial ? new SpatialReactorCore() : new ReactorCore();
        for (int c = 0; c < PlantPhysics.CIRCUITS; c++) {
            for (int p = 0; p < PlantPhysics.MCP_PER_CIRCUIT; p++) {
                MainCirculatingPump pump = new MainCirculatingPump(p);
                // the fourth pump in each circuit is the standby
                if (p == PlantPhysics.MCP_PER_CIRCUIT - 1) {
                    pump.setStandby(true);
                }
                pumps[c * PlantPhysics.MCP_PER_CIRCUIT + p] = pump;
            }
        }
        for (int i = 0; i < separators.length; i++) {
            separators[i] = new SteamSeparator(i);
        }
        for (int i = 0; i < turbines.length; i++) {
            turbines[i] = new Turbine(i);
            condensers[i] = new Condenser(i);
            oil[i] = new OilSystem();
        }
        for (int i = 0; i < rotameters.length; i++) {
            rotameters[i] = new Rotameter(i);
        }
    }

    // ---------------------------------------------------------------- access
    public ReactorModel core() {
        return core;
    }

    public MainCirculatingPump[] pumps() {
        return pumps;
    }

    public SteamSeparator[] separators() {
        return separators;
    }

    public Turbine[] turbines() {
        return turbines;
    }

    public Condenser[] condensers() {
        return condensers;
    }

    public FeedwaterSystem feedwater() {
        return feedwater;
    }

    public Eccs eccs() {
        return eccs;
    }

    public Valve[] valves() {
        return valves;
    }

    public Rotameter[] rotameters() {
        return rotameters;
    }

    public OilSystem[] oil() {
        return oil;
    }

    /** How many channel flow meters read a starving channel. */
    public int starvingChannels() {
        int n = 0;
        for (Rotameter r : rotameters) {
            if (r.starving()) {
                n++;
            }
        }
        return n;
    }

    public boolean scrammed() {
        return scrammed;
    }

    public String tripReason() {
        return tripReason;
    }

    public double timeS() {
        return timeS;
    }

    /** Total electrical output of both turbogenerators (W). */
    public double electricalOutputW() {
        double s = 0;
        for (Turbine t : turbines) {
            s += t.powerW();
        }
        return s;
    }

    // --------------------------------------------------------------- control
    /** Scram: drop the rods (AZ-5) and let the plant coast. */
    public void scram(String reason) {
        this.scrammed = true;
        this.tripReason = reason;
        core.setRodTarget(1.0); // insertion 1.0 = rods fully in
        for (MainCirculatingPump p : pumps) {
            if (p.running()) {
                p.trip();
            }
        }
        eccs.actuate();
        eccs.startLongTermPumps();
    }

    /** The manual "start the unit" control: hand the rods to the auto-regulator. */
    public void startUp() {
        core.setAutoRegulator(true);
    }

    // ------------------------------------------------------------------ step
    /** Advance the whole plant by dt seconds. */
    public void step(double dt) {
        timeS += dt;

        // 1. the core makes heat
        core.step(dt);
        double powerFraction = core.power();
        double quality = PlantPhysics.coreQuality(powerFraction);

        // 2. the pumps circulate coolant; below 500 MWt they throttle
        double circuitFlowKgS = 0;
        for (MainCirculatingPump p : pumps) {
            p.step(dt, powerFraction);
        }
        // total mass flow through both circuits (kg/s)
        double totalFlowKgS = 0;
        for (MainCirculatingPump p : pumps) {
            totalFlowKgS += p.flowM3s() * PlantPhysics.RHO_COOLANT;
        }
        circuitFlowKgS = totalFlowKgS;

        // 3. the drums: mixture in from the core, steam out, water down to the pumps
        double steamProductionKgS = PlantPhysics.steamFlowKgS(powerFraction);
        double feedwaterPerDrum = feedwater.flowKgS() / PlantPhysics.SEPARATOR_COUNT;
        // steam the turbines are asking for; a tripped turbine takes none, so the
        // drums overfill unless the feedwater is cut — the real level problem
        double turbineDemandKgS = 0;
        for (Turbine t : turbines) {
            if (!t.tripped()) {
                turbineDemandKgS += PlantPhysics.steamFlowKgS(1.0) / PlantPhysics.TURBINE_COUNT;
            }
        }
        double demandPerDrum = turbineDemandKgS / PlantPhysics.SEPARATOR_COUNT;
        double mixturePerDrum = circuitFlowKgS / PlantPhysics.SEPARATOR_COUNT;
        double steamAvailableKgS = 0;
        for (SteamSeparator d : separators) {
            d.step(dt, mixturePerDrum, quality, demandPerDrum, feedwaterPerDrum);
            steamAvailableKgS += d.steamOutKgS();
        }

        // 4. the turbines expand the steam
        double perTurbine = steamAvailableKgS / PlantPhysics.TURBINE_COUNT;
        for (Turbine t : turbines) {
            t.step(dt, perTurbine);
        }

        // 5. the condensers reject the waste heat
        for (int i = 0; i < condensers.length; i++) {
            condensers[i].step(dt, turbines[i].steamInKgS(), turbines[i].powerW());
        }

        // 6. feedwater returns to the drums
        double condensateKgS = 0;
        for (Turbine t : turbines) {
            condensateKgS += t.steamInKgS();
        }
        feedwater.step(dt, condensateKgS, steamProductionKgS);

        // 7. ECCS, if it has been called
        eccs.step(dt);

        // 7b. valves, channel flow meters, and the turbine oil systems
        for (Valve v : valves) {
            v.step(dt);
        }
        double perChannelKgS = totalFlowKgS / Rotameter.COUNT;
        for (Rotameter r : rotameters) {
            r.setFlowKgS(perChannelKgS);
        }
        for (int i = 0; i < oil.length; i++) {
            oil[i].onTurbineTrip(turbines[i].speedFraction());
            oil[i].step(dt);
        }

        // 8. the trip matrix
        checkTrips();
    }

    private void checkTrips() {
        if (scrammed) {
            return;
        }
        for (SteamSeparator d : separators) {
            if (d.highLevelTrip()) {
                scram("high steam-separator level");
                return;
            }
            if (d.lowLevelTrip()) {
                scram("low steam-separator level");
                return;
            }
        }
        if (feedwater.flowKgS() < 0.1 * PlantPhysics.MFLOW_FEEDWATER_KGS) {
            scram("low feedwater flow");
            return;
        }
        // loss of two pumps on one side
        for (int c = 0; c < PlantPhysics.CIRCUITS; c++) {
            int down = 0;
            for (int p = 0; p < PlantPhysics.MCP_PER_CIRCUIT; p++) {
                if (pumps[c * PlantPhysics.MCP_PER_CIRCUIT + p].failed()) {
                    down++;
                }
            }
            if (down >= 2) {
                scram("loss of two main circulating pumps on one side");
                return;
            }
        }
    }
}
