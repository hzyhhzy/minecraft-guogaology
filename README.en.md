# Guogaology · 果糕逻辑

[简体中文](README.md) | English

![Guogaology](docs/branding/guogaology-mark.png)

**v0.5.1 · Minecraft Java / Fabric · Four builds and hidden fo262 regression passed**

Three connected worlds: natural-scale Outer Guogaology, monumental Inner Guogaology, and the eerie Guogao Underworld. The Outer world supplies four ordinal minerals and seven ordinary creatures; crystal cores come from the two deeper worlds. Reversible enhancement supports equipment and held Denxi Manuscripts, whose passive effects work in either hand with offhand priority. Use the inventory **Book** button to edit an offhand manuscript; changes apply when its menu closes. No bosses are included.

Authorized Googology Dimension 1.0.0 (Alice version) terrain, templates and mineral/equipment art are reconstructed in maintainable source. See [provenance and adaptations](docs/OUTER-IMPORT.md) and [current gameplay rules](docs/MERGE-REVIEW-CHECKLIST.en.md).

## Installation

Download **one** JAR matching your Minecraft version from [Releases](https://github.com/hzyhhzy/minecraft-guogaology/releases). Install Fabric Loader and the matching Fabric API, then put both mod JARs in your instance's `mods` folder. Clients and servers need matching versions of Guogaology.

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

## Travel

All rings are twelve blocks in a 5×5 frame without corners, with an empty 3×3 center.

- Overworld: whole cakes (Guogao blocks may be mixed in) plus an apple → Outer.
- Outer: 12 mineral storage blocks, freely mixing ω/ε/Γ/Ω, plus one Ω mineral → Inner.
- Inner: 12 Guogao blocks of any color plus one Guogao block → Underworld.
- Return: 12 matching Return Frames plus one Return Token → previous layer.

Outer Return Frames cost 4 cobblestone + 1 plank; Inner Return Frames cost 4 Ordinal Shards + 1 plank; Dread Forest Return Frames cost 4 Guogao Dreadsoil + 1 Dread Fir Plank. Each craft yields one frame; use any planks where unspecified. Twelve crafts make one ring. A Return Token costs one plank above one stick. Frame types cannot be mixed. All materials are locally obtainable; Ordinal Crystals drop shards when broken by hand. Normal arrivals create directly usable reverse gates. Horizontal scales are 1:1:4:16. Falling to Y≤−500 in Outer Guogaology leads to Inner; void damage remains active during the descent and is reduced by protection. Falling through the Inner void leads to the Underworld at Y=500. Underworld gravity is quarter strength and prevents fall damage.

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

The batch build writes validated JARs, dependencies, and SHA-256 checksums to `build/releases/0.5.1/`. Models, textures, and notation datasets are included in this repository. Building does not require the author's spreadsheets, launcher profile, or historical work files.

Run `python tools/audit_resources.py --check` and `python tools/audit_localization.py --check` to check assets and both languages. The four-target GitHub Actions workflow runs these checks as well.

`src/main` and `src/client` provide the 1.21.1 implementation. `ports/common` and target-specific overrides adapt modern APIs; `ports/shared-sources.txt` lists shared algorithms. Edit source files, not generated build directories. The logo can be rebuilt with `tools/generate_brand.py` and the optional Pillow dependency.

Version 0.5.1 adds downhill-pointing greater-than ornaments to Underworld chains and restores the full earlier rewards in all eight landmarks. All four builds, resource and language checks, and 39 Python tests pass. An isolated hidden fo262 / Minecraft 26.2 validates actual generated chains and 8,192 native chest fills with Sodium, Voxy and the standalone flight-speed utility loaded; five screenshots were inspected. The normal fo262 instance has been updated to 0.5.1, with the previous JAR backed up and hashes verified. This patch has not been pushed or published; see [the validation record](docs/WORK-0501.md). Other Minecraft versions receive compile and resource checks only, not runtime playtesting. Earlier [validation records](docs/VALIDATION.md) remain available. Install the Python test dependencies with `python -m pip install -r requirements-dev.txt` before running the test suite.

## License

Since 0.3.1, original project content is licensed under **GNU GPL v3 only**, with a [Minecraft linking exception](LICENSE-MINECRAFT-EXCEPTION). Redistribution must provide corresponding source and license notices as required by GPL. The exception does not license Minecraft itself for redistribution.

Earlier copies distributed under MIT retain their original license. Third-party components retain their own licenses.

[GPL v3](LICENSE) · [Copyright and scope](COPYRIGHT) · [Third-party notices](THIRD_PARTY.md)

[0.5.1 implementation and validation](docs/WORK-0501.md) · [Offline enhancement simulator](docs/enhancement-simulator.html) · [0.5.0 release notes](docs/RELEASE-0.5.0.md)
