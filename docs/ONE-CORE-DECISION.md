# The three cores — why there are three, and which one survives

Written 2026-11-14, after the Director asked: *"WAIT we have TWO cores! WHYYY…
so which is the most historically and physics accurate core? because we CANNOT
test two, there has to be 1 only."*

**There are three, not two.** `ReactorModel` has three implementations. This note
states what each is, what each is worth, and which one must survive.

---

## 1. Why there are three (it was accretion, not design)

They were added in sequence, each meant to *supersede* the last, but the old ones
were never retired — so all three are still selectable:

| # | Core | Added | Commit | The idea |
|---|---|---|---|---|
| 1 | `ReactorCore` | first | `54edd2f` "Coupled time-dependent core" | lumped point kinetics — a *training-simulator* structure |
| 2 | `SpatialReactorCore` | then | `5533b75` "Run the RBMK core on the full 3D solver, live" | make it spatial: 3D static eigenvalue, still lumped power |
| 3 | `TransientReactorCore` | last | `cfaacf2` "run the spatial core LIVE… on its own thread" | make it *time-dependent*: the flux IS the state |

Each is reachable in-game, and they disagree. That is the incoherence:

| command | core used |
|---|---|
| `/rbmk plant start` (**the default**) | `ReactorCore` (lumped) |
| `/rbmk plant start 3d` | `SpatialReactorCore` |
| `/rbmk plant start spatial` | `TransientReactorCore` |
| `/rbmk stress` | `ReactorCore` (lumped) |
| `/rbmk plant scram` | whichever is running |

So **the default the Director has been testing is the least accurate one.**

---

## 2. What each actually is — measured on the identical AZ-5 scenario

All three were run on the same sequence (hold at power, withdraw rods, then AZ-5)
in this repo, same machine:

| | `ReactorCore` | `SpatialReactorCore` | `TransientReactorCore` |
|---|---|---|---|
| Reactivity from | a hand-fitted sum of deltas | 3D static eigenvalue (`RbmkSolver3D`) | 3D time-dependent flux (`RbmkTransientSolver`) |
| Power from | point kinetics | point kinetics on the spatial k | the flux itself (no point kinetics) |
| Spatial resolution | **none** (one lumped point) | 1872 channels × 28 nodes | 1872 channels × 28 nodes |
| Void/temp feedback loop | **closed** (fitted coefficients) | **closed** (enters through cross sections) | **OPEN — frozen constants** |
| Xenon | yes | yes | **none** |
| Rod withdrawal raises power? | yes | yes | yes |
| **AZ-5 drops the power?** | **yes** (fission → 0.004) | partial — stuck at 0.20 | yes on the flux (0.36 → 0.05), but see below |
| Feedback loop closed (void/Doppler)? | yes (fitted coefficients) | yes (via cross sections) | **NO — frozen constants** |
| Tested? | yes (`AccidentSequence`, 15/15) | yes (11/12, known AZ-5 failure) | **no self-test** |

The three do **not** agree on the single most important behaviour — whether AZ-5
fully shuts the reactor down. That is precisely why there is "no coherent baseline".

### The decisive defect in `TransientReactorCore`

The rods *do* work — `setRodInsertion` changes the flux, so AZ-5 drops the power
(measured: fission 0.36 → 0.05). But `grep` proves the loop is open: it **never
calls `solver.setCoreState(...)`** and **never writes `voidFraction` or
`fuelTempC`** after construction. Those two are `volatile` constants fixed at 0.05
and 640 °C, and `xenon` is fixed at 1.0. So the *positive void coefficient* — the
entire physical reason the accident happened — **never enters the calculation**, and
the fuel never heats up no matter how much power is made. It is the most elegant
solver in the repo, wired to a *partial* reactor: rods move the flux, but the core
cannot feed back on itself. It cannot reproduce the accident.

### The decisive defect in `SpatialReactorCore`

Its static rod worth is fine — measured from `RbmkSolver3D`, full rod travel is
**−7.8 %** of reactivity, right in the real RBMK's 7–10 % band. But in the *live*
loop it does not shut down: after AZ-5 the fission sits at 0.20 and `k_eff` at
1.04. The live reactivity read (`balanceKeff()`) is not tracking the rod move, so
the regulator and the kinetics see a stale reactivity. A live-coupling bug, not a
cross-section bug. (Pre-existing; verified by `git stash` on the baseline.)

---

## 3. Which is the most accurate — the answer

**A spatial core, not the lumped one.** The reason is the project's own purpose.
The accident *is* a spatial phenomenon:

- the **positive scram** is a property of the rod's *axial geometry* (the graphite
  displacer at the bottom of the rod) meeting a *bottom-peaked axial flux* — a
  purely spatial effect;
- the **positive void coefficient** acts where the steam is, not uniformly;
- the **ORM violation** (almost all rods out) is about *where* the absorbers are.

Point kinetics **cannot derive** any of these. `ReactorCore` **asserts** the answer
instead: it carries a hand-fitted `RodWorth.bottomPeaked()` differential curve and
a fitted `rhoExcess`. Its own javadoc calls it *"the structure of an operator-
training simulator."* That is fine for a simulator — and it is exactly what the
project's SGB rule forbids for the phenomenon under investigation: **you would be
assuming the conclusion you set out to test.**

So: **the one surviving core must be spatial.** The lumped `ReactorCore` is the
*least* accurate for this project, however convenient it is.

Of the two spatial cores, `TransientReactorCore` (`RbmkTransientSolver`) is the
more correct **architecture** — the flux is the state, there is no lumped
amplitude and no eigenvalue lag — while `SpatialReactorCore` is closer to
*working* (its loop is closed; only the live rod read is broken). They are two
maturities of the same idea and should be **unified into one**, not kept apart.

---

## 4. The decision

**One core. It is spatial. It is the time-dependent solver, with the feedback loop
closed.**

Concretely:
1. Take `TransientReactorCore` + `RbmkTransientSolver` as the base (flux is the state).
2. Close its loop: each step compute void and fuel/graphite temperature from the
   power, call `setCoreState(...)`, and add the xenon chain — i.e. port the loop
   already written in `SpatialReactorCore`.
3. Calibrate the power scale (the transient solver's mean fission needs the same
   1.0-at-nominal zero the others use).
4. Add the missing self-test; prove AZ-5 shuts it down and rod withdrawal raises it.
5. Make it the only core the plant can select.

**Retire `ReactorCore` and `SpatialReactorCore`.** Under the project's ZERO
DELETION POLICY this needs the Director's explicit confirmation — nothing is
removed until he says so.

**Why the time-dependent one and not `SpatialReactorCore`:** a reactor *transient*
is, by definition, time-dependent. `SpatialReactorCore`'s hybrid (a static
eigenvalue feeding a lumped amplitude) is the source of its rod-response lag, and
it is the less honest model for an excursion. The transient solver needs more work
to finish, but it is the right machine to finish.

---

## 5. The one-line answer

We have three cores because each was meant to replace the last and none was
retired. **The accurate one is the spatial one — specifically the time-dependent
solver, once its feedback loop is closed — and the default you have been testing
(the lumped `ReactorCore`) is the least accurate.** Collapse to one; the rest
requires your say-so to remove.
