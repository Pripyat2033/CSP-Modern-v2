# Reactor geometry — what it means here, and where we stand

## 1. The claim

Reactor geometry is a science, not a style. A reactor is **critical by a
margin**: k_eff sits within ~0.1 % of 1.0, so the neutron economy is marginal and
marginals are decided by geometry — the migration length against the buckling,
the moderator-to-fuel ratio, and where the absorbers sit. A design is approved
because it was *calculated* to close that balance, not because it looks right. A
1 m cube with a sound effect has no neutron economy at all.

This doc records what that means for CSP-Modern-v2, and what is still missing.

## 2. What the sim consumes — and what it does not

The physics consumes **real metres and material fractions**, never block counts.
The one scale decision is where the world is drawn, and it never enters a
calculation:

- `RbmkGeometry` holds real SI: 250 mm pitch, 88 mm channel, 7 m active height,
  14 m stack, 4.5 m displacer, 0.4 m/s rods.
- `RbmkXsec` **homogenises one lattice cell** into two-group cross sections —
  graphite 0.75, water/steam 0.12, fuel 0.13 — the standard reactor-physics move.
- `RbmkSolver3D` runs a real 3D two-group diffusion solve on the physical mesh:
  **51×51 radial × 28 axial**, one cell per 250 mm pitch, masked to the active
  circle (52,416 active nodes = 1,872 channels × 28).
- `RbmkCore` holds per-channel, per-node material/flux/void/temperature.

`RbmkCore` states the point directly: the core is **data**, and "its granularity
does not depend on how many blocks the world spends on it." So the 1:4 vs 1:1
question is a **render** decision; the physics has always run at 1:1.

## 3. What is already genuine

- Two-group diffusion, per-channel/per-node, live (not a chat command).
- Point kinetics + xenon chain + reactivity feedbacks, coupled in `ReactorCore`.
- The rod-worth axial curve, including the graphite-displacer **positive scram**,
  reproduced end-to-end by `AccidentSequence`.
- `MachineValidator` checks a built multiblock's shell against its spec.

## 4. Where we are not SG+ yet

1. **Cross sections are placeholders.** `RbmkXsec` calls its values
   "representative … TO VERIFY against a NIKIET/SKALA dataset." Real reactor
   geometry means cross sections *derived from the actual lattice* (a lattice code
   — WIMS/CASMO/GETERA — or primary data), not hand-set.
2. **Criticality is calibrated, not earned.** `ReactorCore.calibrate()` sets
   `rhoExcess` so ρ = 0 at the nominal point — the code itself calls this "the
   structure of an operator-training simulator." The raw geometry gives a cold
   k_eff of **1.265** (see §5); the sim clamps it. For SG+, the geometry must
   produce k_eff = 1 and the **positive void coefficient from the geometry**, not
   from a tuned constant.
3. **The core map is approximate.** The counts are exact (1,661 fuel + 211
   control), but *which* cells are AZ / РР / АР / УСП is a regular pattern until a
   cartogram (core drawing) is obtained. The flux shape and the accident depend on
   the real map.
4. **The geometry is not yet load-bearing.** The build and the physics both derive
   from `RbmkLattice`, so they are consistent by construction — but the mission's
   gate ("mis-built is refused and flagged") needs the solver to read the *built*
   core, so a wrong build yields a wrong k_eff.

## 5. The measured budget (the engine turns over)

> **Scope of these numbers — read this first.** They measure the **neutron solver's
> arithmetic only**: CPU time to step the core. They say **nothing** about the whole
> energoblok — the pipework, valves, pumps, tanks and meters, their block entities,
> or their rendering. The platform question (can Minecraft carry the *whole
> building*) is **open**, and the only authoritative test is the Director running it
> in-game. See `CHARTER.md` §4.

Single-thread, in-sandbox JDK, on this machine:

| What | Cost | Budget | Verdict |
|---|---|---|---|
| Core state step (lumped) | **0.187 ms** | 50 ms/tick | 267× headroom |
| 3D cold solve | 409 ms, 93 iters (≈9 ticks) | — | one-off |
| 3D warm iteration | **4.42 ms** | 50 ms/tick | ~11 fit in a tick |
| 3D live (6 iters/tick) | **26.4 ms/tick** | 50 ms/tick | fits at 53 % |
| Core memory | 1.65 MiB state + 0.80 MB flux | — | negligible |

Build side (already measured in-game): the whole plant is **528,620 blocks**
(core 231,316 + energoblok 297,304); **392 ms** placed at once, or **9 s** metered
at 3,000/tick with the tick staying comfortable.

So: the simulation carries a full per-channel RBMK **with room to spare**, and a
live 3D solve **fits inside one tick** — heavy, but it fits. The "one block per
core node" alternative (52,416 blocks) is the cost we *avoid* by keeping the core
as data.

## 6. What "the core is SG+" means (acceptance test)

Per `docs/SPECIMEN-SHEET.md` — *invented → fail, missing → fail*:

- [ ] k_eff = 1.0 **earned from the geometry** at the operating point (not
      calibrated).
- [ ] The positive void coefficient **emerges from the lattice**, matching the
      reference value within a stated tolerance.
- [ ] Cross sections from a cited lattice calculation (or NIKIET/SKALA data), each
      with a source.
- [ ] The rod pattern pinned to a real cartogram (or explicitly UNKNOWN).
- [ ] The solver reads the **built** core, so a mis-built core is refused and
      flagged.

## 7. The bit-scale layer (presentation, and why it matters)

The physics is 1:1 already. What is *not* yet 1:1 is the **build you can check**:
at 1:4 a channel is a stand-in block, so the world cannot be measured against a
drawing at true scale. At bit scale (N = 16 → 62.5 mm bits) a channel is a real
~1.4-bit tube in a real 4-bit lattice cell — the core you walk through *is* the
core the solver reads, at true scale. That is the argument for the bit engine:
not looks, but that it makes the build **verifiable against the design**, which is
the whole point of the field this doc is about.

**The whole site is bit-scale.** `PROJECT_NOTES.md` makes bit-scale "the default
and mandatory standard" and forbids Block-Scale (plain 1 m blocks) on the main
ChNPP site — because the plant's working parts (88 mm channel on a 250 mm lattice,
6 m construction grid) are smaller than a metre and cannot be placed with a 1 m
tool. There is no "1 m block shell": the metre block is only the engine's storage
container for 16³ bits. See `SCALE-DECISION.md`.

Items §4.1–§4.4 are the reactor-geometry work and need no bit engine; the bit
engine is the presentation layer that closes the loop in §6.
