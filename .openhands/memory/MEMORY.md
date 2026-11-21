# CSP-Modern-v2 — project memory

## ORM IS PER-ROD NOW; ROD WORTH IS CALIBRATED (commit 0bf2a54, 2026-11-18)
T3 done. ORM comes from the 211 rods in `Cartogram` (1,884 channels transcribed from
the unit 4 SUZ panel SVG: fuel 1,661 + control 211 + detector 12; АЗ 24, ПК-АЗ 24,
АР 12, УСП 24, manual 127). `RodWorth` calibrates on the ONE panel figure — 24 АЗ =
-2 beta ⇒ one АЗ rod = 5.354e-4 dk/k — replacing the hand-set `SCALE = 0.08`; the
group reproduces -2.00 beta exactly. `CORE_ORM`, `CORE_PERIOD` (a log-rate
MEASUREMENT, not `l/rho`), the per-rod limit switches and selsyn lamps all read the
core now; SKALA `K0S0000` answers ORM with the 43/30-rod set points.

**"Rod overmode" — measured, and much smaller than I first wrote.** Driving a manual
rod fully home leaves the top 0.228 m of the core unabsorbed (6.772 m absorber vs
7.0 m core), so its worth gives back a little. MEASURED: the total effect is
**4.17 rods, 2.0% of the margin**, ORM peaking at insertion 0.911 (209.4 rods) and
falling to 205.3 at full home. My earlier "~2.9-rod dip" was wrong; the right number
is 4.17 rods and it is an ORM peak, not a dip. Mesh-refined 28->560 nodes: effect
SURVIVES (-5.2e-6 dk/k at every resolution), so it is real geometry, NOT a
discretisation artefact. Note the peak exists partly because ORM divides by
`unitWorth = worth(RR, x=1.0)`, i.e. the LEAST negative point of the curve.

**ORM IS A 28-STEP STAIRCASE (known limitation).** Worth is integrated on 28 axial
nodes (250 mm each), so 87% of sampled insertion positions produce NO change in ORM
and one node step jumps it by up to 13.7 rods. The margin is therefore quantised at
~7 rods, far coarser than the 43/30-rod trip set points it is compared against.
Needs sub-node (analytic or 10x-refined) integration before ORM trips are trusted.

**The 12 ПК-РР rods have no known positions** (`rod.pk_rr_positions`): the panel draws
all 127 manual rods in one colour. Count + geometry carried, positions NOT guessed.

**Test-harness trap:** `PlantSystem.startUp()` used to spawn a wall-clock physics
thread, making `step()` a no-op — PlantSelfTest was timing-dependent and passed 4/5.
Now `startUp(boolean threaded)`; tests pass `false`. Real physics costs ~3.2 ms per
5 ms step ⇒ a full suite run is ~25 min; use ≥500 s timeouts.

**Open gap:** the three solvers still call the UNIFORM `RbmkGeometry.rodMaterials(double)`
while `RodWorth` uses per-type. Worth is per-type, live flux response is not.

## RBMK RADIAL-FLUX TILT BUG — FOUND & FIXED (commit 59bc0e7, 2026-11-18)
Ben's gut ("the RBMK is simulated wrong") yielded ONE concrete, reproducible bug.
The LIVE 3D fast flux was tilted ~12x to one side in a SYMMETRIC core (L/R=12.75,
mirror error 1.20). Cause: `RbmkLattice.channels()` fit the control count to 211 by
dropping pitch-3 cells ONE AT A TIME by radius; the r=24.19 tie removed BOTH x=-24
rods and left x=+24 unmatched. A one-sided absorber pattern against the reflective
radial BC excites a near-degenerate tilt mode. FIX: drop whole symmetry ORBITS. Now
control is 211/211 mirror-symmetric; tilt L/R=1.24, mirror error 0.148 (8x better).
`RbmkGeometrySelfTest` now FAILS on any unpaired rod. A slow residual tilt (1.24->
1.79/60k steps) is genuine physics (weak radial coupling, reflective BC), not a bug.

## "IS IT A TABLE / MOVIE SET?" — ANSWERED EMPIRICALLY (2026-11-18)
Ben's recurring fear: a lookup table = finite rows/cols = lossy/scripted. The answer,
now locked by `DiscriminationSelfTest` (12 checks): the table holds **110 numbers on the
VOID AXIS ONLY** (11 void states x 10 group constants). Temperature (fuel+graphite),
xenon, and the 3D flux SHAPE are NOT tabulated — they are computed LIVE each tick. The
test drives the live solver with a spatially NON-UNIFORM core it was never built around:
voiding the right half raises k and moves the flux centroid +11.0; the left half gives
the mirror image; asymmetric rods tilt the flux to the withdrawn side; the tilt is
GRADED (0.2->+7.78, 0.8->+10.99). None of that is scripted — it EMERGES from diffusion.

**BONUS BUG (same family as 59bc0e7): fuel channels were not mirror-symmetric.** Disc =
1885 cells, core = 1872 channels, 13 spare dropped ONE AT A TIME by radius -> 3 unpaired
(-23,-8),(-8,-23),(-23,8). 13 is ODD and no rim cell is on x=0, so a fully symmetric drop
is IMPOSSIBLE — exactly ONE channel must stay unpaired. FIX: drop in whole x-mirror
orbits in `RbmkLattice.channels()` step 3. Unpaired 3->1; uniform centroid -0.029->+0.004.
`RbmkGeometrySelfTest` now 33 checks (asserts unpairedFuel==1). Commits: d06234a, a46a9b4.

## TWO RULE-2 FIXES LANDED (2026-11-18, later) — all live-path self-tests pass
1. **Control-material consistency (HIGH).** `RbmkTransientSolver.setRodInsertion` built
   rods from HAND-SET `RbmkXsec.GRAPHITE/B4C` while `RbmkSolver3D` used the DERIVED
   table. B4C sa2 was 0.45 vs derived 36.31 — **81x too small** (hand-checked: inf-
   dilute Σa(B-10)=84 cm^-1; derived 36.3 is self-shielded, ratio 2.3). Live rods were
   ~80x weak. FIX: both solvers now use `controlCell(controlGraphite()/WATER/controlB4C())`.
2. **Divergence ceiling (SMALL).** `RbmkSolver3D.KEFF_CEILING=10.0`; `stepOnce` flags a
   huge FINITE k_eff (old guard caught only non-finite).
New `RodMaterialConsistencySelfTest` (6 checks) locks both in.
**Consequence (real, correct):** the live core now CAN shut down. 26-April end power
10.861->8.549 (peak +48.3% survives); k_bal now reaches 0.97 on full scram. Recalibrated
`ExperimentVariantsSelfTest` ("not shut down" = >3x nominal AND >5x normal run, not
"still rising"). **ALL THREE old known-failures are now FIXED: `SpatialReactorCoreSelfTest`
12/12, `OpenMcBridgeSelfTest` 11/11, `RbmkSpatialSelfTest` 18/18.** The last one
failed because it compared the (0,0) cell — a CONTROL channel — against a FUEL cell
at the rim; replaced with radial region means. The ~0.1% margin is CORRECT: with the
reflective BC a homogeneous core is EXACTLY flat, so do not "fix" a small margin by
adding leakage.

**FULL MC-FREE SUITE IS GREEN: 20 tests, 218 checks, 0 failures** (2026-11-18). The
last two fixes (AZ-5 `AccidentSequence`, `RbmkSpatialSelfTest`) were both BAD
ASSERTIONS, not bad physics — check WHAT a failing assertion measures before
touching the model.

**STILL OPEN audit items (report, don't guess):** fuel temp is a power-only scalar (no
flow, no per-node) + unregistered TAU_FUEL/TAU_GRAPHITE/FUEL_PER_POWER; feedback radially
uniform; coolant temp not fed to neutronics; graphite-temp link GATED OFF until the
OpenMC table ships. Full detail in `.openhands/memory/2026-11-18.md`.

**TOOLCHAIN trap (3rd restart):** the sandbox loses JDK + GRADLE_USER_HOME + gradle
distro on restart; only `build/classes/**` survives. Re-install: `sudo apt-get update &&
sudo apt-get install -y openjdk-21-jdk-headless`. To compile/test WITHOUT gradle (avoids
the loom lock entirely): gson is at
`/workspace/mcserver/libraries/com/google/code/gson/gson/2.10/gson-2.10.jar`; compile the
Minecraft-free set = `rbmk/{RbmkCore,RbmkGeometry,RbmkLattice,RodType}.java` +
`rbmk/physics/*.java`; run with `-cp /tmp/out:src/main/resources:<gson>` (resources MUST
be on the classpath or the lattice loads as "unknown").

## FABRIC VERSIONS — there is NO "Fabric 1.16" (2026-11-18)
The mod is MC **1.20.1**: gradle.properties pins loader 0.15.11, yarn 1.20.1+build.10,
Fabric API 0.91.0+1.20.1. Live Fabric meta: loader latest 0.20.1; API for 1.20.1 up to
0.92.9+1.20.1. Test server uses loader 0.16.10 + API 0.92.12. Pinned values are valid,
just conservative.

## THE MATERIALS GAP -- Ben's THIRD gut-catch, PROVEN (2026-11-17)
Ben said (no jargon): "I have a gut feeling we're subtly making a huge mistake
and it'll affect the reactor... I see OpenMC as suspicious... shouldn't this be a
chain of systems?" He was RIGHT AGAIN. Two findings:
1. THE GRAPHITE TEMPERATURE LINK IS DEAD IN THE UNIFIED CORE. `graphiteTempC` is
   computed (UnifiedReactorCore L127, L345) but NEVER READ -- not passed to the
   solver, no getter. So graphite heating has ZERO effect on reactivity in the
   live core. The positive graphite coefficient (a real RBMK destabilising
   property, part of the accident) is INERT. `ReactivityModel.GRAPHITE_COEFF` is
   only used by the RETIRED ReactorCore.
2. THE GRAPHITE COEFFICIENT HAS THE WRONG SIGN IN CODE: -2.0e-5 vs INSAG-7
   Table II-I +6e-5 and NUREG-1250 sec 2.3.2.2 +3.2e-5..+6e-5, both POSITIVE.
   provenance `feedback.graphite_coeff` status=to_verify, documents the conflict.
   PROVEN BY OPENMC (new script openmc/rbmk_graphite_coeff.py): sweep graphite
   600->1200 K at fixed fuel/coolant 600 K -> DERIVED coeff = +1.3e-5, POSITIVE.
   Independent Monte Carlo agrees with the two agencies. The code's sign is wrong.
   (Data limit: graphite natural-carbon HDF5 ships at 600/900/1200 K; use those.)
- FIX BUILT (commit e097ad0, 2026-11-18). graphiteTempC now feeds the neutronics:
  RbmkGraphiteTemp scales the homogenised cell constants by measured slopes, gated
  to the MATCHING lattice table (see below); ReactivityModel.GRAPHITE_COEFF flipped
  to +6e-5. Details in 2026-11-18.md + docs/DERIVED-NOT-FITTED.md 5c.
- **THE MATCHED-PAIR RULE (the durable lesson).** A derived per-constant slope is a
  FRACTIONAL change of the table it was measured from, so it is only valid against
  THAT table. The graphite slopes (from the OpenMC cell) flip the whole-core effect
  NEGATIVE against the shipped 1-D table. Hence `RbmkGraphiteTemp.matchesLattice()`
  gates the link on `LatticeTable.source().startsWith("openmc")`. **The graphite link
  and the pending OpenMC-table shipment (Step 1b) are a matched pair — ship together
  or the link is inert.** Generalise: any derived scaling must name its source table.
- **S(alpha,beta) tables top out at 800 K** (c_H_in_H2O and c_Graphite both). OpenMC
  temperature interpolation covers the free-gas XS but NOT the thermal law, so the
  graphite slope is a 600-800 K chord, anchored at the nominal 600 degC operating
  point in Java (factor = 1 there). Magnitude is to_verify (nonlinear ss21/ss12).
