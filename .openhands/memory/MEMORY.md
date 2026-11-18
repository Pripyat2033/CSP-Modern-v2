# CSP-Modern-v2 — project memory

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

BIG SANDBOX GOTCHA: **there is NO numpy/scipy in the sandbox Python.** So the
lattice pipeline (emit_constants, rbmk_spectrum, verify.py) CANNOT run here — it
is a BEN-SIDE step. Write any new lattice tooling numpy-free so it is testable
here. Consequence: the committed rbmk_lattice.json is hand-maintained until Ben
re-runs the generator.

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
