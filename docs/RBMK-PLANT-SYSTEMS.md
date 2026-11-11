# The RBMK-1000 and its plant systems (pre-1986)

Reference for modelling the whole plant at 1:4, not just the core. Every figure is
sourced; anything uncertain is marked **TO VERIFY**. Sources are listed at the end.

This document exists because the RBMK-1000 is **not one machine**. It is thousands
of hand-placed graphite blocks, 1,872 separate channels, two coolant circuits,
eight pumps, four steam drums, two turbines and everything downstream. Each of
those is a system in its own right, and each must be present and behave as it did
in reality. The core is one part of the plant; the plant is the test.

---

## 1. The reactor core (second generation: ChNPP 3/4)

| Quantity | Value | Source |
|---|---|---|
| Active core diameter | 11.8 m | WP §37 |
| Active core height | 7.0 m | WP §37 |
| Graphite stack | cylinder **14 m dia x 8 m tall**, ~1,700 t | WP §36 |
| Graphite block (plan) | **250 x 250 mm** | WP §36 |
| Graphite block (axial) | 200-600 mm, varies by position | WP §36 |
| Channel hole in block | 114 mm | WP §36 |
| Lattice pitch | 250 mm (one block = one cell) | WP §62 |
| Fuel channels | **1,661** (2nd gen; 1st gen 1,693) | WP §48 |
| Control channels | **211** (2nd gen; 1st gen 170) | WP §48 |
| Total channels | **1,872** | WP §48 |
| Pressure tube (ТК) | Zr-2.5%Nb, 84 mm ID, 4 mm wall | WP §1, §46 |
| Fuel rod | Zr-1%Nb, 13.6 mm OD, 0.825 mm wall, 3.64 m | WP §57 |
| Fuel assembly | 2 sub-assemblies x 18 rods, 10.025 m, active 6.862 m | geometry note |
| Fuel mass (stationary) | 192 t | WP §62 |
| Maximum fuel temp | 600 °C | WP §57 |
| Maximum graphite temp | 730 °C | WP §36 |
| Graphite heat (direct) | ~5.5% of fission heat | WP §47 |
| Graphite atmosphere | helium-nitrogen, ~0.2 MPa, inert | WP §35 |

### 1a. The lattice count — RESOLVED (2026-11-10)

The channel count and the geometry did **not** agree, and this was the fidelity
gap. At 250 mm pitch (one block = 0.25 m), a circle holds pi*R^2 cells:

| Circle diameter | cells |
|---|---|
| 11.75 m (47 blocks) | 1,741 |
| 12.00 m (48) | 1,793 |
| 12.25 m (49) | **1,885** |
| 14.00 m (56) | 2,453 |

An 11.8 m circle holds only 1,741 cells, so **1,872 channels cannot fit inside the
11.8 m active region**. The resolution, from the sources:

* the **graphite stack** is **14 m diameter** (WP §36) — 56 blocks, 2,453 cells;
* the channels occupy the inner **49-block circle (12.25 m)** — 1,885 cells, which
  is the smallest 250 mm circle that holds 1,872 channels;
* the **outer ring (49 -> 56 blocks, 568 cells)** is graphite-only: the radial
  reflector, no channels;
* the **11.8 m "active core"** (WP §37) is the fuelled region, inside the channel
  region.

So: 1,872 channels (1,661 fuel + 211 control) in a 12.25 m channel region, inside
a 14 m graphite stack. The block counts are now self-consistent.

**TO VERIFY:** the *spatial map* — which exact cells hold AZ / РР / АР / УСП / fuel.
The counts are exact; the arrangement is approximated (a regular, roughly uniform
pattern) until a core drawing (cartogram) is obtained. WP §113 gives the control
breakdown for Smolensk 3: 24 fast-acting scram (БАЗ), 32 bottom-inserted (УСП),
9 local automatic regulators, 18 local protection, 128 manual = **211**.

---

## 2. The multiple forced circulation circuit (МЦК / MFC)

Two **independent** cooling circuits, each serving one half of the reactor.

