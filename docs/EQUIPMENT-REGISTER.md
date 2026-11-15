# EQUIPMENT REGISTER — the completeness authority for the energoblok

**What this is.** The register of everything a real RBMK-1000 operator would
recognise inside an energoblok — every machine, tank, pump, valve, crane, fan and
panel — with a source for each, and a status saying how far it has got into the
build. It is the machine-checkable answer to the Director's standing concern:

> "If I did not name every machine, it will not get modelled — and I cannot list
> them all."

That fear was correct, and the register is the fix. The list is **researched from
the documents, not dictated from memory**, so a missing item is a visible red
entry rather than a silent omission.

## The mechanism

Two halves, kept in step:

- **`src/main/resources/equipment-register.json`** — the data (one record per item).
- **`plant/EquipmentRegister.java`** — the loader and the gate
  (`namedOnly()`, `unsourced()`, `summary()`), plus
  **`plant/EquipmentRegisterSelfTest.java`** (12 checks).

The gate does not fail the build — the plant is legitimately incomplete — but it
prints the findings, so "what is still missing" is a number that cannot quietly go
to zero while the build is unfinished. **An empty findings list would mean the
register had drifted out of date**, which is the exact failure this exists to stop.

### Status values

| Status | Meaning |
|---|---|
| `modelled` | Built and validated in-game (has a spec and is placed). |
| `specified` | Has a spec/geometry but is not yet placed, or is a partial build. |
| `named-only` | Named in a source, not yet built. **The work queue.** |
| `unsourced` | Named but with no source — **must be resolved or removed.** |

## The sources (primary first)

- **INSAG-7** — IAEA, *The Chernobyl Accident: Updating of INSAG-1*, 1992.
  Table II-II **"Main equipment for an RBMK unit"** is a numbered component list
  with per-unit counts and masses (Fig. 11-2 cross-section). This is the spine of
  the register. It is where the **ventilation fans** and the **cranes** come from.
- **NUREG-1250** — US NRC, *Implications of the Accident at Chernobyl for Safety
  of US Commercial Nuclear Power Plants*, 1989. The systems descriptions
  (§2.1–2.6) give the MFC circuit, ECCS/САОР, accident localization, gas circuit,
  control-rod cooling and the electrical supply.
- `docs/RBMK-PLANT-SYSTEMS.md` — the project's own sourced system notes.
- Wikipedia — **lead only, never an authority.** Anything resting only on a wiki
  is marked as such and is a finding.

## What the register says today

From `EquipmentRegisterSelfTest`:

```
equipment register: 106 items — 25 modelled, 0 specified, 78 named-only, 3 unsourced
```

**The three `unsourced` items are deliberate and correct** — they are real
equipment that the project has not yet traced to a source:

- Instrument compressed-air supply system
- Generator hydrogen cooling system
- Fire suppression system (plant-wide)

Under the "nothing is guessed" rule they are **flagged, not filled in**. Resolving
them (or removing them if they were not present) is a real, named task.

## The items the Director specifically worried about — they ARE here

The register was written to catch exactly the easy-to-forget equipment. Present,
sourced, and mostly `named-only`:

| Item | Source | Status |
|---|---|---|
| **VDN suction ventilator** (level +43.0) | INSAG-7 II-II #21 | named-only |
| **Exhaust ventilator** (level +35.0) | INSAG-7 II-II #22 | named-only |
| **САОР / ECCS hydraulic accumulators** (12 × 25 m³) | INSAG-7 II-II #30, NUREG-1250 §2.6.2 | modelled |
| **ECCS pumps / header / fast-acting valve** | INSAG-7 Fig. 11-1 #31–33 | named-only |
| **Diesel generators** (3 × 5.5 MW, <15 s start) | NUREG-1250 §2.5.3 | named-only |
| **Bridge cranes** (central hall, MCP room, machine hall) | INSAG-7 II-II #19,20,32 | named-only |
| **Refuelling machine** (~350 t) | INSAG-7 II-II #18 | named-only |
| **Reactor gas circuit** (80% He / 20% N₂) | NUREG-1250 §2.2.4 | named-only |
| **Control-rod cooling circuit** | NUREG-1250 §2.2.3 | named-only |
| **Pressure suppression pool** (3200 m³) | NUREG-1250 §2.6.6 | named-only |
| **Hydrogen removal system** (heater/contactor/blower) | NUREG-1250 §2.6.7 | named-only |

## How to use it (the loop)

1. **Add a real item** you know is missing (with a source) — it becomes a
   `named-only` finding.
2. **Build an item** — change its status to `modelled` (and give it a spec).
3. **Run the gate** — `EquipmentRegisterSelfTest`, or in-game `/rbmk register`.
4. The `named-only` list is the honest "what is still to build". It shrinks as the
   build grows, and it can never be silently empty.

## What this does NOT yet do (recorded, not hidden)

- The register is **not yet exhaustive**. INSAG-7 Table II-II and NUREG-1250 cover
  the major and many minor components; a full energoblok inventory (every valve,
  every instrument, every cable route) needs the plant's own operational manual and
  technical description (техническое описание), which we do not have yet.
- The gate is a **self-test + command**, not yet wired into the SGB "done"
  criterion. The next step is to make "SGB" refuse while any item is `unsourced`.
