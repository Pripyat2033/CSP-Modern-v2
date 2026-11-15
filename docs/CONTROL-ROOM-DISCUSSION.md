# The 1983 Technological Regulation — found, and what it says about the control room

**Status:** discussion document. No code changed. Prepared for Ben to decide.
**Date:** 2026-11-15.

---

## 1. We found the document (the Reddit lead was real)

Ben's lead was correct. The Reddit user **u/Due_Leopard6736**, in
[r/chernobyl post `1hagr52`](https://www.reddit.com/r/chernobyl/comments/1hagr52/),
posted a list of Chornobyl documents and shared a public Google Drive folder.
The folder's `unsorted` sub-folder contains **148 files**, including the exact
document we wanted:

```
Soyuzatomenergo_Tekhnologicheskij-Reglament-po-Ekspluatatsii-
3i4-energoblokov-ChAES-s-reaktorami-RBMK1000_1983.pdf
```

Saved in this repo at
`docs/primary-sources/Reglament-1E-S-II-1983-ChAES-RBMK-1000.pdf`
(3,573,384 bytes, **99 pages**). Plain text extracted to
`docs/primary-sources/reglament-1E-S-II-1983.txt` (164,032 chars).

**Provenance / authenticity checks (all pass):**

| Check | Value |
|---|---|
| Title page | «ТЕХНОЛОГИЧЕСКИЙ РЕГЛАМЕНТ по эксплуатации 3 и 4 энергоблоков Чернобыльской АЭС с реакторами РБМК-1000. 1Э-С-II» |
| Issuing body | Министерство энергетики и электрификации СССР, ВПО «Союзатомэнерго» |
| Approved | Г.А. Веретенников, «25» XI 1983 |
| In force | приказ № 953 от 15.12.1983 |
| PDF metadata | Subject `Регламент 4 блока ЧАЭС`; Author `Чернобыльская АЭС`; Keywords `ЧАЭС, авария 1986 года`; created 2007-04-01 (scan of an original) |
| Signatories | Директор В.П. БРЮХАНОВ; Гл. инженер Н.М. ФОМИН; Зам. по эксплуатации А.С. ДЯТЛОВ; Зам. по науке М.А. ЛЮТОВ; Нач. ПТО А.Д. ГЕЛЛЕРМАН |
| Change log | «ЛИСТ РЕГИСТРАЦИИ ИЗМЕНЕНИЙ» with entries from 15.11.84 onward |

Every signatory is a real, known ChNPP figure, and Dyatlov signing the
regulation the plant ran on is exactly the kind of primary-source linkage the
project demands.

---

## 2. Why this is the keystone source

This is **the operating regulation itself** — the document that defines what
"normal" was, and therefore the baseline that 26 April 1986 deviated from. It
directly serves the original forensic goal (the turbine run-down test):

- **Chapter 3** lists the reactor's emergency protections **АЗ-1 … АЗ-5** with
  their trigger conditions and setpoints. It states, verbatim, that closing the
  turbine stop valves trips **АЗ-5** and that «импульс в АЗ-5 подается в течение
  30 сек» (the АЗ-5 impulse is delivered within 30 s). The regulation therefore
  *contains the exact coupling the test abused* — the turbine run-down → stop
  valves close → АЗ-5 → full rod insertion.
- **Chapter 9** sets the operational reactivity margin: «не менее 26÷30
  стержней» at nominal power, and «при снижении … до 15 стержней реактор должен
  быть немедленно заглушен». This is the **ORM rule** the test night violated.
- **Chapter 3** also states the АЗ-5 condition «повышение давления в реакторном
  пространстве до аварийной уставки – 0,15 кгс/см²» — a protection the
  accident sequence ran past.

So the regulation is not just an equipment list; it is the **rulebook against
which the accident is judged**, written by the people who ran the plant.

---

## 3. What the regulation says about the control room (the instrumentation gap)

The in-world RBMK-1000 we built currently has **zero panels and zero
instrumentation** — it is observable and controllable only by console command.
The regulation tells us precisely what *should* be there, because it names the
workstations and the systems that were monitored.

### 3.1 Control-room topology (from §1.1–1.5)

| Term | Meaning | Role |
|---|---|---|
| **БЩУ-О** | Блочный щит управления — оперативный | The operative block control room. Workstation of the **ЗНСС** (deputy shift supervisor). |
| **БЩУ-Н** | Блочный щит управления — неоперативный | Non-operative panel room (SIUT's auxiliaries, start-up panels). |
| **ЦЩУ** | Центральный щит управления | Central station control room. Workstation of the **НСС** (station shift supervisor). |
| **РПУ** | Резервный пульт управления | Reserve control point — used only if БЩУ-О is unusable (fire / threat to life). Ch. 21. |
| **Пульт «А»** | Desk "A" | The reactor desk — **СИУР** (senior reactor control engineer) runs the reactor from here. |
| **Пульт «Т»** | Desk "T" | The turbine desk — **СИУТ** runs the turbines from here. |

### 3.2 Operator posts named in the regulation

`СИУР` (reactor), `СИУТ` (turbine), `СИУБ` (unit), `ЗНСС` / `НСС` (shift
supervisors), `НСБ` (block shift supervisor). Each post maps to a physical desk
and a set of panels — a natural, sourced structure for an in-world БЩУ.

### 3.3 What the operator actually watched (systems named in the text)

Frequency of mention in the 99 pages, as a rough "what mattered" heat map:

| System | Mentions | Notes |
|---|---|---|
| СУЗ | 111 | Control & protection system |
| МПЦ (КМПЦ) | 98 (30) | Multiple forced-circulation loop |
| ГЦН | 89 | Main circulation pumps |
| барабан-сепаратор (БС) | 72 | Steam drums — level & pressure |
| СФКРЭ | — | Energy-release distribution monitoring (channels ДКЭ-В/Р) |
| СЦК | — | Centralised monitoring system |
| КЦТК | — | Fuel-channel integrity monitoring |
| БЩУ | 29 | Block control room |
| турбогенератор | 23 | Turbogenerators |
| прибор | 25 | Instruments (general) |
| щит | 76 | Panels (general) |
| САОР | 7 | Emergency core cooling |
| УСГС | 7 | Recombiners (hydrogen) |

Plus the panel hardware named directly: **МТК** (mnemonic control board),
**МТО** (deviation board), **ПАП-ГПП** (address panel of the group display),
**ИК** (ionisation chambers), **КД** (fission chambers), **ИВ** (output
indicators), **ИСС** (count-rate meters).

### 3.4 A de-facto equipment taxonomy

The regulation's **ПЕРЕЧЕНЬ СОКРАЩЕНИЙ** (list of abbreviations) is effectively
a sourced system/equipment taxonomy of the energoblok — ~110 terms covering
pumps (`ГЦН`, `ПЭН`, `АПЭН`, `КН-I/II`, `НР`, `НОАП`, `НОНП`, `НПК`, `НСУЗ`,
`НЧК`, `МНУ`), valves (`ГПЗ`, `ГРК`, `ЗРК`, `ДРК`, `СРК`, `ГПК`, `ПК-*`),
turbine plant (`ТГ`, `ЦВД`, `ЦНД`, `СПП`, `ПНД`, `БРУ-Д/К/ТК`), electrical
(`ДГ`, `РДЭС`, `АБ`, `АБП`, `КРУ`, `ОРУ`, `ТСН`), safety (`АЗ*`, `САОР`,
`СОС`, `ББ`, `ПРА`), and monitoring (`СФКРЭ`, `СЦК`, `КЦТК`, `МТК`, `МТО`).
This should be folded into `equipment-register.json` as the authority for
**named-only → specified** promotions (that is task T2, next session).

---

## 4. Options for the in-world control room (discussion only — no code)

The gap: the plant runs, but you cannot *see* or *steer* it except by command.
Four shapes we could build, roughly increasing in effort and fidelity:

- **Option A — Scenery.** A faithful 3D БЩУ: pulы «А» and «Т», БЩУ-О/Н, ЦЩУ,
  РПУ, the panel blocks (МТК/МТО) modelled to the real layout, but **inert**.
  Cheapest; gets the room right; does not close the gap.

- **Option B — Data-driven panels.** Each *modelled* equipment item in the
  register exposes named signals (flow, level, pressure, position). A "panel
  block" aggregates signals and renders gauges/lamps, and can send control
  actions back to the physics. The register becomes the wiring table. Medium
  effort; closes the gap; SGB-consistent (every gauge traces to a source).

- **Option C — Hybrid (recommended).** Build the room (A) and make **only the
  panels that matter for the accident sequence** live (B): ГЦН status, БС level
  & pressure, ORM (rods inserted), АЗ-1..АЗ-5 state, turbine stop valves,
  СФКРЭ/power. This is the smallest live surface that lets a learner *recreate
  and understand the test* — the project's actual purpose.

- **Option D — Full fidelity БЩУ.** Every one of the ~110 systems gets a panel.
  Years of work; not justified until A–C are done.

**SGB note:** whatever we choose, every displayed value must trace to a
computed or sourced quantity (no invented dials). Option B/C make that a
property of the design rather than a retrofit.

---

## 5. Questions for Ben

1. Is the goal the **room** (a place to stand) or the **instrumentation** (a way
   to observe/control)? Both, or one first?
2. Should the first live panels be exactly the **accident-sequence set**
   (Option C), or a broader "normal operations" set?
3. Do we model the operator **posts** (СИУР/СИУТ/ЗНСС) as seats with their own
   desks, so the training courses can assign roles?
4. Console commands — keep as a hidden "engineering" interface, or retire them
   once panels exist?

---

## 6. The other question: can Minecraft run custom engines alongside/under it?

Yes — and this project already proves it. Minecraft's own game loop is not
open to replacement, but a **mod runs inside the same JVM, in-process**, and can
call out to anything. The patterns, cheapest to most invasive:

- **Out-of-process + IPC (what we do now).** OpenMC runs as a separate program;
  a bridge feeds results in. Minecraft only *displays*. This is the project's
  core principle and it already works. Best isolation, no JVM constraints.
- **In-process native library (JNI/JNA).** A C/C++/Fortran solver as a `.so`
  loaded by the mod (the repo already carries `librbmk_solver.dylib`). Fast, but
  a crash takes the game down; platform-specific.
- **Pure-JVM engine.** A second "engine" written in Java, ticked from a
  server-tick hook (`END_SERVER_TICK`). This is what `SpatialReactorCore` already
  does — a real 3D neutron-diffusion solve running live at ~4.4 ms/tick.
- **Server-side only.** For a dedicated server, all of the above run headless;
  clients are just renderers. Good for the "course server" idea.

**Bottom line:** you do not run an engine *under* Minecraft, you run it *beside*
it (separate process or a co-resident library/JVM engine) and use Minecraft as
the 3D front-end. The physics budget is already measured: ~10× headroom on the
50 ms tick for a full 1:1 core. The honest limit is not the engine — it is how
many *displayed* quantities we choose to wire up.

---

## 7. Next steps (not started)

1. Fold the regulation's taxonomy into `equipment-register.json`; promote
   named-only items to specified, resolve the remaining unsourced (task T2); the 1983 regulation has now sourced 36 of them.
2. On Ben's decision for §4, spec the chosen option before any code.
3. Keep the regulation text in-repo as the citation source for both the register
   and the control-room build.
