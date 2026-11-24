# Pearl X architecture

## Goals

1. One shared implementation of the mod's behaviour that runs on 17 Minecraft versions.
2. Two features that are genuinely independent: separate state, separate timers, separate switches.
3. No server component, no network access, no telemetry, minimal disk and memory use.
4. Version-specific code confined to a small, replaceable adapter.

## Layers

```
common/src/main/java/dev/pearlx/core        version-independent core (no Minecraft imports)
version-adapters/<group>/java/dev/pearlx/mc Minecraft-facing adapter (one per API generation)
versions/<mc>/                              Gradle module: picks one version + one adapter
```

`PearlXCore` receives a `ClientPort` implementation from the adapter and never imports a Minecraft
type. Everything that needs the game - reading entities, spawning particles, playing sounds, sending
a chat line, resolving the current server or world name, checking whether a chunk is loaded - goes
through that interface. This is what makes one core work across the 1.21 and 26.x API generations.

The adapter is deliberately small: an entrypoint, the port implementation, a settings screen and a
Mod Menu hook. Rendering and GUI code is the part of Minecraft that changes most often, so it lives
in the adapter and only there; the core talks about particles and sounds by enum
(`ParticleKind`, `SoundCue`) instead of game classes.

## Feature separation

| | Feature One (tracker) | Feature Two (animation) |
|---|---|---|
| State | `PearlTracker` (records, lifecycle) | `AnimationEngine` (live pearls, sessions) |
| Timer | `SaveScheduler` reading `savingInterval*` | per-pearl delay reading `animationDelay*` |
| Switches | `trackerEnabled`, `detectionEnabled`, `autoSaveEnabled`, `trackServer(s)`, `trackSingleplayer` | `animationEnabled`, `visualEffectsEnabled`, `soundEffectsEnabled` |
| Input | `PearlSnapshot` list | the same `PearlSnapshot` list |

Both features receive the **same single entity scan** from `PearlXCore` each tick, and each one
decides for itself whether it is enabled. The animation engine keeps its own `firstSeenAt` per pearl,
so it can time its own delay without asking the tracker anything. Disabling the tracker therefore
leaves animations working (they simply have no persisted records), and disabling animations leaves
tracking working.

`PearlXCore.tick()` order is: one scan, tracker pass, animation pass, saving-timer poll, optional
chat summary. There is no path in which the saving configuration is read by the animation code or
vice versa; the unit tests assert this in both directions.

## Tracking lifecycle

`ACTIVE` -> the client sees the pearl this tick.
`UNLOADED` -> the client does not see it. A pearl can be missing because it landed, was retrieved or
despawned, or because its chunk is no longer loaded. The two cases are distinguished with the client
chunk cache:

* chunk loaded + entity gone -> the pearl is really gone: drop the record immediately;
* chunk not loaded -> keep the record (marking it unloaded) so a temporary disappearance never
  destroys a valid location, and expire it after the configured retention time.

Restored records (`RESTORED`) come from disk after a restart and are re-attached to a live entity if
the same pearl id appears again. They deliberately never trigger an animation, so switching servers
cannot replay an old effect.

## Storage

* Location: `config/pearlx/pearlx.json` for settings, `config/pearlx/worlds/*.json` for records.
* One file per server/single-player world, named from a sanitised prefix plus a short SHA-1 of the
  world key, so any address maps to a safe file name without collisions.
* Writes are atomic (temp file + move), skipped when the normalised payload is unchanged, and
  performed on a single daemon thread for periodic saves. World switches and shutdown flush
  synchronously so nothing is lost.
* Empty worlds delete their file instead of leaving an empty document behind.

## Performance

* One entity scan per client tick, iterating only the client's rendered entity list.
* Records are plain field objects; positions are rounded to 2 decimals to keep payloads tiny.
* Capped at 128 tracked records and 32 concurrent animation sessions.
* Animation frames spawn at most 14 particles per pearl per tick and stop with the session.
* No background timers, no polling threads, no mixins, no per-frame allocation of note.

## Failure handling

The client tick is wrapped: if the mod ever throws, it logs the stack trace, tells the player once,
and disables itself for the session instead of crashing the game. Corrupt JSON is moved aside as
`.corrupt` and replaced with defaults instead of being silently discarded.

## Why no mixins

Everything Pearl X needs is observable from public client APIs plus Fabric API's client tick and
lifecycle events. Avoiding mixins removes a whole class of breakage between Minecraft versions, so
there is no refmap or mixin configuration to keep in sync per version.

## Why ten adapters

The port interface is small, but particle spawning, chunk queries, server/world naming and GUI widget
constructors did change between 1.21.1, 1.21.2, 1.21.4, 1.21.5, 1.21.6, 1.21.9, 1.21.11, 26.1, 26.2
and 26.3. Each adapter is a copy that was compiled against its versions; sharing one file blindly
would mean claiming compatibility that was never compiled. 1.21.11 is deliberately separate from
1.21.9/1.21.10 because it moved `ThrownEnderpearl`, renamed `ResourceKey.location()` and changed the
`CycleButton` builder, none of which compile on 1.21.9/1.21.10.

## Remapping

1.21.x ships obfuscated, so those modules use Loom's remapping plugin with Mojang official mappings
and produce a remapped mod jar. 26.x ships unobfuscated: Mojang publishes no mapping file, Fabric
publishes no Yarn or intermediary for it, and the game jar already uses its own class names. Those
modules use Loom's non-remapping plugin, declare no mappings, use plain `implementation` Fabric
dependencies and emit the jar directly.