| Quantity | Value | Source |
|---|---|---|
| Circuits | 2, one per reactor half | WP §83 |
| Pumps per circuit | 4 (3 running, 1 standby) | WP §83 |
| Total pumps | 8 (6 running) | WP §83 |
| Pump capacity | 5,500-12,000 m3/h; nominal **8,000 m3/h** | WP §86 |
| Pump motor | 6 kV electric | WP §86 |
| Nominal total flow | ~48,000 m3/h (6 x 8,000) | derived |
| Throttled flow (<500 MWt) | 6,000-7,000 m3/h per pump | WP §86 |
| Coolant inlet temp | **265-270 °C** | WP §88 |
| Coolant outlet temp | **284 °C** | WP §88 |
| System pressure | **6.9 MPa** | WP §88 |
| Pressure headers | 1 per circuit | WP §83 |
| Group distribution headers | **22 per circuit**, 38-41 channels each | WP §83 |
| Per-channel flow control valve | yes, one each | WP §86 |
| Per-channel flow meter | ball type | WP §86 |

**Flow path:** lower water lines -> common pressure header (per circuit) -> 22
group distribution headers -> 38-41 pressure channels each -> **boiling in the
core** -> upper steam lines (one per channel) -> steam separators -> downpipes ->
MCP suction -> back to the reactor.

---

## 3. Steam separators (drum separators, барабан-сепаратор)

| Quantity | Value | Source |
|---|---|---|
| Count | **4** (2 per circuit) | WP §83; WN |
| Type | thick horizontal drum | WP §83 |
| Diameter | 2.8 m | WP §83 |
| Length | 31 m | WP §83 |
| Wall thickness | 100 mm | WP §83 |
| Weight each | 240 t | WP §83 |
| Steam pressure | 6.9 MPa | WP §88 |
| Steam quality at outlet | ~15% | WP §84 |
| Downpipes per separator | 12, to MCP suction headers | WP §84 |
| Steam collectors | 2 per separator | WP §84 |
| Location | side compartments above the reactor top | WP §83 |

The separator water level is controlled by feedwater flow, with the deaerators as
the reservoir (WP §91). Trips on high/low separator level, high steam pressure,
low feedwater flow, loss of two MCPs (WP §90). The separator compartment reached
**270 °C** in service and its concrete was found to be shifting (WP CNPP §54) —
a real structural concern at ChNPP 3/4.

---

## 4. Turbines and generators

| Quantity | Value | Source |
|---|---|---|
| Turbogenerators per unit | **2**, each 500 MWe | WP §136 |
| Turbine model | **K-500-65/3000** (block 4: -2 variant) | WP §136, §138 |
| Cylinders | 5: one high-pressure + four low-pressure | WP §85, §136 |
| Speed | 3,000 rpm | WP §136 |
| Steam flow per turbine | **82,880 t/h** | WP §136 |
| Generator | TVV-500 / TBB-500, 20 kV, 50 Hz | WP §136 |
| Generator cooling | stator water, rotor hydrogen | WP §136 |
| Turbogenerator size | 39 m long, 1,200 t | WP §136 |
| LP separators-preheaters | 5 | WP §85 |
| Machine hall length | 600 m | WP §136 |

---

## 5. Condensate and feedwater

| Quantity | Value | Source |
|---|---|---|
| Condenser | after each turbine, steam condensed | WP §85 |
| Condensate path | condenser -> 1st-stage pump -> ion-exchange purifier -> 2nd-stage pump -> deaerators | WP §85 |
| Deaerators | **4 per unit**, also feedwater storage | WP §85 |
| Feedwater temperature | **155-165 °C** | WP §89 |
| Feedwater pumps | to steam separator bottoms | WP §84 |
| Return water from separator | 284 °C (vs 155-165 from feedwater) | WP §89 |

The **feedwater temperature** matters physically: at low power the returning
feedwater is a larger fraction and the inlet temperature rises toward saturation,
which is a real reactivity concern (WP §88-89).

---

## 6. Emergency core cooling (САОР / ECCS)

| Quantity | Value | Source |
|---|---|---|
| Subsystems | 3, tied to the coolant headers | WP §96 |
| Short-term | 2 groups x 6 accumulators = **12 tanks** | WP §97 |
| Accumulator charge | water + nitrogen at **10 MPa** | WP §97 |
| Each group capacity | 50% of max coolant flow to the damaged half | WP §97 |
| Third group | electric pumps from the deaerators | WP §97 |
| Long-term (damaged half) | 3 pairs of pumps from suppression pools | WP §98 |
| Long-term (intact half) | 3 pumps from condensate storage | WP §98 |
| Pump power | essential 6 kV + diesel generators | WP §98 |

---

## 7. Accident localization / pressure suppression

* Reactor cavity overpressure protection: steam relief assemblies in the floor ->
  Steam Distributor Headers -> rupture discs -> **Steam Distribution Corridor**
  (level +6) -> vertical pipes -> **bubbler pools** at levels **+3 and +0**
  (WP §119).
