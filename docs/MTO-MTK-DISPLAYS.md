# МТО / МТК displays — the unit-fingerprint question

Ben's observation: **the МТО displays were removed from units 3 and 4 before the
accident and replaced with a second МТК display.** He asks: *why then do the
regulations still mention МТО displays?* This note records what the regulation
actually says, because the answer is a genuine clue.

## What the units 3/4 regulation says

`docs/primary-sources/reglament-1E-S-II-1983.txt` is the **units 3 and 4**
regulation (*«по эксплуатации 3 и 4 энергоблоков Чернобыльской АЭС»*). It is
approved **25 XI 1983** (order № 953 of 15.12.1983) with amendments through 1985,
and states at the end that its text **corresponds to its state on 26.04.1986** —
i.e. it is the pre-accident text.

Both displays are defined in its abbreviation list (p.8):

- **МТК — мнемотабло контроля** ("control mimic board"), line 186.
- **МТО — мнемотабло отклонений** ("deviation mimic board"), line 187.

And **МТО is referenced operationally**, in the emergency-action chapter
(ch. 10, «Действия персонала при отклонениях»):

- **10.8.3** — «При появлении **на МТО СЦК** сигнала "Кз≤1"…» (on appearance of a
  "Кз≤1" signal on the МТО СЦК…).
- **10.8.4** — «…сигналов "Кз≤1" в одном районе **на МТО СЦК**…».

МТК is referenced in ch. 2 (refuelling, line 525), ch. 4 (line 624-628) and
ch. 10.21.1 (line 2853), where the text names **two** boards: «включить табло
**МТК-1**… включить табло **МТК-2** на контроль температуры газа» — i.e. a
**МТК-1 and a МТК-2**.

## The discrepancy

If the МТО was physically removed from units 3 and 4 before the accident, then the
units-3/4 regulation current to 26.04.1986 **still referencing МТО СЦК** is a real
discrepancy. Possible explanations (none yet confirmed):

1. **Inherited text.** The regulation may derive from the units 1/2 regulation
   (*1Э-С-I*) and the МТО references were not updated when units 3/4 changed the
   displays — common in Soviet documentation practice.
2. **МТО СЦК is a sub-display.** "МТО СЦК" may be a specific СЦК (Скала) device
   distinct from the general "МТО displays", and may have survived the change.
3. **Timing.** The display change may post-date the last amendment to this text.

**This is unresolved and needs evidence.** It is exactly the kind of question that
a photograph can settle — but not by me (see below).

## Why this matters: the displays are a unit fingerprint

Ben distinguishes the units by **equipment present/absent** — specifically the
МТО vs a second МТК — not by signage. That means the *display complement* is a
**fingerprint of which unit a control room is**. This is useful in two ways:

- It lets a photographed control room be **identified** by which displays it has.
- It gives a **checkable prediction** for the reconstruction: units 3/4 should
  show **two МТК boards and no МТО**; units 1/2 the МТО.

## The vision problem

The agent **cannot see photographs** — only text and tool output. Therefore:

- Unit identification from a photo (by ceiling or by equipment) must be done by a
  **human** (Ben), who can see.
- The agent **can** read **text** in a photo, via OCR (`tools/ocr_panel.py`): the
  labels printed on the displays (МТК-1, МТК-2, МТО, СЦК, panel legends) are
  machine-readable. So a photo of a control room can be **partly identified and
  mapped by OCR of its own labels**, without the agent seeing the picture.

This split — human sees, agent reads and builds — is the working method, not a
dead end.
