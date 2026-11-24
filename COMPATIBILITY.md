# Pearl X compatibility

Verified on **2026-11-24** against the Mojang version manifest, `meta.fabricmc.net`, the Modrinth API
and this repository's own builds.

## How "supported" is defined here

A version is listed as supported only when Pearl X has a **dedicated Gradle module that compiled
against that exact Minecraft version** with `./gradlew buildAll`. Nothing was run inside a live game
client in this environment (no display, no Minecraft account), so runtime behaviour is covered by
unit tests of the shared core and by compilation of every Minecraft-facing adapter - not by an
in-game session.

## Matrix

| Minecraft | Pearl X jar | Adapter sources | Java | Fabric Loader | Fabric API | Mod Menu (optional) | OneConfig (optional) | Build |
|---|---|---|---|---|---|---|---|---|
| 1.21 | `pearlx-1.21-1.0.0+mc1.21.jar` | `v1_21_1` | 21 | 0.20.1 | 0.102.0+1.21 | 11.0.5 | not published | compiled |
| 1.21.1 | `pearlx-1.21.1-1.0.0+mc1.21.1.jar` | `v1_21_1` | 21 | 0.20.1 | 0.116.18+1.21.1 | 11.0.5 | 1.9.4 | compiled |
| 1.21.2 | `pearlx-1.21.2-1.0.0+mc1.21.2.jar` | `v1_21_3` | 21 | 0.20.1 | 0.106.1+1.21.2 | 12.0.1 | not published | compiled |
| 1.21.3 | `pearlx-1.21.3-1.0.0+mc1.21.3.jar` | `v1_21_3` | 21 | 0.20.1 | 0.114.1+1.21.3 | 12.0.1 | not published | compiled |
| 1.21.4 | `pearlx-1.21.4-1.0.0+mc1.21.4.jar` | `v1_21_4` | 21 | 0.20.1 | 0.119.4+1.21.4 | 13.0.4 | 1.9.4 | compiled |
| 1.21.5 | `pearlx-1.21.5-1.0.0+mc1.21.5.jar` | `v1_21_5` | 21 | 0.20.1 | 0.128.2+1.21.5 | 14.0.2 | 1.9.4 | compiled |
| 1.21.6 | `pearlx-1.21.6-1.0.0+mc1.21.6.jar` | `v1_21_8` | 21 | 0.20.1 | 0.128.2+1.21.6 | 15.0.2 | not published | compiled |
| 1.21.7 | `pearlx-1.21.7-1.0.0+mc1.21.7.jar` | `v1_21_8` | 21 | 0.20.1 | 0.129.0+1.21.7 | 15.0.2 | not published | compiled |
| 1.21.8 | `pearlx-1.21.8-1.0.0+mc1.21.8.jar` | `v1_21_8` | 21 | 0.20.1 | 0.136.1+1.21.8 | 15.0.2 | 1.9.4 | compiled |
| 1.21.9 | `pearlx-1.21.9-1.0.0+mc1.21.9.jar` | `v1_21_9` | 21 | 0.20.1 | 0.134.1+1.21.9 | 16.0.1 | not published | compiled |
| 1.21.10 | `pearlx-1.21.10-1.0.0+mc1.21.10.jar` | `v1_21_9` | 21 | 0.20.1 | 0.138.4+1.21.10 | 16.0.1 | 1.9.4 | compiled |
| 1.21.11 | `pearlx-1.21.11-1.0.0+mc1.21.11.jar` | `v1_21_11` | 21 | 0.20.1 | 0.141.6+1.21.11 | 17.0.1 | 1.9.4 | compiled |
| 26.1 | `pearlx-26.1-1.0.0+mc26.1.jar` | `v26_1` | 25 | 0.20.1 | 0.155.3+26.1.2 | 18.0.2 | 1.9.4 | compiled |
| 26.1.1 | `pearlx-26.1.1-1.0.0+mc26.1.1.jar` | `v26_1` | 25 | 0.20.1 | 0.155.3+26.1.2 | 18.0.2 | 1.9.4 | compiled |
| 26.1.2 | `pearlx-26.1.2-1.0.0+mc26.1.2.jar` | `v26_1` | 25 | 0.20.1 | 0.155.3+26.1.2 | 18.0.2 | 1.9.4 | compiled |
| 26.2 | `pearlx-26.2-1.0.0+mc26.2.jar` | `v26_2` | 25 | 0.20.1 | 0.161.0+26.2 | 20.0.3 | 1.9.4 | compiled |
| 26.3 | `pearlx-26.3-1.0.0+mc26.3.jar` | `v26_3` | 25 | 0.20.1 | 0.167.0+26.3 | 21.0.1 | 1.9.4 | compiled |