- GEMINI "chain of solvers" question: the project ALREADY is that chain
  (PlantSystem 8-stage loop; RbmkSolver3D deterministic live; OpenMC offline only).
  See the "SEPARATE SOLVERS" entry. The real gap is the graphite link above, NOT
  an architecture change. Do not add an external backend (already decided).
## BEN'S "SEPARATE SOLVERS" QUESTION (Gemini advice), answered 2026-11-17
Ben (via Gemini) proposed: don't use Monte Carlo live; use deterministic solvers;
split into modular interacting solvers (neutronics / thermal-hydraulics /
structural). Checked against the repo -- THE PROJECT ALREADY DOES ALL OF THIS:
- Neutronics live = RbmkSolver3D (deterministic 3D 2-group diffusion), NOT Monte
  Carlo. OpenMC is NOT in the live path (grep confirms: zero refs in the core).
- Multi-physics coupling = PlantSystem.step(): an 8-step loop (core -> pumps ->
  separators -> turbines -> condensers -> feedwater -> ECCS -> trip matrix). This is
  the MOOSE-style "separate solvers talking in a loop" Gemini describes.
- Energy groups are a DATA property, not hard-coded: RbmkSolverMG runs N groups
  (G=2 reproduces RbmkSolver3D exactly). So "nodal/multi-group" is already possible.
- Thermal-hydraulics = ChannelCooling (channel energy balance, quality, slip), not
  DNS. DNS is a research tool for single subchannels; real plant codes use subchannel
  methods -- so Gemini overstates DNS as "the cutting edge" for whole-core T-H.
- WHERE GEMINI IS RIGHT AND IT MATTERS: "use OpenMC once OFFLINE to generate the
  baseline data matrices, then let the modular solvers interpolate them in real time"
  -- THAT IS EXACTLY OUR STEP-1 PLAN. It independently validates it.
- THE SUBTLE WRONGNESS: Gemini frames it as "stop using Monte Carlo for real-time".
  We never did. Our open bug is the OFFLINE data SOURCE: the live table is built
  from the 1-D cell, not the validated OpenMC cell. So the fix is not "remove Monte
  Carlo" -- it is "use the RIGHT offline source to build the table".
- GENUINE GAP FOUND: no structural/materials solver (thermal expansion, graphite
  swelling). Gemini names it; we don't have it. Worth noting, not urgent.
- EXTERNAL BACKEND vs in-process: already researched (docs/ENGINE-DECISION.md +
  docs/RESEARCH-minecraft-custom-engines.md). Decision: in-process is right (the
  mod IS the alongside case, ~10x headroom). Do not re-litigate.
## STEP 2 DONE: the void-worth dial is now VISIBLE (commit ca9f5ac, 2026-11-17)
- `RbmkXsec.voidWorthScale()` exposes the dial (0.233). New overloads
  `RbmkXsec.fuelCell(...,voidWorthScale)` and `RbmkSolver3D.setCoreState(...,dial)`
  let a caller solve with the dial ON (shipped) vs OFF (raw cell).
- In-game `/rbmk dial` prints the dial and measures the SAME 3D core both ways.
- `VoidDialSelfTest` (4 checks) proves the dial is LOAD-BEARING and is written to
  tell us when it becomes unnecessary. MEASURED: dial ON = +0.0216 (ships);
  dial OFF (raw cell) = +0.0770; raw/shipped = 3.56x = the physical error cancelled.
- provenance `xsec.void_worth_scale` now carries a `visible` note. Physics UNCHANGED
  (default path still uses the dial). checkProvenance green; UnifiedReactorCore 6/0,
  PhysicsProvenance 31, LatticeTable 14.
- NEXT = STEP 1 of the plan (the real fix): regenerate rbmk_lattice.json from the
  CORRECTED OpenMC cell (openmc/rbmk_cell.py) so the raw number approaches +0.020
  and the dial can be deleted. Needs an OpenMC -> 2-group-constants emitter (none
  exists; openmc/rbmk_cell.py builds the model but does not emit the table).
- PUSH still blocked: OpenHands secrets endpoint returns HTTP 500; 10 commits local.
## "IS IT JUST A TABLE?" -- Ben's challenge, answered 2026-11-17
Ben (not a programmer) asked whether a reactor that runs on "a table" is a real
dynamic sim or just a scripted model. Honest answer, both halves:
- IT IS NOT A TABLE OF OUTCOMES. There is no "if power=X set reactivity=Y" anywhere.
  `LatticeTable` is a table of MATERIAL PROPERTIES (two-group cross sections at 11
  void fractions, linearly interpolated) -- the same kind of data every real reactor
  code reads from a nuclear-data library. The lookup is of *material*, not of *answer*.
- WHAT ACTUALLY RUNS: `RbmkSolver3D` is a real 3D two-group neutron-DIFFUSION solver.
  Mesh ~1793 channel cells x 28 axial = ~50,204 nodes, red-black Gauss-Seidel with
  over-relaxation (omega=1.5 safe, 1.85 diverges), the FLUX is the state, converged
  every step. `balanceKeff()` gives production/loss. This is a genuine solver, not a
  curve.
- FEEDBACK IS CLOSED AND EMERGENT: physicsStep -> void+fuelTemp into xsec -> flux ->
  power -> heat -> ChannelCooling (energy balance, quality, slip) -> void -> back.
  Xenon iodine pit included. The accident is NOT scripted: `NoAccidentModeSelfTest`
  reflection-scans for any field named accident/mode/experiment and drives the SAME
  object from nominal to runaway by ordinary rod moves only. Real self-test, passes.
- WHERE BEN IS RIGHT: the core is a real solver but its MATERIAL DATA comes from the
  wrong (1-D) cell AND its feedback MAGNITUDE is DIALED by hand. Live hand-set dials:
  VOID_WORTH_SCALE=0.233, DOPPLER_REMOVAL_PER_K=1e-5, XENON_MACRO_ABS=1.34e-4, rod
  mix 0.75 graphite/0.25 absorber. (VOID_FEEDBACK_SCALE=0.41, DOPPLER_FEEDBACK_SCALE
  =0.020 are FALLBACK-only.) So: real equations, tuned coefficients -- a flight sim
  with real aero equations but hand-tuned stability derivatives. The ENGINE is honest;
  some DIALS are not yet sourced. That is precisely the gap the plan closes.
## THE REAL OPEN THREAD: the reactor DIALS the void number, it does not DERIVE it
Found 2026-11-17 by tracing where the shipped core's neutron data comes from. This
is what Ben was sensing -- "we shouldn't HAVE an issue".
- THE CHAIN: src/main/resources/rbmk_lattice.json (the 2-group table the core runs
  on) is generated by tools/lattice/emit_constants.py -> rbmk_cell1d.py, i.e. the
  DETERMINISTIC 1-D CELL. Confirmed: table k_inf@void0 = 1.1331 == compare_curves
  "fast" 1.13294. The root-cause doc itself says that 1-D cell is ~3.3x too high.
- The 3D solver runs on that table and gives a WHOLE-CORE void worth of +0.086 =
  4.3x INSAG-7 (+0.020). So the error is NOT only the missing rods -- it is baked
  into the source neutron data.
- `RbmkXsec.VOID_WORTH_SCALE = 0.233` (hand-set) blends the cell toward zero-void so
  the core READS +0.022 ~ INSAG-7. So the reactor reads the right number because a
  hand factor cancels a 4.3x physical error. The SHAPE is derived; the MAGNITUDE is
  dialled. This is the "two errors cancel" trap one level up.
- WHY IT MATTERS: change the table (regenerate it) and the hand factor silently goes
  wrong. The number is right by construction, not by physics -- exactly what the
  project's SGB rule forbids.
- MEASURED today (shipped core, reactivity convention): rods nominal(0.80) +0.0216;
  rods out(0.00) +0.0244 -- both ~lit only because of the scale.
- THE CLEAN FIX: regenerate rbmk_lattice.json from the CORRECTED OpenMC cell
  (openmc/rbmk_cell.py, explicit 18-rod cluster, +0.032..0.038) instead of the 1-D
  cell; then the raw number is right and the hand scale can shrink or go. Needs an
  OpenMC -> 2-group-constants emitter (none exists yet; openmc/rbmk_cell.py builds
  the model but does not emit the table). THAT is the next real task.
## "WILL WE EVER NEED THE OLD CORES BACK?" — investigated 2026-11-17
Ben asked before deleting anything. Findings (all verified by grep/run today):
- THE PHYSICS ENGINES ARE SHARED, NOT AT RISK. Deleting the 3 core *wrappers*
  (ReactorCore / SpatialReactorCore / TransientReactorCore) does NOT delete
  `RbmkTransientSolver` or `RbmkSolver3D` -- the unified core uses both. So the
  reusable heavy asset survives any wrapper deletion.
- ReactorCore (lumped): ZERO instantiations in src/main = truly dead. Only unique
  holders: it is the sole user of `RodWorth` (bottom-peaked fitted curve). Its
  real value = SPEED: 0.187 ms/tick vs the spatial core's ~26 ms unthreaded. But
  the unified core runs THREADED and measured ~1.5x REAL TIME on this machine
  (3.2 ms per 5 ms step; construct+start ~12 s), so speed is not currently binding.
- SpatialReactorCore: only used by SpatialReactorCoreSelfTest. Static-eigenvalue
  hybrid; superseded.
- TransientReactorCore: NOT deprecated and STILL LIVE-REACHABLE via
  `/rbmk plant start spatial` (PlantTicker.startPlantTransient). Its one unique
  feature (own thread, open loop) is now subsumed -- UnifiedReactorCore has
  `startThreaded()` AND the closed loop. So it is genuinely redundant.
- ReactivityModel.POWER_COEFF is doc/legacy only; `ReactivityModel.reactivity()`
  is NEVER called. UnifiedReactorCore still DECLARES a `@Deprecated POWER_COEFF`
  field (line 107) that is unused = remnant of standing goal 3 (drop the term).
- RECOVERY IS FREE: all three files are in git HEAD/history; `git show
  HEAD:src/.../ReactorCore.java` restores any of them. Nothing is truly lost.
REAL (narrow) reasons one might want them: (a) a slow machine where threaded
real-time factor drops below 1.0 -> a cheap fallback core; (b) an operator-
TRAINING use where the lumped "training-simulator" structure is wanted; (c)
headless/CI reference; (d) an A/B known-good comparison if the spatial core is
ever suspected of a bug. NONE of these is a current requirement.
## THE UNIFIED CORE — status check (2026-11-17, asked by Ben)
Ben's Q: "we unified the cores and ditched the others, right?" ANSWER: ~90% yes.
- `UnifiedReactorCore` (physics/) IS built and IS the one core. Commit af5456a
  "Build the unified core..." 2026-11-15; retired the others in 9fbb281 2026-11-16.
  It takes: flux-is-state from TransientReactorCore (RbmkTransientSolver) + closed
  void/temp/xenon feedback (setCoreState) from SpatialReactorCore. VERIFIED: its
  self-test 6/6 passes today; /rbmk plant start runs it.
- BUT the old cores are NOT deleted -- repo AGENTS.md has a "ZERO DELETION POLICY":
  agents may NOT rm/del; a class is parked with @Deprecated + "TODO: User to delete
  this file later". So they linger. Only BEN can delete them.
- LOOSE ENDS (real, still-reachable): (1) `/rbmk plant start spatial` STILL runs the
  old TransientReactorCore (PlantTicker.startPlantTransient), and that class is NOT
  even @Deprecated. (2) `SpatialReactorCoreSelfTest` still exercises the old
  SpatialReactorCore. (3) ReactorCore (lumped) is truly dead -- zero instantiations.
  `/rbmk plant start 3d` is already harmless (startPlant(boolean) ignores the flag
  and runs unified). `/rbmk stress` already uses unified.
