# CHARTER

The standing rules of the reconstruction, agreed with the Director. Three constraints,
and the evidence that each is sound.

## 1. Fidelity target — systemic, and as visual as the platform allows

**Every element is one a real operator would recognise. Nothing is invented.**

A real *энергетик* trained on the plant must find, here, what they knew: the right
controls, in the right places, doing the right things, with the right labels. What is
*not* promised is a photograph of the paint.

- Systemic fidelity (function, position, label, state, logic) is **mandatory**.
- Visual fidelity is **as high as the platform allows** — and the method (a
  high-resolution panel picture with each control a named, clickable point, as shown by
  the MAKET) carries hundreds of controls per room cheaply.
- Nothing invented. Nothing missing. Every element traces to a reference. Gaps are marked
  `[UNSOURCED]` and flagged, never guessed. (Detail: `SPECIMEN-SHEET.md`.)

**Test:** would a real operator find anything here that was not there, or miss anything
that was? Invented → fail. Missing → fail.

## 2. Platform posture — original, open, mirrored, pinned

**The plant is run by this project. Any outside presence — of any kind — is a guest under
escort, never an authority.**

- Original code; no game assets redistributed; non-commercial.
- Open and mirrored, so the work exists in many copies and cannot be unmade from one.
- Pinned toolchain, so an upstream change cannot silently break the build.
- The aspiration: a hardened server where the project holds the keys — a whitelist, and
  rooms locked to anyone not admitted. (Detail: `PLATFORM.md`.)

## 3. Performance budget — measured, not assumed

**One server tick must stay under 50 ms (twenty ticks a second). A feature that cannot
meet the budget does not ship.** The system must never require the renderer: all logic
runs headless.

**The budget is met and the GO is given.** On the Director's machine (a non-commercial
PC), the full workforce was put in the world and stood:

- **608 workers** across **10 enterprises**, plus the barge — **with no frame drop**.

That is the Director's own criterion — *"if Minecraft can handle all 10 Enterprises and
the full workforce, that is a sign we are good to proceed"* — and it was met. **We
proceed.**

**Tools that made it measurable:** `/zone muster [count]` (bring the crowd on a little at
a time), `/zone perf` (the tick time and a plain verdict), `/zone demob` (clear the
ground). `WorkforceSelfTest` guards the number itself.

## The method, in one line

Model the **procedure and the system**, place the **visuals** on the platform's best
terms, and **measure** everything rather than assume it.

---

*Agreed 2026-11-09. Amendable by the Director.*
