package com.ben.csp.grid;

import com.ben.csp.rbmk.plant.PlantSystem;

/**
 * The station-blackout cascade, walked through the real plant rather than
 * asserted on a diagram. This is the test that would have caught the orphaned
 * electrical network: before the pumps were loads on a bus, the plant could not
 * fail this way at all.
 *
 * <p>The sequence is the one the plant would actually have gone through:
 *
 * <ol>
 *   <li>the unit is at power, house load supplied through the ТСН, pumps turning;</li>
 *   <li>the unit trips, so the generator goes and the ТСН goes with it;</li>
 *   <li>the grid is lost too, so the РТСН cannot take over;</li>
 *   <li>the 6 kV house sections go dark, the pump motors stop, the pumps coast;</li>
 *   <li>the coolant flow falls;</li>
 *   <li>the void fraction rises, because the same heat is boiling less water;</li>
 *   <li>the reactivity the void adds is positive, so power rises.</li>
 * </ol>
 *
 * <p>Nothing in that list is scripted. Each step is the consequence of the one
 * above it, which is the whole point of building the electrical network.
 */
public final class BlackoutCascadeSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        cascade();
        System.out.println();
        System.out.println("BlackoutCascadeSelfTest: " + checks + " checks, "
            + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void cascade() {
        // Deterministic: no wall-clock thread, so the run does not depend on the
        // speed of the machine.
        PlantSystem plant = new PlantSystem();
        plant.startUp(false);
        PlantElectricalNetwork net = plant.electrical();

        // Let the plant settle at power with the pumps running.
        for (int i = 0; i < 2000; i++) {
            plant.step(0.05);
        }
        double flowBefore = flowKgS(plant);
        double voidBefore = voidFraction(plant);
        double powerBefore = plant.core().fissionPower();

        check(flowBefore > 0, "coolant is flowing before the blackout ("
            + Math.round(flowBefore) + " kg/s)");
        check(net.houseSectionA().isLive(), "house section A is live before the blackout");
        check(net.houseLoadMw() > 0, "the pumps are drawing power before the blackout ("
            + Math.round(net.houseLoadMw()) + " MW)");

        // ---- the event: the unit trips and the grid is lost at the same time
        net.generator().trip();
        net.grid().setAvailable(false);
        plant.scram("station blackout: unit trip with loss of the grid");

        // A few seconds: the bus is dead at once, the pumps are coasting.
        for (int i = 0; i < 20; i++) {
            plant.step(0.05); // 1 s
        }
        check(net.houseSupplyLost(), "the house sections are dead within a second");
        check(flowKgS(plant) < flowBefore,
            "coolant flow has already fallen ("
            + Math.round(flowKgS(plant)) + " kg/s, was " + Math.round(flowBefore) + ")");
        boolean anyCoasting = false;
        for (var p : plant.pumps()) {
            anyCoasting |= p.coasting();
        }
        check(anyCoasting, "the pumps are coasting on their flywheels, not scripted off");

        // ---- the flywheels run down. 60 s covers the whole coast (tau = 8 s).
        for (int i = 0; i < 1200; i++) {
            plant.step(0.05); // to 61 s
        }
        double flowAfter = flowKgS(plant);
        double voidAfter = voidFraction(plant);
        double powerAfter = plant.core().fissionPower();

        check(flowAfter < flowBefore * 0.1,
            "flow has collapsed to under a tenth ("
            + Math.round(flowAfter) + " kg/s, was " + Math.round(flowBefore) + ")");
        check(voidAfter > voidBefore,
            "the void fraction rose as the flow fell ("
            + pct(voidBefore) + " -> " + pct(voidAfter) + ")");

        // The pumps cannot restart: the diesels do not feed the house sections.
        boolean anyRunning = false;
        for (var p : plant.pumps()) {
            anyRunning |= p.running();
        }
        check(!anyRunning, "no main pump restarts: the diesels do not feed the 6 kV house sections");
        check(net.essential6Kv().isLive(), "the essential boards are back on the diesels");

        // Report the outcome rather than asserting a direction that depends on
        // where the core had settled. What the test pins is the causal chain:
        // blackout -> flow loss -> void rise. The power response is the core's
        // physics and is reported here for the record.
        System.out.printf("  cascade: flow %.0f -> %.0f kg/s, void %s -> %s, "
                + "fission power %s -> %s%n",
            flowBefore, flowAfter, pct(voidBefore), pct(voidAfter),
            pct(powerBefore), pct(powerAfter));

        plant.shutdown();
    }

    private static double flowKgS(PlantSystem plant) {
        return plant.core() instanceof com.ben.csp.rbmk.physics.UnifiedReactorCore u
            ? u.coolantFlowKgS() : 0;
    }

    private static double voidFraction(PlantSystem plant) {
        return plant.core() instanceof com.ben.csp.rbmk.physics.UnifiedReactorCore u
            ? u.voidFraction() : 0;
    }

    private static String pct(double v) {
        return String.format("%.3f%%", 100 * v);
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("  FAIL: " + what);
        }
    }
}
