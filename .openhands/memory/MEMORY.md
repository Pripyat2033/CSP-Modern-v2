# Project memory (.openhands/memory/MEMORY.md)

Curated index. Full prose for every topic is in `ARCHIVE-narrative.md` (verbatim,
nothing lost) and the per-day logs `YYYY-MM-DD.md`. Keep this file under ~6000
characters: content at the top is truncated first, so only durable, load-bearing
facts belong here.

## MISSION (Ben, 2026-11-09) — read before every decision
Forensically reconstruct the Chernobyl RBMK-1000 Unit 4 accident in Minecraft
Fabric (MC 1.20.1). **Absolute rule: "nothing is guessed"** — every constant
cited/derived or explicitly marked UNKNOWN/TO VERIFY.
Ben **distrusts INSAG-7/Western sources**; prefers ROSFOND-2010 and Russian
primary sources / designer intent. His goal: run the Unit 4 turbine rundown test
and see it fail correctly. **Accuracy is a floor, not a ceiling.**
**ARL standard** (`PROJECT_NOTES.md` line 18) overrides the SGB: full-fidelity
radiological simulation, no watering down.
Ben's rejection of the April-26-vs-regular-night test is correct: varying only
ORM/void/power is a *discrimination check*, not validation. Real validation =
humans standing in the control room working the actual procedure.

## REKORD = 4 layers (the honest build order)
1. **Isotope decay data** — ENSDF. Fully citable, zero guessing.
2. **Inventory at t=0** — from design-time burnup (`tools/lattice/burnup.py`
   already depletes: `Cell.burn()`, lumped FP). No in-game depletion needed.
3. **Source term / release** — CONTESTED, published estimates vary >2x, so it
   must be a sourced range, never one invented number.
4. **Transport + dose + effects** — dispersion, deposition, dose,
   TissueDamageModel. Needs a Linux `librbmk_solver.so` (only .dylib exists).
The 686 files in `/workspace/rosfond_uploads` are NEUTRON XS, NOT decay data.

**REKORD FORENSICS (2026-11-18): the radiation sim was NEVER written.**
Max lines ever across all reachable+unreachable commits = 469. The jars match
source exactly. 8 of the initial 28 radiology files were ZERO-LINE stubs
(incl. `IsotopeLoader`). What survived is the **mandate + data model**, not
physics. There was nothing to recover.

## REKORD LAYER 1 — DONE (commit dde2d7f, 2026-11-18)
54 nuclides from IAEA NDS/ENSDF via `tools/radiology/fetch_decay_data.py`;
`IsotopeLoader` filled in (was a 0-line stub); `IsotopeDataSelfTest` 38/38.
Four traps that each produced a WRONG number — do not "simplify" them away:
1. **Isomers**: query the GROUND nuclide, then select the level whose `decay`
   column is `IT`. Energy-order selection gives a 190 ps level instead of
   Tc-99m's 6.006 h.
2. **Repeated transitions**: ENSDF lists some gamma lines twice and NOT in
   strength order. I-132 667.7 keV appears at 13.9% AND 98.7% — keep the
   STRONGEST (keeping the first row understated dose 7x).
3. **X-rays are inside the gamma list** (Co-60 7.677 keV in both at 3.815%) —
   drop the GAMMA copy or the photon is counted twice.
4. **Beta is `bm` = MEAN energy**, not endpoint.
Layer 1 contains NO activity/release/dose. `radiology.release_groups` is
`to_verify`. Detail: `docs/REKORD-LAYER-1-NUCLIDE-DATA.md`.

## FABRIC/MC VERSIONS — there is NO "Fabric 1.16"
MC **1.20.1**, loader **0.15.11**, yarn 1.20.1+build.10, Fabric API
**0.91.0+1.20.1**, loom `1.6-SNAPSHOT`. Verified 2026-11-18: loader latest
**0.20.1**, API for 1.20.1 up to **0.92.12+1.20.1**, Loom now **1.19.4**.
Loom **1.16.3** exists → "Fabric 1.16" almost certainly means *Loom* 1.16.
Pins are valid, just conservative. Four distinct numbers; never conflate them.
Nothing needs changing.

