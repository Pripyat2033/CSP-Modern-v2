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
