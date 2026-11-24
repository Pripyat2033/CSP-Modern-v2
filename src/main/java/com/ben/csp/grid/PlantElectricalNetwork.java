package com.ben.csp.grid;

import com.ben.csp.rbmk.plant.MainCirculatingPump;

import java.util.ArrayList;
import java.util.List;

/**
 * The plant's electrical network: the buses, the transformers between them, the
 * switchyard, and the loads that hang off it. This is what makes the plant's own
 * equipment <em>switchable</em> the way a real station is — every supply path is a
 * breaker, and every breaker can be open, closed, or opened by protection.
 *
 * <p>The topology is the real one, in four tiers:
 *
 * <pre>
 *   750/330 kV switchyard  <- unit generators (4 units) + national grid
 *        |                        |
 *        | РТСН (reserve)         | ТСН (unit auxiliary)
 *        v                        v
 *   6 kV house sections  (main circulating pumps, 36 MW)
 *        |
 *        | service transformer
 *        v
 *   0.4 kV services
 *   ---
 *   essential 6 kV boards  <- 3 x 5.5 MW diesels (15 s start)
 *   АБП / 220 V DC         <- batteries, no-break (protection, RCPS, SKALA)
 * </pre>
 *
 * <p>The supply rule is deliberately simple and physical: a bus is live if a
 * closed incomer connects it to something that is actually producing. Nothing is
 * live by default, and nothing survives its source.
 */
public final class PlantElectricalNetwork {

    // ------------------------------------------------------------- switchyard
    private final ElectricalBus switchyard =
        new ElectricalBus("sy", "750/330 kV switchyard", 750);

    // --------------------------------------------------------- house sections
    private final ElectricalBus houseSectionA =
        new ElectricalBus("ha", "6 kV house section A", 6);
    private final ElectricalBus houseSectionB =
        new ElectricalBus("hb", "6 kV house section B", 6);
    private final ElectricalBus services =
        new ElectricalBus("svc", "0.4 kV services", 0.4);
    private final ElectricalBus essential6Kv =
        new ElectricalBus("ess", "6 kV essential boards", 6);
    private final ElectricalBus dcBoard =
        new ElectricalBus("dc", "220 V DC (АБП)", 0.22);

    private final List<ElectricalBus> buses = List.of(
        switchyard, houseSectionA, houseSectionB, services, essential6Kv, dcBoard);

    // ------------------------------------------------------------- equipment
    private final UnitGenerator generator;
    private final GridSupply grid;
    private final Transformer tsn;
    private final Transformer rtsn;
    private final Transformer serviceTransformer;
    private final List<DieselGenerator> diesels = new ArrayList<>();
    private final BatteryBank battery;

    /** True once the essential boards have lost voltage and the diesels were called. */
    private boolean dieselCallLatched;

    public PlantElectricalNetwork() {
        this("unit4", 1000.0);
    }

    public PlantElectricalNetwork(String unitId, double generatorRatedMw) {
        this.generator = new UnitGenerator(unitId + ".gen", "Unit generator", generatorRatedMw);
        // Import limit is the switchyard tie capacity, not the grid's strength.
        // TO VERIFY against the plant's electrical single-line diagram.
        this.grid = new GridSupply(unitId + ".grid", 500.0);

        // 63 MVA is sized to carry the house load with margin. TO VERIFY.
        this.tsn = new Transformer(unitId + ".tsn", Transformer.Role.UNIT_AUXILIARY, 63);
        this.rtsn = new Transformer(unitId + ".rtsn", Transformer.Role.RESERVE_AUXILIARY, 63);
        this.serviceTransformer = new Transformer(unitId + ".svc", Transformer.Role.SERVICE, 4);

        // 3 x 5.5 MW, auto-start < 15 s (NUREG-1250 §2.5.3).
        for (int i = 0; i < 3; i++) {
            diesels.add(new DieselGenerator(unitId + ".dg" + (i + 1), 5.5));
        }
        // Battery energy is not stated in the sources on hand; sized so the
        // no-break load rides through the diesel start plus a margin. TO VERIFY.
        this.battery = new BatteryBank(unitId + ".abp", 60.0);

        wire();
    }

    private void wire() {
        // The generator and the grid both land on the switchyard.
        switchyard.addIncomer(generator.breaker().id(), generator.breaker().name());
        // ТСН is tapped off the generator terminals, ahead of the generator
        // breaker — that is why a unit trip does not necessarily kill it at once
        // but a generator trip does. Modelled as: fed from the switchyard.
        tsn.feed(houseSectionA);
        tsn.feed(houseSectionB);
        rtsn.feed(houseSectionA);
        rtsn.feed(houseSectionB);
        serviceTransformer.feed(services);
        // House sections are cross-connected through a tie breaker, as the real
        // plant's are, so one transformer can carry both.
        houseSectionA.addIncomer(tsn.hvBreaker().id() + ".a", "ТСН -> A");
        houseSectionB.addIncomer(tsn.hvBreaker().id() + ".b", "ТСН -> B");
        houseSectionA.addIncomer(rtsn.hvBreaker().id() + ".a", "РТСН -> A");
        houseSectionB.addIncomer(rtsn.hvBreaker().id() + ".b", "РТСН -> B");
        essential6Kv.addIncomer("ess.a", "essential board supply");
        dcBoard.addIncomer("dc.a", "АБП");
    }

