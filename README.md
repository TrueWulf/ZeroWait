<div align="center">

# ZeroWait

**Boot Minecraft, not a loading screen.**

A client-side Fabric mod that gets you from "Play" to the title screen as
fast as the game allows — and trims everything that made you wait.

[![Modrinth downloads](https://img.shields.io/modrinth/dt/zerowait?logo=modrinth&label=downloads&color=00af5c)](https://modrinth.com/mod/zerowait)
[![Modrinth version](https://img.shields.io/modrinth/v/zerowait?logo=modrinth&color=00af5c)](https://modrinth.com/mod/zerowait/versions)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)

[Download](https://modrinth.com/mod/zerowait) · [All versions](https://modrinth.com/mod/zerowait/versions) · [Issues](https://github.com/TrueWulf/ZeroWait/issues)

</div>

## Why

Vanilla spends the first seconds of launch waiting for things the title
screen never uses: sound engines, entity renderers, clouds, waypoints,
unicode fonts. ZeroWait reorders that work. The menu appears when the menu
is ready — the rest finishes in the background while you already sit on the
title screen.

## How it works

The startup resource reload is split in two:

- the **immediate pass** loads only what the title screen draws — fonts,
  language, core textures;
- the **deferred pass** loads everything else a few ticks later, while you
  look at the panorama instead of a progress bar.

Around that split, ZeroWait:

- runs resource loading on a properly sized thread pool (up to 16 threads)
  instead of vanilla's fixed one;
- closes the Mojang overlay the moment the immediate pass finishes;
- skips the unihex font set until the deferred pass;
- replaces the blocking crash-report class preload with a cheap memory
  reserve;
- prefetches the CPU string on a background thread.

The measured boot time is printed to the log and shown in the corner of the
title screen, so every change on your side is measurable.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/).
2. Download the jar for your game version from
   [Modrinth](https://modrinth.com/mod/zerowait/versions).
3. Put it in `mods/` and launch.

Supported versions: **1.21 – 1.21.11** and **26.1 – 26.3**.

## Configuration

Everything lives in `config/zerowait.json`, created on first launch.
Defaults are safe, nothing needs to be touched:

| Option | Default | |
|---|---|---|
| `deferStartupReload` | `true` | the two-pass reload itself |
| `deferUnicodeFonts` | `true` | unihex fonts moved to the deferred pass |
| `skipLoadingOverlay` | `true` | close the overlay as soon as possible |
| `dedicatedReloadExecutor` | `true` | sized thread pool for resource loading |
| `fastCrashPreload` | `true` | cheap crash-report preload |
| `prefetchCpuInfo` | `true` | background CPU string fetch |
| `showBootTimeOverlay` | `true` | boot time on the title screen |
| `deferredReloadStartDelayTicks` | `0` | delay before the deferred pass |
| `moddedImmediatePatterns` | `[]` | listeners forced into the immediate pass |

## Compatibility

- Client-side only, works with vanilla servers and singleplayer.
- Built on MixinExtras wrap-operations instead of raw redirects, so it
  coexists with ModernFix, VMP, Lithium, Krypton and friends.
- Every hook degrades gracefully: if the game changes underneath it, the
  game still starts and the mod simply goes dormant with a log warning.
- If the deferred pass fails, a full vanilla reload runs automatically.

## Building from source

```sh
./gradlew buildAll
```

Jars for every supported version land in `versions/*/build/libs/`.
Java 21 builds the 1.21.x targets, Java 25 the 26.x ones — Gradle
provisioning handles both.

## License

[GPL-3.0-or-later](LICENSE)
