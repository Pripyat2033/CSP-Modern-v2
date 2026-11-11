# RBMK-1000 machine geometry — the real numbers, and what they become

This file exists because the plant machines were being built as plain coloured
blocks. They are not decorations; each is a specific machine of a specific size,
and the Director's rule is that we build the machine, not a cube that stands for
it. Every figure below is quoted from a source, then converted once, at 1:4
(1 Minecraft block = 0.25 m — see `RbmkGeometry.METRES_PER_BLOCK`).

Scale reminder: the reactor core is 56 blocks across (14 m stack). The machines
are comparable in size — a steam drum is 124 blocks long, a turbogenerator is
156 blocks long. This is a real power station, not a model of one.

## Steam separator drums (барабан-сепаратор)

Four drums, in side compartments above the reactor top. **Pairs** of drums per
half; two steam collectors per drum.

| property | value | source |
|---|---|---|
| diameter | 2.8 m | Wikipedia, *RBMK* §"steam separators ... each has 2.8 m diameter" |
| length | 31 m | ibid. "31 m length" |
| wall thickness | 10 cm | ibid. "wall thickness of 10 cm" |
| mass | 240 t | ibid. "weighs 240 t" |
| count | 4 | ibid. "pairs of thick horizontal drums" |

At 1:4 → **11 blocks diameter x 124 blocks long**, horizontal cylinder, axis
along the reactor's long axis. This is why the separator is a *cylinder*, not a
box: it is a pressure vessel.

## Turbogenerators (турбогенератор)

Two per unit, in the 600 m machine hall beside the reactor building.

| property | value | source |
|---|---|---|
| electrical output | 500 MW each (2 x 500 = 1000 MWe) | *RBMK* §"pair of 500 MW hydrogen-cooled turbogenerators" |
| turbine type | K-500-65/3000, Kharkiv (Turboatom) | *RBMK* §"the venerable five-cylinder K-500-65/3000" |
| cylinders | 1 high-pressure + 4 low-pressure = 5 | *RBMK* §"one high-pressure rotor and four low-pressure ones" |
| generator | TVV-500 (also written TBB-500) | *RBMK* §"electrical generators are the TVV-500" |
| speed | 3000 rpm | *RBMK* §"nominal rotational speed is 3000 rpm" |
| overall length | 39 m | *RBMK* §"The turbogenerator is 39 m long" |
| HP cover diameter | ~4.25 m | turbine datasheet §"патрубок Ду-1200" (1.2 m HP exhaust) |
| LP cover diameter | ~7.5 m | turbine datasheet §"ЦНД", 4 x Ду-1200 exhaust |
| turbine (5 cylinders) length | ~70 m | turbine datasheet; the 39 m in *RBMK* counts the turbogenerator |
| overall mass | 1200 t | *RBMK* §"its total weight is 1,200 t" |
| rotor mass (both) | ~200 t | *RBMK* §"combined weight of the rotors is almost 200 t" |
| stator cooling | water; rotor cooling | hydrogen | *RBMK* §"stator is cooled by water ... rotor by hydrogen" |

At 1:4 → **156 blocks long**. The machine is a long shaft: the **five cylinders**
(1 HP + 4 LP) in a row, then the generator body. Only the five massive white
circular **covers** show in the hall — the rotors, diaphragms and blade rings are
inside. Block 4 had the newer K-500-65/3000-2 (*RBMK* §"Block 4 had the newer
ones").

## Main circulating pumps (ГЦН)

Two independent circuits, **four pumps each** (three running, one standby) = 8.

| property | value | source |
|---|---|---|
| capacity | 5,500–12,000 m3/h | *RBMK* §"capacity of 5,500–12,000 m3/h" |
| normal flow | 8,000 m3/h per pump | ibid. "normal coolant flow is 8000 m3/h per pump" |
| motor | 6 kV electric | ibid. "powered by 6 kV electric motors" |
| count | 8 (4 per circuit) | ibid. "four main circulating pumps ... one half of the reactor" |

## Deaerators (деаэратор)

Four per unit; they also serve as feedwater storage tanks.

| property | value | source |
|---|---|---|
| count | 4 | *RBMK* §"four deaerators where dissolved ... gases are removed" |
| role | deaeration + feedwater storage | ibid. "deaerators also serve as storage tanks for feedwater" |
| location | deaerator room, above the control room | ibid. "control room is located below the floor of the deaerator room" |

## Condensers

One under each turbine; coolant flow 82,880 t/h.

| property | value | source |
|---|---|---|
| coolant flow | 82,880 t/h per turbine | *RBMK* §"coolant flow for each turbine is 82,880 t/h" |
| condensate temperature | ~40 C | derived in `PlantPhysics.T_CONDENSATE_C` |

## ECCS accumulators (САОР)

| property | value | source |
|---|---|---|
| groups | two groups of six | *RBMK* §"two groups of six accumulator tanks" |
| total | 12 | ibid. |
| gas blanket | nitrogen, 10 MPa | ibid. "water blanketed with nitrogen under pressure of 10 megapascals" |
| each group supplies | 50% of max coolant flow | ibid. "Each group can supply 50% of the maximum coolant flow" |

## What this changes in the build

Each machine becomes a **multiblock**: a casing shell of the right shape and
size, with a **controller** block. The controller validates the shell and reports
whether the machine is formed. The shapes are the real shapes:

| machine | shape | size (blocks, 1:4) |
|---|---|---|
| steam separator drum | horizontal cylinder (axis X) | 124 long x 11 dia |
| turbogenerator | composite: 5 cylinder covers + shaft + generator | 157 x 27 x 19 |
| condenser | box under the turbine | 80 x 48 x 20 |
| main circulating pump | composite: pump body + 6 kV motor | 3 x 6 x 3 |
| deaerator | box | 24 x 24 x 40 |
| ECCS accumulator | box | 3 x 3 x 6 |

A **composite** machine is several casings under one controller, each with its own
block — the turbogenerator is five white cylinder covers, a shaft and an iron
generator body on one foundation. The builder places exactly the cells the
validator checks, so a machine that reads "formed" really is the machine.

Sources: English Wikipedia articles *RBMK* and *Chernobyl Nuclear Power Plant*,
which cite the plant design literature; and the K-500-65/3000 construction
datasheet (atomas.ru/rbmk), for the cylinder and cover geometry. Where a figure is
a block-game approximation (cylinder cover diameter, cylinder length, condenser
box, pump box) it is marked in the code as an approximation, because the real
sub-dimensions are not in these sources.
