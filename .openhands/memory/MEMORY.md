# CSP-Modern-v2 — project memory

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
