# Survival guide · 0.3.12

[简体中文](SURVIVAL-GUIDE.md) | English

**Outer Googology supplies minerals and basic equipment; Inner Googology and the Guogao Underworld supply crystal cores.** The Outer world retains seven ordinary donor creatures. Hydra creatures and bosses are excluded; giant Hydra plants remain in the Inner garden.

The Outer LHO Edge is confined to a narrow coast around the void. Inner LHO islands and neighboring land taper into a real air gap. Terrain changes affect newly generated chunks only. Gummies on Outer Laver tables may mix plain and embossed styles, with an 80% chance of plain per gummy. All four plain colors can be harvested, sliced, and used in the existing portal rituals.

## Travel and local return materials

Every ring has twelve blocks in a 5×5 outline without corners and an empty 3×3 center.

| From | Offering | To |
|---|---|---|
| Overworld, whole-cake/Guogao ring | Apple | Outer |
| Outer, 12 mineral storage blocks (ω/ε/Γ/Ω, any mixture) | One Ω mineral | Inner |
| Inner, 12 Guogao blocks of any color | One Guogao block | Underworld |
| Outer, 12 Outer Return Frames | One Return Token | Overworld |
| Inner, 12 Inner Return Frames | One Return Token | Outer |
| Underworld, 12 Dread Forest Return Frames | One Return Token | Inner |

Return frames have fixed recipes: 4 cobblestone + 1 plank for Outer, 4 Ordinal Shards + 1 plank for Inner, and 4 Guogao Dreadsoil + 1 Dread Fir Plank for Underworld. Each recipe yields one frame, so craft twelve times for one ring. Use any planks where unspecified. The three frame types cannot be mixed. One plank above one stick crafts one Return Token, and activation consumes one token. All materials are locally obtainable; Ordinal Crystals drop shards when broken by hand. No mineral, intact core or carried apple is required. Ordinary terrain blocks and wood no longer qualify as return frames or offerings. Outer-to-Inner specifically requires the nine-mineral storage blocks, not ores, and an Ω offering. Normal arrivals also create a reverse gate. Breaking a frame disables the entire gate.

Destination styles are ivory/gold Omega for surface/home, indigo matrix-and-mountain for Inner, and dark jade Guogao for Underworld. The portal surface sits approximately 0.3 blocks above its cell's bottom.

Horizontal scales are Overworld:Outer:Inner:Underworld = 1:1:4:16. Falling to Y≤−1000 in Outer leads to Inner Y=500; void damage remains active during the descent, so insufficient protection can be fatal. Falling to Y≤−120 in Inner leads to Underworld Y=500; quarter gravity and no fall damage apply there. New destinations still require chunk generation. Approaching a portal only prepares chunks. Entering first reuses a nearby complete, safe portal, then searches for safe land, and finally tries a small platform built only in air. Loading and search have time and per-tick limits. An unsuccessful trip is silently cancelled and the player is returned outside the source gate; leaving allows another attempt. Containers and structures are not overwritten. `/googology` shows help; administrators have `visit`, `inner`, `guogao`, and `return`.

## Four mineral tiers

Only Outer naturally generates ω, ε, Γ and Ω ores, including understone variants in its underground biome. The two deeper dimensions no longer generate ores. All four drop usable minerals directly, support Fortune and Silk Touch, and can be mined with an ordinary iron pickaxe. There is no core-dependent processing or exponential ore-mining gate.

Each tier crafts a pickaxe, sword, bow, four armor pieces, a Manuscript, and a reversible nine-mineral storage block. Manuscript passive effects work in either hand, with offhand priority when both hands hold one. Names omit “Stone”, for example ω Pickaxe or Ω Chestplate. Each Manuscript has a distinct scroll/folio icon. Epsilon mineral art uses ε₀ to distinguish rotated symbols; its name remains ε. Number pickaxes/swords remain: three Number Stones give the floor of their average digit, with ordinary stone-to-netherite stats at levels 0–9.

| Tier | Mining | Pick attack | Sword attack | Gear slots | Manuscript slots | Regional core limit |
|---|---:|---:|---:|---:|---:|---:|
| ω | 9 | 6 | 8 | 2 | 2 | Lv1 |
| ε | 11 | 8 | 10 | 4 | 3 | Lv2 |
| Γ | 14 | 10 | 12 | 6 | 4 | Lv2 |
| Ω | 18 | 12 | 16 | 8 | 6 | Lv3 |

Numeric pickaxe harvesting tiers are stone for digits 0–4, iron for 5–7, diamond for 8, and netherite for 9. Ordinary mining speed and attack still progress with the digit; mining stone does not bypass an ore's harvesting requirement.

Native full-set armor is 16/18/20/22 and toughness 4/8/12/16. Bare armor has no extra custom damage multiplier. Each slot uses its weighted share of the full values.

## Enhancement and Manuscripts

