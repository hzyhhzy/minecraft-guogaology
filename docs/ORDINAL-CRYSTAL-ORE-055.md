# 0.5.5 Ordinal Crystal ore generation

Ordinal Crystal ore uses Minecraft's native `minecraft:ore` feature. Its three placement batches match the vanilla Minecraft 26.2 iron resources directly:

| Batch | Attempts per chunk | Height provider | Vein size |
|---|---:|---|---:|
| Upper | 90 | Triangle from Y80 to Y384 | 9 |
| Middle | 10 | Triangle from Y−24 to Y56 | 9 |
| Small | 10 | Uniform from dimension bottom to Y72 | 4 |

The vanilla in-square placement, biome filter and zero air-exposure discard are preserved. Attempts do not guarantee a vein: the native algorithm must find an eligible host block, and floating landmass air naturally rejects attempts. This is an exact **iron placement benchmark**, not a claim that different terrain geometries contain exactly the same final blocks per square kilometre. Giant raw-iron veins are not copied.

Seven Inner biomes and all four Underworld biomes carry the same three features at `UNDERGROUND_ORES` (step 6). Outer biomes and their four equipment-mineral distributions are untouched. The upper triangle supplies elevated mountains/floating continents; the two lower batches supply lower islands and Underworld rock. This direct, maintainable iron benchmark replaces the earlier draft's custom local-burial distribution and exterior-only exposure filter. Caves may expose crystal ore normally.

Three visual ore variants share the same resource:

- Numeric host: the ten exact `ordinal_stone` / `_1` … `_9` blocks become `ordinal_crystal_ore`.
- LHO transparent host: `absence_glass` becomes `empty_crystal_ore`.
- Underworld host: `rootbound_stone` becomes `dread_crystal_ore`.

Bedrock, plants, lake beds/soil, normal buildings, existing minerals and crystals are not replacement targets. Ore generation does not place finished crystals or regional cores. The current pre-feature cave-surface restoration and numeric-rock pass remain responsible for the ordinary terrain substrates.

`tools/generate_ordinal_crystal_ore.py` owns the two configured features, three placed features, four ore/host tags and the feature memberships of those eleven biomes. It extends the general `guogaology:ores` tag for extra-excavation classification. Run after broad imported-resource generation. Underworld variants regenerated from `guogao_forest` inherit the same features; rerunning the generator is idempotent.

`tools/tests/test_ordinal_crystal_ore.py` verifies exact vanilla placement equality when the cached vanilla 26.2 JAR is available, all host blocks, once-per-step biome membership, generator roundtrips and an untouched Outer-biome sentinel. Integrated generation samples are recorded in `WORK-055.md`.
