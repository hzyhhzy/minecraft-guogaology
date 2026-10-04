# Survival and materials guide · 0.3.2

[简体中文](SURVIVAL-GUIDE.md) | English · [Project overview](../README.en.md)

Guogaology has two dimensions, eight biomes, eight major landmarks, ordinal mining, Denxi equipment, and crystal enhancements. Custom creatures and bosses are not implemented yet.

## Travel

Build a 5×5 frame without corners from 12 whole cakes or Guogao blocks, leaving the 3×3 center empty. Colors and frame materials may be mixed. Throw an apple into it for an Overworld–Guogaology portal, or a Guogao block for an Underworld portal. A Guogao portal used inside the Underworld leads back to Guogaology. Breaking the frame disables the whole portal.

Horizontal distances scale as **Overworld : Guogaology World : Guogao Underworld = 1 : 4 : 16**. Falling to Y≤−120 in Guogaology sends you to Y=500 in the Underworld, where gravity is quarter strength and fall damage is disabled. A first trip to an unexplored destination still needs time to generate its chunks.

Use `/googology` for help. Portals are the normal return route; `/googology return` requires administrator permissions.

## Gathering and common materials

| Material | Source | Use |
|---|---|---|
| Ordinal Shard | Break a Lv1 Ordinal Crystal by hand or with a tool; landmark chests | Four make a Lv1 crystal; one Lv1 crystal can be crafted back into four shards. Also used in patterns and advanced mineral processing. |
| Guogao Slice | Craft one Guogao block into four slices | Food |
| Laver Sheet | Craft one Giant Laver Blade into four sheets | Laver Table Planks |
| Compute Chip | Craft a server rack or terminal into two chips; landmark chests | Proof Boundary Crystal Clusters |

Lv1 Ordinal Crystals use glowstone-style drops: normally 2–4 shards, Fortune increases the yield up to four, Silk Touch preserves the block, and explosions reduce drops through vanilla rules. Mining a rack or terminal drops the block; crafting it performs the dismantling.

ω tree logs, Laver Veins, and Power Tower Wood have wood-related recipes. Dread Fir logs and leaves are fireproof. Digit bricks, sequence lights, and Guogao lantern tiles retain their saved appearance when picked up and placed again.

Player-placed regional cores, TREE colored nodes, landmark relics, the five rare LHO materials, and higher-grade Ordinal Crystals can be recovered intact with any tool or by hand. Lv1 Ordinal Crystals still follow the shard rules. Naturally generated blocks have their own tool requirements; see the [harvesting reference](BLOCK-BALANCE.en.md).

## Six mineral tiers and equipment

The tiers are **Omega (ω), Epsilon Zero (ε₀), Gamma Zero (Γ₀), Psi (ψ), Mountain Strata, and Proof Boundary**. Ores generate in Number Stone and Guogao Dreadstone, with a matching ore variant for each host. They drop materials or fragments directly; no smelting is required. Generation changes affect newly generated chunks only.

The final ore count depends on vein candidates, terrain, overlaps, and exposure. Exposed candidates retain 10% of placements, cave-wall candidates retain 50%, and buried candidates retain all placements. Caves include chambers of different sizes and long, narrow passages; some open onto the surface or a mountainside.

The first four tiers yield usable crystal materials directly. The last two need crafting; consult the recipe book for layouts:

| Output | Ingredients per item |
|---|---|
| Mountain Strata Crystal | 4 Mountain Strata Ore Shards + 2 Ordinal Shards + 2 Y-Sequence Mountain Stones + 1 Lv1 Sequence Core |
| Proof Boundary Crystal Cluster | 4 Proof Boundary Ore Shards + 2 Compute Chips + 2 Proof Strata Stones |
| Proof Boundary Crystal | 4 Proof Boundary Crystal Clusters + 4 Ordinal Shards + 1 Lv1 Boundary Core |

Each tier supplies a Denxi Pickaxe, Denxi Sword, and Ordinal helmet, chestplate, leggings, and boots. Nine mineral materials compress into a storage block and can be unpacked again. Three Number Stones craft a Numeric Denxi Pickaxe or Sword: the stored level is the floor of their average, from 0 to 9. There is no numeric armor.

Ordinary mining speed is separate from **Denxi speed**, which applies to ordinal ores. Numeric tools progress from stone-like to netherite-like ordinary attributes; ordinal tools improve these attributes more gradually. Lower-tier tools can still mine higher-tier ordinal ores, but may be extremely slow. A level-0 numeric pickaxe has the same base Denxi speed as a diamond pickaxe; level 9 mines Omega Ore in about five seconds. Installed cores accelerate ordinal-ore mining exponentially without giving that exponential speed to ordinary stone.

## Enhancement tables

