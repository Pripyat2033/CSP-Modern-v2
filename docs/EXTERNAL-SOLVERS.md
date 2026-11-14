# External solvers for the energoblok — equipment inventory and solver mapping

Written for Ben, 2026-11-13, in answer to: *"OpenMC/ROSFOND is not the end of the
argument — there must exist external systems (like OpenMC) for the turbines,
feedwater pumps, condensers, heat exchangers, technical water supply and every
other item of plant in a real energoblok. Find what was actually in a 1986
energoblok and find accurate external solvers for that equipment."*

This document does two things:

1. **Part A** — the equipment of a real 1986 RBMK-1000 energoblok, item by item,
   with the Soviet designations, as a **bill of plant**.
2. **Part B** — the **external solver** that models each item, with its real
   licence and an honest note on whether it is freely usable.

It also explains why the current five-line loop is "too short", and what it
becomes when each line is backed by a real solver.

**Source status:** figures are from the sources listed at the end and from
`docs/RBMK-PLANT-SYSTEMS.md` (WP = Wikipedia *RBMK*, WP-CNPP = Wikipedia
*Chernobyl Nuclear Power Plant*, which itself cites the IAEA Bulletin). Anything
not yet pinned to a primary source is marked **TO VERIFY**. Tavily was over quota
during this session, so verification was done against Wikipedia's REST API and
the projects' own sites/GitHub — noted per item.

---

# Part A — What was in a 1986 RBMK-1000 energoblok

An "energoblok" is one reactor plus everything that turns its heat into
electricity. For ChNPP Units 3 & 4 (second-generation RBMK-1000), the items are:

## A1. Reactor island

| Item | Soviet designation | Key figures | Source |
|---|---|---|---|
| Reactor | РБМК-1000 | 1,872 channels (1,661 fuel + 211 control), 3,200 MWt (3,000 nominal) | WP §48, WP-CNPP |
| Graphite stack | — | 14 m dia × 8 m, 250×250 mm blocks, ~1,700 t | WP §36 |
| Fuel channel | ТК | Zr-2.5%Nb, 84 mm ID | WP §1, §46 |
| Control rod | СУЗ / Стержень СУЗ | B4C absorber + graphite displacer | WP |
| Upper biological shield | Схема Е / «Елена» (UBS) | Ø17 m, 3 m thick, ~2,000 t | WP §40 |

## A2. Multiple forced circulation circuit (МЦК)

| Item | Soviet designation | Key figures | Source |
|---|---|---|---|
| Main circulating pump | **ГЦН** (Главный циркуляционный насос) | 5,500–12,000 m³/h; nominal 8,000; 6 kV motor | WP §83, §86 |
| Pump per circuit | — | 4 (3 running, 1 standby); 8 total, 6 running | WP §83 |
| Steam separator (drum) | **Барабан-сепаратор** | 2.8 m dia × 31 m, 10 cm wall, 240 t, ~6.9 MPa | WP |
| Downcomers / suction headers | — | 12 downpipes per separator | WP |
| Group distribution header | ГРК | 22 per circuit, 38–41 channels each | WP |
| Flow control valve (per channel) | — | one per channel | WP §86 |
| Ball flow meter (per channel) | Шариковый расходомер | one per channel | WP §86 |

## A3. Turbine island

| Item | Soviet designation | Key figures | Source |
|---|---|---|---|
| Steam turbine | **K-500-65/3000** | 500 MW, 5 cylinders (1 HP + 4 LP), 3,000 rpm; Kharkiv | WP-CNPP |
| Turbogenerator | **ТВВ-500** (also written TBB-500) | 500 MW, 20 kV, 50 Hz, H2-cooled rotor, water-cooled stator | WP-CNPP |
| Turbogenerators per unit | — | 2 × 500 MW | WP-CNPP |
| Turbine steam flow | — | 82,880 t/h per turbine at 500 MW | WP-CNPP |
| Separator-reheater | ПСП (промежуточный сепаратор-пароперегреватель) | 5 low-pressure separator-preheaters, reheat to 165 °C | WP |
| Condenser | **Конденсатор** | under each LP cylinder | WP |
| Condensate pumps | КН (конденсатные насосы) | 1st and 2nd stage | WP |
| Machine hall | — | 600 m long, shared by units 3 & 4 | WP-CNPP |

## A4. Feedwater / deaeration