## TWO ABSOLUTE RULES (commit cc2b88a) — ONE CORE, NO FLAG
No accident flag may decide the outcome; emergent physics only. Self-tests must
show opposite outcomes from one code path.

## THE KEYSTONE SOURCE
The 1983 regulation is in hand (commit 60bf8dd). Energoblok = sealed
compartments. Site planning follows Soviet genplan norms — do not invent figures.

## BUILD & RUN
Export `JAVA_HOME` (gradle fails without it). `--offline` works for
compileJava/compileTestJava; it FAILS for dependency resolution (foojay plugin).
Self-tests are plain `main()` classes run with:
`java -cp build/classes/java/main:build/classes/java/test:src/main/resources:<gson> <FQCN>`
Gson lives in the gradle wrapper dist lib.

## GOTCHAS THAT COST REAL TIME
- `tools/gen_*.py` have **no arg parsing** — running one "to see" writes dozens
  of files into `src/main/resources/assets/`. Undo: `git clean -fdq` that dir.
- Gradle's fabric-loom lock can be held by an unreapable zombie gradle → use a
  fresh `GRADLE_USER_HOME`. The gradle wrapper jar is gitignored; fetch it.
- Tavily is over quota — use `curl` + `pypdf`.
- MC client testing needs a real server; there is no server in the repo. Recipe
  in the 2026-11-12 log.
- A lost sandbox is recoverable from `/tmp/traj` — replay recipe in the
  2026-11-15 log.

## SOURCES ON HAND
- INSAG-7: `https://www-pub.iaea.org/MTCD/Publications/PDF/Pub913e_web.pdf`
  (Table II-II = main equipment list, the spine for `equipment-register.json`).
- NUREG-1250 local at `/workspace/tools/nureg1250.pdf`.
- ROSFOND ENDF at `/workspace/tools/openmc_data/endfb-viii.0-hdf5`.
- IAEA NDS: `https://nds.iaea.org/relnsd/v1/data?fields=levels|decay_rads
  &nuclides=X&rad_types=g|a|x|bm` (single nuclides only, needs a User-Agent).

## ENGINE RESEARCH — DONE, do not re-research
`docs/RESEARCH-minecraft-custom-engines.md` + `ENGINE-DECISION.md`. The mod
already runs a pure-Java engine live alongside MC with ~10x headroom. FFM is out
(needs JDK 22; project targets 17).

## EQUIPMENT REGISTER (commit f4829e9)
`equipment-register.json` (106 items) + `EquipmentRegister` gate + 18-check
self-test + `/rbmk register`. Status: 25 modelled, 78 named-only, 3 unsourced
(compressed air, generator H2 cooling, fire suppression — flagged, NOT guessed).

## VERIFIED PHYSICS RESULTS (do not re-derive)
- **Derived void, not fitted** (commit 0d67c7d): channel energy balance gives
  outlet quality ~15% and mean void 0.25-0.45, vs the old fitted 0.05. The void
  coefficient at the working point matches the sourced INSAG-7 value. A
  reflective axial BC is CORRECT (a uniform core gives FLAT flux = perfect
  reflector; a leakage BC made it worse).
- **ORM is per-rod**; rod worth is calibrated (commit 0bf2a54).
- **Radial-flux tilt bug** found and fixed (commit 59bc0e7).
- **Void-coefficient "5-7x" discrepancy**: BOTH tools were wrong (commit
  456cc18).
- `ExperimentVariantsSelfTest` 7/7: normal AZ-5 = +0.4% peak, ends 0.29; 26 April
  AZ-5 = +190.4% peak, ends 14.1 — same code path. Caveat stated in the test:
  power is arbitrary units (ratios meaningful, absolutes not) and the model is
  NOT quantitatively validated (9267 pcm gap to OpenMC).