- So: production path = unified only. Two vestigial hooks remain for comparison.
Details: docs/ONE-CORE-DECISION.md §6 (DONE + its 2026-11-15 correction: xenon now
genuinely coupled, graphite temp STILL readout-only -- no sourced coefficient).
## VOID-COEFF "5-7x" SOLVED: both tools were wrong (2026-11-17, commit 456cc18)
The 5-7x was a ratio between two of our OWN tools; BOTH were wrong, opposite ways.
Gold standard = explicit 18-rod cluster Monte Carlo (openmc/decompose_test.py, He gap,
600K, ENDF/B-VIII.0), dk/dvoid 0->0.3:
  A repo OpenMC cell as written  +0.017   <- buggy baseline
  D real geom + correct VF       +0.038   } agree to 19%
  E explicit 18-rod cluster      +0.032   }
  deterministic 1-D cell         +0.115   <- ~3.3x too HIGH (ring+diffusion method)
Repo cell was ~2x too LOW: fuel/water VF 0.62/0.26 (true 0.324/0.570) AND 80 mm bore
(true 88 mm; 80 mm is the ASSEMBLY width). 5-7x = 0.115/0.017 = 6.8x. Both fixed in
openmc/rbmk_cell.py. The "OpenMC corroborates INSAG-7 +0.02" claim was CIRCULAR (the
bug pulled OpenMC onto the lit number) -> WITHDRAWN in provenance.json + OpenMcResult.
CLEARED: S(a,b), temperature, density, and the gap medium (gap is HELIUM, not water -
water-filled gap probes gave inflated +0.13/+0.16; with He it falls to +0.03/+0.04).
CONVENTION: rbmk_void_coeff.py reports REACTIVITY dk/(k1k2): shipped +0.021 = +0.035
simple dk/k slope. Bridge test band widened 0.015-0.06 (old band fit the buggy model).
PLANT UNCHANGED: void worth still anchored to INSAG-7 +0.020; core cannot run away.
CONTROL RODS (Director's hypothesis, 2026-11-17, commit 9a94f84): the real core is 1661 fuel : 211 rods (~1 rod/8 fuel); our cell has NONE. openmc/control_rod_test.py (3x3 wrapped supercell, centre = rod) in the reactivity convention dk/(k1k2), void 0->0.5: bare fuel channel +0.031; +1 THIN rod/8 fuel -0.001; +1 FAT rod -0.015. The rods FLATTEN the void response -> the bare-channel model overstates it. Bare +0.031, rods pull down, whole-core lit +0.020: consistent. NOT the whole core (211 rods, ~240 burnable absorbers, leakage) so direction+size only. Argues for the spatial core.
Details: docs/VOID-COEFF-ROOT-CAUSE.md, .openhands/memory/2026-11-15.md.

## SANDBOX WIPE = rebuild JDK+Gradle (2026-11-17)
A reset wiped the toolchain (JDK, Gradle) along with ephemeral ~. Persist under
/workspace/tools: JDK17 -> /workspace/tools/jdk (adoptium temurin tarball), Gradle 8.6
-> /workspace/tools/gradle-8.6 (services.gradle.org zip; chmod +x bin/gradle).
env: export JAVA_HOME=/workspace/tools/jdk; PATH=$JAVA_HOME/bin:/workspace/tools/gradle-8.6/bin:$PATH
`unzip` is NOT installed -> use python zipfile. `--offline` FAILS (foojay plugin).
javac-only self-tests: gson at /workspace/mcserver/libraries/.../gson-2.10.jar.
OpenMC: source /workspace/tools/omc/env.sh (rebuild: tools/openmc/setup_openmc.sh).
LESSON: cleanup globs `rm -rf cluster_*`/`matched_*` also delete `cluster_test.py`
etc. -- scope rm globs to output dirs only (this bit me three times).

## KEYSTONE SOURCE IN HAND: the 1983 regulation (2026-11-15, commit 60bf8dd)
`docs/primary-sources/Reglament-1E-S-II-1983-ChAES-RBMK-1000.pdf` — the actual
«Технологический регламент по эксплуатации 3 и 4 энергоблоков ЧАЭС с реакторами
РБМК-1000» (1Э-С-II, Союзатомэнерго, in force 15.12.1983; signed Bryukhanov/
Fomin/Dyatlov/Lyutov/Gellerman). 99 pp; text at `.../reglament-1E-S-II-1983.txt`.
Found via Reddit (r/chernobyl 1hagr52 -> public Google Drive folder). Use it as
the citation authority for the equipment register AND the control room. It gives:
Ch.3 = АЗ-1..АЗ-5 protections (incl. turbine stop-valve -> АЗ-5 within 30 s — the
test's exact coupling); Ch.9 = ORM 26-30 rods, shutdown at 15; the abbreviation
list = ~110-term equipment taxonomy (БЩУ-О/Н, ЦЩУ, РПУ, пульт «А»/«Т», СФКРЭ, СЦК,
КЦТК, МТК/МТО...). Discussion doc: `docs/CONTROL-ROOM-DISCUSSION.md`.
See daily log 2026-11-12 for the research method (Arctic Shift + Drive).

## MISSION (corrected by Ben 2026-11-09) — read before every decision
This is a **forensic, 1:1, Science Grade or Better (SGB) reconstruction of the
RBMK-1000 at ChNPP and the night of 26 April 1986**. End state = stand in the
control room, press the same buttons, watch the same behaviours. The plant must be
built to **exact scale and spec**. The enterprises / workforce / construction
systems are the MEANS to build it; the **Stroiteli exist because a human cannot
build to the required exact scale**. Rules: accuracy over drama (if it did not
happen it does not go in); nothing guessed (cite the source or mark unknown);
fidelity is GATED not assumed (set-out разбивка before, as-built исполнительная
съёмка after; mis-built = refused AND flagged); two zones of honesty (Unit 4 +
control room = real simulation; elsewhere real where known, approximations marked).
Full text in `AGENTS.md` (top). Terminology: Soviet name in-game, tooltip =
romanised + English.


Minecraft Fabric mod, MC 1.20.1 (Yarn mappings). Simulates ChNPP at 1:1 scale;
core gameplay is a construction-workforce / agent system. Repo
`Pripyat2033/CSP-Modern-v2`, working branch `OpenHands`. Read `AGENTS.md` for
project conventions.

## Energoblok = sealed compartments (Ben's correction, 2026-11-09, commit 59ca3c7)
An RBMK energoblok is **separate pressure-resistant concrete boxes**, and **the
boxes ARE the containment** (the "RBMK has no containment" claim is false). Pumps
in their own rooms left+right; reactor in its own room; turbines/feedwater/
condensers on islands in the Turbine Hall; **two separators per box** left+right.
Code: `rbmk/plant/Compartment.java` (sealed box, real design pressure: pump rooms
0.45, cavity/separator 0.18, header 0.08 MPa), `EnergoblokLayout.java` (7 sealed
boxes + 5 islands + `Placement` records), `MachineSizes.java` (dimensions split
out of MachineSpecs so layout stays MC-free), `HoleMask.java` +
`ConstructionJob.Mode.PIERCED_RADIAL` (UBS = disc minus 1,872 channel holes),
`block/ContainmentBlock.java` + assets. `RbmkWorldBuilder.planPlant` builds box
shells then places machines inside their rooms.

**Standalone-test rule:** the javac harness compiles ONLY MC-free classes against
`build/classes/java/main`. A class a self-test loads must not touch net.minecraft
at class-load (`static Block` fields fail `<clinit>` even if unused). The mapped MC
jar is NOT on disk; `gradle compileJava` is the real gate. `MachineValidatorSelfTest`
names `MachineSpec` so it is NOT in the harness and `gradle compileTestJava` already
fails on it — pre-existing, not a regression. `EnergoblokLayoutSelfTest` = 231 checks.

## The plant-platform question — OPEN (Ben, 2026-11-09)
A reactor-only bench is NOT the platform question. The whole **energoblok** must
run: pipework, valves, pumps, tanks, meters, their block entities, and rendering,
all at once. The authoritative test is **Ben running the full energoblok in
Minecraft himself** (same form as the 608-worker GO), never a headless bench.
Recorded in `docs/CHARTER.md` §4.

**Pipework module — BUILT 2026-11-09** (`com.ben.csp.rbmk.module`): `PlantModule`
(interface, pure data) + `PlantCell(x,y,z,Role)` + `PipeNetwork` (МЦК; params
`columns`=group-distribution-headers, `step`=channels per valve+meter pair) +
`ModuleRegistry` (`all()` + `audit()`). Real result **56,003 cells** = 48,515
pipe + 3,744 valve + 3,744 meter (2 valves + 2 meters per each of the 1,872
channels). `audit` is the modularity guarantee: refuses a module hitting a
compartment wall or a machine; the reactor cavity is modelled PIERCED (free if
y<=-4, or (x,z) is a channel column). `planPlant` now composes modules in (was:
`Part.PIPES` counted but never placed — fixed). `ConstructionJob` gained
`Mode.CELLS` + `Part.VALVES/METERS`; builder gained plan/place/clear/countsOf
module; new blocks `PLANT_VALVE_BLOCK`/`PLANT_METER_BLOCK`; command
`/rbmk plant module list|build [immediate|demo]|remove`. Verified live: build
demo immediate 56,003 blocks/250 ms; remove cleared exactly its own cells.
Still missing for a full energoblok: pumps/tanks/drums as BLOCKS are placed as
shells already; their block ENTITIES + rendering are not yet in scope of a test.

## Build & run (cheap checks; use these constantly)
```
export JAVA_HOME=$(ls -d $HOME/jdk/jdk-* | head -1)
export PATH=$JAVA_HOME/bin:$HOME/gradle/gradle-8.6/bin:$PATH
gradle compileJava compileClientJava --no-daemon --console=plain   # ~8s
gradle build --no-daemon --console=plain                           # ~12s
gradle runServer --no-daemon --console=plain                       # ~90s to "Done!"
```
Always redirect to a /tmp log and grep `error:|BUILD`. `compileJava compileClientJava`
is the fast gate after any edit; `build` + a clean `runServer` boot is the final gate.
`runServer` leaves `run/world/session.lock` — kill leftover java `runServer`/`KnotServer`
procs and `rm -f run/world/session.lock` before re-booting, else SessionLock exception.

## Source-set layout
Loom `splitEnvironmentSourceSets()` is enabled. Common/server code in
`src/main/java`; client-only code in `src/client/java` (all `net.minecraft.client.*`,
renderers, screens, client keybinds). `src/aecs` is a separate source set (javaparser).
**Never reference a client-only class from `src/main`** — it breaks the dedicated server.

## Gotchas learned the hard way
- Fabric `ResourceManager.findResources(prefix, pred)`: `prefix` is a directory-style
  match. For a file at `data/csp/x.json` use `findResources("", id -> id.getPath().equals("x.json"))`;
  `findResources("x", …)` silently returns nothing. Register server reload listeners with
  `ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(...)` in the ModInitializer.
- Missing assets are the common "stub": a renderer can reference a texture/model that was
  never created → purple/black missing-texture at runtime. Audit by grepping every
  `"textures/…"` Identifier literal and every `blockstates/`+`models/item/` name against
  registered blocks/items.
- The repo historically had many root-vs-subpackage duplicate classes under
  `com.ben.csp`. A class is LIVE iff another file resolves it via `import pkg.Simple;`,
  a same-package reference, or wildcard `import pkg.*;`. After deleting any class,
  recompile immediately (all source sets compile) to catch a mistaken deletion.
- Item registration pattern: `ModItems` exposes `public static final Item` fields;
  each item takes `Item.Settings` in its ctor. The mod's creative tab is `ModItemGroups`
  (FabricItemGroup) keyed by `itemGroup.csp-modern`.
- `PersistentState`: when data "saves" but never round-trips, diff the writeNbt key
  type vs the fromNbt read type (KartografManager had write=Compound, read=String).

## Site planning = Soviet genplan norms (do not invent figures)
The prombaza is planned to **СНиП II-89-80\*** "Генеральные планы промышленных
предприятий" — the norm a 1986 designer worked to. Package
`com.ben.csp.zone.genplan`: `Zone.of(FacilityType)` divides the site by function
(предзаводская / производственная / подсобная / складская, §3.8); `SiteNorms`
quotes every figure with its clause (fire breaks табл. 1*: 6 m non-combustible,
12 m storage; roads §3.47-3.48, 3.75 m a lane; rail §3.52, 3.75 m track spacing,
6 m to a building; **minimum building density 50 %** for concrete/RC works, from
the Приложение). `GenplanZones.layOut(boundary, roster)` packs lots by zone,
deterministic; `SitePlan` is now a thin facade over it. When adding site
planning, cite the clause in `SiteNorms` — do NOT invent distances.

**Reading blocked Russian norms**: the norm hosts (docs.cntd, gostrf, meganorm,
sniprf) are dead/blocked from the sandbox and searx/Qwant/Jina are CAPTCHA/401.
The Wayback Machine works: fetch the archived copy with `id_` in the URL
(`web.archive.org/web/<ts>id_/http://…`) and decode **cp1251**; it 429s if
hammered, so one fetch per ~50 s. SNiP II-89-80 full text lives at
`web.archive.org/web/20030506031443id_/http://www.vashdom.ru/snip/II-89-80/`.

## Session logs
- `2026-11-09.md` — genplan by functional zone (СНиП II-89-80*), version reset
  0.9.5→0.0.1, public download reissued; earlier: loaders, Kartograf NBT fix,
  missing assets, dead-code removal, creative tab, full build + server verification.

## THE PLANT IS COUPLED SYSTEMS, NOT ONE MACHINE (2026-11-09)

`com.ben.csp.rbmk.plant` holds the RBMK-1000 plant as separate systems, each with
its own state and physics: MainCirculatingPump x8 (6 run + 2 standby, coastdown),
SteamSeparator x4 (drum level + trips), Turbine x2, Condenser x2, FeedwaterSystem,
Eccs, Valve, Rotameter x1872, OilSystem. `PlantSystem` couples them and carries
the trip matrix; `PlantTicker` runs it live on the server clock (CSPMod registers
it). Commands: `/rbmk plant start|scram|status|build`.

Physics that must not be "simplified": turbine work = SPECIFIC WORK 655 kJ/kg
(500 MWe / 763 kg/s), not latent-drop x efficiency; condenser heat = steam x
(H_steam - H_feedwater) - turbine work, not the full latent drop; drum level
floats on NET (feedwater - steam); rods: insertion 1.0 = FULLY IN (scram ->
setRodTarget(1.0)); the auto-regulator needs integral action or it settles at 1.13.
Full detail + the live-verified numbers in `2026-11-09.md`.

Blocks: PlantEquipmentBlock + ModBlocks (steam_separator, turbine, condenser, mcp,
feedwater, eccs_accumulator, plant_pipe); `RbmkWorldBuilder` places core solid and
plant as SHELLS (solid deaerators = 92k blocks = 2.2 s tick spike). Tests: 102
checks (Geometry 28, Physics 15, Spatial 16, Accident 15, Plant 28).

## SCALE CAPACITY - THE CORE IS CHEAP; THE BUILD BURST IS THE COST (2026-11-09)

Measured, not guessed (sandbox: Java 17, -Xmx3G). The whole plant built (262k
blocks) = 183 ms. Plant RUNNING critical = 2.45 ms/tick; the server's OWN
profiler reports 600 ticks / 29.95 s = 20.03 TPS (full real-time). The authentic
3D solver = 4.34 ms/tick. /rbmk stress 32 = 4.4M blocks placed -> 2.7-3.3 ms/tick
steady; 32 live reactor sims = 0.03 ms/tick each. Tick budget is 50 ms -> ~20x
headroom per unit.

WHY: the core blocks are INERT. FuelChannelBlockEntity is dead code (never
registered); GraphiteBlockEntity is registered but has NO getTicker, so nothing
ticks per-block. The physics is ONE GLOBAL SIMULATION (ReactorCore = ~20 doubles
+ the 52k-node solver, stepped once/tick). Behaviour lives in a few doubles, not
in 78,000 blocks - so "no simplification" and "it runs" are compatible.

THE REAL COST IS THE ONE-TIME CONSTRUCTION BURST: 4.4M blocks in one command =
3.7 s hitch. That is what the build gate + Stroiteli + the work clock exist to
meter. NEVER judge capacity by a monolithic build command. Untested: real client
rendering (Ben's GPU), real chunk gen, 32x full plants. Tool: /rbmk stress <n>.

## CONSTRUCTION IS METERED NOW; CLIENT TEST KIT EXISTS (2026-11-09)

The build burst is fixed: `rbmk/ConstructionJob` (cursor-based plans) +
`rbmk/ConstructionScheduler` drain the queue at 3000 blocks/tick with auto
back-off above 35 ms MSPT. `/rbmk build` and `/rbmk plant build` ENQUEUE;
`... immediate` keeps the one-shot path for burst measurement. Verified: 262,368
blocks over ~4 s, ZERO hitch (the old one-shot was a 3.7 s hitch). `/rbmk stress
<n> plant` = full plants: 16 plants (4.2M blocks) live = 0.16 ms/tick sim.

Client test kit for Ben (he must run it - the client FPS is the one number the
sandbox cannot get): flat world "Energoblok-Test" with ONE Energoblok built
metered + running, glass viewing platform, spawn (0,70,145). `tools/
make_energoblok_kit.py` -> `/workspace/serve_energoblok/Energoblok-Test-Kit.zip`
(4 MB: world + mods[csp-modern, fabric-api] + README), served on 12001. Verified
the packaged world loads clean and runs. Flat-world note: 1.20.1 layers start at
y=-64, surface at y=0; build at y=1.

## RBMK CORE: SCHEMA = SECTIONS, CONTROL DISTRIBUTED, BORE IS REAL (2026-11-09)

Three corrections the Director forced, all now built (branch OpenHands):

1. **"Schema" is Soviet for the reactor's structural SECTIONS, not a file.** Схема Е
   IS the upper biological shield; the LBS, vessel (кожух) and side water tank are
   the others. We had every dimension in `RbmkGeometry.java` and built none. Now
   `block/SchemaBlock.java` + `RbmkWorldBuilder.planCore` build the graphite stack
   INSIDE its sections: core = 253,780 blocks (graphite 78,496, fuel 53,152, rod
   6,752, UBS 43,500, LBS 21,032, vessel 6,528, water tank 44,320). Was a bare
   97,496 lump. `ConstructionJob` gained a RADIAL region (disc/annulus).
2. **Control is distributed, not on the rim.** `RbmkLattice.channels()` used to mark
   the outermost 211 cells as control (uncontrollable centre). Now a sub-lattice
   every 3rd cell: nearest control at the centre, every channel within ~3.2 blocks of
   a rod; counts unchanged. `RbmkGeometrySelfTest` asserts the DISTRIBUTION now.
3. **The graphite block has a real BORE.** `tools/gen_graphite_bore.py` writes
   graphite/fuel_channel/control_rod as frames (8 cuboids) around an open bore + an
   inset top lip; the channel sits IN the bore, graphite all around. Blocks are
   nonOpaque. COST: ~8x geometry on ~139k core cells — the likely client FPS hit.
4. **The charter gate is enforced.** `/rbmk build` and `/rbmk plant build` call
   `PlantBuildAuthorisation` and refuse unless enterprises ready + prombaza
   designated; only an explicit `demo` sub-command bypasses (loud warning).
5. **Lesson:** when the Director uses a domain term, ASK what it means. When a test
   asserts only counts, add the property that matters (distribution). Never present
   a placeholder as a finished thing.

## PLANT MACHINES ARE MULTIBLOCKS (2026-11-09)

The RBMK-1000 plant machines are not decorated cubes — each is a real machine
whose real size is cited in `docs/RBMK-MACHINE-GEOMETRY.md` (Wikipedia + design
literature; Tavily was out of quota, Wikipedia API works; the K-500-65/3000
cylinder geometry came from the atomas.ru/rbmk datasheet, cp1251-encoded). At
1:4: steam separator 124x11x11 (2.8 m x 31 m horizontal drum), condenser
80x20x48, deaerator 24x40x24, ECCS 3x6x3.
`MachineSpec`/`MachineSpecs`/`MachineValidator` + `MachineControllerBlock`:
the controller is a real shell cell; using it validates the shell and reports
formed / "incomplete — N of M shell blocks missing". `/rbmk machine` (stand on a
controller) reports it; read-only so not gated. `ConstructionJob` has a CYLINDER
mode for the drums.

**Composite machines (commit `6d251e0`).** `MachineSpec` also supports a machine
of several casings under one controller: the **turbogenerator is 5 cylinder
covers (1 ЦВД + 4 ЦНД, white) + a shaft + the ТВВ-500 generator on a foundation**
(157x27x19 blocks ~39 m), and the **ГЦН is a pump body + its 6 kV motor** (3x6x3).
Each part carries its own casing block; `MachineValidator` checks each shell cell
against the part's block. `RbmkWorldBuilder.planPlant` now builds every machine
from its spec via `addMachine()`, so the builder places exactly the cells the
validator checks. New blocks: turbine_cover, generator, mcp_motor. Tests:
`MachineValidatorSelfTest` 16 -> 29 checks; full suite 41 tests / 1761 checks.

**Lesson:** when a machine is a box shell with no controller, it is a placeholder
even if its dimensions are right — the Director wants the machine, structured and
validated. The turbogenerator especially is not a box: it is five covers on a
shaft, and only the covers show in the hall.

**Gotcha:** `tools/gen_plant_models.py` overwrites models with a generic cube —
it clobbered the hand-crafted `control_rod.json` (from `gen_graphite_bore.py`).
That name is now excluded from the generator; re-check hand-crafted models after
running it.

## MC TESTING GOTCHA (cost real time)

`setblock` / `execute if block` **silently fail with "That position is not
loaded"** for chunks outside the loaded radius near spawn. `forceload add` takes
at most **256 chunks per call** — split a big area into several calls. The plant builds ~300
blocks out, so its far end is unloaded. Always `forceload add x1 z1 x2 z2` first.
Command-parse errors log as `...<--[HERE]`; `say` output is `[Server] NAME`.
Grep the exact token, not prose.

## SKALA scope: pre-1986 ONLY - no DIIS, no video monitors (2026-11-09)

Ben: **DIIS did not exist in 1986; it was a 1990s upgrade.** Confirmed from the
Chornobyl Family SKALA restoration (they saved Unit 3's machine; get transcripts
with `yt-dlp --write-auto-sub` - Tavily is still over quota). Build to: two
independent VNIIEM-3 machines, each a V-3M processor, 20K 24-bit words (+8K
shared); pre-1986 output is paper (RTA-80 teletype, ACPU-128 drum printer) plus
MTK/MTO mnemonic mosaic displays, **no video monitors**; operator enters a code
on a request device (7 columns of buttons) and reads the answer; programs PRIZMA,
DREG (its tape became the accident investigation's primary source), KRV, KCTK,
KIKD, RAO. `com.ben.csp.skala` now has ONE loop (DREG); `ScanType`/`runDiisScan`/
the DIIS code entry are removed; the self-test asserts DIIS is NOT seeded.

## Kit worlds MUST ship creative + cheats (2026-11-09)

A server-created `level.dat` defaults to survival (`GameType=0`) with cheats off
(`allowCommands=0`), so Ben could not fly. `tools/make_energoblok_kit.py` now
forces `GameType=1`+`allowCommands=1` (via nbtlib) into the shipped
`world/level.dat`. Gradle `compileTestJava` fails "cannot access Block" -
PRE-EXISTING; tests run via the standalone javac/java harness, not `gradle test`.

## STRESS RESULT: Minecraft does NOT die - the block count is not the wall (2026-11-09)

Ben's real question was "can Minecraft cope or does it die", and the answer is
measured, not hoped. `/rbmk stress <n>` builds n real 1:4 cores (253,780 blocks
each) as real blocks, then ticks n ReactorCore physics for 15 s and reports the
per-tick cost. In-sandbox server (4 GB heap, flat world), one continuous run:

| cores | blocks     | MSPT after build | verdict |
|-------|-----------|------------------|---------|
| 1     | 253,780   | 10.1 ms | comfortable |
| 8     | 2,030,240 | 9.1 ms  | comfortable |
| 16    | 4,060,480 | 15.7 ms | comfortable |
| 32    | 8,120,960 | 34.5 ms | comfortable |
| 64    | 16,241,920| 67.7 ms | overloaded, NO crash |
| 1 plant (core+equipment) | 353,292 | 2.3 ms | comfortable |

Block placement is **~250k blocks/s** (16.2 M blocks in 19.1 s). The `peak` MSPT
(up to 19,134 ms) is the ONE-SHOT synchronous build burst inside a single tick,
NOT steady-state load; with the normal metered scheduler it is spread out. The
core physics cost is tiny (0.01-0.04 ms/tick) because it is the point-kinetics
`ReactorCore`. The **full 3D spatial solver** is a separate, offline cost:
`RbmkSolver3DBench` = 4.52 ms per outer iteration over 52,416 active nodes
(cold solve 344 ms; ~11 iters fit in a 50 ms tick). It is NOT wired into the game
tick (only `/rbmk test` uses it) - that is the next real test if per-channel
spatial physics must run live. Bottom line: the engine has ~10x headroom on a
real 1:1 RBMK-1000 (~20-30k blocks); the earlier "kids' art piece" feeling was a
MODEL problem (1:4 makes the avatar 4x too small and every block 0.25 m), not an
engine problem.

## FULL 3D CORE RUNS LIVE (2026-11-09, commit 5533b75) — the DOA question, answered

The gap was real: the plant ran a reaction, but the **live core was spatially
lumped** (point kinetics); `RbmkSolver3D` ran only once (`/rbmk test`). Now the
plant can run on the **full 3D solver, ticked live**. `SpatialReactorCore`
implements the new `ReactorModel` interface (which `ReactorCore` also
implements), so `PlantSystem` holds either. `/rbmk plant start 3d` selects it;
`/rbmk plant status` prints k_eff and ms/tick.

**Measured live in-server:** power ~1.0 (3000-3500 MWt), fuel 641 C, xenon at
equilibrium, k_eff ~0.689, **4.4 ms/tick over 52,416 nodes**, zero tick-lag
warnings over a 78 s run. So a spatially resolved RBMK reaction DOES run live
inside Minecraft with ~10x headroom on the 50 ms tick.

**What it took to make it a reactor, not just a solver** (this is the expensive
knowledge):
- `RbmkXsec` cell feedbacks were ~40x too strong (Doppler -5e-4/K vs real
  ~-1e-5/K), so power collapsed. Calibrated with `VOID_FEEDBACK_SCALE=0.41`,
  `DOPPLER_FEEDBACK_SCALE=0.020` to match `ReactivityModel` (void +0.030/unit,
  Doppler ~-2e-5/K). The spatial and lumped cores now agree on the physics.
- The spatial rod worth is **-0.93 per unit insertion** (vs the lumped core's
  0.035) — the automatic regulator MUST use the spatial value or it hunts. The
  fix that finally stabilised it: add the core's own **power coefficient**
  (POWER_COEFF=-0.005, as in `ReactivityModel`) to the spatial reactivity, and a
  reactivity-nulling АР with `powerGain=0.002` (gain 0.001-0.005 all give
  +/-9% hold; 0.01+ hunts). Result: mean power 1.00, holds 0.91-1.09.
- `calibrate()` MUST leave the solver flux converged (`solve(300,1e-7)`) — a
  looser reference solve leaves the shape wrong and the first live tick reads a
  bad k_eff and the core collapses.
- `RbmkSolver3D.setRodInsertion` now puts `RbmkGeometry.rodMaterials` axial
  material (B4C / water telescope / graphite displacer) on control channels, so
  the rod's axial geometry — and therefore the positive scram — is visible.

Test: `src/test/.../physics/SpatialReactorCoreSelfTest.java` (12 checks; run in
the standalone harness). Rebuilt jar 739,464 B; deployed to `mcserver/mods/`.

**Still open:** Ben's geometry/scale critique (fuel-channel cube, UBS thickness,
MCP in the annular tank, no UBS penetrations, real layout) is NOT addressed.

## PLANT INSTRUMENTS (valve + meter BEs) — DONE 2026-11-09

`PLANT_VALVE_BLOCK` / `PLANT_METER_BLOCK` are no longer stateless blocks: they are
`PlantInstrumentBlock` (BlockWithEntity) that binds on placement to the nearest
`RbmkLattice.channels()` cell and reads live state via **`PlantInstruments`** (pure,
no MC types, so it is headless-testable). The meter shows the plant's own
`Rotameter` flow (nominal 5.34 kg/s); the valve shows the circuit `Valve` position.
BEs sync via NBT + `updateListeners(...,3)` on change; client renderers draw a
tinted cuboid (valve handwheel rises with position; meter float height = reading,
green/red starving). `/rbmk instrument [pos]` reads it.

LESSON: a server console command batch runs INSIDE one tick — block-entity ticks
run at END of tick, so `rbmk instrument` in the SAME batch as `setblock`/`plant
start` reads stale. Stage console input ~3 s apart when verifying live BEs.

## LIVE CORE = SPATIAL FLUX, NOT POINT KINETICS (2026-11-09, commit 85a3b6a)

The live reactor must run `physics/RbmkTransientSolver`: time-dependent 2-group
diffusion over the real lattice (1872 ch x 28 nodes) with six delayed groups per
node — the FLUX is the state. NO point kinetics. `ReactorCore` (lumped) and
`SpatialReactorCore` (spatial rho fed to point kinetics) are now superseded for
the live core; do not use them as the reactor. `tools/ReactorConsole` runs it
headless (rod/scram/run) — `java -cp build/classes/java/main ...ReactorConsole demo`.
Verified: steady 1.0000; rods 0.80->0.70 => power 1->9.98 in 20 s; ~1.9 ms per
1 ms step. Still to close: thermal-hydraulic feedback into the cells.

## SPATIAL CORE RUNS LIVE IN MINECRAFT (commit cfaacf2)
`/rbmk plant start spatial` runs the time-dependent spatial flux core at 1 ms on a
daemon thread (TransientReactorCore); server thread only reads state. Verified:
20.34 TPS, 0 lag, 0 exceptions, live AZ-5 works. The physics is DECOUPLED from the
tick - Minecraft hosts the RBMK, it does not have to "cope" with it. Runs ~0.42x
real time (2 ms CPU / 1 ms physics) on one thread here. Build with the SYSTEM
gradle (`gradle build -x test --offline`); ./gradlew is broken (wrapper jar
missing). Server+harness at /workspace/mcserver (drive.py + /tmp/mc_cmd).

## PROVENANCE IS NOW MACHINE-CHECKED; LIBRARY IS A CHOICE (2026-11-09)

Mission rule 2 ("nothing is guessed") is now enforced, not just remembered.
`src/main/resources/provenance.json` registers every physics constant with a
status: `verified` | `to_verify` | `placeholder` (placeholder legal only as a
marked `fallback`). `physics/PhysicsProvenance.java` loads it; the gradle task
`checkProvenance` (in `check`) WARNS on `to_verify` and FAILS on an unresolved
placeholder. 11 reference constants are currently flagged to_verify — that list
IS the "what still needs a NIKIET/design source" answer. Do not overload
`enterprise/Provenance.java` (binary, facility-scoped) for this; physics needs
the `to_verify` middle state.

The lattice data source is selectable: `tools/lattice/sources.py` + `--source
endf|rosfond` (or env `LATTICE_SOURCE`). `endf` = ENDF/B-VIII.0 from the openmc
JSON mirror (unattended). `rosfond` = ROSFOND-2010, LOCAL only: drop ENDF-6 files
in `tools/lattice/rosfond_raw/`, run `fetch_rosfond.py`; `endf6.py` is a
numpy-free MF=3 parser that refuses LR!=0 rather than mis-read. emit_constants.py
stamps `library`/`libraryStatus` into rbmk_lattice.json; `LatticeTable.library()`
carries it into the game.

**SHIPPED LIBRARY IS NOW ROSFOND-2010 (2026-11-18).** `rbmk_lattice.json` says
`library=ROSFOND-2010`, `libraryStatus=verified`, k_water 1.28932 / k_void 1.40010
/ rho +0.06137 (+9.44 beta). numpy 2.5.5 + scipy 1.18.1 + njoy ARE installed in
this sandbox, so the pipeline RUNS here (the old "no numpy, Ben-side only" note is
dead). Regenerate with `python3 emit_constants.py --source rosfond`.

**THE ENDF↔ROSFOND "2.66x VOID COEFFICIENT" CLAIM WAS AN ARTIFACT.** `mixture_macros`
takes `Sa = St - Se`, so inelastic (MT=4) MUST be in `Se` first. The openmc ENDF
mirror 404s for U-238 MT=4, so inelastic was counted as ABSORPTION (~6x too much
fast absorption). Fixed by reconstructing `St - Se - Sf - Scap` when MT=4 is
absent. Post-fix the libraries agree to 1.4% (+9.31 vs +9.44 beta): the library
choice does NOT move the void coefficient, a missing reaction channel did. Guarded
by `test_neutron_conservation.py` (9 checks, asserts St == Sa + Ss and that the
buggy form really does inflate absorption 20x in the worst group). See
`docs/ROSFOND-SWAP-MEASURED.md`.

**`xsec.void_worth_scale` = 0.233 IS STILL AN INSAG-7 ANCHOR — Ben's decision.**
Measured whole-core: dial 0.233 -> +0.0227 drho/dvoid at the working point;
dial 1.0 (raw derived cell) -> +0.0810, i.e. 4x INSAG-7. Removing the dial is the
"second change" in `docs/ROSFOND-DATA-PATH.md` §3. NOT done: it would make the
model contradict the only published whole-core number we have. Ask Ben.

ROSFOND (Ben's question): we do NOT strictly need it; it is a PROVENANCE win, not
an accuracy one. The NIKIET-specific values (beta_eff, rod worth, ORM depth, void
coeff) live in DESIGN documents, not in any evaluated library — a library swap
cannot supply them. ABBN-RF (few-group) beats pointwise ROSFOND for us. Size is
UNVERIFIED (all portals 403/timeout from here); do not assert a number.

## PHYSICS CONSTANTS SOURCED FROM INSAG-7; TWO STUBS FOUND (2026-11-09 cont.)
Primary source: INSAG-7, `https://www-pub.iaea.org/MTCD/publications/PDF/Pub913e_web.pdf`
(HTTP 200, no bot wall). Table II-I (printed p.105) = ChNPP Unit 4 design table.
Sourced + applied: VOID_COEFF 0.030->0.020, DOPPLER_COEFF -1.0e-5->-1.2e-5
(ReactivityModel). provenance.json entries now carry `sourceUrl`+`quote`;
PhysicsProvenanceSelfTest REQUIRES a URL+quote on every `verified` entry.
Open sign conflict: INSAG-7 says the GRAPHITE temp coeff is POSITIVE (+6e-5); the
code has -2.0e-5. Recorded to_verify, not silently changed (it flips a self-test).
PRE-EXISTING REGRESSION (bisected to ebaab1c, NOT from this session):
SpatialReactorCoreSelfTest AZ-5 check FAILS on baseline. Cause: the derived lattice
feedbacks are ~5-7x too strong (dk/d(void)=+0.105 vs INSAG-7 0.020), so the fuel
cooldown after a scram adds more positive reactivity than the rod worth removes.
The legacy VOID/DOPPLER_FEEDBACK_SCALE that tamed this was dropped when the derived
constants were wired in. Recorded as provenance `spatial.void_feedback` (to_verify).

## NON-INSAG SOURCE FOR THE FEEDBACK SIGNS: NUREG-1250 (2026-11-09)
Ben distrusts INSAG (INSAG-1 was misled), so corroborate independently.
**NUREG-1250** = US NRC "Report on the Accident at the Chernobyl Nuclear Power
Station", Jan 1987, `https://www.nrc.gov/docs/ML0716/ML071690245.pdf` (22.7 MB,
215 pp, text layer extractable with pypdf). Sec. 2.3.2.2 says verbatim: "The
graphite temperature coefficient is positive." Sec. 2.3.2.3: fuel-temp coeff is
"the only coefficient that is negative". Table 2.4 (scanned, degraded) reads
graphite ~+3.2e-5 (fresh) .. +6e-5 (equilibrium) /degC — agrees with INSAG-7
+6e-5. Its coefficients are compiled from SOVIET primary sources (Dollezhal 1980a
[the RBMK chief designer], USSR 1986, Romanenko 1982, Virgil'ev 1979), so it is
not two Western agencies echoing each other. Registered as `NUREG-1250` in
provenance.json `_primarySources`. Verdict: the code's NEGATIVE graphite coeff
has zero support; flip it (only the magnitude is open: fresh vs equilibrium).

## OpenMC IS INSTALLABLE IN-SANDBOX (verified 2026-11-09, NOT yet installed)
pip has no openmc wheel ("No matching distribution found"), but **micromamba from
conda-forge solves it**: download `https://micro.mamba.pm/api/micromamba/linux-64/latest`
(tar.bz2, extract bin/micromamba), then `micromamba create -n omc -c conda-forge
openmc` — dry-run resolved: 157 packages, 208 MB download. OpenMC 0.16.0 on
conda-forge. Cross-section DATA is separate: `openmc.org/data/` and
`github.com/openmc-dev/data` (200 OK). NNDC ENDF/B-VIII.0 dir is 404 from here.
So the earlier "lattice pipeline is Ben-side only" note is now FALSE for OpenMC
proper; the numpy/scipy gap is also closed by conda (conda-forge has numpy).

**STATUS 2026-11-18: NOT installed, and NOT needed to build or test.** The mod's
bridge reads a precomputed `openmc_void_coeff.json` (`OpenMcBridgeSelfTest`, 11/11),
so no OpenMC at test time. The NJOY path (see ROSFOND below) replaced it for cross
sections. Only reach for the micromamba recipe if you actually need to re-run a
transport calculation.

## OpenMC = the MECHANICS engine; Minecraft = VISUALISATION only - PROVEN (2026-11-13)

Ben's "true test" is met. The neutron physics is computed OUTSIDE Minecraft by
OpenMC (real Monte Carlo, ENDF/B-VIII.0), and the mod only DISPLAYS it.

- Install: micromamba env `omc` at `/workspace/tools/mamba/envs/omc` (bin/python,
  bin/openmc run directly - do NOT depend on the ephemeral `/tmp/mm`). Data:
  `/workspace/tools/openmc_data/endfb-viii.0-hdf5` (3.4 GB tar.xz from anl.box.com
  -> 6.6 GB; 556 neutron + 100 photon + 31 thermal; `c_Graphite` S(a,b) present).
  Point `OPENMC_CROSS_SECTIONS` at its `cross_sections.xml`.
- Model: `openmc/rbmk_cell.py` = one RBMK channel (250 mm graphite cell, 18-rod
  assembly homogenised in the 80 mm bore) as an infinite reflective lattice cell.
  `openmc/rbmk_void_coeff.py` sweeps coolant density and derives the VOID
  COEFFICIENT FROM FIRST PRINCIPLES - no tabulated coefficient is fed in.
- RESULT: k RISES as the channel voids (1.3107 full water -> 1.3324 near full
  void), turning over ONLY at full void (1.2917) because water is also a
  moderator. Operating-point void coeff = **+0.015 dk/k per unit void**, vs the
  documented INSAG-7/NUREG-1250 +0.02. Independent corroboration, and it confirms
  the Java spatial solver's +0.105 is ~5x too strong.
- BRIDGE: `openmc_void_coeff.json` copied to `src/main/resources/`; read by
  `OpenMcResult.java`; `/rbmk openmc` prints it. `OpenMcBridgeSelfTest` = 11
  checks. VERIFIED LIVE: `gradle runServer` boots the Fabric server, the console
  command prints the whole sweep.
- Two OpenMC gotchas: (1) `mix_materials` REJECTS materials carrying S(a,b) -
  mix first, then `add_s_alpha_beta` on the mix; (2) every surface needs a
  `boundary_type` or the run errors out.
- Physics note: a single 13.6 mm rod in a 250 mm cell gives k=0.46 (wrong); the
  channel must carry the full 18-rod assembly (k=1.31, sensible).
- STILL OPEN: re-anchor the Java model (`GRAPHITE_COEFF` is negative; the spatial
  void feedback is +0.105) to these OpenMC/sourced values - needs Ben's go-ahead.

## DERIVED VOID (not fitted) — commit 0d67c7d, 2026-11-12

`UnifiedReactorCore` no longer uses the fitted `void = 0.05 + 0.08*(power-1)`.
`ChannelCooling` derives the void from the channel energy balance: 10,000 kg/s,
320 kJ/kg, ~15% outlet quality → core-average void **0.355** (the fitted 0.05 was
~7× too low). The void is applied to the neutronics as a **core average** (the
axial profile is computed but not yet coupled — see below).

**Two-errors-cancel trap (the lesson):** with the void derived, the core ran away.
Cause: the derived lattice void worth is **+0.086 dk/dvoid = 4.3× INSAG-7's
+0.020**. The old fitted slope was too shallow, hiding it; the two errors cancelled
to a stable-by-accident −0.005. Fix: `RbmkXsec.VOID_WORTH_SCALE = 0.233` blends the
derived cell toward its zero-void baseline (`base + f*(cell-base)`) so the SHAPE is
derived and only the MAGNITUDE is set to the source. Result dk/dvoid = +0.0222.

**Plant coupling is real:** `PlantSystem` → `UnifiedReactorCore.setCoolantFlowKgS`;
outlet quality read back from `u.cooling().outletQuality()`. Halving flow raises
void 0.355→0.528 and P 1.00→1.11 with **no accident flag**. This is the payoff.

**provenance.json status enum has NO `derived`** — unknown → PLACEHOLDER, and
`PhysicsProvenanceSelfTest` fails on a non-fallback placeholder. A computed-but-
unconfirmed value must be `to_verify`. New keys: `xsec.void_worth_scale`,
`coolant.derived_void`, `coolant.slip_ratio`.

**Axial coupling is now ON (default), and the old "doubles the void worth" note was
WRONG.** Measured: axial +0.0248 vs uniform +0.0222 dk/dvoid — only 12% higher. The
real bug was a **seed inconsistency**: seeded at cosine void (mean 0.355) but running
at flux-shape void (mean ~0.64), so the core started super-critical and drifted. Fix:
`UnifiedReactorCore.selfConsistentVoid()` iterates (void profile × flux shape) to a
fixed point at construction (3 static passes, ~5 s). Nominal hold went 1.2% → **0.0%**
drift/30 s. A reflective axial BC is CORRECT here (a uniform core gives a FLAT flux =
perfect reflector = a stand-in for the axial graphite reflector; a leakage BC made it
worse and was removed). The bottom-peaked accident shape comes from the ROD geometry.
`DerivedVoidSelfTest` (13 checks) + all 12 rbmk self-tests pass; gradle compileJava
EXIT 0. Details in `.openhands/memory/2026-11-12.md` and `docs/DERIVED-NOT-FITTED.md`
§5a/§5b. New provenance keys: `coolant.axial_coupled_void`, `xsec.void_worth_axial`.

## EQUIPMENT REGISTER — the completeness authority (commit f4829e9, 2026-11-12)

Ben's fear: "if I don't name every machine you won't model it, and I can't list
them all." Fix = a register **researched from the documents**, not dictated.
`src/main/resources/equipment-register.json` (106 items) + `plant/EquipmentRegister.java`
(gate: `namedOnly()`, `unsourced()`, `summary()`) + `EquipmentRegisterSelfTest` (12
checks) + `/rbmk register` in-game. Status per item: modelled / specified /
named-only / unsourced. Today: **25 modelled, 78 named-only, 3 unsourced** (compressed
air, generator hydrogen cooling, fire suppression — flagged, NOT guessed).

**THE SPINE SOURCE: INSAG-7 Table II-II "Main equipment for an RBMK unit"** — a
numbered component list WITH per-unit counts and masses (Fig. 11-2). It is where the
easy-to-forget items come from: **ventilation fans** (VDN suction at level +43.0,
exhaust at +35.0), **cranes** (central hall 50/10 tf, MCP room 50/10 tf, machine hall
125 tf), refuelling machine (~350 t), controlled-leakage tanks. NUREG-1250 §2.1-2.6
gives the systems (MFC, ECCS/САОР 12×25 m³ accumulators, accident localization,
pressure-suppression pool 3200 m³, gas circuit 80% He/20% N₂, control-rod cooling).
Fetch INSAG-7 with curl from `https://www-pub.iaea.org/MTCD/Publications/PDF/Pub913e_web.pdf`;
NUREG-1250 is local at `/workspace/tools/nureg1250.pdf`. **Tavily is over quota** —
use curl + pypdf, not the search tool. NEXT: the register is not yet exhaustive (needs
the plant's техническое описание) and the gate is not yet wired to the SGB "done" rule.

## TEST KIT — how to build & serve it (Ben cannot test a raw jar)

No server lives in the repo; build one. Full recipe in `.openhands/memory/2026-11-12.md`.
Short form: download Fabric server launcher + Fabric API 0.92.12+1.20.1 for MC 1.20.1,
`eula=true`, flat world, run with a stdin FIFO, `/rbmk plant build demo` (the plain
`build` REFUSES by the hard gate), `forceload add -64 -64 64 64`, `rbmk plant start`,
`save-all flush`, `stop`; then `tools/make_energoblok_kit.py` and serve the zip on
port 12000. **Gradle needs `JAVA_HOME` exported; `--offline` fails (foojay plugin).**

## ENGINE RESEARCH is already done (2026-11-12)
`docs/RESEARCH-minecraft-custom-engines.md` (259 lines, sourced) + `ENGINE-DECISION.md`
answer "can Minecraft run a custom engine alongside/under it?": yes — the mod already
IS the alongside case (pure-Java engine ticked live, ~10× headroom). Native/JNI/FFM
possible but unnecessary; FFM is final only on JDK 22 and the project targets Java 17,
so FFM is out. Don't re-research this.

## SALVAGE RECIPE (2026-11-15) — recovering a lost sandbox from its trajectory
When a sandbox vanishes mid-task, the work is recoverable from the trajectory
files (`/tmp/traj/event_*.json`). Replay every terminal action into a clean
worktree at the branch point with a script (`/tmp/replay4.py`): heredoc cat,
python3 heredocs, `python3 -c` writers, `sed -i`, `mkdir -p`, `cp/mv`, `rm -f`,
`curl -o <repo path>`, then `git add`+`git commit`. **Order matters**: downloads
and copies must run BEFORE the python that consumes them, and `rm` must be
honoured or commits include files the session had deleted. Recovered 35 commits
with byte-identical subjects + the uncommitted ROSFOND fix (commit ed52052 on
`recover/rosfond-data-path`). Verified: `gradlew compileJava` OK, all 12 Java
self-tests PASS, provenance gate OK, lattice py tests 9/9+11/11+8/8.
Two env traps: the fabric-loom lock can be held by an unreapable zombie gradle
(use a fresh `GRADLE_USER_HOME`); the gradle wrapper jar is gitignored and must
be fetched. Full detail in `.openhands/memory/2026-11-15.md`.

## ROSFOND DATA PATH (the fix Ben was mid-way through, 2026-11-15)
ROSFOND neutron files carry their thermal/epithermal resonances in MF=2 and their
MF=3 pointwise XS is ZERO below the resonance range (U-235 fission 0.0 b at
0.0253 eV). Read raw, you get a moderator-only cell. The fix is NJOY
RECONR->BROADR reconstruction into the pointwise JSON cache
(`tools/lattice/reconstruct_rosfond.py`). Also fixed: natural carbon (ROSFOND has
graphite as one ZA-6000 eval, so prefer `Cnat` where present, else the C-12+C-13
split), metastable isomers (prefer ground state over the 'M' file), and ENDF-6
floats with a space in the exponent.

**SOLVED & RUN (commit daaed39, 2026-11-15).** `reconstruct_rosfond.py` now drives
NJOY DIRECTLY (hand-written RECONR/BROADR deck, parse the PENDF with the lattice's
own `endf6.py`) — NOT `openmc...from_njoy`, because **OpenMC is not on PyPI** and
could never run in the sandbox. `pip install njoy==2016.78` ships a real binary, so
the whole path needs only that. Proven on real ENDF/B-VIII.0 files: U-235 fission
586.6 b / capture 99.4 b @0.0253 eV (repo `verify.py` targets 585±10, 99±4);
Pu-241 1012 b / 363 b. `fetch_rosfond.py` now REFUSES to cache a fission/capture
section that is zero at thermal energy (resonance-trap guard) instead of silently
writing a wrong number. `test_reconstruct_rosfond.py` 7/7. NJOY deck gotchas: (1)
read MAT from a real 80-col record, NOT the `$Rev::` header, or NJOY HANGS on
stdin; (2) RECONR card2 `mat 2/` (ncards=2) — `mat/` and `mat 1/` fail; (3) BROADR
reads tape 20+21, writes tape 22 — pre-create tape22. Drag-and-drop upload page
`tools/rosfond_upload_server.py` runs on port 12000 for handing over the archive.

## PHENOMENA REGISTER — the physics twin of the equipment register (commit 5869560, 2026-11-15)

Ben's deepest fear: "if I don't name every mechanic you won't code it, and I can't
list them all." Solved for HARDWARE already (`equipment-register.json`); there was NO
twin for PHYSICS, so an unnamed phenomenon could vanish silently (what xenon did).
`src/main/resources/phenomena-register.json` (27 phenomena) + `physics/PhenomenaRegister.java`
(gate) + `/rbmk phenomena` + `PhenomenaRegisterSelfTest` (19 checks; cross-checks the
register against the code). **Spine source = INSAG-7 §2 "Features of the reactor"** (the
numbered list of features that had a primary influence on the accident). New status
`readout` = computed+displayed but NOT coupled (the xenon failure mode, surfaced).
Today: 14 modelled, 2 readout (graphite temp, power-coeff readout), 11 named-only
(ORM limit, pump cavitation, natural circulation, dryout/CHF, spatial xenon oscillation,
burnup, samarium, ECCS, bubbler pools, FP release, graphite fire), 0 unsourced. The
named-only list IS the to-do. Fetch INSAG-7: curl the IAEA Pub913e_web.pdf (works).
**Fidelity answer to Ben: "identical" is impossible and claiming it would be the xenon
lie; what is deliverable is NO functional delineation (every phenomenon coupled+measured
or listed, never silently absent) + no accident mode + every constant sourced/flagged.**
Running the jar in Minecraft tests the MOD boot + experience, NOT the physics.

## MACHINERY REGISTER — the third fidelity leg (commit 11f4227, 2026-11-15)
The project now has THREE fidelity registers, each a gate:
1. `equipment-register.json` (140) — does the machine EXIST? `/rbmk register`
2. `MachineValidator` — is its SHELL the right shape? (geometry)
3. **`machinery-register.json` (17) — does it BEHAVE as the real machine?** `/rbmk machinery`
Leg 3 answers Ben's "what if the feedwater pump's flow rate is wrong?" Each performance
quantity ties BY NAME to a `PlantPhysics` constant; `MachineryRegister.conformant()`
reads it by reflection and fails if the code DRIFTED from the recorded value.
`MachineryRegisterSelfTest` = 14 checks, PROVEN non-vacuous (a wrong value IS flagged;
editing MCP_FLOW_RATED_M3S 8000→7000 failed the test for real). Counts: 4 verified,
13 to_verify, 0 unsourced, 0 drifted. The 13 to_verify = the research to-do (they trace
to WP-keyed figures not yet pinned to a primary doc). NOTE provenance.json is still
CORE-PHYSICS only; machine perf lives in machinery-register.json.
**The three registers are the answer to "how do we trust it?": exist + shape + behaviour,
all sourced, all machine-checked, none silently absent.**

## PROMBAZA COMPLIANCE DESK + SUPPLY SEAM (2026-11-15, commits 15f5dcb + this one)

Ben asked why the enterprise/prombaza shouldn't enforce compliance instead of the mod
author making everything "accurate" up front. The site DID check its own deliveries —
**входной контроль** (incoming inspection), **дефектовка** (defect-finding by disassembly).
~Half the workforce ended up on it; the project ran ~50% longer.

**FRAMING CORRECTION (Ben, this session): the driver was NOT a decree from above.** No
ministry said to check. It was LOCAL and selfish: defective material backed up the sidings
(Yanov station) and a standing wagon was a per-wagon-per-day FINE (**простой вагонов**,
demurrage); and the workforce was sick of stripping out junk already built in. The check was
the compromise between "clear the yard" and "don't install junk". Do not let the compliance
read as official. This is the whole point of the mod: the plant is NOT born perfect.

**THE FIVE PIECES:**
- `prombaza/IncomingControl.java` — the gate: detection × thoroughness (falls with load,
  floor 0.25) × cost (man-hours). Dispositions INSTALLED/REWORKED/RETURNED/QUARANTINED; a
  defect that is NOT found installs anyway as a **latent** defect. 33 checks.
- `prombaza/GoodsYard.java` — the rail siding: wagons, free time, demurrage. `standingCount()`
  IS the desk's load, so a backed-up yard rushes the check. 9 checks.
- `prombaza/PrombazaControl.java` + `/prombaza control|yard|receive <id>` — desk as state.
- `zone/MaterialSupply.java` — THE SEAM: train arrives (all wagons on the siding) → desk →
  only INSTALLED/REWORKED reaches the site. Variable supplier quality, so the plan ≠ reality.
  10 checks.
- `rbmk/plant/Commissioning.java` — test-on-install. **PLACEHOLDER**: the real ChNPP
  commissioning procedure is UNKNOWN (Ben is explicit, 2026-11-15) — the slide-rule/mental/
  Skala methods are an INVENTED illustration, never quote them as fact. Research before it
  drives reactor behaviour (see docs/PROMBAZA-COMPLIANCE-DESK.md RESEARCH TODO). NO in-game
  command yet — library only. Safe part: a machine is *commissioned, not declared*; a reading
  outside the band fails; a GOOD machine can fail a tight band. 11 checks.

**WIRING (the previously-missing half):** `EnterpriseLifecycle.setSupply` +
`advanceFacility(day)` routes deliveries through the desk; `ZoneEnterprises.setSupply`;
`ZoneCommand.spawn` sets it; `ZoneTicker.ensureSupply` re-attaches EVERY tick so a
save-reload self-heals; `PrombazaState` persists `yards`+`desks`.

**LESSONS:** a gate not wired to a command/delivery is not a gate (`MaterialBatch`,
`Naryad`, `Prorab.simulateInspection` all existed but were ORPHANED). Java `Random` with
consecutive seeds gives near-identical first draws — use ONE Random drawn sequentially.
The full 66-test suite does NOT finish in 400 s; run the affected packages.
5 fidelity legs now: EquipmentRegister (exists?), MachineValidator (shell?), MachineryRegister
(behaves?), IncomingControl (checked? what got through?), Commissioning (works on first start?).
- SEARCH NOTE: archive.org book text is lending-gated (403); all web search engines
  CAPTCHA-walled; Tavily 432, Google Books 429. OpenLibrary search/inside.json works but
  needs ALL terms.

## SITE SELECTION + "TALK TO PEOPLE, NOT TERMINALS" (2026-11-15, Session 4)

Ben's correction: 11 slash commands to found a zone is too many AND historically
impossible — in 1970 nobody typed into a terminal; it was people talking. **Slash
commands must not be the player's interface; the secretary/dialogue is.** His real
focus is the enterprises/prombaza (the reactor detour was only to be sure it works).

**THE TALKING MACHINERY ALREADY EXISTS, UNUSED for the zone:** `DialogueListener`
(chat->speech), `DialogueEngine` (SHRDLU), `DialogueIntent`, `SekretarEntity` (the
secretary — NEVER SPAWNED), `planning/Planerka` (full планёрка, wired to nothing),
telephony speech. Command the zone = extend those + spawn the secretary. No new system.

**SITE-SELECTION RESEARCH** (`docs/HISTORY-SITE-SELECTION.md`, sourced): CM decree
29 Sep 1966 (11.9 GW, 8 nuclear, Central region); 1965-66 Теплоэлектропроект surveyed
**16 points** in Kyiv/Vinnytsia/Zhytomyr, **2 finalists Ladyzhyn & Kopachi**, Kopachi
chosen; Order No.297 11 Dec 1969 formed the Directorate at Kopachi; site 4 km from
Kopachi by **Yanov station**; Bryukhanov director 1970-86 (wanted VVER, overruled to
RBMK); first concrete 15 Aug 1972; Unit 1 on 26 Sep 1977; Pripyat founded 4 Feb 1970.
**UNVERIFIED: the snowy-field/helicopter/toast story** — stage as reconstruction only.

**DESIGN:** `docs/SITE-SELECTION-DESIGN.md` — founder the zone by SPEECH (secretary
takes orders; Director picks the site from the surveyed candidates); commands stay as
an admin back door. Fixes the hardcoded "pb1" in `ZoneState.spawn`.
**RESEARCH METHOD THAT WORKS when Tavily is 432:** curl + Wikipedia REST API
(`/api/rest_v1/page/html/<title>`) + the institution's own site (chnpp.gov.ua). The
`web-researcher` subagent can hit 500 iterations and die — prefer targeted curl.

## OPENMC IS INSTALLED (corrects an earlier wrong note, 2026-11-18)
OpenMC 0.16.0 lives at `/workspace/tools/omc/env` (bin/python, bin/openmc) with
ENDF/B-VIII.0 data at `/workspace/tools/openmc_data/endfb-viii.0-hdf5/cross_sections.xml`.
Run with `export PATH=/workspace/tools/omc/env/bin:$PATH` and
`export OPENMC_CROSS_SECTIONS=<that xml>`. `pip install openmc` still fails (not on
PyPI) and the repo's `./openmc/` dir still shadows `import openmc` from the root.
The lattice pipeline does not need it, but it is available as an independent check.

## CONTROL-ROD MODEL BUG (open, 2026-11-18)
`RbmkSolver3D.setRodInsertion` fills control channels from `RbmkXsec.WATER`, a
hand-set CONSTANT with no void dependence (D2=0.16 vs the fuel cell's 0.80), so
control channels carry zero void feedback and are ~4.5x too thin. Measured: rods
mute the 3-D core's void worth only 4% (+0.0515 -> +0.0493 at dial 1.0). The 75/25
homogenisation is NOT the cause (pure B4C still 84%). The 1-D cell has no rod zone,
so no lattice table can carry rod feedback. OpenMC's rod test is itself confounded
(inserting a rod deletes 1/9 of the fuel; a no-absorber water channel gives +0.064).

## CONTROL ROD: an impossible rod, and the stale-build trap (2026-11-18)
`RbmkXsec.controlCell()` had a hand-set 0.75/0.25 graphite/rod split. That 0.25
implies a **Ø141 mm rod in a Ø114 mm bore** (1.24x, impossible) and overstated rod
absorption by **53%**. Now DERIVED: `CONTROL_HOLE_FRACTION = (pi/4)*114²/250² =
0.16327` (provenance `xsec.control_hole_fraction`, to_verify — 16.33% is a hard
UPPER BOUND). Physics preserved: positive scram +0.00494, 26 April still raises
power 38.6% and still fails to shut down. **The rod OD is genuinely unpublished**
(Reglament, INSAG-7 EN+RU, NIKIET book, RU Wikipedia all checked) — stop hunting
it; `docs/RBMK-CORE-GEOMETRY.md` §3b documents the bound.

**THE STALE-BUILD TRAP (cost a whole session):** `LatticeTableSelfTest` "failed"
as a "pre-existing" ENDF/B-VIII.0 failure. It was a **stale
`build/resources/main/rbmk_lattice.json`** (ENDF) while `src/main/resources` said
ROSFOND-2010/verified. `./gradlew processResources` fixed it — suite then went
**31/31 green, the first fully green run**. If a resource-reading test fails,
diff src vs build resources BEFORE believing it is a code defect.

**Rod travel:** the panel's `rod.travel` 17-18 s is status *verified* and is the
authority — NOT the round 0.4 m/s (that gives 16.4 s, outside the source). Use
`ROD_FULL_TRAVEL_S = 17.5`, `ROD_INSERTION_PER_S = 1/17.5`; never speed/CORE_HEIGHT
(mixes the 6.55 m travel with the 7 m core). `RbmkXsec` is a record: `sa2()`, not `.sa2`.

## DEPENDENCIES — settled 2026-11-18 (stop re-checking)
**"Fabric 1.16" does not exist** — 1.16 is a *Minecraft* version. Mod pins MC 1.20.1,
Fabric Loader 0.15.11, Fabric API 0.91.0+1.20.1, Java 17+.
**OpenMC is NOT on PyPI** ("No matching distribution found") and is NOT needed: no
shipped code imports it; `tools/lattice` only mentions the `openmc-data-storage`
GitHub JSON mirror. Trap: `import openmc` inside the repo silently resolves to the
repo's own `openmc/` directory — test imports from `/tmp`.
**ROSFOND path deps = numpy, scipy, njoy, pypdf** (all installed & proven):
`pip install njoy` ships a REAL 4.9 MB ELF binary + a wrapper at `~/.local/bin/njoy`
(wrapper needs the binary; pass a DECK FILE, put `~/.local/bin` on PATH).
`reconstruct_rosfond.py --only U235` works; U-235 @0.0253 eV = **fission 584.90 b,
capture 98.66 b** (targets 585+-10, 99+-4). Lattice self-tests are STANDALONE
scripts (`python3 test_endf6.py`), NOT pytest. **Java 31/31 suites green.**

## CONTROL ROD DIAMETERS + the "rod fills its channel" bug (commit 4745769, 2026-11-18)
`controlCell()` used to give the rod the WHOLE 114 mm bore. It is Ø70 mm. DOE/NE-0076
Table G-2 (OSTI 7037704, PDF p.177) — B4C 65 OD/57.5 ID, clad 70 mm, displacer 74 mm —
is the ONLY source in the repo for rod diameters (lengths came from the Russian panel;
diameters were absent entirely). Fix = three regions: rod envelope + annular water +
graphite outside the bore. Control-cell sa2 5.931 → 2.238 cm^-1 (2.65x). The annular
water matters: it is what the displacer expels = the positive-scram mechanism.
**Take ONLY the diameters from DOE** — its lengths (5.12 m absorber / 5 m displacer)
are a different build, 24 % off the panel's 6.772 / 4.56 m, so `rod.diameters` is
`to_verify`. Conflicts recorded not reconciled: `rod.density` (1.65 vs 2.52 g/cm3),
`rod.insert_s_insag7` (INSAG-7 18–21 s vs panel 17–18 s; both agree on 0.4 m/s),
`rod.openmc_model_geometry` (script uses rod_r 2.5 cm = the 50 mm inner bore, not the
65 mm B4C). **OpenMC 0.16.0 IS installed** at `/workspace/tools/omc/env` (+ ENDF/B-VIII.0
at `/workspace/tools/openmc_data/`) — `import openmc` from the repo root is shadowed by
the repo's own `openmc/` dir. Full suite 72/72; provenance gate passes.

## SCRAM RATE + power() UNITS (commit ac51f3d, 2026-11-18) — read before quoting ANY number
`ExperimentVariantsSelfTest` hardcoded 0.2 insertion/s; the model's own constant is
0.057/s (17.5 s stroke). **26 April peak: +35.4 % at the fake rate, +188.0 % at the real
one.** Direction never wrong, magnitude was 5x low. The test now reads
`RbmkGeometry.ROD_INSERTION_PER_S`. **`power()` is ARBITRARY UNITS (reference state = 1.0),
NOT "times nominal"** — the 26 April variant is seeded at 5.63 of those units; only
ratios are meaningful, no absolute MW exists. B4C density: 2.52 is the theoretical
crystal density, 1.65 is 65.5 % of it (a real pellet) — probably the same material
quoted dense vs fabricated, and "typo for 2.65" is impossible (>100 %). Density moves
the excursion only 0.9 points; **fuel temperature is FROZEN in the accident test**, which
is the biggest remaining fidelity gap. Trap: patching `src/main/resources/*.json` does
NOT affect tests — the classpath reads `build/resources/main/`.

## ACCIDENT-SEQUENCE TESTS: the trap that hid a 5x error (2026-11-18)

`gradlew check` runs ONLY `checkProvenance`. Every physics test is a `main()`
class run by hand. **"72/72 passed" never included `AccidentSequence`** — which
was failing 2/12 on clean HEAD. Always run the `main()` classes explicitly; the
classpath comes from the `printTestCp` task now in `build.gradle`.

Three ways an RBMK test can pass while measuring nothing (all three were live):
1. **Measuring at the wrong operating point.** The xenon pit was checked after
   withdrawing rods back to 0.82 — power ~0.98, where flux burns Xe faster than
   I-135 makes it. The pit is LOW-flux and HOURS long (I-135 T1/2 6.57 h); the
   old wait was 30 min. Correct model: hold 200 MWt (what the operators held
   00:28-01:23) and let АР withdraw rods to hold it — insertion 0.900->0.782.
2. **A regulator with no setpoint.** `setAutoRegulator(true)` latches the setpoint
   to CURRENT power. Called when power is ~0 it "holds" zero, the core dies, and
   every later check passes vacuously (P_fis 0.0000 -> 0.0000). Use
   `setPowerSetpoint()` and call it AFTER engaging. АР must also be OFF for manual
   rod moves and for the scram, or it fights them.
3. **Measuring the wrong power.** `power()` = fission + decay heat; decay
   (EQUILIBRIUM 6.5 % of nominal) is flux-independent, so at low power a real
   neutron excursion vanishes into rounding. Use `fissionPower()`.

## THE BARE SOLVER HAS NO THERMAL FEEDBACK — and this invalidates old peak figures

`RbmkTransientSolver` takes fuel temperature as an INPUT. Only
`UnifiedReactorCore` relaxes it (TAU_FUEL 10 s, anchored to
`FuelTemperature.MAX_FUEL_C` 600 °C). `ExperimentVariantsSelfTest` bypassed that
and passed a frozen `640 + 600*(p-1)` — the very formula `FuelTemperature.java`
exists to replace. Consequence: **starting power was just how long the settle
loop ran** (26 April P0 = 1.53 / 2.36 / 5.63 / 32.8 at 10/20/40/80 s), so every
"peak %" from that path was arbitrary. With fuel temperature allowed to move the
26 April peak drops from +188 % to about +89-140 % depending on the thermal model.

**Worse: state B is not a critical core at all.** Seeded at xenon 1.0 (unpoisoned)
with rods at 0.08 it is simply supercritical, so it can never settle. Bisection on
`solver.balanceKeff()` gives **Xe ~ 3.12** as the ratio that makes it critical.
The real 26 April state was critical BECAUSE of xenon poisoning — that is what
the ORM violation means. Open question for Ben: define the two start states by
(a) solving their critical xenon, (b) driving them there with АР from
`selfConsistentSeed()`, or (c) running the variants on `UnifiedReactorCore` like
`AccidentSequence` does. I lean (c).

## ONE CORE, NO FLAG — Ben's two absolute rules (commit cc2b88a, 2026-11-18)

**1. `UnifiedReactorCore` is the ONLY core.** `ReactorCore` (lumped point kinetics),
`SpatialReactorCore` (static eigenvalue), `TransientReactorCore` (open-loop threaded)
are all retired. They are `@Deprecated` AND unreachable — de-wired, NOT deleted,
because `AGENTS.md` has a **ZERO DELETION POLICY**: never run `rm`; comment out or
deprecate and tell Ben to delete. The de-wiring that mattered was
`PlantTicker.startPlantTransient()`, which could still instantiate the open-loop
core in front of an operator. Also removed: `PlantSystem.shutdown()` and
`/rbmk plant status` `instanceof TransientReactorCore` branches. `UnifiedReactorCore`
no longer reads `ReactivityModel.POWER_COEFF` (was @Deprecated dead code, but the
reference meant production still depended on the lumped model).

**2. NOTHING may encourage/force the accident.** Ben: "it's impossible, it's
illegal... the accident can occur under regular normal operating conditions
regardless of what the operating crew do" (Dyatlov's claim). No flag, no mode, no
setter that writes the answer. The accident must EMERGE from physics + geometry +
thermal properties.

**The guard is `NoAccidentModeSelfTest` (6 checks) — extend it, never weaken it.**
It scans (a) `UnifiedReactorCore` FIELDS for accident/mode/experiment, (b) all
PUBLIC METHODS for any that WRITE power/reactivity/keff/flux/xenon (a setter is a
better forcing mechanism than a flag and looks like ordinary API), (c) the SOURCE
of all 27 `rbmk/physics` classes for forcing vocabulary (explode, detonate, cheat,
blowup, forceAccident, accidentMode) with comments+strings stripped so honest prose
about the accident does not trip it.

**PROOF it is emergent** (stock core, only `setRodTarget`/`setAutoRegulator`):
nominal P=0.9999 → rods 0.90 P=0.0706 → АР holds 200 MWt 4 h, Xe=1.65x, ins 0.821 →
rods 0.10 rho=+0.0196 → AZ-5: rho +0.019561→+0.023023, P_fis 0.143→0.151→0.104.
Positive scram comes from the graphite-displacer rod geometry; the low margin from
the void worth. Nothing asks for it.

**Test-run recipe:** `gradlew check` runs ONLY `checkProvenance` — every physics test
is a `main()` class. Classpath: `./gradlew -q printTestCp > /tmp/testcp.txt`, then
`java -cp "$(cat /tmp/testcp.txt)" com.ben.csp.rbmk.physics.<Class>`. `/tmp` is
wiped on terminal reset. The 6 h xenon tests take ~10 min each; run them in the
background with nohup.