| Item | Soviet designation | Key figures | Source |
|---|---|---|---|
| Deaerator | **Деаэратор** | also feedwater storage; 4 per unit | WP |
| Feedwater pump | ПЭН (питательный электронасос) | from deaerators to drums | WP |
| Ion-exchange purifier | — | in condensate and main loop | WP §84, §85 |

## A5. Emergency / safety

| Item | Soviet designation | Key figures | Source |
|---|---|---|---|
| ECCS accumulator | **САОР** | 10 MPa, 12 tanks per unit | WP, repo |
| ECCS pumps | — | high- and low-pressure | WP |
| Diesel generators | ДГ | auto-start < 15 s | WP-CNPP |
| Turbogenerator rundown | — | 45–50 s of essential power | WP-CNPP |
| Gas circuit | — | helium-nitrogen, one per two reactors | WP §47, §78 |
| Control-rod cooling | — | independent water circuit, 40–70 °C | WP §68 |
| SKALA computer | **СКАЛА** | VNIIEM-3 / V-3M; PRIZMA, DREG, KRV, KCTK, KIKD, RAO | Chornobyl Family (repo) |

**Technical water supply** (the Director named it): the condenser cooling-water
system and the service-water (circulating-water) pumps that reject heat to the
cooling pond / spray ponds. This is the **ТВС / техническое водоснабжение**. It
is part of the turbine island's heat sink and must be modelled with the condenser.
**TO VERIFY** its exact pump count and designations against a construction drawing.

---

# Part B — External solvers, mapped to the equipment

Two facts decide everything here:

1. **Solver interoperability is a solved problem.** Codes are coupled by a
   **coupling library** (preCICE) or a **standard interface** (FMI/FMU), or, for
   the code that is already in this project, a **file/API bridge** (the OpenMC
   route: OpenMC writes a result, the front-end reads it). See Part C.
2. **Several nuclear system codes are NOT freely available.** RELAP5-3D,
   TRACE and ATHLET are export-controlled or licence-restricted. The open
   ecosystem (OpenMC, OpenFOAM, MOOSE, Modelica, pandapower, DWSIM) is the
   realistic, distributable path. This is stated honestly per item below.

| Domain | Item it models | Recommended external solver | Licence / access | Open? |
|---|---|---|---|---|
| Neutronics (steady) | core, k_eff, flux | **OpenMC** | MIT-style (MIT/ANL copyright) | **yes** |
| Neutronics (alt.) | core | **Serpent 2** (VTT) | restricted (research licence) | no |
| Coupled neutronics + TH | core + channel | **OpenMC + THM / MOOSE-based** | OpenMC MIT; MOOSE LGPL-2.1 | **yes** |
| System thermal-hydraulics | МЦК loop, drums, ECCS | **RELAP5-3D** (INL) | US export-controlled | no |
| System TH (alt.) | МЦК loop | **TRACE** (US NRC) | NRC code-user licence | no |
| System TH (alt.) | МЦК loop | **ATHLET** (GRS) | commercial licence | no |
| System TH (open alt.) | МЦК loop | **OpenFOAM-based TH + Modelica** | GPL / OSI | **yes** |
| Subchannel (core TH) | fuel channel, DNB | **CTF** (CASL) | open (ORNL) | **yes** |
| Subchannel (alt.) | fuel channel | **SUBCHANFLOW** (KIT) | open | **yes** |
| CFD (3D flow) | pump inlet, header, mixing | **OpenFOAM** | GPL | **yes** |
| Turbine cycle | K-500-65/3000, separators, condensers | **EBSILON Professional** | commercial | no |
| Turbine cycle (open alt.) | turbine island | **Modelica + ThermoPower** | Modelica (free) / ThermoPower | **yes** |
| Turbine cycle (open alt.) | turbine island | **DWSIM** | GPL / CAPE-OPEN | **yes** |
| Heat exchangers | condensers, reheaters | **Modelica ThermoPower / DWSIM / OpenFOAM** | see above | **yes** |
| Electrical (grid, house load) | ТВВ-500, 6 kV boards, rundown | **pandapower** | BSD-3-Clause | **yes** |
| Coupling layer | ties all of the above | **preCICE** | LGPL-3.0 | **yes** |
| Standard model exchange | ties all of the above | **FMI / FMU** | open standard | **yes** |
| Multiphyics framework | host for the above | **MOOSE** (INL) | LGPL-2.1 | **yes** |

## B1. The nuclear-specific codes — the honest access story

