# Instrumentation scope — what the model's signal count does and does not cover

Ben questioned the figure **"7,013 signals"**, suspecting the real plant has far
more. He is right. This note records exactly what 7,013 is, and what it leaves out.

## What 7,013 is

`Instrumentation.all()` in the model builds this list, and it is the sum of:

| Group | Signals |
|-------|---------|
| Fuel channels (1661) × 4 — FLOW, VOID, QUAL, TEMP | 6,644 |
| Control channels (211) × 1 — rod position | 211 |
| Drum separators (4) × 2 — LEVEL, PRESS | 8 |
| MCPs (8) × 2 — FLOW, SPD | 16 |
| Turbines (2) × 3 — SPD, P, condenser cooling water | 6 |
| ДКЭ in-core detectors (SPD) | 124 |
| Core scalars — power, reactivity, ORM, period | 4 |
| **Total** | **7,013** |

So the number is a **modelling artefact**, not a sourced plant figure. It is
dominated by one choice — **four analogue values per fuel channel** (6,644 of the
7,013). Change that modelling choice and the number moves; it was never a measured
property of the plant.

## What 7,013 omits

1. **All discrete signals.** Valve and gate positions, pump running/stopped,
   switch and contact states, protection and interlock states, and — crucially —
   the **alarm annunciator lamps themselves**. On a real board these discrete
   points vastly outnumber the analogue readings, and they are exactly what the
   operators watched.
2. **The whole non-reactor block.** Turbine hall (detailed), deaerators,
   feedwater pumps and heaters, condensers, generator and electrical, auxiliary
   and emergency systems, the backup diesel generator panel, radiation monitoring.
3. **Multiple analogue points per real location.** A real fuel channel has more
   than one thermocouple and flow tap; the model collapses these.

## Consequence

- The real plant's signal population is **much larger** than 7,013.
- **SKALA is filled to its own capacity** (its register and processing budget),
  not to any "7,013". The model's number should never be quoted as the plant's.
- For the reconstruction, the signal list is a **starting point** and must be
  extended — discrete signals first, then balance-of-plant — before it can define
  what a full control board displays.

## Correction applied

The figure was previously labelled "7,013 **sourced** signals", which implied it
was a documented plant quantity. It is now labelled a **modelling count**, and
`docs/CONTROL-ROOM-OPTIONS.md` option D no longer claims the room's content is
fully defined by the model.
