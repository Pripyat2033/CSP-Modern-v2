# SKALA monitored every signal — the target scope

Ben's directive (15 Nov): **SKALA monitored every single signal there was.** Not a
subset, not just the analogue reactor readings — *every* signal, discrete and
analogue, including every alarm. The model must therefore aim to carry the
**complete** signal population of the unit, because that is what the real machine
watched, and the whole point of the reconstruction is fidelity to the real machine.

This supersedes any earlier framing in which "7,013 signals" was treated as a
figure to be matched. 7,013 is a modelling artefact of the reactor analogue core
(see `docs/INSTRUMENTATION-SCOPE.md`); it is a **floor**, not a target.

## What "every signal" means, concretely

The real unit's signals fall into these families, and **all** must be present:

1. **Analogue process signals** — temperatures, pressures, flows, levels, powers.
   Partly present (reactor side); balance-of-plant mostly absent.
2. **Discrete process signals** — valve and gate positions, pump
   running/stopped/tripped, switch and contact states, damper and gate positions.
   Almost entirely absent.
3. **Protection and interlock signals** — the АЗ-1…АЗ-5 logic, ПРА, ТЗБ, СРВ,
   ПРВ, and every trip. The regulation names these repeatedly (АЗ-5 ×33, СРВ ×35,
   ПРВ ×23); the model carries almost none.
4. **Control-rod signals** — per rod, not per core: position, НК/ВК limit
   switches, ПК overcompensation, selsyn dial lamps. See `docs/ROD-SIGNALS.md`.
5. **Annunciator (alarm) signals** — every lamp on the МТК / МТО makets: the
   colour-coded, grouped, flashing annunciator points. **This is the largest and
   most important family for the control room**, and it is entirely absent.
6. **Electrical signals** — generator, transformers, switchgear, auxiliaries.
   Absent.
7. **Radiation-monitoring signals** — absent.

## Why this matters

- SKALA's whole purpose was *centralised control* — it existed to read and record
  **all** of it. A model that carries only the reactor analogue core does not
  exercise SKALA as it really was.
- The control room is an **annunciator machine** above all. The alarm signals
  (family 5) are not an afterthought; they are the room's primary content. So the
  "every signal" directive and the control-room question are the **same** problem,
  not two.

## The honest consequence

The model today carries a small fraction of the real signal population — the
reactor analogue core and little else. "Every signal" is therefore a **large**
body of work, not a tweak. It is, however, exactly the work the project exists to
do, and it is **well-sourced**: the regulation enumerates protections, rod types
and signal names, and the SKALA restoration material describes the machine's input
families.

## First concrete step

Add the **control-rod signal family** properly, because it is fully sourced from
the regulation and it is the smallest complete family:

- give each control channel a **rod type** (ЛАР, АР, АЗ, ПК-АЗ, ПК-РР, РР, УСП);
- carry **position** plus **НК** and **ВК** limit switches per rod;
- carry the **ПК** overcompensation signal where the type has one;
- carry the two **selsyn lamps** (green full-depth, orange top-of-stops) once the
  dial layout is confirmed from a photograph.