    // ------------------------------------------------------------- accessors
    public ElectricalBus switchyard() { return switchyard; }
    public ElectricalBus houseSectionA() { return houseSectionA; }
    public ElectricalBus houseSectionB() { return houseSectionB; }
    public ElectricalBus services() { return services; }
    public ElectricalBus essential6Kv() { return essential6Kv; }
    public ElectricalBus dcBoard() { return dcBoard; }
    public List<ElectricalBus> buses() { return buses; }
    public UnitGenerator generator() { return generator; }
    public GridSupply grid() { return grid; }
    public Transformer tsn() { return tsn; }
    public Transformer rtsn() { return rtsn; }
    public Transformer serviceTransformer() { return serviceTransformer; }
    public List<DieselGenerator> diesels() { return diesels; }
    public BatteryBank battery() { return battery; }
    public boolean dieselCallLatched() { return dieselCallLatched; }

    /** Put a main circulating pump motor on a house section. */
    public PumpMotor addPumpMotor(ElectricalBus bus, String id, MainCirculatingPump pump,
                                  double ratedMw) {
        PumpMotor motor = new PumpMotor(id, pump, ratedMw);
        bus.addLoad(motor);
        return motor;
    }

    /** Put a pump motor on section A or B by index parity, as the real plant splits them. */
    public PumpMotor addPumpMotor(String id, MainCirculatingPump pump, double ratedMw,
                                  boolean sectionA) {
        return addPumpMotor(sectionA ? houseSectionA : houseSectionB, id, pump, ratedMw);
    }

    // ------------------------------------------------------------------ step
    /**
     * Recompute the whole network for this instant and push the result to every
     * load. Supply is resolved top-down: switchyard first, then the transformers
     * that tap it, then the buses they feed, then the loads.
     *
     * @param dt seconds since the last step
     */
    public void step(double dt) {
        // 1. The switchyard. It is live if the unit generator is synchronised, or
        //    if the grid is available through its tie.
        boolean unitUp = generator.isSynchronised();
        boolean gridUp = grid.isAvailable();
        switchyard.setLive(unitUp || gridUp);

        // 2. House sections. Normal supply is ТСН; reserve is РТСН. Either is
        //    enough, which is the whole point of having a reserve.
        double houseDemand = houseSectionA.connectedMw() + houseSectionB.connectedMw();
        double unitSupplyMw = unitUp ? generator.outputMw() : 0;
        double gridSupplyMw = gridUp ? grid.importLimitMw() : 0;

        double viaTsn = tsn.deliver(unitSupplyMw, houseDemand);
        double viaRtsn = rtsn.deliver(gridSupplyMw, houseDemand - viaTsn);
        boolean houseLive = (viaTsn + viaRtsn) > 0 || (unitUp || gridUp);
        houseSectionA.setLive(houseLive);
        houseSectionB.setLive(houseLive);

        // 3. Services hang off the house sections.
        services.setLive(houseLive);

        // 4. Essential boards: live from the house sections if they are up,
        //    otherwise from the diesels once they have started.
        if (houseLive) {
            essential6Kv.setLive(true);
            dieselCallLatched = false;
            for (DieselGenerator d : diesels) {
                if (d.isRunning()) {
                    d.stop();
                }
            }
        } else {
            dieselCallLatched = true;
            for (DieselGenerator d : diesels) {
                d.startSignal();
                d.step(dt);
            }
            double dieselMw = diesels.stream().mapToDouble(DieselGenerator::availableMw).sum();
            essential6Kv.setLive(dieselMw > 0);
        }

        // 5. The DC board is no-break: live unless the battery is flat.
        dcBoard.setLive(!battery.isExhausted());
        if (dcBoard.isLive()) {
            battery.discharge(dcBoard.demandMw(), dt);
        }
    }

    /** Total load on the house sections, in megawatts. */
    public double houseLoadMw() {
        return houseSectionA.demandMw() + houseSectionB.demandMw();
    }

    /** Total connected house load regardless of state, in megawatts. */
    public double connectedHouseLoadMw() {
        return houseSectionA.connectedMw() + houseSectionB.connectedMw();
    }

    /** True if any house section has lost its voltage. */
    public boolean houseSupplyLost() {
        return !houseSectionA.isLive() || !houseSectionB.isLive();
    }

    @Override
    public String toString() {
        return String.format(
            "electrical: switchyard %s, house %.1f/%.1f MW, essential %s, DC %.0f%%",
            switchyard.isLive() ? "live" : "DEAD", houseLoadMw(), connectedHouseLoadMw(),
            essential6Kv.isLive() ? "live" : "DEAD", 100 * battery.stateOfCharge());
    }
}
