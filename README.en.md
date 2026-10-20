# Guogaology · 果糕逻辑

[简体中文](README.md) | English

![Guogaology](docs/branding/guogaology-mark.png)

**v0.6.1a (local compatibility backport) · Minecraft Java / Fabric** · [Published releases](https://github.com/hzyhhzy/minecraft-guogaology/releases)

This directory starts from the exact 0.6.1 source and backports the optional Minecraft 26.3 JEI tag protection, plus the newly requested plant item-display, wooden door/slab drop and native-sign compatibility fixes. It does not include portal, recipe, generation or other mechanism changes from 0.6.2 or later. All 101 Python checks, four offline builds and the exact artifact-change audit pass. This backport passes 10/10 real Minecraft 26.3 TCP cases: 20 first-pickup stages, 59 JEI queries, 70 native item/wood behavior checks and 56 native model checks. See the [0.6.1a work record](docs/WORK-061A.md) for provenance, final artifact hashes and validation scope. This local backport has not been merged into main or published.

Version 0.6.0 brings together resource-acquisition changes, biome-core recipes, survival advancements, row-based Laver table playback and landscape artwork. It includes Alice (AliceLdl)'s original 0.5.4.1 changes and four-version integration. Astra firs retain a straight axis, SCG Junctions use vivid rose-purple with gold and ivory, and the woven crown keeps its 64-stage cyclic gradient. See [release notes](docs/RELEASE-0.6.0.md), [the current survival guide](docs/SURVIVAL-GUIDE.en.md) and [guide milestones](docs/ADVANCEMENTS-057.en.md).

Three connected worlds: natural-scale Outer Guogaology, monumental Inner Guogaology, and the eerie Guogao Underworld. The Outer world supplies four equipment minerals and seven ordinary creatures. The deeper worlds supply biome materials and Ordinal Crystal Ore, which combine into crafted cores. Reversible enhancement supports equipment and held Denxi Manuscripts, whose passive effects work in either hand with offhand priority. Use the inventory **Book** button to edit an offhand manuscript; changes apply when its menu closes. No bosses are included.

Authorized Googology Dimension 1.0.0 (Alice version) terrain, templates and mineral/equipment art are reconstructed in maintainable source. See [provenance and adaptations](docs/OUTER-IMPORT.md) and the [current survival guide](docs/SURVIVAL-GUIDE.en.md).

**The original 0.5.4.1 changes were made by Alice (AliceLdl).** This repository reconstructs maintainable source from the supplied JAR and adapts it to all four Minecraft targets. Original authorship and integration work are recorded separately in [Contributors](AUTHORS.md) and the [0.5.4.1 provenance and validation notes](docs/WORK-0541-IMPORT.md).

## Installation

Download **one** JAR matching your Minecraft version from [Releases](https://github.com/hzyhhzy/minecraft-guogaology/releases). Install Fabric Loader and the matching Fabric API, then put both mod JARs in your instance's `mods` folder. Clients and servers need matching versions of Guogaology.

Version0.6.0 passes focused isolated26.2 checks withSodium/Voxy and26.3 checks with both the native renderer andSodium. The26.3 Ordinal Ore encoding and terrain-heightmap tags are repaired. The two other targets pass build/resource checks; see the [exact validation scope](docs/WORK-060.md).

Version 0.5.5 passed a local 26.2 dedicated Fabric server check with a separate client: manuscript/station interactions, dimension travel, flight and saved-world restart. The test bound only to 127.0.0.1 and was not exposed to the LAN or Internet. See [the test record and scope](docs/DEDICATED-SERVER-055.md); this was not a multiplayer capacity benchmark.

A separate local test with Voxy World Gen V2 2.4.3 on the server and Voxy 0.2.19-beta on the client passed distant-data reception, return trips and fresh-client reception after restart in all three additional dimensions. The server does not need the Voxy renderer. Tested at 32-chunk radius/2 active tasks; cold generation causes substantial load. See [the LOD multiplayer test](docs/DEDICATED-VOXY-055.md).

The same 1.21.1 JAR for version 0.5.3 was tested on NeoForge 21.1.248 with Sinytra Connector beta.16 and Forgified Fabric API (beta.17 was also tested with 0.5.2). Version 0.6.0 retains those compatibility fixes, but Connector was not rerun for this update. Use Forgified Fabric API instead of ordinary Fabric API in that setup. Connector is optional; see [tested dependencies and scope](docs/CONNECTOR-COMPATIBILITY.md).

| Minecraft | Java | Fabric Loader | Fabric API used for builds |
|---|---|---|---|
| 1.21.1 | 21+ | 0.16.10+ | 0.116.17+1.21.1 |
| 1.21.11 | 21+ | 0.19.3+ | 0.141.6+1.21.11 |
| 26.2 | 25+ | 0.19.3+ | 0.153.0+26.2 |
| 26.3 | 25+ | 0.19.3+ | 0.161.0+26.3 |

The internal mod ID is `guogaology` starting with 0.5.0. This change includes no old-save migration or aliases for the former IDs. The standalone core gallery and flight-speed utility are separate projects and are not required.

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
- Ordinal and numeric equipment can use native enchantments or socketed cores, with the two routes mutually exclusive. Remove all cores or all enchantments to switch. Core combat, gathering and manuscript effects retain their established rules.

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

The batch build writes JARs, dependencies, and SHA-256 checksums to `build/releases/0.6.0/`. Models, textures, and notation datasets are included in this repository. Building does not require the author's spreadsheets, launcher profile, or historical work files.

Run `python tools/audit_resources.py --check` and `python tools/audit_localization.py --check` to check assets and both languages. The four-target GitHub Actions workflow runs these checks as well.

`src/main` and `src/client` provide the 1.21.1 implementation. `ports/common` and target-specific overrides adapt modern APIs; `ports/shared-sources.txt` lists shared algorithms. Edit source files, not generated build directories. The logo can be rebuilt with `tools/generate_brand.py` and the optional Pillow dependency.

Version 0.6.0's release scope and actual validation results are tracked in [the release work record](docs/WORK-060.md). The published [v0.5.3](https://github.com/hzyhhzy/minecraft-guogaology/releases/tag/v0.5.3) and its [historical validation record](docs/WORK-0503.md) remain available. Historical results do not describe additional tests for this update. Install the Python test dependencies with `python -m pip install -r requirements-dev.txt` before running the test suite.

## License

Since 0.3.1, original project content is licensed under **GNU GPL v3 only**, with a [Minecraft linking exception](LICENSE-MINECRAFT-EXCEPTION). Redistribution must provide corresponding source and license notices as required by GPL. The exception does not license Minecraft itself for redistribution.

Earlier copies distributed under MIT retain their original license. Third-party components retain their own licenses.

[GPL v3](LICENSE) · [Copyright and scope](COPYRIGHT) · [Third-party notices](THIRD_PARTY.md)

[0.6.0 release and validation](docs/WORK-060.md) · [Offline enhancement simulator](docs/enhancement-simulator.html) · [Current survival guide](docs/SURVIVAL-GUIDE.en.md) · [0.6.0 release notes](docs/RELEASE-0.6.0.md)