- **OpenMC** — MIT-developed Monte Carlo neutron transport; the project already
  uses it. Licence is MIT-style (verified: LICENSE reads "Copyright (c) 2011-2026
  Massachusetts Institute of Technology, UChicago Argonne LLC, and OpenMC
  contributors"). **Freely usable.** This is the neutronics solver.
- **Serpent 2** (VTT, Finland) — continuous-energy Monte Carlo; the alternative
  neutronics code. **Restricted research licence**, not open. Keep as a
  cross-check only.
- **RELAP5-3D** (Idaho National Laboratory) — the classic system thermal-hydraulic
  code; models a whole coolant loop (pumps, drums, pipes, ECCS). **US
  export-controlled**, distribution restricted. The "right" tool for the МЦК
  loop, but you cannot simply ship it.
- **TRACE** (US NRC) — best-estimate system TH; **NRC code-user licence**.
- **ATHLET** (GRS, Germany) — system TH; **commercial licence**.

**Consequence:** the МЦК loop's *correct* solver (RELAP5-3D / TRACE / ATHLET) is
not freely distributable. The open path is a **Modelica + OpenFOAM** thermal-
hydraulic model, or a MOOSE-based TH application, validated against published
RBMK data. That is more work than dropping in RELAP5, but it is the path that can
be shared with students — which is the project's stated purpose.

## B2. The turbine island — where the current loop is shortest

The five-line loop shows `turbines: 1000 MWe @ 3000 rpm` and `condensers:
rejecting 2200 MW` as **numbers**. The real physics of the turbine island is a
**thermodynamic cycle**: steam at the drum leaves at ~6.9 MPa, expands through
five cylinders with reheat, exhausts to the condenser, and the condensate is
pumped back. That is a **rankine-cycle** calculation, and it has real solvers:

- **EBSILON Professional** (Steag/Iqony) — industry-standard thermodynamic cycle
  simulation for power plants (verified: "Thermodynamic cycle simulation software
  for energy plants"). **Commercial.** The "right" tool; not free.
- **Modelica + ThermoPower** (Francesco Casella, Politecnico di Milano) —
  open-source library of power-plant components (turbines, heat exchangers,
  pumps, drums) in the Modelica language. **Free.** The realistic open path for
  the K-500 cycle.
- **DWSIM** — open-source CAPE-OPEN chemical/process simulator (verified on
  Wikipedia; GPL). Good for the heat-exchanger and feedwater train.
- **OpenFOAM** — for the *3-D* parts: pump inlet flow, header distribution,
  condenser tube-side mixing, where a lumped model is not enough.

## B3. Electrical

- **pandapower** — open-source power-system modelling, load-flow and
  short-circuit analysis (verified: BSD-3-Clause; "open source tool for power
  system modeling, analysis and optimization"). Models the 6 kV essential boards,
  the ТВВ-500 generators, and the **turbogenerator rundown** (the 45–50 s that
  powered the pumps on the night of 26 April — directly relevant to the accident).

---

# Part C — How the solvers work together (the coupling architecture)

The project already proved the pattern: **OpenMC computes the neutron physics;
the front-end reads the result.** That is a *file/API bridge*, and it generalises.

Three coupling mechanisms, in increasing order of tightness:

1. **File / API bridge (loosest, simplest).** Each solver runs, writes a result
   (JSON/CSV), the conductor reads it. This is what OpenMC does now. Good for
   slow, offline physics (neutronics, cycle design). **Start here.**
2. **FMI / FMU (standard model exchange).** A solver is exported as a Functional
   Mock-up Unit and stepped by a master. Standard, tool-agnostic. Good for the
   dynamic plant (pumps, drums, turbines) on a common time step.
3. **preCICE (tight, partitioned multi-physics).** An open coupling library
   (verified: LGPL-3.0; "open-source coupling library for partitioned multi-physics
   simulations... couples existing programs/solvers") with quasi-Newton
   acceleration and data mapping. For the tightest couplings — e.g. conjugate
   heat transfer between the graphite stack and the coolant.

**The conductor.** `SimulationConductor` in the code is the intended hub. It
should orchestrate: neutronics (OpenMC) → channel TH (CTF/Modelica) → МЦК loop
(Modelica/OpenFOAM TH) → turbine cycle (Modelica ThermoPower) → electrical
(pandapower) → back to the core. That is the "many solvers in concert" the
Director means, and the seam is the same one OpenMC already uses.

## C1. What the five-line loop becomes

| Now (one summary line each) | With external solvers |
|---|---|
| `core: power 0.995` | OpenMC k_eff + spatial flux → per-channel power |
| `pumps: 48,000 m³/h` | pump-curve model (Modelica) + МЦК loop pressure drop; cavitation check |
| `drums: 6.9 MPa` | drum mass/energy balance, level control, steam quality ~15% |
| `turbines: 1000 MWe` | K-500 Rankine cycle (ThermoPower/EBSILON), reheat, 82,880 t/h |
| `condensers: 2200 MW` | condenser heat-exchanger model + **technical water supply** heat sink |

The loop is short because it is a **steady-state summary**. Backed by real
solvers it becomes a **transient system of systems** — which is exactly what is
needed to answer the project's founding question: *what was done differently on
the night of 26 April 1986?* The turbine rundown, the pump behaviour as the flow
dropped, the drum level — all of these are transient plant behaviour, and they
need the real solvers, not a summary line.

---

# Part D — Recommendation, and the honest caveats

1. **Keep OpenMC as the neutronics solver** (already done, MIT-licensed).
2. **Adopt Modelica + ThermoPower as the open turbine/heat-exchanger/МЦК
   platform**, with OpenFOAM for the 3-D spots and pandapower for the electrical
   side. This is the fully open, distributable path — the one that serves the
   teaching purpose.
3. **Use preCICE or FMI/FMU as the coupling layer**, and make `SimulationConductor`
   the master that drives them.
4. **Treat RELAP5-3D / TRACE / ATHLET / EBSILON as validation references, not
   dependencies** — they are restricted, but published results can calibrate the
   open models.

**Caveats, stated plainly:**

- The equipment list in Part A is sourced to Wikipedia's *RBMK* and *Chernobyl
  Nuclear Power Plant* articles (the latter citing the IAEA Bulletin) and to the
  project's own docs. The **technical water supply** details and the exact pump
  designations need a construction drawing — marked **TO VERIFY**.
- The restricted codes (RELAP5-3D etc.) are the *right* tools for the МЦК loop;
  the open alternatives are a **substitute**, not an equal. This is a real
  trade-off, and it is the Director's call whether to model the loop in open
  Modelica/OpenFOAM or to validate against published RELAP5 results.
- No single open solver covers the whole energoblok. The **architecture is
  several solvers in concert** — the Director is right — and the work is in the
  coupling, not in any one code.

---

## Sources

* **WP** — Wikipedia, *RBMK* (section numbers as of 2026-11-10). Retrieved via
  the Wikipedia REST API this session. https://en.wikipedia.org/wiki/RBMK
* **WP-CNPP** — Wikipedia, *Chernobyl Nuclear Power Plant* (cites IAEA Bulletin
  22(2), Zheludev & Konstantinov). Retrieved via the Wikipedia REST API.
  https://en.wikipedia.org/wiki/Chernobyl_Nuclear_Power_Plant
* **OpenMC** — openmc-dev/openmc; MIT-style licence (LICENSE: MIT / UChicago
  Argonne). https://github.com/openmc-dev/openmc
* **preCICE** — preCICE.org; LGPL-3.0; "open-source coupling library for
  partitioned multi-physics simulations". https://precice.org/
* **pandapower** — BSD-3-Clause; power-system analysis.
  https://github.com/e2nIEE/pandapower
* **OpenFOAM** — GPL; CFD toolbox. https://github.com/OpenFOAM/OpenFOAM-dev
* **MOOSE** — LGPL-2.1; INL multiphysics framework.
  https://github.com/idaholab/moose
* **ThermoPower** — Modelica power-plant library (F. Casella).
  https://github.com/casella/ThermoPower
* **EBSILON Professional** — commercial thermodynamic cycle simulation.
  https://www.ebsilon.com/en/
* **DWSIM** — open-source CAPE-OPEN process simulator.
  https://en.wikipedia.org/wiki/DWSIM
* **Serpent 2** — VTT Monte Carlo transport (restricted).
  https://en.wikipedia.org/wiki/Serpent_(software)
* **RELAP5-3D** — INL system TH (export-controlled).
  https://en.wikipedia.org/wiki/RELAP5-3D
* Cross-checked against `docs/RBMK-PLANT-SYSTEMS.md` and
  `docs/RBMK-MACHINE-GEOMETRY.md` in this repo.
