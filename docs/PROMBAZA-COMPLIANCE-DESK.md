# The prombaza's входной контроль desk — the site checks its own deliveries

Ben's question: *"Instead of trying to make everything accurate here, why isn't
the enterprise and the prombaza itself ensuring the compliance checks?"*

Short answer: it is now, and it should be. But the reason matters, and the first
draft of this doc got it wrong. Read the next section before anything else.

## It was NOT a rule from above

Do not read this as an official procedure. No decree from a ministry said
incoming goods must be inspected, and nobody at the Politburo wrote "thou shalt
check". The driver was **local, selfish and practical**:

- Material piled up on the **rail sidings** — at Chernobyl, the siding at
  **Yanov station**. A wagon left standing past its norm cost the enterprise
  money, per wagon, per day (**простой вагонов**, demurrage). A backed-up yard
  was a **fine**.
- The workforce was **sick of stripping out defective material that had already
  been built in** — re-doing work is expensive and demoralising.

So the site was pulled both ways at once: **clear the yard** (stop the fines) but
**do not install junk** (stop the rework). The check was the **compromise** it
settled on. It was not compliance for its own sake; it was a fight under a fine,
and the desk is where that fight is fought.

## Why the actor, not the author, enforces it

1. It is **their** yard, **their** fine, and **their** rework. A central check
   has none of that pull, so it does not model the real behaviour.
2. The check **costs**, and the cost is the interesting part. Roughly half the
   workforce ended up on the receiving dock, and the project ran about **50%
   longer**. A model where compliance is free is a lie.
3. The check is **imperfect and load-dependent**. Busiest yard, most rushed
   check, most defects through. This is the real failure mode, and it must be
   modelled, not hidden behind a perfect pass/fail.

## The deeper fear: a perfect reactor

The real risk this design is fighting is that an AI builds a **perfect plant** —
everything works first time, the numbers come out clean, the reactor is born
correct. It was not. On the RBMK units the equipment was **tested on
installation**, by the plant's own engineers, with the plant's own arithmetic —
a **slide rule, mental calculation, and Skala** where it helped. So:

- the figure that came back was **not the figure on the drawing** — it was that
  figure plus the error of whoever measured it and however they worked it out;
- tests **failed**, and a machine went back;
- and even a **good** machine could fail its first test, because crude arithmetic
  can put a good reading outside a tight band.

Nothing comes out of the vault perfect. Some things come out **wrong and have to
be measured again**. That is what `plant/Commissioning` models.

## The pieces that already existed — and were orphaned

The mod had the machinery and had left it unplugged. `logistics.MaterialBatch`
carried `QualityStatus { PENDING, PASSED, DEFECTIVE, QUARANTINED }`, an
`inherentQuality`, and a list of `Defect`s the workforce had to *find*
(`markDefectAsFound`, `Defect.discoverability`). `work.Naryad` had
`REJECTED`/`PENDING_INSPECTION`. `agent.Prorab` had `simulateInspection`. **None
were wired to anything.** So "why isn't it checking?" answered itself: it was
built and unplugged. A gate that is not wired to a command or a delivery is not a
gate.

## What was built

Five new pieces, each one a leg of the same idea.

| Class | What it is | Self-test |
| --- | --- | --- |
| `prombaza/IncomingControl` | the gate: detection x thoroughness (falls with load, floor 0.25) x cost | 33 checks |
| `prombaza/GoodsYard` | the siding: wagons, free time, **demurrage**; the backlog is the desk's load | 9 checks |
| `prombaza/PrombazaControl` | the desk as state: receipts, cleared/held/latent, man-hours booked | (in the two above) |
| `zone/MaterialSupply` | the seam: variable supplier quality -> the yard -> the desk -> the site | 10 checks |
| `rbmk/plant/Commissioning` | test on install: slide rule / mental / Skala; a good machine can fail | 11 checks |

**The dispositions are the workforce's own**: a clean item is `INSTALLED`; a
found defect goes to the on-site workshop (`REWORKED`) or back to the supplier if
too severe (`RETURNED`); a suspect item with no proven defect is `QUARANTINED`.
The dangerous case — a defect **nobody found** — is `INSTALLED` but flagged
**latent**: it goes into the building, and only its behaviour betrays it later.

**The site does not get a clean number.** `MaterialSupply` asks the suppliers for
a quantity and receives back only what the desk cleared, at the suppliers' own
spread of quality. Reworked material still arrives (the workshop fixed it);
returned and quarantined material does not, so the site is simply **short**. The
plan and the reality are not the same thing.

## Wiring (so it is not orphaned again)

- `/prombaza control <id>` — the desk report: cleared / held / latent / man-hours.
- `/prombaza yard <id>` — the siding: what is standing, how long, the fine.
- `/prombaza receive <id> <n>` — work standing wagons through the desk. Clearing
  a big queue in one go lets more through: the trade the demurrage creates.
- `ZoneCommand.spawn` hands every enterprise the prombaza's supply line, so all
  material goes through the desk before it reaches a site.
- `ZoneTicker.ensureSupply` re-attaches the line every day, so a world restored
  from a save does not quietly go back to building with unchecked material.
- `PrombazaState` persists the yards and the desks, so the standing wagons and
  the man-hours survive a restart.

## How this joins the other fidelity legs

| Gate | Question it answers |
| --- | --- |
| `plant/EquipmentRegister` | does this machine **exist** on the plant at all? |
| `plant/MachineValidator` | is the machine's **shell** the right shape and size? |
| `plant/MachineryRegister` | does it **behave** to its real performance numbers? |
| `prombaza/IncomingControl` | was what arrived **checked**, and what got through? |
| `plant/Commissioning` | does it **work** when it is first started — or does it fail? |

The first three are checks against the sources. The last two are the checks the
*workforce* does, with the workforce's own fallibility, cost, and crude
arithmetic. They interlock: a latent defect that входной контроль missed is
precisely what commissioning (or, later, the conformance gate) catches by its
effect on behaviour.

## Honest provenance

The **mechanism** in every piece is real and documented: demurrage on standing
wagons, incoming inspection that is load-dependent, commissioning tests worked
out by hand. The **numbers** are not quoted from a source and are marked so:
`GoodsYard.FREE_TIME_DAYS` and `FINE_PER_WAGON_DAY`, `Commissioning.Method` error
bands and `DEFAULT_TOLERANCE`, `MaterialSupply.DEFAULT_SUPPLIERS` and
`UNITS_PER_WAGON`. Each carries a `RECONSTRUCTION` note so it can be replaced
with the real tariff, band, or norm.

The exact wording in Piers Paul Read's *Ablaze* or Adam Higginbotham's *Midnight
in Chernobyl* could not be confirmed this session — both are lending-gated on the
Internet Archive, and every web search endpoint was CAPTCHA-walled or over quota.
The process is therefore named by its standard Soviet terms (входной контроль,
дефектовка, простой вагонов) rather than quoted from the books.

## Still open

The desk is fed by the enterprises' own deliveries and by `/prombaza receive`, but
the two are not yet one stream: an enterprise's delivery goes through the desk
inline, while `/prombaza receive` works a manually-stocked siding. The next step
is to let the player stock a siding and let an enterprise draw from it, so the
backlog the player sees is the backlog the enterprises are building against.
