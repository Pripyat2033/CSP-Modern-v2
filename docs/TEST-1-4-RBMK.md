# THE 1:4 TEST — first time the RBMK has actually been built and run in-world

Written for Ben, 2026-11-13. **This is the test you have been asking for across
two conversations and never got.** It has now been run. The result is below, with
the commands and the numbers, so it can be repeated.

---

## 1. What was wrong (why you could never test it)

Two separate problems, both now understood:

1. **The Gradle wrapper jar was missing from the repo.** `./gradlew` failed with
   `Could not find or load main class org.gradle.wrapper.GradleWrapperMain`. Fixed
   by downloading `gradle-wrapper.jar` for Gradle 8.6. *(This is why the build was
   never reproducible in a fresh sandbox.)*
2. **`/rbmk build` is gated behind the enterprise simulation.** The command that
   builds the core refuses to run until "every enterprise is built, directed and
   manned" and a prombaza is designated. That gate is correct for the real site —
   but it means **nobody had ever run the build**, because the enterprises are not
   built yet. The escape hatch is **`/rbmk build demo`**, which bypasses the gate
   "for a disposable test world."

So the RBMK was never broken. It was **locked behind a gate nobody opened.**

---

## 2. What was run, and what happened

Server: Fabric dev server (`gradlew runServer`), Minecraft 1.20.1, flat world,
4 GB heap, in-sandbox.

```
/rbmk build demo          → core: 231,316 blocks placed (3000/tick, ~4 s)
/rbmk plant build demo    → core + energoblok: 585,307 blocks total (~10 s)
/rbmk plant start         → plant running live on the server clock
/rbmk plant status        → live state
/rbmk status              → tick cost
```

**The 1:4 core is real in-world.** Probes with `execute if block` confirm
`csp-modern:fuel_channel` and `csp-modern:control_rod` at the built coordinates
(e.g. F@1,-60,1; R@3,-60,3). The graphite stack, UBS, LBS, vessel and water tank
are all placed.

**The whole plant runs live:**

```
RBMK-1000 plant, t = 7.9 s
  core:       power 0.995 (3183 MWt), rods 0.78
  pumps:      48000 m3/h total, 6 running
  drums:      level 0.50, 6.9 MPa
  turbines:   1000 MWe (3000 rpm each)
  condensers: rejecting 2200 MW, cooling water out 36.4 C
```

**Tick cost (the number that matters):**

| Measurement | Value | Verdict |
|---|---|---|
| Server tick while plant running | **average 1.54 ms** | comfortable (50 ms budget) |
| `/rbmk test` core running | 4.28 ms/tick | comfortable |
| Tick-lag warnings while running | **0** | — |

The single "Can't keep up!" warning in the log was the one-off **2.7 s cold solve**
inside `/rbmk test` (600 iterations, done synchronously), **not** the running plant.
Once the plant is running, the tick is ~1.5 ms.

**Verdict: the 1:4 RBMK builds and runs, live, comfortably, with room to spare.**

---

## 3. Two real bugs this test exposed (found only by running it)

1. **`k_eff = -0.847` from the cold solve.** `/rbmk test` reports a *negative*
   k_eff, which is physically impossible (k_eff is a positive ratio of neutron
   production to loss). The running core reports "k-eff state" and holds power at
   0.995, so the *live* regulator works — but the *cold-solve diagnostic* is
   returning a nonsense number. This must be found and fixed before k_eff can be
   trusted as an SGB output. **This is exactly the class of bug that only appears
   when you actually run the thing** — which is the point of this test.

2. **The cold solve blocks the tick for 2.7 s.** `/rbmk test` runs 600 iterations
   synchronously inside one tick, hitching the server. For a diagnostic that is
   acceptable; it should not be on a path the player triggers during normal play.

---

## 4. What this proves, and what it does not

**Proves:**
- The 1:4 build works: core + full energoblok, 585k blocks, in ~10 s metered.
- The plant runs live at **~1.5 ms/tick** — a small slice of the 50 ms budget.
- The physics, the pumps, drums, turbines and condensers all evolve together on
  the server clock (this is the "solvers in concert" architecture, already working
  for the reactor + thermal-hydraulic loop).

**Does not prove (still open):**
- **Bit-scale (1:1) performance.** This test was the 1:4 block build. The bit
  engine is still a stub. That remains the one unmeasured risk.
- **k_eff correctness** — see bug 1 above.

---

## 5. On "many solvers in concert" (the Director's other point)

The Director is right that OpenMC/ROSFOND is **not the end of the argument**. The
project is, and will be, **several specialised solvers working together**. The
architecture already reflects this:

- **`PlantSystem` / `PlantTicker`** run a `ReactorModel` plus `MainCirculatingPump`,
  `SteamSeparator`, `Turbine`, `Condenser`, `FeedwaterSystem`, `Eccs`, `Valve`,
  `Rotameter`, `OilSystem` — each a separate model, stepped together every tick.
  This is the pattern; it already works (see §2).
- **`ReactorModel`** is an interface with two implementations today
  (`ReactorCore` point-kinetics, `SpatialReactorCore` 3D), so the neutron solver is
  already swappable.
- **`SimulationConductor`** exists as the intended hub for orchestrating multiple
  models.

The honest position: the **neutron** solver is furthest along (and now externally
grounded in OpenMC/ROSFOND). The **turbine**, **MCP**, **CFD** and **thermal-
hydraulic** solvers are present as lumped models and need the same treatment the
neutron solver got — a cited, validated core each, run in concert. That is a
**roadmap**, not a blocker, and the coupling architecture to hold them is already
in place and proven by this test.

---

## 6. The one line

**The 1:4 RBMK was never broken — it was gated and unbuilt. It now builds
(585,307 blocks) and runs live at ~1.5 ms/tick, comfortably. The next honest steps
are: fix the `k_eff = -0.847` diagnostic bug, and then measure the bit-scale
build, which is the only thing still unproven.**
