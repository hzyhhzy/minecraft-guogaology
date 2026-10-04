# Guogaology · 果糕逻辑

[简体中文](README.md) | English

![Guogaology](docs/branding/googology-mark.png)

**v0.3.2 · Minecraft Java / Fabric**

Explore worlds inspired by large numbers, ordinal notation, and the googology community. Guogaology adds two dimensions, eight biomes, monumental landmarks, ordinal ores, Denxi equipment, crystal fusion, and reversible enhancements. There are currently no custom creatures or bosses; the landmarks include arenas reserved for future encounters.

## Installation

Download **one** JAR matching your Minecraft version from [Releases](https://github.com/hzyhhzy/minecraft-guogaology/releases). Install Fabric Loader and the matching Fabric API, then put both mod JARs in your instance's `mods` folder. Clients and servers need matching versions of Guogaology.

| Minecraft | Java | Fabric Loader | Fabric API used for builds |
|---|---|---|---|
| 1.21.1 | 21+ | 0.16.10+ | 0.116.17+1.21.1 |
| 1.21.11 | 21+ | 0.19.3+ | 0.141.6+1.21.11 |
| 26.2 | 25+ | 0.19.3+ | 0.153.0+26.2 |
| 26.3 | 25+ | 0.19.3+ | 0.161.0+26.3 |

The internal mod ID remains `googology`. The standalone core gallery and flight-speed utility are separate projects and are not required.

## Language

The same JAR includes **English and Simplified Chinese**. Choose **Options → Language → English (US)** or **简体中文（中国大陆）** in Minecraft; no translation pack is needed. Block and item names, saved variants, equipment tooltips, enhancement screens, creative tabs, biome and landmark names, advancements, sound subtitles, and command help follow your language. Mod Menu also has localized names and descriptions when its translation options are enabled.

The names **Guogao** and **Denxi** are intentional transliterations of community terms. Guogao refers both to fruit confectionery and to the anxious, sweating expression associated with unusually difficult or strange ordinal notations. Denxi is the equipment family name and the name of its special ordinal-mining speed.

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

The first seven biomes belong to the Guogaology World. Guogao Dreadwood is in the separate Guogao Underworld. Six tiers of ores support pickaxes, swords, and armor; nine crystal families support fusion and equipment enhancement.

[Survival guide](docs/SURVIVAL-GUIDE.en.md) · [Harvesting reference](docs/BLOCK-BALANCE.en.md) · [Chinese–English terminology](docs/TERMINOLOGY.md)

## Entering the dimensions

Arrange 12 whole cakes or Guogao blocks of any color in an End-portal-shaped ring: a 5×5 outline without corners, surrounding an empty 3×3 center. You may mix frame materials.

- Throw an **apple** into the ring to travel between the Overworld and the Guogaology World.
- Throw a **Guogao block** into the ring to enter the Guogao Underworld; using this portal there returns you to the Guogaology World.
- Horizontal distances scale as **Overworld : Guogaology World : Guogao Underworld = 1 : 4 : 16**.

Falling into the Guogaology void sends you to the Underworld. Gravity there is one quarter of normal, and falling causes no damage. `/googology` shows help; administrators can use `/googology return`.

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

The batch build writes validated JARs, dependencies, and SHA-256 checksums to `build/releases/0.3.2/`. Models, textures, and notation datasets are included in this repository. Building does not require the author's spreadsheets, launcher profile, or historical work files.

Run `python tools/audit_resources.py --check` and `python tools/audit_localization.py --check` to check assets and both languages. The four-target GitHub Actions workflow runs these checks as well.

`src/main` and `src/client` provide the 1.21.1 implementation. `ports/common` and target-specific overrides adapt modern APIs; `ports/shared-sources.txt` lists shared algorithms. Edit source files, not generated build directories. The logo can be rebuilt with `tools/generate_brand.py` and the optional Pillow dependency.

All four targets receive build and resource checks. Current gameplay verification is limited to an isolated Minecraft 26.2 instance; this does not imply full playtesting of the other versions. See [validation records](docs/VALIDATION.md).

## License

Since 0.3.1, original project content is licensed under **GNU GPL v3 only**, with a [Minecraft linking exception](LICENSE-MINECRAFT-EXCEPTION). Redistribution must provide corresponding source and license notices as required by GPL. The exception does not license Minecraft itself for redistribution.

Earlier copies distributed under MIT retain their original license. Third-party components retain their own licenses.

[GPL v3](LICENSE) · [Copyright and scope](COPYRIGHT) · [Third-party notices](THIRD_PARTY.md)