## Why versions are grouped into adapters

Minecraft's client APIs changed in steps during the 1.21 series (1.21.2, 1.21.4, 1.21.5, 1.21.6,
1.21.9 each moved rendering, particle or GUI code) and again for the 26.x line. Pearl X therefore
keeps nine small adapter source sets instead of one:

| Adapter | Minecraft versions | Reason it exists |
|---|---|---|
| `v1_21_1` | 1.21, 1.21.1 | First 1.21 API generation. |
| `v1_21_3` | 1.21.2, 1.21.3 | 1.21.2 API refresh. |
| `v1_21_4` | 1.21.4 | 1.21.4 changes. |
| `v1_21_5` | 1.21.5 | 1.21.5 changes. |
| `v1_21_8` | 1.21.6, 1.21.7, 1.21.8 | Shared post-1.21.6 API generation. |
| `v1_21_9` | 1.21.9, 1.21.10 | 1.21.9 rendering/particle changes (typed dragon-breath particle). |
| `v1_21_11` | 1.21.11 | 1.21.11 moved `ThrownEnderpearl` into `projectile.throwableitemprojectile`, renamed `ResourceKey.location()` to `identifier()` and changed `CycleButton.builder` to take the initial value up front. |
| `v26_1` | 26.1, 26.1.1, 26.1.2 | 26.x generation with Java 25; also `sendSystemMessage` instead of `displayClientMessage`. |
| `v26_2` | 26.2 | 26.2 renamed `Minecraft.setScreen` to `setScreenAndShow`. |
| `v26_3` | 26.3 | 26.3 shares the 26.2 API generation. |

Every module still compiles against its own Minecraft version, so the jar you download was built
against exactly the game version you run.

## Dependency notes

* **Fabric Loader 0.20.1** was the current stable loader when this matrix was captured; the jars
  declare `fabricloader >=0.16.0` so older loaders keep working.
* **Fabric API** is a hard dependency because Pearl X uses its client tick and lifecycle events.
  The exact build used per version is in the table and in `gradle/versions.gradle`.
* **Build plugins differ by generation.** Minecraft 1.21.x ships obfuscated and is built with the
  remapping plugin (`fabric-loom`) plus Mojang official mappings. Minecraft 26.x ships
  unobfuscated - Mojang publishes no mapping files for it and Fabric has no Yarn or intermediary
  for it - so those modules use Loom's non-remapping plugin (`net.fabricmc.fabric-loom`), declare no
  mappings and use plain `implementation` dependencies. Their jar is the final artifact, with no
  remap step.
* **Mod Menu** is optional (`compileOnly` at build time, `suggests` in `fabric.mod.json`). Without it
  the mod still runs; you just cannot open the settings screen.
* **OneConfig** is optional and never required. OneConfig publishes a different build per Minecraft
  version under one shared version number, and its public configuration API is Kotlin/Compose-based,
  so Pearl X does not link against it. On versions where OneConfig exists it lists Pearl X in its own
  mod list (OneConfig consumes Mod Menu entries), and Pearl X's screen opens from there. Versions
  where OneConfig has no release are listed as "not published" above; Pearl X works normally there.

## What was verified, and what was not

Verified:

* every one of the 17 modules compiles and produces a jar with `./gradlew buildAll`;
* `fabric.mod.json` in each jar declares the correct Minecraft range and Java level;
* the shared core passes its JUnit test suite (`./gradlew :tests:test`);
* timer independence, tracking clean-up, storage separation and both feature switches are covered by
  those tests.

Not verified (no game client available here):

* in-game rendering of the animation particles and sounds;
* the settings screen layout at different GUI scales;
* actual pearl detection on a live server;
* Mod Menu opening the screen;
* behaviour differences between two real servers with the same address but different ports.

If you can run the game, these are the checks worth doing first; see the "Testing" section of the
[README](README.md).
