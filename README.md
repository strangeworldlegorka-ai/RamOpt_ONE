# RAM Optimizer (ramopt) — NeoForge 1.21.1

Aggressive but narrowly-scoped RAM optimizations, built specifically to avoid
stepping on Sodium / Lithium / Starlight / Veil.

## What it does

| Subsystem | File | What it touches |
|---|---|---|
| NBT string interning | `mixin/CompoundTagInternMixin.java` | One mixin, `StringTag.valueOf` |
| Soft-reference cache eviction | `util/SoftCache.java`, `cache/CacheRegistry.java` | Only caches that opt in |
| BlockPos pooling | `util/BlockPosPool.java` | Only RamOpt's own helper code |
| Inactive-entity side-cache compaction | `compaction/InactiveEntityCompactor.java` | Only RamOpt's own per-entity side tables |

Everything is wired through the NeoForge event bus (`event/TickHandler.java`)
except the single mixin, which is documented in-file with exactly why that
target was chosen and how to fix it if a future mappings update shifts the
constructor signature.

All four subsystems are individually toggleable in
`config/ramopt-server.toml`, generated on first launch from
`config/RamOptConfig.java`.

## Why it's "non-conflicting" by construction, not by luck

- No mixin targets chunk generation, the light engine, entity AI/tick
  scheduling, or rendering — the exact areas Lithium, Starlight, and
  Sodium/Veil own.
- The one mixin shipped targets a single, narrow vanilla method
  (`StringTag.valueOf`) that none of those mods touch.
- `SoftCache` / `CacheRegistry` never reach into another mod's internal
  fields via reflection — registration is opt-in and additive.
- `InactiveEntityCompactor` does **not** mixin into vanilla's
  `SynchedEntityData` or brain/memory internals (that would be both fragile
  across versions and a likely collision point with Lithium's own entity
  tracking optimizations). It only manages RamOpt's own side-tables.

## Building

This is a standard NeoForge MDK-style Gradle project targeting 1.21.1 /
NeoForge 21.1.x. You'll need:

```
./gradlew build
```

with network access to `maven.neoforged.net` and Mojang's libraries (the
sandbox this was written in does not have that access, so this project has
**not** been compiled or run** — treat it as a solid, complete starting point
and fix up any mapping drift `gradlew` surfaces, most likely in
`CompoundTagInternMixin`'s `@At(target = ...)` descriptor if a future
mappings revision changes it).

Output jar lands in `build/libs/ramopt-1.0.0.jar`.

## Extending

- To add a new soft-evicting cache from elsewhere in your codebase:
  ```java
  SoftCache<ResourceLocation, BakedStuff> cache = new SoftCache<>("my-cache");
  CacheRegistry.register(cache);
  ```
- To pool something else the way `BlockPosPool` does, copy its pattern
  (`ThreadLocal<ArrayDeque<T>>`, borrow/return around a lambda) rather than
  fighting over a shared global pool.
- Every new mixin you add: ask "does Sodium/Lithium/Starlight/Veil touch this
  class?" first. If yes, prefer an event-bus hook or a narrower target.