Enhancement tables have reversible sockets and highlight eligible cores in the inventory. Basic/Advanced/Ultimate tables accept core grades1/2/3; the ungraded Ordinal Crystal is also accepted. Ordinal Cores fit manuscripts only. Mineral equipment has2/4/6/8 sockets; manuscripts have2/3/4/6, with grade caps1/2/2/3. Removing cores preserves wear; broken equipment releases installed cores.

Manuscripts have no durability and open their own socket menu by right-clicking. They work in either hand; **the offhand takes priority when both hands hold manuscripts**. Ordinal Crystals and Ordinal Cores are manuscript-only, and Criticality cannot enter manuscripts. Ordinal equipment, bows and manuscripts reject enchantments; numeric and vanilla tools remain enchantable. Normal realms are Outer and the vanilla dimensions; deep realms are Inner and Underworld.

| Core | Equipment | Manuscript |
|---|---|---|
| Sequence | Pick mines 1/2/3 extra blocks, or 1/4/9 deep; root-sum-squares, rounded down | Equivalent Efficiency II/IV/VI, highest copy; max with native Efficiency |
| Power Tower | Sword/bow attack | Attack, calculated separately from the weapon |
| Hydra Bud | Fortune II/IV/VI or switchable Silk Touch from Lv1; Looting II/IV/VI; bow infinity/multishot/piercing | Reach +1/2/3 blocks, root-sum-squares |
| Empty Set | Wear protection 4/16/64, summed | Ordinary targeting range 30% / no proactive targeting / no ordinary retaliation; Lv2+ night vision |
| Laver Condensation | Helmet oxygen consumption 1/4, 1/16, zero; Aqua Affinity; boots Depth Strider I/II/III | Heal 1/2/3 HP per 4 seconds, root-sum-squares; food floor 10 at Lv2, 19 at Lv3 |
| Criticality | Sword/bow burst within 3 blocks including the target; 15%/22.5%/30%, root-sum-squares | Incompatible |
| Boundary | Armor protection | Protection; Lv1 jump/fall protection, Lv2 slow flight, Lv3 creative-style flight; Lv2+ fire resistance |
| Heart of Guogao | Sword/bow harmful effects, highest copy | Health +4/8/12 HP, root-sum-squares; immunity; Lv2+ real inventory Totem rescue |
| Ordinal Core (including the base crystal) | Incompatible | Attack, protection, multiplies Laver healing, extends Guogao effects; no mining/wear/reach/health bonuses |

Identical attack/protection cores within an item use root-sum-squares in both realm types. Each armor piece calculates its Boundary amount independently, then head/chest/legs/feet combine with weights 20%/40%/25%/15%. Native armor and toughness apply first; custom protection then divides damage, including fall, fire, sonic and void damage. Bare armor has no extra custom multiplier.

Normal attack is weapon base + bare-manuscript 0.5/1/1.5/2 HP + weapon Tower + manuscript Tower + manuscript Ordinal. Tower grades add 1/2/3 HP; Ordinal adds 0.5/1/1.5/2.5 HP. Deep attack keeps the same weapon base and multiplies independent bare-book, book-Ordinal, book-Tower and weapon-Tower factors. Bare book percentages are 5%/10%/15%/20%; Tower is 25%/50%/100%; Ordinal is 5%/10%/20%/40%. Same-source copies still use root-sum-squares.

Let A be armor's weighted Boundary amount, Q the manuscript Boundary amount, U its Ordinal amount, and b the bare manuscript amount. Protection is `(1+2A)*(1+b+Q+U)` normally, or `(1+2A)*(1+b)*(1+Q)*(1+U)` deep. Damage is divided by this factor. Original armor wear points are independently consumed with probability `1/(protection * Empty-Set-wear-factor)`.

Healing is `sqrt(sum(level²))*(1+bare-book-healing+Ordinal-healing)` HP per 4 seconds, requiring Laver. Bare-book healing is 10%/20%/30%/40%; Ordinal is 10%/20%/30%/50%, combined by root-sum-squares. Guogao health comes only from the active manuscript, identically in both realms. Lv2 prevents Poison/Hunger/Weakness; Lv3 additionally prevents Nausea/Slowness/Wither/Blindness/Darkness. Lv2+ rescue consumes a real inventory Totem; no free revivals.

Guogao weapon effects last two seconds before bonuses: Lv1 Slowness I/Poison II; Lv2 Slowness II/Wither II/Weakness I; Lv3 Slowness III/Wither III/Weakness II/Nausea, plus 20% Blindness. Bare manuscript duration bonuses are 25%/50%/75%/100%; Ordinal 25%/50%/75%/125%, root-sum-squares.

Boundary Lv1 adds one jump-height block per copy and multiplies fall damage by 0.25 once. Higher grades retain Lv1 jump bonuses. Lv2 grants slow flight/fall immunity; Lv3 overrides it, adding collision immunity. Deep sprint flight multiplies both axes by eight relative to ordinary non-sprinting creative flight. Double-jump toggles flight; book removal permits a safe landing without overriding creative/spectator or external permissions.

