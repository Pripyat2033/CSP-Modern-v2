# Why the prombaza, not the mod, enforces compliance

Ben's question: *"Instead of trying to make everything accurate here, why isn't
the enterprise and the prombaza itself ensuring the compliance checks?"*

Short answer: it is now, and it should be. The history says so, and the code had
the pieces but had left them unconnected.

## The history

A great deal of what reached the Chernobyl site was defective, off-spec, or
against regulation. The response was not to trust the delivery and not to rely
on a distant ministry to catch it. The site raised its own workshops and checked
each item itself: the workforce stripped an object down, inspected it, and only
then let it into the building. The reported cost was that roughly half the
workforce ended up on this receiving-and-checking work, and that the job took
about **50% longer** as a result.

Two Russian terms cover it:

- **входной контроль** — incoming inspection: the check performed on goods as
  they are received, before they are accepted.
- **дефектовка** — defect-finding by disassembly: taking an item apart to find
  and assess its defects. This is standard repair-shop practice, not a metaphor.

*Verification note:* the exact phrase used in Piers Paul Read's *Ablaze* or Adam
Higginbotham's *Midnight in Chernobyl* could not be confirmed in this session —
both books are lending-gated on the Internet Archive, and every web search
endpoint (Google, Bing, DuckDuckGo, Ecosia, Brave) returned a CAPTCHA wall while
Tavily and the Google Books API were over quota. The two terms above are the
standard Soviet industrial terms for the process; the books' own wording is not
quoted here because it could not be read.

## The point for the mod

This is why a compliance check belongs to the **actor that receives the goods**,
not only to a central authority and not only to the mod author writing "accurate"
values up front:

1. A central check is a single point of failure and a single point of trust.
   The site did not have one; each industrial base had its own desk.
2. The check has a **cost**, and the cost is the interesting part. Diverting the
   workforce to inspection is why the project ran long. A model that makes
   compliance free is not faithful.
3. The check is **imperfect and load-dependent**. When the site is busiest, the
   check is least thorough, and defects slip through. This is the historically
   real failure mode, and it is exactly the one that must be modelled rather than
   hidden behind a perfect pass/fail.

## What was built

The pieces existed but were unconnected. `logistics.MaterialBatch` already
carried `QualityStatus { PENDING, PASSED, DEFECTIVE, QUARANTINED }`, an
`inherentQuality`, and a list of `Defect`s that the workforce had to *find*
(`markDefectAsFound`). Nothing drove it: `MaterialBatch` was referenced by
nothing but itself.

Three new pieces connect it:

- **`prombaza/IncomingControl`** — the gate itself. It models detection
  (a defect is found with probability set by `Defect.discoverability()` and the
  check's thoroughness), thoroughness (falls as the arrival load per inspector
  rises, with a floor), and cost (inspection man-hours make the job take longer).
  Its disposition is the workforce's own rule: a clean item is `INSTALLED`; a
  found defect is `REWORKED` on site or `RETURNED` if too severe; a suspect item
  with no proven defect is `QUARANTINED`. The dangerous case — a defect nobody
  found — installs the item but flags it **latent**.
- **`prombaza/PrombazaControl`** — the desk, as real state on a prombaza. It
  receives batches, records each verdict, books the inspection man-hours, and can
  report what was cleared, what was held, and how many installed items are still
  carrying a latent defect.
- **`/prombaza control <id>`** — the desk, reachable in world. No orphaned code:
  the gate is wired to a command and to the persistent state.

Self-test: `prombaza/IncomingControlSelfTest` — **33 checks**, all passing,
including the load case (a defect that is hard to see slips through when the desk
is busy), the latent case (installed but not passed clean), and the cost case
(inspection at half the building effort gives the "50% longer" multiplier).

## How this joins the other fidelity legs

There are now four gates, and they answer different questions:

| Gate | Question it answers |
| --- | --- |
| `plant/EquipmentRegister` | does this machine **exist** on the plant at all? |
| `plant/MachineValidator` | is the machine's **shell** the right shape and size? |
| `plant/MachineryRegister` | does it **behave** to its real performance numbers? |
| `prombaza/IncomingControl` | was what arrived **checked**, and what got through? |

The first three are checks against the sources. The fourth is the check the
*workforce* does, with the workforce's own fallibility and cost. A latent defect
that входной контроль misses is precisely what the performance conformance gate
(`MachineryRegister`) exists to catch later, by its effect on behaviour — so the
two are designed to work together: the desk models what a busy site let through,
and the conformance check catches what the desk missed.

## Next step

The desk is transient state today; the receipts are rebuilt from deliveries as
they arrive. The next step is to persist the desk (receipts, man-hours booked)
and to drive it from the real delivery path — `LogisticsManager` and
`ConstructionSite.deliverMaterials` currently move a bare double of material
without creating a `MaterialBatch`, so nothing yet reaches the desk in a live
game. That wiring is the open work.
