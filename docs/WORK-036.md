# 0.3.6 — LHO boundaries and plain Guogao

## Authorized scope

- [x] Outer world: retain `lho_edge`, restrict it to the vicinity of real LHO void; restore neighboring ordinary biomes and their features farther away.
- [x] Inner world: taper both sides of LHO boundaries into a genuine empty gap. Keep existing biomes; scenery and buildings must not bridge the gap.
- [x] Add four plain Guogao gummy colors, without emoji relief. Each gummy on an outer Laver table independently has an 80% plain / 20% embossed roll (user explicitly permits mixed tables); retain the existing gummy count distribution.
- [x] Validate resources, localization, bounded edge behavior, inner separation, and gummy placement/harvesting.
- [x] Build 1.21.1 / 1.21.11 / 26.2 / 26.3; runtime validation only in isolated hidden fo262.
- [x] Install the main mod only if normal fo262 is closed, with backup and hash verification.

World generation changes apply to newly generated chunks. No old-world migration or Voxy lighting changes are included.

## Implementation

- Outer `LhoBorderBiomeSource` keeps the original climate source and a second source without LHO entries. An original edge sample is retained only within 48 blocks of the original surface void; otherwise its ordinary inland biome is restored. Climate ties use declaration order, avoiding the vanilla nearest-leaf cache's query-order dependence. Per-thread caches are bounded and never request chunks.
- Inner terrain subtracts a smooth density cut around the boundary between LHO and the strongest other biome. The central gap is air through the terrain height; lakes do not fill it. Natural scenery and representative buildings avoid crossing the gap as whole structures. No extra inner biome is registered.
- Four `plain_*_guogao` blocks reuse the existing frosted gummy body and omit all relief elements. Color, collision, translucency, loot, fruit recipes, and portal use are preserved. Only outer table placement uses the independent 80/20 choice; inner/underworld embossed scenery retains its existing block selection.

## Validation

- Four release targets built and passed package checks. JARs and hashes: `build/releases/0.3.6/manifest.json`.
- Resource audit: 2,402 reachable resources, no missing or unused resources. Localization audit: 407 matching bilingual keys, 245 block names, 56 non-block item names; no missing translations or format conflicts.
- Hidden fo262 full regression: `build/runtime-qa/boundaries-036-d`; 203 mechanics/template checks plus 473 material checks passed, as did real feature placement, three-world travel, and normal shutdown.
- New biome-source survey covers 16.78 km² at 16-block sample spacing, without generating all those chunks: 10,030 previous edge samples become 3,688 bounded coastline samples and 6,342 ordinary inland samples; all 7,011 void samples remain void. The previously barren wide-edge site is restored. A climate-tie regression remains stable after 128 intervening queries.
- Inner checks cover 486 gap columns with the actual 4×8×4 interpolated density and fluid rules. One actual fully decorated generated column additionally contains air at all 376 checked heights, confirming that this is not merely transparent glass.
- Independent gummy rolls: 8,034 plain in 10,000. Actual table placement across all four rotations yielded 293 plain and 67 embossed gummies, including 19 mixed tables. All four colors pass translucent registration, creative inventory, fruit-ritual, and hand-harvest checks. Screenshots verify plain bodies alongside the original embossed versions.
- Focused visual follow-up: `build/runtime-qa/boundaries-036-close`, using `--boundaries-only`, passed the same new checks and normal shutdown. Its third screenshot shows the LHO glass coast and the neighboring solid mainland separated by open air. Both test runs loaded the exact final 26.2 release hash below.

These are targeted source, generation, and runtime checks, not a full-modpack performance benchmark. Runtime validation remains restricted to Minecraft 26.2; the other three versions are compile/package checked only.

## Installation

At 2026-10-05 23:55 +08:00, process checks confirmed Minecraft was closed. Installed `googology-dimension-26.2-0.3.6.jar` into the normal `fo262/mods` directory. SHA-256: `2d11105d1726ec7b775ac54b80334e32bb41858274947b7d678db43b7f369f58`, identical to both runtime-test copies. Exactly one main Mod is present; all 55 other files retain their previous hashes. Saves and configuration were not modified.

Previous 0.3.5 JAR and installation record are preserved in `build/backups/fo262-before-0.3.6/`. No GitHub push or release was performed.
