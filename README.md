# Pearl X

A lightweight, entirely client-side Minecraft Fabric mod that tracks the Ender Pearls **you** throw
and plays an independent pearl travel animation. Nothing is uploaded, no server-side installation is
required, and the two features are completely independent of each other.

* **Feature One - Pearl Location Tracker:** notices pearls you throw, follows them, and saves their
  locations locally (per server and per single-player world) on its own configurable timer.
* **Feature Two - Universal Pearl Travel Animation:** plays a configurable pearl-themed particle and
  sound animation on its own, separate configurable timer.

| | |
|---|---|
| Mod loader | [Fabric Loader 0.16.0+](https://fabricmc.net/use/installer/) |
| Minecraft | 1.21 - 1.21.11, 26.1 - 26.3 (see [COMPATIBILITY.md](COMPATIBILITY.md)) |
| Environment | Client only (no server component, no mixins into server classes) |
| Dependencies | [Fabric API](https://modrinth.com/mod/fabric-api) required, [Mod Menu](https://modrinth.com/mod/modmenu) or [OneConfig](https://modrinth.com/mod/oneconfig)required |
| Java | 21 for 1.21.x builds, 25 for 26.x builds |
| License | MIT |

## Features

### Feature One: Pearl Location Tracker

* Detects Ender Pearls thrown by the local player only - other players' pearls are ignored.
* Tracks several pearls at once, each with its own record and position.
* Saves locations as compact JSON under `config/pearlx/worlds/`, one small file per server or
  single-player world.
* Uses a stable identifier: the server address you connected to, or the single-player level name.
* Cleans records up automatically when a pearl lands or is retrieved, and keeps records safe when a
  pearl is merely out of view in an unloaded chunk.
* Writes only when something actually changed, on a background thread.
* Has its own saving interval (value + seconds/minutes/hours) that controls nothing else.

### Feature Two: Universal Pearl Travel Animation

* Watches for your thrown pearls independently of the tracker.
* Waits for its own configurable trigger delay, then plays a pearl-themed animation: an expanding
  portal ring, end rod sparks, electric sparks, dragon breath and a closing burst.
* Plays synced sounds (start and finish) with a configurable volume.
* Visual effects and sound can be toggled separately; 0% volume is silent.
* One animation per pearl, several pearls animate independently, and animations are cancelled when a
  pearl lands, is retrieved, or the world changes.
* Never replays an outdated animation after a restart or a server switch.

### Configuration

* Full tabbed settings screen (General, Tracker, Animation, Storage, Advanced) opened from Mod Menu.
* Descriptive labels and tooltips on every control.
* Confirmation dialogs for every destructive storage action.
* Settings persist in `config/pearlx/pearlx.json` and are validated on load.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder.
3. *Required* Install [Mod Menu](https://modrinth.com/mod/modmenu) to reach the settings screen or [OneConfig](https://modrinth.com/mod/oneconfig)
4. Download the Pearl X jar that matches your exact Minecraft version from the
   [latest release](https://github.com/notdeathlyatall/Pearl-X/releases/latest) and put it in `mods`.
5. Launch the game. Pearl X works on any server without the server installing anything.

Pearl X never connects to the internet, contains no telemetry, and stores everything on your own
computer.

## Supported Minecraft versions

Each Minecraft version has its own jar, compiled against that exact version. See
[COMPATIBILITY.md](COMPATIBILITY.md) for the full matrix, dependency versions and what "supported"
means here.

| Series | Versions |
|---|---|
| 1.21 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11 |
| 26 | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 |

## Configuration guide

Open **Mod Menu → Pearl X → Configure**.

### General

| Setting | Default | What it does |
|---|---|---|
| Enable Pearl X | on | Master switch. Off stops tracking, saving and animations. |
| Enable Pearl Location Tracker | on | Feature One switch. Does not affect the animation feature. |
| Enable Universal Pearl Travel Animation | on | Feature Two switch. Does not affect tracking. |

### Tracker (Feature One)

| Setting | Default | What it does |
|---|---|---|
| Enable pearl detection | on | Detect and follow pearls thrown by you. |
| Enable automatic location saving | on | Save on Feature One's timer. Tracking continues when off. |
| Saving interval | 30 seconds | 1-60 seconds, 1-60 minutes or 1-24 hours. Feature One only. |
| Saving interval unit | seconds | Seconds / minutes / hours for the saving timer. |
| Track multiplayer servers | on | Keep separate records for each server. |
| Track single-player worlds | on | Keep separate records for each world. |
| Keep unloaded pearl records | 30 minutes | How long a pearl in an unloaded chunk is remembered. |

### Animation (Feature Two)

| Setting | Default | What it does |
|---|---|---|
| Enable animation feature | on | Feature Two switch. Independent of the tracker. |
| Enable visual effects | on | Particles. Sound can run without them. |
| Trigger delay | 1 second | 1-60 seconds, 1-60 minutes or 1-24 hours. Feature Two's timer. |
| Trigger delay unit | seconds | Seconds / minutes / hours for the animation timer. |
| Animation duration | 2 seconds | 1-15 seconds of animation once triggered. |
| Enable sound effects | on | Sounds. Visuals can run without them. |
| Sound volume | 70% | 0-100%. 0% is silent. |

### Storage

Reset current server locations, reset current single-player world locations, reset all server
locations, reset all world locations, or clear every saved tracking record. Each action asks for
confirmation, and none of them touch your settings.

### Advanced

Debug logging, a tracking summary in chat, and "reset configuration to defaults". Resetting the
configuration never deletes stored pearl locations.

## The two timers

Pearl X has two timers that never influence each other.

| Property | Location Saving Timer | Animation Timer |
|---|---|---|
| Purpose | Write pearl locations to disk | Start the pearl travel animation |
| Configuration | `saving_interval_value` / `saving_interval_unit` | `animation_delay_value` / `animation_delay_unit` |
| Numerical input | yes | yes |
| Seconds / minutes / hours | yes | yes |
| Enable/disable | automatic saving switch | animation feature switch |
| Persistence | own fields in the config file | own fields in the config file |
| Affects the other timer | never | never |

They are implemented in separate classes (`SaveScheduler` and `AnimationEngine`), configured by
separate fields, and covered by separate unit tests that assert changing one leaves the other
untouched.

## How tracking behaves at the edges

* **Landed or retrieved:** the pearl's chunk is loaded and the entity is gone, so the record is
  removed immediately.
* **Out of view:** the pearl's chunk is not loaded, so the client simply cannot see the entity. The
  record is kept, marked unloaded, and expires after the configured time (default 30 minutes).
* **Server switch / disconnect:** records are flushed before leaving the world so the last known
  positions survive.
* **Game restart:** stored records are restored for the world and re-attached to live entities when
  they appear again. Restored pearls never replay an animation.

## Storage format

```
config/pearlx/
├── pearlx.json            # settings
└── worlds/
    ├── play-example-com-1a2b3c4d.json
    └── survival-world-5e6f7a8b.json
```

Each world file is tiny, pretty-printed JSON:

```json
{
  "version": 1,
  "world": "server:play.example.com",
  "label": "server play.example.com",
  "updated": 1765000000000,
  "pearls": [
    {
      "id": "0f1e2d3c-...",
      "x": 128.53, "y": 65.0, "z": -204.11,
      "dimension": "minecraft:overworld",
      "thrownAt": 1764999990000,
      "updatedAt": 1764999995000,
      "status": "ACTIVE"
    }
  ]
}
```

Files are written atomically and only when their content changed. Nothing is ever uploaded, and no
database or external service is used.

## Building

Pearl X is a multi-version Gradle build: one subproject per Minecraft version, each compiling the
shared core plus one version adapter.

Requirements: **JDK 25** (Fabric Loom 1.19.4 requires it) and a network connection for the first
build. 1.21.x modules still compile with `--release 21`, so the resulting jars run on Java 21.

```bash
# everything: 17 jars in build/dist
./gradlew buildAll

# a single Minecraft version
./gradlew -PpearlxModules=mc1.21.8 :mc1.21.8:build

# core unit tests (no Minecraft required)
./gradlew :tests:test
```

On Windows use `gradlew.bat` instead of `./gradlew`. Convenience wrappers are included:
`./build-all.sh` and `build-all.bat`.

## Testing

`./gradlew :tests:test` runs JUnit 5 tests against the Minecraft-independent core:

* timer independence in both directions,
* pearl detection and ownership filtering,
* multiple simultaneous pearls,
* landed / retrieved clean-up, unloaded-chunk retention and expiry,
* per-server and per-world storage, scoped resets, write de-duplication, restart persistence,
* animation trigger timing, duplicate prevention, duration, cancellation and volume 0,
* all seven independence rules from the specification (tracking vs animation, saving vs animation,
  auto-save vs tracking, sound vs visuals in both directions).

Everything Minecraft-facing (particles, sounds, the settings screen, Mod Menu) is compiled against
every supported version but has **not** been run inside a game client, because this project was
built in a headless environment. See [COMPATIBILITY.md](COMPATIBILITY.md) for exactly what was and
was not verified.

## Screenshots

There are no screenshots in this repository yet: the project was developed in a headless
environment without a Minecraft client, so the settings screen could not be captured. The screen is
built entirely from vanilla widgets (tab buttons, checkboxes, sliders, cycle buttons and
confirmation dialogs) and inherits the game's own styling. Contributions of screenshots from
in-game runs are welcome.

## Known limitations

* No in-game verification was possible in this environment; only compilation for every supported
  version plus unit tests of the core logic.
* OneConfig is detected but not wired into its own settings page: OneConfig 1.9.4's public
  configuration API is Kotlin/Compose-based and is published as a different artifact for every
  Minecraft version, which would turn an optional integration into a hard, version-pinned
  dependency. Pearl X therefore opens through Mod Menu, and OneConfig users can reach it from
  OneConfig's own mod list. See [COMPATIBILITY.md](COMPATIBILITY.md) for the exact OneConfig
  coverage. OneConfig is never required.
* The "show tracking information" option prints a chat summary rather than a HUD overlay, so the mod
  needs no version-specific HUD rendering code.
* A pearl whose chunk stays unloaded for longer than the configured retention time is forgotten, by
  design, to keep the store from growing forever.
* The client cannot see server-side events, so "landed" and "retrieved" are both inferred from the
  pearl entity disappearing from a loaded chunk.

## License

MIT - see [LICENSE](LICENSE).

Pearl X is not affiliated with Mojang Studios or Microsoft.