Place equipment in the upper-left slot, then insert or remove cores directly in its open sockets. Shift-click transfers are supported. Compatible cores in your inventory are highlighted. The right panel shows equipment statistics and **enhancement points**; these points are not themselves final damage, mining-block counts, or experience percentages.

| Table | Regional cores | Ordinal Crystals |
|---|---|---|
| Basic | Lv1 | Lv1–2 |
| Advanced | Lv1–2 | Lv1–3 |
| Ultimate | Lv1–3 | Lv1–4 |

Equipment also limits the enhancement grade: Omega/Epsilon Zero allow grade 1, Gamma Zero/Psi allow grade 2, and Mountain Strata/Proof Boundary allow grade 3. The six tiers have **2, 4, 4, 6, 6, and 10** sockets respectively; numeric tools have one. The lower of the table's and equipment's limits applies. Ordinal Crystals Lv1 and Lv2 both count as grade 1 for this limit.

| Core | Compatible equipment | Effect |
|---|---|---|
| Sequence Core | Pickaxe | Area mining; sneak to mine a single block |
| Power Tower Core | Sword | More attack damage |
| Hydra Bud | Pickaxe, sword, armor | More durability capacity |
| Empty Set Core | Pickaxe, sword | Longer reach |
| Laver Core | Pickaxe, sword | More experience gained |
| Criticality Core | Sword | Blast splash against nearby targets, without terrain destruction |
| Boundary Core | Armor | More ordinal defense |
| Heart of Guogao | Armor | More maximum health |
| Ordinal Crystal | Pickaxe, sword, armor | Improves the equipment's primary attribute |

Copies of the same core add their effects. Regional Lv1/2/3 cores contribute 1/1.5/2 enhancement points; Ordinal Crystal Lv1/2/3/4 contributes 0.5/1/1.5/2 points. Denxi speed uses a separate effective level sum: a regional core contributes its level, and an Ordinal Crystal contributes its level minus one. Each effective level multiplies ordinal-ore mining speed by four.

Ordinal armor already provides extra damage reduction without cores. Defense from the four pieces is added before the reduction is calculated. It covers attacks, falls, fire, and lava, but not the void. Installing or removing cores preserves the proportion of durability already lost. When equipment breaks, its installed cores drop intact.

## Core fusion and splitting

The eight regional core families each have Lv1–3; Ordinal Crystals also have Lv4. Fusion recipes are shapeless crafting-table recipes:

| Output | Ingredients |
|---|---|
| Lv2 Sequence / Hydra / Laver / Criticality / Boundary / Guogao core | 8 Lv1 cores of that family + 1 Lv2 Ordinal Crystal |
| Lv2 Power Tower Core | 5 Lv1 Power Tower Cores + 1 each of the red, green, and blue TREE nodes + 1 Lv2 Ordinal Crystal |
| Lv2 Empty Set Core | 2 each of the Lv1 Empty Set, fffZ, and FOS materials + 1 Absent Hydra Psi + 1 Absent Hydra Z + 1 Lv2 Ordinal Crystal |
| Any regional Lv3 core | 8 Lv2 cores of that family + 1 Lv3 Ordinal Crystal |
| Lv2/3/4 Ordinal Crystal | 9 Ordinal Crystals of the preceding level |

Higher Ordinal Crystals split back into nine of the preceding level; Lv1 splits into four shards. Regional cores do not have reverse recipes to lower levels. Natural scenery mostly contains Lv1; the final chamber of the Guogao Finality Tree also holds eight regional Lv2 treasures. Higher-level Laver Cores cannot replace a table's Lv1 core, and higher-level Boundary Cores cannot replace tape endpoints.

## Landscape interactions

**Laver tables** are detected from blocks at their actual location and require a Laver Core. They do not depend on the world seed. Empty cells are silent; incomplete patterns can still play safely. Right-click to start or stop.

**Natural LHO materials**—Empty Set, fffZ, FOS, and ψ/Z branches—need an Ordinal Crystal anchor: hold a crystal in your offhand or place one within a 5×5×5 neighborhood centered on the material. Use a pickaxe to collect natural anchored material. Once placed by a player, it stays stable and can be collected with any tool.

**Turing tapes** use built-in program #14263231. Right-click a cell to toggle its content; a rising redstone signal starts it. Exactly two of its six face neighbors must be tape blocks or Lv1 Boundary Cores. Right-angle and vertical turns are supported. Cores are endpoints and are never read or written. Breaks, branches, endpoints, or unloaded neighbors stop the head; the tape does not extend itself.

The creative inventory has seven Guogaology tabs: Terrain (plants and terrain blocks), Building (architecture and decoration), Numbers (digits and sequences), Lights (lamps and Guogao), Cores, Equipment (including enhancement tables), and Materials (including ores).