* Capacity: simultaneous rupture of **two** pressure channels; more lifts the
  cover plate ("Structure E", nicknamed "Elena") (WP §119).
* Leaktight compartments: pump rooms 0.45 MPa, headers/inlets 0.08 MPa, reactor
  cavity 0.18 MPa (WP §120).
* Hydrogen removal: 800 m3/h air extraction (WP §122).

---

## 8. Other systems to model

* **Gas circuit** (helium-nitrogen): one circuit serves two reactors; cools the
  graphite via the control-rod channels; moisture/temperature monitored as a
  leak indicator (WP §47, §78).
* **Control rod cooling**: independent water circuit, 40-70 °C (WP §68).
* **Ion-exchange / purification**: in the main loop and in the condensate path
  (WP §84, §85).
* **Electrical**: essential 6 kV boards, diesel generators (auto-start <15 s),
  turbogenerator rundown 45-50 s (WP §132, §136).
* **Oil / lubrication, sealing, valves, rotameters**: each MCP has flow control,
  non-return check, and shutoff valves; each channel a flow-control valve and a
  ball flow meter (WP §86). These are the "small" systems the Director named.
* **SKALA** process computer: two independent VNIIEM-3 machines, each built on a
  V-3M processor with 20K 24-bit words of memory (plus 8K shared). It is the
  reactor's monitoring system; the operator reads it by entering a code on a
  request device and reading the answer. It is seeded with PRIZMA (reactor power
  and rod positioning), DREG (parameter registration — the tape that became the
  accident investigation's primary source), and the monitoring programs KRV
  (channel coolant flow), KCTK (channel integrity), KIKD (detector health) and
  RAO (emergency deviations). Output is on paper (RTA-80 teletypes, ACPU-128 drum
  printers) and MTK mnemonic displays; the pre-1986 machine had **no video
  monitors**. The **DIIS** auxiliary system (and its DIIS-2000 firmware), which
  added video monitors and a faster display path, is a **1990s upgrade** fitted at
  Chernobyl after Unit 4 was destroyed, so it is deliberately not modelled.
  (Chornobyl Family SKALA restoration — the machine was saved from Unit 3. The
  figure of 7,200 analog + 6,500 digital inputs is from WP CNPP §42-43 and still
  needs a primary source.)

---

## 9. Plant layout (Chernobyl Unit 4)

* Reactor hall above the core; UBS ("Schema E" / "Elena"), Ø17 m, 3 m thick,
  ~2,000 t, on 16 rollers (WP §40).
* Control room **below the deaerator room floor**; both between the reactor and
  turbine buildings (WP §112).
* Machine hall (turbine hall) **600 m long**, adjacent to the reactor building
  (WP §136).
* Reactor pit: 21.6 x 21.6 x 25.5 m (WP §34).
* Ventilation stack: ~150 m **TO VERIFY**.

---

## 10. What the simulation must carry (physics per system)

1. **Core**: per-channel, per-node flux, void, temperature (built — RbmkSolver3D).
2. **MFC loop**: coolant enthalpy rise, boiling onset, flow from pump curves.
3. **Steam separators**: mass/energy balance, drum level, steam pressure, steam
   quality ~15%.
4. **Turbines**: steam consumption vs. power (82,880 t/h per turbine at 500 MW).
5. **Condensers**: heat rejection, condensate temperature.
6. **Feedwater**: deaerator level/temperature, feedwater flow to drums.
7. **ECCS**: accumulators (10 MPa, 12 tanks), pumps.
8. **Protection**: the trip matrix (separator level, pressure, feedwater, pumps).

Every one of these is a separate system with its own state; coupling them is the
real test. A chat command cannot stand in for it.

---

## Sources

* **WP** — Wikipedia, *RBMK* (section numbers as of 2026-11-10).
  https://en.wikipedia.org/wiki/RBMK
* **WP CNPP** — Wikipedia, *Chernobyl Nuclear Power Plant*.
  https://en.wikipedia.org/wiki/Chernobyl_Nuclear_Power_Plant
* **WN** — World Nuclear Association, *RBMK Reactors*.
  https://world-nuclear.org/information-library/nuclear-fuel-cycle/nuclear-power-reactors/appendices/rbmk-reactors

Cross-checked against the reactor-physics literature already in
`docs/RBMK-CORE-GEOMETRY.md`.
