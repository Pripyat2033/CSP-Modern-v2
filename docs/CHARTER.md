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

## 4. The plant-platform question — OPEN, and the real test (2026-11-09)

Rule 1 sequences the work: the enterprises first, then the plant. The Director
**deliberately broke that rule** for one bounded reason — to find out whether this
mod can carry an RBMK-1000 *at all* before the workforce spends a month building a
plant the platform cannot run. His analogy: a car whose engine does not turn over.

**What the spike has and has not shown.** A reactor-only bench shows the neutron
solver's arithmetic fits a tick (lumped step 0.187 ms; a live 3D solve 26.4 ms/tick
of the 50 ms budget — see `REACTOR-GEOMETRY.md` §5). **That is not the platform
question, and it proves nothing about it.** The platform question is the **whole
energoblok** — the pipework, valves, pumps, tanks and meters running through the
building, their block entities, and the rendering of all of it, at once. A real
RBMK-1000 is mostly *piping*: 22 group distribution headers per circuit feeding
38–41 channels each, **a flow-control valve and a ball flow meter on every one of
the 1,661 channels**, an upper steam line per channel, 12 downpipes per separator.
None of that is built yet: `planPlant` places walls and machine shells only, and
`Part.PIPES` is declared and counted but **never placed (always 0)**.

**So the spike is not closed — it is redefined.** The authoritative test is the one
the Director named: **he runs the full energoblok inside Minecraft, himself.** No
bench the agent produces is authoritative; a headless number measures CPU arithmetic,
not the tick with entities and rendering. The exit criterion is his own, in the same
form as the 608-worker GO:

> If Minecraft can carry the **whole energoblok** — every compartment, every machine,
> every pipe, valve, meter and tank, running — then the platform is proven and the
> enterprises proceed.

Until that is run, the exemption stays **open and bounded**: no reactor/equipment
*content* beyond what the platform test requires.

---

## The method, in one line

Model the **procedure and the system**, place the **visuals** on the platform's best
terms, and **measure** everything rather than assume it.

---

*Agreed 2026-11-09. Amendable by the Director.*
