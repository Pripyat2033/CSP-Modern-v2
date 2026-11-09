# CSP-Modern-v2 — project memory

Minecraft Fabric mod, MC 1.20.1 (Yarn mappings). Simulates ChNPP at 1:1 scale;
core gameplay is a construction-workforce / agent system. Repo
`Pripyat2033/CSP-Modern-v2`, working branch `OpenHands`. Read `AGENTS.md` for
project conventions.

## Build & run (cheap checks; use these constantly)
```
export JAVA_HOME=$(ls -d $HOME/jdk/jdk-* | head -1)
export PATH=$JAVA_HOME/bin:$HOME/gradle/gradle-8.6/bin:$PATH
gradle compileJava compileClientJava --no-daemon --console=plain   # ~8s
gradle build --no-daemon --console=plain                           # ~12s
gradle runServer --no-daemon --console=plain                       # ~90s to "Done!"
```
Always redirect to a /tmp log and grep `error:|BUILD`. `compileJava compileClientJava`
is the fast gate after any edit; `build` + a clean `runServer` boot is the final gate.
`runServer` leaves `run/world/session.lock` — kill leftover java `runServer`/`KnotServer`
procs and `rm -f run/world/session.lock` before re-booting, else SessionLock exception.

## Source-set layout
Loom `splitEnvironmentSourceSets()` is enabled. Common/server code in
`src/main/java`; client-only code in `src/client/java` (all `net.minecraft.client.*`,
renderers, screens, client keybinds). `src/aecs` is a separate source set (javaparser).
**Never reference a client-only class from `src/main`** — it breaks the dedicated server.

## Gotchas learned the hard way
- Fabric `ResourceManager.findResources(prefix, pred)`: `prefix` is a directory-style
  match. For a file at `data/csp/x.json` use `findResources("", id -> id.getPath().equals("x.json"))`;
  `findResources("x", …)` silently returns nothing. Register server reload listeners with
  `ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(...)` in the ModInitializer.
- Missing assets are the common "stub": a renderer can reference a texture/model that was
  never created → purple/black missing-texture at runtime. Audit by grepping every
  `"textures/…"` Identifier literal and every `blockstates/`+`models/item/` name against
  registered blocks/items.
- The repo historically had many root-vs-subpackage duplicate classes under
  `com.ben.csp`. A class is LIVE iff another file resolves it via `import pkg.Simple;`,
  a same-package reference, or wildcard `import pkg.*;`. After deleting any class,
  recompile immediately (all source sets compile) to catch a mistaken deletion.
- Item registration pattern: `ModItems` exposes `public static final Item` fields;
  each item takes `Item.Settings` in its ctor. The mod's creative tab is `ModItemGroups`
  (FabricItemGroup) keyed by `itemGroup.csp-modern`.
- `PersistentState`: when data "saves" but never round-trips, diff the writeNbt key
  type vs the fromNbt read type (KartografManager had write=Compound, read=String).

## Session logs
- `2026-11-09.md` — gap-fill: loaders, Kartograf NBT fix, missing assets, dead-code
  removal, creative tab, full build + server verification.
