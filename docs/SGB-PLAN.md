# THE SGB PLAN — what we actually have to do

Written for Ben, 2026-11-13. Plain language. No physics talk unless it changes a
decision.

You asked two honest questions:

1. *"I do not know how to download the ROSFOND zips here."*
2. *"Nor do I see how that lets us build a Science Grade or Better full-fidelity
   RBMK-1000."*
3. *"Give me something I can use in Minecraft."*

Here are the three straight answers, then the plan.

---

## 1. ROSFOND — you are right, and here is the corrected position

**Correction (2026-11-13), superseding the earlier draft of this section.** Two
things in the first draft were wrong, and Ben caught both:

1. It said ROSFOND was a ~70 GB download to hunt for. **Wrong.** Ben already has
   ROSFOND installed, and it is not 70 GB. There is nothing to download.
2. It treated ROSFOND as a "nice-to-have, parked" item and leaned on
   ENDF/B-VIII.0 as the working library. **Wrong, and against the mission rule.**
   Ben's rule is now hard: **no US/Western nuclear data** — the numbers that drive
   the reactor must be Soviet, verified, cross-referenced. ENDF/B-VIII.0 is US
   data and is **not acceptable as the source of record**.

**So ROSFOND is not parked. It is the required data source.**

What it is: ROSFOND-2010 is Russia's national evaluated nuclear data library
(IPPE lineage) — the numbers that say how likely a neutron is to hit uranium,
graphite or water. OpenMC is the Monte Carlo calculator; **ROSFOND is the physics.**
OpenMC holds no data of its own, so "OpenMC is US software" does not put US
physics into the answer — but the *library* absolutely does, which is exactly why
the library must be ROSFOND.

**The path now works, and is tested.** OpenMC cannot read ROSFOND's ENDF-6 files
directly, and (a real trap) its Python API refuses to write its HDF5 format from
an ENDF-origin object. The supported route is NJOY2016, which ships with OpenMC
from conda-forge. That route is now implemented and verified end-to-end:

```
tools/openmc/rosfond_to_openmc.py      # ROSFOND ENDF-6 -> OpenMC HDF5 + cross_sections.xml
tools/openmc/test_rosfond_to_openmc.py # 15/15 checks pass
docs/OPENMC-WINDOWS-WSL.md             # how to install OpenMC on Windows 11 (WSL2)
```

A real ENDF-6 evaluation converts in ~30 s per nuclide. When a needed element is
absent from the ROSFOND set, the converter **says so** — it never silently falls
back to a Western library.

The earlier "nice-to-have" framing was wrong. ROSFOND is the source of record;
ENDF/B-VIII.0, if used at all, is only a temporary stand-in to prove the pipeline
runs before the ROSFOND conversion is finished.

---

## 2. The thing you can use in Minecraft right now

It already exists — it was built in the earlier sessions and it survived. In the
salvage folder:

```
salvage/artifacts/Energoblok-Test-Kit.zip   (4.2 MB, complete)
```

It is a **ready-to-play world** — a real `level.dat`, real region files, both
mods. To play it:

