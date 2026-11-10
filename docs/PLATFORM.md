# PLATFORM — the project's posture toward the platform it runs on

Minecraft is owned by Microsoft. The Director's position is that the ChSZO and ChNPP
must be run by this project, not by any external party: any outside presence, including
Microsoft's, would be **a guest under escort** — never an authority over the plant. This
document records how the project is shaped so that position is practical.

## The posture we already hold

- **Original code.** The mod is our own work; it redistributes **no** game assets.
- **Non-commercial.** No sale, no paywall.
- **Open and mirrored.** The source is public on GitHub; anyone may clone it. This is
  the strongest practical defence: a work that exists in many copies cannot be unmade
  from one place.
- **Pinned toolchain.** The Fabric API and loader versions are pinned, so an upstream
  change cannot silently break the build.

## What the platform can and cannot do

- It **can** change its API or ship a game update that breaks mods. (Mitigation: pin
  versions; keep the mod working on a known game version.)
- It **can** ask a distribution site to remove a listing. (Mitigation: mirrors; the jar
  is also kept locally; single-player needs no external service to run.)
- It **cannot** unmake published, mirrored source.

## Open question (not yet verified)

The Director recalled a case of a private server being shut down over terms of service.
**We have not been able to verify that specific case**, and we will not assert it as fact
until we can. What is established is that the official Minecraft Usage Guidelines and
EULA permit non-commercial mods distributed as original work — which is the shape this
project already takes.

## The aspiration the Director named

A hardened server where the project holds the keys: a whitelist, and rooms that are
locked to anyone not admitted — so that a visitor, of any kind, is exactly that. The
telephone exchange already models access by line; the same idea applies to the site.
