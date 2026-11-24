package com.ben.csp.grid;

import com.ben.csp.rbmk.plant.MainCirculatingPump;
import com.ben.csp.rbmk.plant.PlantSystem;

/**
 * Self-test for the unit electrical network: that the plant's own equipment is
 * switchable, that the main circulating pumps are real 6 MW loads on a 6 kV bus,
 * and that losing the supply takes the pumps with it in the right order.
 *
 * <p>The last check is the one that matters. It walks the sequence the plant
 * would actually have gone through on a station blackout — supply lost, motors
 * dead, pumps coasting on their flywheels, flow falling — and asserts each step
 * follows from the one before it rather than from a flag.
 */
public final class PlantElectricalNetworkSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        houseLoadIsThirtySixMegawatts();
        motorsAreRealLoads();
        reserveSupplySavesTheUnit();
        stationBlackoutTripsThePumps();
        dieselsCannotCarryTheMainPumps();
        turbinesRundownAtSourcedRate();
        aScrammedPumpStaysScrammed();

        System.out.println();
        System.out.println("PlantElectricalNetworkSelfTest: " + checks + " checks, "
            + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ cases

    private static void houseLoadIsThirtySixMegawatts() {
        PlantSystem plant = new PlantSystem();
        PlantElectricalNetwork net = plant.electrical();

        eq(8, plant.pumps().length, "the unit has eight main circulating pumps");
        eq(36.0, net.connectedHouseLoadMw(), 1e-9,
            "six running pumps at 6 MW is a 36 MW house load");
        eq(36.0, net.houseSectionA().connectedMw() + net.houseSectionB().connectedMw(), 1e-9,
            "the 36 MW is split across the two house sections");
        // each section carries three running motors; the two standby pumps draw
        // nothing until they are started
        eq(18.0, net.houseSectionA().connectedMw(), 1e-9,
            "section A carries three running motors, 18 MW");
        eq(18.0, net.houseSectionB().connectedMw(), 1e-9,
            "section B carries three running motors, 18 MW");
    }

    private static void motorsAreRealLoads() {
        PlantElectricalNetwork net = new PlantElectricalNetwork();
        MainCirculatingPump pump = new MainCirculatingPump(0);
        PumpMotor motor = net.addPumpMotor("t.motor", pump, 6.0, true);

        eq(6.0, motor.demandMw(), 1e-9, "an energised motor draws its rating");
        check(pump.running(), "the pump is running while its motor is energised");

        motor.setEnergised(false);
        eq(0.0, motor.demandMw(), 1e-9, "a de-energised motor draws nothing");
        check(!pump.running(), "losing the motor stops the pump being driven");
        check(pump.coasting(), "the pump is left coasting on its flywheel, not stopped dead");

        motor.setEnergised(true);
        check(pump.running(), "restoring the motor restarts the pump");
    }

    private static void reserveSupplySavesTheUnit() {
        PlantElectricalNetwork net = new PlantElectricalNetwork();
        MainCirculatingPump pump = new MainCirculatingPump(0);
        PumpMotor motor = net.addPumpMotor("r.motor", pump, 6.0, true);

        net.step(0.1);
        check(net.houseSectionA().isLive(), "the house section is live at power");
        check(!net.houseSupplyLost(), "house supply is intact");

        // The unit trips but the grid is there: the reserve transformer carries
        // the auxiliaries, which is exactly what the reserve is for.
        net.generator().trip();
        net.step(0.1);
        check(net.grid().isAvailable(), "the national grid is available");
        check(net.houseSectionA().isLive(),
            "with the unit tripped the РТСН keeps the house section live");
        check(!net.houseSupplyLost(), "a unit trip alone is not a station blackout");
        check(pump.running(), "the pump keeps running on reserve supply");
    }

    private static void stationBlackoutTripsThePumps() {
        PlantElectricalNetwork net = new PlantElectricalNetwork();
        MainCirculatingPump pump = new MainCirculatingPump(0);
        PumpMotor motor = net.addPumpMotor("b.motor", pump, 6.0, true);
        net.step(0.1);
        check(pump.running(), "pump running before the blackout");

        // The real event: the unit trips AND the grid is lost.
        net.generator().trip();
        net.grid().setAvailable(false);
        net.step(0.1);

        check(net.houseSupplyLost(), "losing both supplies is a station blackout");
        check(!net.houseSectionA().isLive(), "house section A is dead");
        check(!motor.isEnergised(), "the pump motor is unpowered");
        check(!pump.running(), "the pump is no longer driven");
        check(pump.coasting(), "the pump coasts down on its flywheel");
        check(net.dieselCallLatched(), "the diesels are called on loss of the boards");
    }

    private static void dieselsCannotCarryTheMainPumps() {
        // The real unit network, with all eight pump motors on it.
        PlantElectricalNetwork net = new PlantSystem().electrical();
        net.generator().trip();
        net.grid().setAvailable(false);

        // Run past the diesel start delay.
        for (int i = 0; i < 200; i++) {
            net.step(0.1);
        }
        check(net.essential6Kv().isLive(), "after 15 s the diesels carry the essential boards");
        check(net.dieselCallLatched(), "the diesel call is still latched");
        check(!net.houseSectionA().isLive(), "the house sections stay dead: the diesels do not feed them");

        double dieselMw = net.diesels().stream().mapToDouble(DieselGenerator::availableMw).sum();
        eq(16.5, dieselMw, 1e-9, "three 5.5 MW sets make 16.5 MW");
        check(dieselMw < net.connectedHouseLoadMw(),
            "the diesels cannot carry 36 MW of main pumps, by design");
    }

    /**
     * A scram trips the pumps while the boards are still live. The plant tick then
     * tells the bus its loads are energised, and a naive "restore the motor" would
     * restart every pump the scram just stopped — undoing AZ-5. Only a pump that
     * the motor itself stopped may be restarted by the motor.
     */
    private static void aScrammedPumpStaysScrammed() {
        PlantSystem plant = new PlantSystem();
        plant.startUp(false);
        for (int i = 0; i < 400; i++) {
            plant.step(0.05);
        }
        check(plant.electrical().houseSectionA().isLive(),
            "the house section is live when the scram happens");

        plant.scram("test: scram with the boards live");
        for (int i = 0; i < 200; i++) {
            plant.step(0.05);
        }

        boolean anyRunning = false;
        for (var p : plant.pumps()) {
            anyRunning |= p.running();
        }
        check(!anyRunning, "a scrammed pump is not restarted by a live bus");
        plant.shutdown();
    }

    private static void turbinesRundownAtSourcedRate() {
        // The rundown constant was 20 s, which the source contradicts (45-50 s).
        double tau = com.ben.csp.rbmk.plant.PlantPhysics.TURBINE_COAST_TAU_S;
        check(tau >= 45.0 && tau <= 50.0,
            "the turbine rundown time constant is the sourced 45-50 s, not the old 20 s");
    }

    // ---------------------------------------------------------------- helpers

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("  FAIL: " + what);
        }
    }

    private static void eq(double expected, double actual, double tol, String what) {
        checks++;
        if (Math.abs(expected - actual) > tol) {
            failures++;
            System.out.println("  FAIL: " + what + " (expected " + expected
                + ", got " + actual + ")");
        }
    }

    private static void eq(int expected, int actual, String what) {
        checks++;
        if (expected != actual) {
            failures++;
            System.out.println("  FAIL: " + what + " (expected " + expected
                + ", got " + actual + ")");
        }
    }
}
