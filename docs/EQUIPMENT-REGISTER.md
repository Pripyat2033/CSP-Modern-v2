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
equipment register: 140 items — 25 modelled, 0 specified, 114 named-only, 1 unsourced
```

**One `unsourced` item remains** — real equipment the project has not yet traced
to a source:

- Fire suppression system (plant-wide)

The other two former `unsourced` items were **resolved against the 1983 operating
regulation** (see below): the instrument compressed-air system is the compressor
station (Reglament §8.4.18), and the generator hydrogen cooling is named in the
regulation's §8.4. The fire-suppression system is *not* named in the regulation,
so it stays flagged. Under the "nothing is guessed" rule it is **flagged, not
filled in**.

## The 1983 operating regulation as a source (added 2026-11-15)

The plant's own **Технологический регламент по эксплуатации 3 и 4 энергоблоков
ЧАЭС с реакторами РБМК-1000, 1Э-С-II (Союзатомэнерго, введён 15.12.1983)** —
99 pages, signed by Bryukhanov, Fomin and Dyatlov — is now in-repo
(`docs/primary-sources/`) and is a primary source for the register. It names
equipment the earlier sources missed:

| Item | Regulation reference | Status |
|---|---|---|
| **Главная паровая задвижка (ГПЗ)** — main steam stop valve; closing it trips АЗ-5 | Ch.3 п), §2.4.2 | named-only |
| **Стопорно-регулирующий клапан (СРК)** — turbine stop/control valve | Ch.3 п), Ch.10 | named-only |
| **Маслонасос уплотнений (МНУ)** — MCP seal-oil pump | Ch.10 | named-only |
| **Гидростатический подшипник (ГСП)** — MCP hydrostatic bearing | §ГЦН | named-only |
| **Валоповоротное устройство (ВПУ)** — turbine turning gear | Ch.13 | named-only |
| **Насосно-теплообменная установка (НТУ)** — pump/heat-exchanger unit, schemes Л/Д | §2.4 | named-only |
| **Азотно-кислородная станция (АКС)** — nitrogen-oxygen station | Ch.10 | named-only |
| **Мокрый газгольдер (МГ)** — wet gas holder | abbreviation list | named-only |
| **Компрессорная станция** — compressor station, 6 kgf/cm² air | §8.4.18 | named-only |
| **Бак / насос чистого конденсата (БЧК/НЧК)** — clean-condensate reserve, 2000 m³ | §2.4, §8.4.11 | named-only |
| **Канал охлаждения отражателя (КОО)** — reflector cooling channels | abbreviation list | named-only |
| **СФКРЭ / СЦК / МТК / МТО** — energy-release monitoring, central monitoring, boards | Ch.18, §9.3 | named-only |
| **КРБ / СК** — radiation-safety monitoring | abbreviation list | named-only |
| **БЩУ-О/Н, ЦЩУ, РПУ** — block/central/reserve control rooms, desks А/Т/П/Э | §1.1–1.5, §2.4.10, Ch.21 | named-only |
| **КАЗ** — AZ-5 scram button | abbreviation list, Ch.3 | named-only |
| **ИК / КД / ИСС / УП / ИВ** — ionisation & fission chambers, count-rate meters, position/output indicators | §2.11 | named-only |
| **ПРА / КРУ / ОРУ / МВ / ТСН / АБП** — protection automation, switchgear, switchyard, breakers, transformers, UPS | §8.4 | named-only |
| **СОС** — sprinkler cooling system of the sealed zone | abbreviation list, Ch.2.6 | named-only |

A first batch of these is now **real in-game blocks** (see below), placed in the
creative tab: ГПЗ, СРК, МНУ, ВПУ, НТУ, БЧК, компрессорная, МГ, пульт БЩУ, КАЗ,
КРБ, МВ and ДГ.

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

- The register is **not yet exhaustive**. INSAG-7 Table II-II, NUREG-1250 and the
  1983 regulation cover the major and many minor components; a full energoblok
  inventory (every valve, every instrument, every cable route) still needs the
  plant's **техническое описание** (technical description) and the equipment
  datasheets, which we do not have yet. The regulation's abbreviation list is the
  best taxonomy we have so far, and it is now largely mined.
- The gate is a **self-test + command**, not yet wired into the SGB "done"
  criterion. The next step is to make "SGB" refuse while any item is `unsourced`.