1. Minecraft Java **1.20.1** + **Fabric Loader 1.20.1** installed.
2. Put both jars from `mods/` into `%appdata%\.minecraft\mods\`
   (`csp-modern-0.0.1.jar` and `fabric-api-0.92.12+1.20.1.jar`).
3. Put the `world/` folder into `%appdata%\.minecraft\saves\Energoblok-Machines\`.
4. Open the world. You spawn on a glass platform facing a row of specimen blocks;
   the whole plant (core + machines) is to your east. It opens in creative with
   cheats on.

Read the `START-HERE.md` inside it — it tells you exactly what to look at and
what to tell me. **This is the loop we should be in: you look, you tell me what's
wrong, I fix it.** Not physics essays.

If the world won't load or the jar won't run, tell me and that becomes the top of
the list.

---

## 3. What "SGB full-fidelity RBMK-1000 (pre-1986)" actually means — in your own words

You already wrote the standard. It is in `docs/SPECIMEN-SHEET.md`:

> **Invented → fail. Missing → fail. Otherwise → pass.**
> "Would a real operator find anything here that was not there, or miss anything
> that was?"

And `PROJECT_NOTES.md` says SGB means **1:1 full-fidelity, no simplifications**,
and that the core ChNPP must be built at **Bit-Scale** (1/16-block detail), with
plain 1 m blocks *forbidden on the main site*.

So "SGB RBMK-1000" is not one thing. It is **three layers**, each with its own
test. Keeping them separate is what stops this feeling overwhelming:

| Layer | What it is | Its test |
|---|---|---|
| **A. The build** | The thing you walk through | Can you measure it against the real drawings and have it match? |
| **B. The physics** | What makes it behave | Does k_eff = 1.0 come *from the geometry*, not from a fudge? |
| **C. The systems** | What makes it run | Does the whole energoblok tick inside 50 ms? |

`docs/REACTOR-GEOMETRY.md` §6 already defines the acceptance test for **B** — the
"core is SG+" checklist. That is the contract. We just have to earn it.

---

## 4. The plan — ordered, so each step is the next step

The steps below are the **gap list** (from `docs/REACTOR-GEOMETRY.md` §4 and the
code's own `TO VERIFY` markers), **sequenced** so that nothing is built on a
number we have not pinned.

### Phase A — Pin the facts (the research gate)

Nothing in B or C can be called SGB until the geometry and the coefficients trace
to a source. This is the "nothing is guessed" rule, and it is *first* because
everything downstream inherits its errors.

- **A1. Core cartogram.** We know the *counts* (1,661 fuel + 211 control) but not
  *which* cell is AZ / РР / АР / УСП. Get a real core map (the loading plan / the
  Unit 4 drawing). **This one matters most** — the accident is a *shape* problem.
- **A2. The 250 mm vs Ø11.8 m contradiction** (`docs/RBMK-CORE-GEOMETRY.md` §1a):
  1,872 channels do not fit an 11.8 m circle at 250 mm pitch. One of the two
  figures is wrong. Pin it from a primary drawing.
- **A3. Control-rod axial lengths** (4.5 m displacer + 1.25 m telescope + B₄C).
  We have these from Wikipedia; confirm from NIKIET/design doc before we freeze them.
- **A4. Machine geometry gaps** (`docs/RBMK-MACHINE-GEOMETRY.md`): the K-500
  cylinder lengths, the hall bay spacing.

Output of Phase A: a frozen, sourced **build spec** — the first time the plant can
be called "to spec" at all.

### Phase B — Make the build load-bearing

Right now the physics and the build are *consistent by construction* (both read
the same `RbmkLattice`), but the solver does **not** read the world you actually
built. So a mis-built core still gives a "correct" answer — which violates rule 3
("mis-built is refused and flagged").

- **B1.** Make `RbmkSolver3D` / `ReactorCore` read the **built** blocks (or the
  build's as-built survey), so the world and the physics cannot disagree.
- **B2.** Wire the as-built check: a core off its design is **refused** sign-off
  and **flagged on the plan** (the `разбивка` / `исполнительная съёмка` gate in
  AGENTS.md rule 3).

### Phase C — Earn the physics (no more fudging)

`docs/REACTOR-GEOMETRY.md` §4 states plainly: *"Criticality is calibrated, not
earned."* `ReactorCore.calibrate()` clamps a cold k_eff of 1.265 to 1.0. For SGB,
the geometry itself must produce 1.0.

- **C1.** Replace hand-set cross sections (`RbmkXsec` "representative … TO VERIFY")
  with a **cited lattice calculation** — which is exactly what the OpenMC work in
  `openmc/` is for, now fed by ROSFOND via `tools/openmc/rosfond_to_openmc.py`
  (see §1).
- **C2.** Remove the calibration clamp: make k_eff = 1.0 **emerge** from the
  geometry at the operating point.
- **C3.** Make the **positive void coefficient emerge from the lattice** (OpenMC
  already shows it does: +0.015/unit), and delete the tuned constant.
- **C4.** Close the thermal-hydraulic loop (`ReactorConsole.java` flags it as
  "NOT YET IN": void and temperatures are not fed back into the cells).

### Phase D — Build the systems, and run the platform test (YOUR test)

This is the **real** question, and `docs/CHARTER.md` §4 says so: *"the platform
question is the whole energoblok."* A real RBMK-1000 is mostly **piping**:

- 22 group distribution headers per circuit, each feeding 38–41 channels
- a **flow-control valve and a ball flow meter on every one of the 1,661 channels**
- an upper steam line per channel, 12 downpipes per separator

None of it is built yet. `planPlant` places walls and machine shells; `Part.PIPES`
is declared and counted but **never placed (always 0)**.

- **D1.** Build the pipework, valves and meters for real.
- **D2.** **You run the full energoblok in Minecraft yourself** and watch the tick
  (`/zone perf`). That is the exit criterion, in your own words: *"If Minecraft can
  carry the whole energoblok — every compartment, every machine, every pipe, valve,
  meter and tank, running — then the platform is proven."*

### Phase E — The bit-scale core (the visual 1:1)

The physics is already 1:1 (real metres, real materials). What is *not* 1:1 is the
**build you can check**. At 1:4 a channel is a stand-in block; at bit scale
(N=16 → 62.5 mm bits) a channel is a real ~0.35-block tube in a real 0.25-block
cell — the core you walk through **is** the core the solver reads.

- **E1.** Implement the bit-scale core (`ChiseledBlockEntity`, 16³ sub-grid).
- This is presentation + verifiability, not new physics. It comes last because it
  is the largest and it does not unblock anything above.

---

## 5. The one next step

Everything above is real, but none of it is *today*. Today's honest next step is
the cheapest thing that gives us information:

**You load `Energoblok-Test-Kit.zip` and tell me what is wrong with it.**

That is the loop that has actually moved this project — a built thing, looked at
by you, corrected. If it loads: walk it, run `/rbmk machine` on a controller, run
`/rbmk plant start` and `/rbmk plant status`, and tell me the first three things
that are not right. If it does not load: tell me the error and that is the job.

While you do that, the agent-side work that is genuinely unblocked is **Phase A**
(pin the facts) and **B1** (make the solver read the built core) — neither needs
you, and both remove fudge from everything downstream.

---

## Appendix — where the pieces already live

- Standard: `docs/SPECIMEN-SHEET.md`, `docs/CHARTER.md`, `PROJECT_NOTES.md`
- Core geometry + the open items: `docs/RBMK-CORE-GEOMETRY.md`
- "Core is SG+" acceptance test: `docs/REACTOR-GEOMETRY.md` §6
- Machine geometry gaps: `docs/RBMK-MACHINE-GEOMETRY.md`
- The external physics engine: `openmc/README.md`
- Playable kit: `salvage/artifacts/Energoblok-Test-Kit.zip`
- The kit's own instructions: its `START-HERE.md`
