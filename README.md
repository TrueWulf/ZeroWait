<div align="center">

# ZeroWait

**Boot Minecraft, not a loading screen.**

[![Modrinth](https://img.shields.io/modrinth/dt/zerowait?logo=modrinth&label=downloads&color=00af5c)](https://modrinth.com/mod/zerowait)
[![Modrinth version](https://img.shields.io/modrinth/v/zerowait?logo=modrinth&color=00af5c)](https://modrinth.com/mod/zerowait)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)

[Modrinth](https://modrinth.com/mod/zerowait) · [Versions](https://modrinth.com/mod/zerowait/versions) · [Report an issue](https://github.com/TrueWulf/ZeroWait/issues)

</div>

ZeroWait is a client-side Fabric mod that makes Minecraft reach the title
screen as fast as possible. It splits the startup resource reload into two
passes: the title screen only waits for what it actually needs, while sounds,
renderers and everything else keep loading in the background.

No new menus, no required setup — drop it in your mods folder and boot.

## Quick Start

1. Install [Fabric Loader](https://fabricmc.net/use/).
2. Drop the ZeroWait jar for your game version into `mods/`.
3. Launch the game.

Grab builds on the
[Modrinth versions page](https://modrinth.com/mod/zerowait/versions) —
1.21 through 1.21.11 and 26.1 through 26.3 are supported.

## How it works

- **Split reload** — the startup resource reload is divided into an
  *immediate* pass (fonts, language, core textures) and a *deferred* pass
  (sounds, entity renderers, clouds, waypoints, splashes). The menu waits
  only for the first one.
- **Dedicated reload executor** — resource loading runs on a properly sized
  thread pool (up to 16 threads) instead of vanilla's conservative one.
- **Overlay trimming** — the Mojang loading overlay ends the instant the
  immediate pass is done.
- **Unicode font deferral** — the heavy unihex set is parsed in the
  background, so menus render instantly.
- **Fast crash preload** — the blocking class preload at boot is replaced
  with a cheap memory reserve.
- **CPU info prefetch** — the CPU string is fetched on a background thread
  during boot.

Boot time is printed to the log and shown in the bottom-left corner of the
title screen.

## Configuration

Everything lives in `config/zerowait.json`, created on first launch with
sensible defaults:

| Option | Default | Description |
|---|---|---|
| `deferStartupReload` | `true` | Split the startup reload into two passes |
| `deferUnicodeFonts` | `true` | Skip unihex fonts on the immediate pass |
| `skipLoadingOverlay` | `true` | Close the Mojang overlay as soon as resources are ready |
| `dedicatedReloadExecutor` | `true` | Use the sized thread pool for resource loading |
| `fastCrashPreload` | `true` | Replace the blocking crash-report preload |
| `prefetchCpuInfo` | `true` | Fetch CPU info on a background thread |
| `showBootTimeOverlay` | `true` | Show boot time on the title screen |
| `deferredReloadStartDelayTicks` | `0` | Extra delay before the deferred pass starts |
| `moddedImmediatePatterns` | `[]` | Class-name patterns forced into the immediate pass |

## Compatibility

- Client-side only; works on singleplayer and vanilla servers.
- Uses MixinExtras wrap-operations instead of raw redirects, so it coexists
  with ModernFix, VMP, Lithium, Krypton and friends.
- Every injection degrades gracefully: if a target disappears in a future
  game update, the game still launches and the mod simply stays inactive.
- A full resource reload is scheduled automatically if the deferred pass
  ever fails.

## Building from source

```sh
./gradlew build            # build the active version
./gradlew buildAll         # build every supported version
```

Jars land in `versions/<version>/build/libs/`. Java 21 is required for
1.21.x targets and Java 25 for 26.x (Gradle toolchains provision them
automatically).

## License

[GPL-3.0-or-later](LICENSE)
