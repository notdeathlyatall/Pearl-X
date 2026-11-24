# Changelog

## 1.0.0 - 2026-11-24

First release.

### Added

* Pearl Location Tracker (Feature One): client-side detection of Ender Pearls thrown by the local
  player, independent tracking of multiple pearls, automatic clean-up when a pearl lands or is
  retrieved, and retention of records whose chunk is merely unloaded.
* Local per-server and per-single-player-world JSON storage with atomic writes, change detection and
  background saving.
* Independent location-saving timer (value + seconds/minutes/hours) with its own enable switch.
* Universal Pearl Travel Animation (Feature Two): portal ring, end rod, electric spark, dragon breath
  and closing burst effects with synced start/finish sounds.
* Independent animation timer (trigger delay + seconds/minutes/hours), configurable duration,
  separate visual and sound switches and configurable volume.
* Full tabbed configuration screen reachable from Mod Menu, with tooltips and confirmation dialogs.
* Storage management: reset current server, reset current single-player world, reset all servers,
  reset all worlds, clear all records.
* Advanced options: debug logging, chat tracking summary, configuration reset to defaults.
* Multi-version Gradle build producing one jar per supported Minecraft version: 1.21 through 1.21.11
  and 26.1 through 26.3.
* JUnit 5 test suite for the Minecraft-independent core, including the seven independence rules.

### Notes

* Client-side only. No server component, no telemetry, no network access.
* OneConfig is not linked against; its per-version Kotlin config API would become a hard dependency.
  Pearl X opens from Mod Menu, which OneConfig also lists.