Empty Set affects ordinary hostile proactive/revenge goals only: appearance and existing targets stay unchanged; special anger/brain mechanics are untouched. Branch yield copies use max; applicable native Fortune/Looting adds to it. The table's Silk Touch switch replaces Fortune. Extra excavation respects tool and region permissions, excludes containers/relics, and stops while sneaking.

Core inventory icons are 64×64 native-pixel perspective sketches of each family's actual first-grade form. Family colors and silhouettes remain identical across grades; top-right numerals 1/2/3 identify grades. The ungraded Ordinal Crystal keeps its original item model and white name. All nine core families use yellow/aqua/purple rarity names for Lv1/2/3. World models remain three-dimensional, animated within 64 blocks, cached static beyond that, and approximated statically by Voxy.

[Enhancement simulator](enhancement-simulator.html?lang=en) · [Core effects](core-effects-reference.html?lang=en) · [Numeric audit](enhancement-numeric-audit.html?lang=en)

## Ordinal bows and sanctuary rewards

Four bows use the vanilla bow layout with three matching minerals replacing sticks, plus three strings. Launch-speed multipliers are 1.1/1.2/1.3/1.5; durability is 768/1152/1536/2304. Native draw time, actual-velocity damage, full-draw random critical bonus, gravity and drag remain. There is no invented tier-specific fixed HP base. Normal core HP bonuses scale with charge; deep factors are snapshotted at release.

Hydra Bud Lv1 provides Infinity in Survival, requiring one ordinary arrow; Lv2 fires three arrows without duplicate direct damage on one target; Lv3 allows each arrow to hit at most two creatures, passing through the first to hit the second. Special ammunition costs one per volley and only the center arrow retains its effects. Critical bursts include the direct target without destroying terrain or harming allies; a target takes at most one direct hit plus one burst per volley.

Every main chest in the eight giant sanctuaries guarantees one matching regional Lv3 core and one Ordinal Core Lv2, plus generous higher-quality thematic goods. Secondary chests contain 6–12 matching Lv1 cores, 30–60 Ordinal Crystals, and fewer lower-grade thematic goods. Harvestable core displays remain.

## Core fusion and existing interactions

All nine core families display Lv1–3; the natural Ordinal Crystal is ungraded. Old Ordinal Crystal Lv2–4 is renamed Ordinal Core Lv1–3 without changing effects or ingredient counts. Regional Lv2 costs eight Lv1 plus one Ordinal Core Lv1, except Power Tower (five tower + one of each TREE color + Ordinal Core Lv1) and Empty Set (two each Empty Set/fffZ/FOS, one ψ, one Z, Ordinal Core Lv1). Regional Lv3 costs eight matching Lv2 plus Ordinal Core Lv2. Four shards ↔ one Ordinal Crystal; nine crystals ↔ one Ordinal Core Lv1; nine cores of the preceding grade ↔ Lv2 or Lv3. Ordinal fusion is reversible. Base-crystal harvesting follows glowstone's 2–4 shard/Fortune/Silk Touch behavior.

Player-placed regional and Ordinal Cores can be recovered with any tool; the base Ordinal Crystal still drops shards. Natural cores remain restricted to Inner and Underworld. Donor ordinary surface tables keep their original wool patterns; the Inner iBLP tables use the shared Laver Condensation Core and live pattern playback. Numbers, laver wood, Guogao sweets and lamps use shared items. Turing tape, anchored LHO fragments, giant landmarks and the final chamber remain available.

Right-click Matrix Number Bricks to cycle 0–15, or colored number lamps to cycle 0–32. A blank lamp changes to 0 on its first click. Number Stones do not cycle. Mined items preserve their edited number and color. The three portal designs follow their actual destination, including return trips.

Christmas-tree scenery uses matching vanilla woods; the two custom Christmas carpentry families are removed. Loquat and Underworld Loquat carpentry also use vanilla oak counterparts; Loquat foliage, saplings and fruit drops remain. Small Outer Christmas trees keep their height and layered conical crowns, using vanilla spruce logs and their dedicated original Christmas leaves. Dread Fir logs produce their own planks, with stairs, slabs, doors, trapdoors, fences, gates, buttons, pressure plates, signs and boats. Dread Fir products are nonflammable and do not convert into spruce; other woods burn normally. Numeric Stone Pickaxes/Swords keep their existing digit levels and attributes.

A server rack costs four Compute Chips, four iron ingots and one redstone dust. A terminal costs two chips, three glass, two iron ingots and two redstone dust. Each recipe produces one block; harvesting yields the block itself, and unpacking either block returns two chips.

See the [current gameplay rules](MERGE-REVIEW-CHECKLIST.en.md) and [import architecture](OUTER-IMPORT.md).
