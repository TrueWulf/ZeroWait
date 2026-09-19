# ZeroWait

**ZeroWait** is a client-side mod that makes Minecraft start faster. It restructures the startup resource reload so the title screen appears as soon as it is actually needed — instead of waiting for every sound, renderer and cloud to be ready first.

No new menus, no required setup: drop it in your mods folder and boot.

## How it works

During startup Minecraft runs a full resource reload before showing the title screen. ZeroWait splits that reload into two passes:

- **Immediate pass** — only what the title screen truly needs (fonts, language, core textures). This is what you wait for.
- **Deferred pass** — everything else (sounds, entity renderers, clouds, waypoints, splashes, etc.) loads in the background right after the menu appears.

On top of that:

- **Dedicated reload executor** — resource loading runs on a properly sized thread pool (up to 16 threads) instead of vanilla's conservative one.
- **Loading overlay trimming** — the Mojang loading overlay ends the moment the immediate pass is done, with no extra fade waiting.
- **Unicode font deferral** — the heavy unihex font set is parsed in the background pass; menus render with the default font set instantly.
- **Faster crash-report preload** — the blocking class preload at boot is replaced with a cheap memory reserve.
- **CPU info prefetch** — the CPU string used by the crash report is fetched on a background thread while the game boots.

The boot time is printed to the log and shown in the bottom-left corner of the title screen.

## Compatibility

- Client-side only. Works on vanilla servers and singleplayer.
- Uses [MixinExtras](https://github.com/LlamaLad7/MixinExtras) wrap-operations instead of raw redirects, so it plays nicely with other optimization mods (ModernFix, VMP, Lithium, Krypton, etc.).
- If any injection fails on a future Minecraft version, the mod degrades gracefully — the game still launches, and the mod simply stays inactive with a warning in the log.
- A full resource reload is automatically scheduled as a fallback if the deferred pass ever fails.

## Configuration

`config/zerowait.json` — created on first launch, sensible defaults, no GUI needed:

| Option | Default | Description |
|---|---|---|
| `deferStartupReload` | `true` | Split the startup reload into immediate + deferred passes |
| `deferUnicodeFonts` | `true` | Skip unihex fonts on the immediate pass |
| `skipLoadingOverlay` | `true` | Close the Mojang overlay as soon as resources are ready |
| `dedicatedReloadExecutor` | `true` | Use the sized thread pool for resource loading |
| `fastCrashPreload` | `true` | Replace the blocking crash-report preload |
| `prefetchCpuInfo` | `true` | Fetch CPU info on a background thread |
| `showBootTimeOverlay` | `true` | Show boot time on the title screen |
| `deferredReloadStartDelayTicks` | `0` | Extra delay before the deferred pass starts |
| `moddedImmediatePatterns` | `[]` | Class-name patterns forced into the immediate pass (for modded listeners that must load early) |

## Supported versions

- **1.21.x** — 1.21 through 1.21.11
- **26.x** — 26.1 through 26.3
- Fabric Loader 0.16+ (Java 21 for 1.21.x, Java 25 for 26.x)

## Notes

- On 26.2+ the Mojang overlay behaves vanilla (the overlay API became private), the resource-splitting optimization itself is fully active.
- Boots are fastest warm: the driver shader cache and OS file cache do their part after the first launch.
- This mod does not touch world loading or server performance — it is strictly about the time from "Play" to the title screen.

## License

GPL-3.0-or-later. Source and details: see the repository.
