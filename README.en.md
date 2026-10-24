# Guogaology · 果糕逻辑

[简体中文](README.md) | English

![Guogaology](docs/branding/guogaology-mark.png)

**Local candidate0.6.14**: continuous, spaced Astra/Underworld tree lights, a rough Underworld bedrock floor, and native-path repairs for core Silk Touch and wooden mechanisms. Kept on`codex/0.6.14-tree-lights-bedrock`, outside main; see[work record](docs/WORK-0614.md). Version0.6.13 visual repairs and0.6.12 mining patterns are verified and integrated into local main, without push or publication. See[visual repairs](docs/WORK-0613.md) and[pattern controls](docs/WORK-0612.md).

**v0.6.11 · Minecraft Java / Fabric** · [Downloads and release notes](https://github.com/hzyhhzy/minecraft-guogaology/releases/tag/v0.6.11)

**0.6.11** updates bow speeds, separate Tower attack values for swords, bows and manuscripts, and innate manuscript attack, with matching equipment previews and offline simulation. It includes 0.6.10's arrow pickup, draw icons, wooden products, red-sand TNT, Silk Touch gummy jars, and core rendering/collision changes. See the [release notes](docs/RELEASE-0.6.11.md#english), [validation record](docs/WORK-0611.md) and [enhancement simulator](docs/enhancement-simulator.html?lang=en). After its initial publication from a separate branch, this release has been integrated into `main` under subsequent authorization; the `v0.6.11` tag and published assets remain unchanged.

Earlier 0.6.9 repairs for extra hits, boat names, furniture facing, stripping and 1.21.1 boats remain included, alongside 0.6.8 JEI compatibility and 0.6.7 world-loading fixes. Bow balance was deferred in the 0.6.10 report; this update implements the subsequently approved design. Oversized sapling/vine drops were not reproduced in the native comparisons and are not claimed as fixed. The separate 0.6.1a backport remains in the [historical release](https://github.com/hzyhhzy/minecraft-guogaology/releases/tag/v0.6.8).

The update retains 0.6.6's Cores and Guogaology recipe-book sections, main-logo tab icon and Silk Touch experience fix, alongside 0.6.5's core recipes, natural materials and landmark rewards and 0.6.4's Absence Core compatibility cleanup. FFFZ/FOS and ψ/Z stability mechanics are unchanged. Earlier blueprint and portal fixes, Alice (AliceLdl)'s original 0.5.4.1 work and all four target adaptations remain included. See the [current work record](docs/WORK-0611.md), [current survival guide](docs/SURVIVAL-GUIDE.en.md) and [guide milestones](docs/ADVANCEMENTS-057.en.md).

Three connected worlds: natural-scale Outer Guogaology, monumental Inner Guogaology, and the eerie Guogao Underworld. The Outer world supplies four equipment minerals and seven ordinary creatures. The deeper worlds supply biome materials and Ordinal Crystal Ore, which combine into crafted cores. Reversible enhancement supports equipment and held Denxi Manuscripts, whose passive effects work in either hand with offhand priority. Use the inventory **Book** button to edit an offhand manuscript; changes apply when its menu closes. No bosses are included.

Authorized Googology Dimension 1.0.0 (Alice version) terrain, templates and mineral/equipment art are reconstructed in maintainable source. See [provenance and adaptations](docs/OUTER-IMPORT.md) and the [current survival guide](docs/SURVIVAL-GUIDE.en.md).

**The original 0.5.4.1 changes were made by Alice (AliceLdl).** This repository reconstructs maintainable source from the supplied JAR and adapts it to all four Minecraft targets. Original authorship and integration work are recorded separately in [Contributors](AUTHORS.md) and the [0.5.4.1 provenance and validation notes](docs/WORK-0541-IMPORT.md).

## Installation

Download **one** JAR matching your Minecraft version from [Releases](https://github.com/hzyhhzy/minecraft-guogaology/releases). Install Fabric Loader and the matching Fabric API, then put both mod JARs in your instance's `mods` folder. Clients and servers need matching versions of Guogaology.

All four 0.6.3 targets passed build/resource checks. That version used isolated 26.2 for native palette, blueprint, placed-block and rendered-scene checks, alongside pure geometry reservation regressions. Other-version, Connector and Voxy runtime environments were not rerun then. Portal/real 0.6.0 save upgrades were tested in 0.6.2, and the four independent 26.3 client/server JEI installation combinations in 0.6.1. See [0.6.3 evidence](docs/WORK-063.md), [portal evidence](docs/WORK-062.md) and [JEI repair evidence](docs/WORK-061.md).

Version 0.5.5 passed a local 26.2 dedicated Fabric server check with a separate client: manuscript/station interactions, dimension travel, flight and saved-world restart. The test bound only to 127.0.0.1 and was not exposed to the LAN or Internet. See [the test record and scope](docs/DEDICATED-SERVER-055.md); this was not a multiplayer capacity benchmark.

A separate local test with Voxy World Gen V2 2.4.3 on the server and Voxy 0.2.19-beta on the client passed distant-data reception, return trips and fresh-client reception after restart in all three additional dimensions. The server does not need the Voxy renderer. Tested at 32-chunk radius/2 active tasks; cold generation causes substantial load. See [the LOD multiplayer test](docs/DEDICATED-VOXY-055.md).

The same 1.21.1 JAR for version 0.5.3 was tested on NeoForge 21.1.248 with Sinytra Connector beta.16 and Forgified Fabric API (beta.17 was also tested with 0.5.2). Version 0.6.3 retains those compatibility fixes, but Connector was not rerun for this update. Use Forgified Fabric API instead of ordinary Fabric API in that setup. Connector is optional; see [tested dependencies and scope](docs/CONNECTOR-COMPATIBILITY.md).

| Minecraft | Java | Fabric Loader | Fabric API used for builds |
|---|---|---|---|
| 1.21.1 | 21+ | 0.16.10+ | 0.116.17+1.21.1 |
| 1.21.11 | 21+ | 0.19.3+ | 0.141.6+1.21.11 |
| 26.2 | 25+ | 0.19.3+ | 0.153.0+26.2 |
| 26.3 | 25+ | 0.19.3+ | 0.161.0+26.3 |

The internal mod ID is `guogaology` starting with 0.5.0. Version 0.6.11 retains saved identities, arrow data formats and 0.6.4's Lv1 Absence Core component cleanup; migration or aliases for the older `googology` namespace are not provided. The standalone core gallery and flight-speed utility are separate projects and are not required.

All four 0.6.11 targets build and pass save-surface audits; 131 Python and 2,321 pure Java checks pass. Primary 26.3 and focused 26.2 runs each pass 1,590 combat checks, including 144 real arrows, 90 melee attacks and 48 native criticals, plus the 0.6.10 repair regressions. Simulator/Java comparisons and real browser checks also pass. Only innate manuscript attack increases: protection, healing, mining and other innate channels remain unchanged. Enchanting and core sockets remain mutually exclusive. See the [current validation record](docs/WORK-0611.md).

The 1.21.x targets have build/static coverage only this round. Version 0.6.11 checks constructed old-item/arrow compressed NBT roundtrips, but does not repeat a whole old-world upgrade. The real old-save upgrade/reopen and two JEI networking modes in 0.6.10 are [historical results](docs/WORK-0610.md), not new 0.6.11 tests. Connector, Voxy and mixed Mod-version networking were not rerun, and arbitrary damaged saves are not promised recoverable. Update both clients and servers.

Historical validation: all four 0.6.5 targets built and 88 Python checks passed. Isolated 26.2 tests for that version in both languages verified 1900 crafts, 8192 native chest fills, 6144 entity-loot draws, recipe-book search/filtering, narrow layout and the 2×2 inventory. Other targets were built only then. Raw stability classes and existing resource entrypoints were preserved; the real saved-world upgrade tests from 0.6.4 remain historical evidence. See [0.6.5 evidence](docs/WORK-065.md) and [0.6.4 evidence](docs/WORK-064.md).

## Language

The same JAR includes **English and Simplified Chinese**. Choose **Options → Language → English (US)** or **简体中文（中国大陆）** in Minecraft; no translation pack is needed. Block and item names, saved variants, equipment tooltips, enhancement screens, creative tabs, biome and landmark names, advancements, sound subtitles, and command help follow your language. Mod Menu also has localized names and descriptions when its translation options are enabled.

The names **Guogao** and **Denxi** are intentional transliterations of community terms. Guogao refers both to fruit confectionery and to the anxious, sweating expression associated with unusually difficult or strange ordinal notations. Denxi now also names the Manuscript, which grants passive effects while held in either hand, with offhand priority; the former exponential ore-mining gate has been removed.

## The worlds

| Biome | Landscape | Landmark |
|---|---|---|
| Matrix Mountains | BMS boards, Y-sequences, angular ridges, mountain laminae | Matrix Folded Keep |
| Power Tower Desert | Power towers, Graham trees, TREE and SCG forms, submerged notation relics | Iterated Timber Pagoda |
| Ordinal Garden | Symbol plants, Veblen crowns, Hydras, ω trees, Ω mushrooms | Six-Branch Crown Court |
| LHO Realm of Absence | Transparent islands, LHO letters, vanishing notation fragments | Missing-Page Galleries |
| Laver Tablelands | Playable iBLP tables, giant laver, Tianyi yarn, Guogao sweets | Laver Triangle Palace |
| Astra Inference Plains | Bright cities, server racks, woven knots, mushroom clouds | Astra Inference Nexus |
| Boundary Highlands | Turing-machine tapes, set shells, proof and formula landscapes | Boundary Calculus Court |
| Guogao Dreadwood | Eerie Christmas trees, thick vines, fractured terrain, lakes | Guogao Finality Tree |

The first seven biomes belong to Inner Guogaology. Guogao Dreadwood is in the separate Guogao Underworld. Outer Guogaology adds the donor's seven natural-scale biomes and four mineral tiers. Nine crystal families support fusion and equipment enhancement in the deeper worlds.

[Survival guide](docs/SURVIVAL-GUIDE.en.md) · [Harvesting reference](docs/BLOCK-BALANCE.en.md) · [Chinese–English terminology](docs/TERMINOLOGY.md)

## Current resource acquisition

- Inner and Underworld wilderness no longer places finished cores or Ordinal Crystal blocks. Gather Matrix Sheets, Power Tower Wood, SCG Junctions, Hydra Sprigs, Laver Sheets, Nuclear Blast Blocks, Set Membranes and Guogao, then combine them with theme materials. Lv1 puts a base Ordinal Crystal at the center; Lv2/Lv3 surround an ε/Γ Mineral Block with eight matching preceding-grade cores. Giant landmarks retain a few display cores and lower-grade chest rewards.
- Ordinal Crystal Ore uses vanilla iron's average density as its benchmark, with numbered-stone, transparent-island and Dreadstone variants. Mine shards; four shards and one base crystal convert both ways. Mining a crystal block returns the block itself.
- LHO disappearance, connected decay and anchored harvesting remain. An Ordinal Crystal stabilizes two neighboring rings; ψ, Z, fffZ and FOS visibly change color. Absence Cores are crafted.
- A Guogao Lamp Shaper converts one lamp into one of 54 shapes of the same color. Ordinary mining gives the basic whole lamp; Silk Touch preserves its expression and tile.
- Ordinal and numeric equipment can use native enchantments or socketed cores, with the two routes mutually exclusive. Remove all cores or all enchantments to switch. See the [0.6.11 release notes](docs/RELEASE-0.6.11.md#english) for combat values; other gathering and manuscript functions remain included.

## Travel

All rings are twelve blocks in a 5×5 frame without corners, with an empty 3×3 center.

- Overworld: whole cakes (Guogao blocks may be mixed in) plus an apple → Outer.
- Outer: 12 mineral storage blocks, freely mixing ω/ε/Γ/Ω, plus one Ω mineral → Inner.
- Inner: 12 Guogao blocks of any color plus one Guogao block → Underworld.
- Return: 12 matching Return Frames plus one Return Token → previous layer.

Outer Return Frames cost 4 cobblestone + 1 plank; Inner Return Frames cost 4 Ordinal Shards + 1 plank; Dread Forest Return Frames cost 4 Guogao Dreadsoil + 1 Dread Fir Plank. Each craft yields one frame; use any planks where unspecified. Twelve crafts make one ring. A Return Token costs one plank above one stick. Frame types cannot be mixed. All materials are locally obtainable: Ordinal Crystal Ore can be mined by hand for shards, and a base crystal can be crafted back into four shards. Normal arrivals create directly usable reverse gates. Horizontal scales are 1:1:4:16. Falling to Y≤−500 in Outer Guogaology leads to Inner; void damage remains active during the descent and is reduced by protection. Falling through the Inner void leads to the Underworld at Y=500. Underworld gravity is quarter strength and prevents fall damage.

Everyone can use `/guogaology` or `/guogaology help`. The following commands require administrator permission level 2:

- `/guogaology tp <outer|inner|underworld|overworld>` goes directly to the named dimension; using it in that dimension only displays a message.
- `/guogaology up` returns one layer: Underworld → Inner → Outer → Overworld. It does not return to a previous position, bed or spawn point.
- `/guogaology kit portal <outer|inner|underworld>` provides the forward portal frame and activation materials for the named destination.
- `/guogaology kit return` provides 12 Return Frames for the current layer and one Return Token.

## Building from source

Install JDK 21, JDK 25, Python 3.10+, and PowerShell. The Gradle wrappers are included; a first build downloads dependencies. Ordinary builds need only Python's standard library.

```powershell
$env:JAVA21_HOME = '/path/to/jdk-21'
$env:JAVA25_HOME = '/path/to/jdk-25'
./build-all.ps1
```

For a single version:

```sh
# JDK 21
./gradlew :build

# JDK 25; replace 26.2 with the modern target you need
./ports/gradlew -p ports -Ptarget=26.2 -PportPython=python :26.2:build
```

The batch build writes JARs, dependencies, and SHA-256 checksums to `build/releases/0.6.11/`. Models, textures, and notation datasets are included in this repository. Building does not require the author's spreadsheets, launcher profile, or historical work files.

Run `python tools/audit_resources.py --check` and `python tools/audit_localization.py --check` to check assets and both languages. The four-target GitHub Actions workflow runs these checks as well.

`src/main` and `src/client` provide the 1.21.1 implementation. `ports/common` and target-specific overrides adapt modern APIs; `ports/shared-sources.txt` lists shared algorithms. Edit source files, not generated build directories. The logo can be rebuilt with `tools/generate_brand.py` and the optional Pillow dependency.

Version 0.6.11's scope and actual validation are tracked in [the work record](docs/WORK-0611.md). The published [v0.6.9](https://github.com/hzyhhzy/minecraft-guogaology/releases/tag/v0.6.9) and its [historical validation record](docs/WORK-069.md) remain available. Historical results do not describe additional tests for this update. Install the Python test dependencies with `python -m pip install -r requirements-dev.txt` before running the test suite.

## License

Since 0.3.1, original project content is licensed under **GNU GPL v3 only**, with a [Minecraft linking exception](LICENSE-MINECRAFT-EXCEPTION). Redistribution must provide corresponding source and license notices as required by GPL. The exception does not license Minecraft itself for redistribution.

Earlier copies distributed under MIT retain their original license. Third-party components retain their own licenses.

[GPL v3](LICENSE) · [Copyright and scope](COPYRIGHT) · [Third-party notices](THIRD_PARTY.md)

[0.6.11 release notes](docs/RELEASE-0.6.11.md#english) · [Validation record](docs/WORK-0611.md) · [Offline enhancement simulator](docs/enhancement-simulator.html?lang=en) · [Current survival guide](docs/SURVIVAL-GUIDE.en.md)
