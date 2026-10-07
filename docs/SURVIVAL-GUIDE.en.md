# Survival guide · 0.4.2

[简体中文](SURVIVAL-GUIDE.md) | English

**Outer Guogaology supplies minerals and basic equipment; Inner Guogaology and the Guogao Underworld supply crystal cores.** The Outer world retains seven ordinary donor creatures. Hydra creatures and bosses are excluded; giant Hydra plants remain in the Inner garden.

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

Activated frames have hardness 200, four times obsidian. They can be mined, but never drop items with any tool, including Silk Touch; breaking one still collapses the entire gate. Unactivated construction materials and return frames retain their original hardness and drops.

Horizontal scales are Overworld:Outer:Inner:Underworld = 1:1:4:16. Falling to Y≤−500 in Outer leads to Inner Y=500; void damage remains active during the descent, so insufficient protection can be fatal. Falling to Y≤−120 in Inner leads to Underworld Y=500; quarter gravity and no fall damage apply there. New destinations still require chunk generation. Approaching a portal only prepares chunks. Entering first reuses a nearby complete, safe portal, then scans real ground. Ordinary replaceable vegetation and thin snow can be cleared, and short foundations can fill height differences of up to three blocks. When complete ground is unavailable, a connected ledge or near-water platform takes priority over a void platform. Exits are no longer deliberately raised into the sky; existing safe gates are not relocated. Loading and search have time and per-tick limits. An unsuccessful trip is silently cancelled and the player is returned outside the source gate; leaving allows another attempt. Containers and structures are not overwritten.

Everyone can use `/guogaology` or `/guogaology help`. The following commands require administrator permission level 2:

- `/guogaology tp <outer|inner|underworld|overworld>` goes directly to the named dimension; using it in that dimension only displays a message.
- `/guogaology up` returns one layer: Underworld → Inner → Outer → Overworld. It does not return to a previous position, bed or spawn point.
- `/guogaology kit portal <outer|inner|underworld>` provides the forward portal frame and activation materials for the named destination.
- `/guogaology kit return` provides 12 Return Frames for the current layer and one Return Token.

## Four mineral tiers

From 0.4.1, vein attempts per chunk are 24/12/6/2 for ω/ε/Γ/Ω, and exposed ore candidates have a 50% air-discard chance. Γ and Ω favor the bottom: Γ height candidates span approximately Y=−64…55, with very few at Y≥50; Ω spans approximately −64…15. Veins can extend around their starting point and still require suitable host stone and biomes. These changes affect newly generated chunks only.

Only Outer naturally generates ω, ε, Γ and Ω ores, including understone variants in its underground biome. The two deeper dimensions no longer generate ores. All four drop usable minerals directly, support Fortune and Silk Touch, and can be mined with an ordinary iron pickaxe. There is no core-dependent processing or exponential ore-mining gate.

Each tier crafts a pickaxe, sword, bow, four armor pieces, a Manuscript, and a reversible nine-mineral storage block. Manuscript passive effects work in either hand, with offhand priority when both hands hold one. Names omit “Stone”, for example ω Pickaxe or Ω Chestplate. Each Manuscript has a distinct scroll/folio icon. Epsilon mineral art uses ε₀ to distinguish rotated symbols; its name remains ε. Number pickaxes/swords remain: three Number Stones give the floor of their average digit, with ordinary stone-to-netherite stats at levels 0–9.

| Tier | Mining | Pick attack | Sword attack | Gear slots | Manuscript slots |
|---|---:|---:|---:|---:|---:|
| ω | 9 | 6 | 8 | 2 | 2 |
| ε | 11 | 8 | 10 | 4 | 3 |
| Γ | 14 | 10 | 12 | 6 | 4 |
| Ω | 18 | 12 | 16 | 8 | 6 |

Numeric pickaxe harvesting tiers are stone for digits 0–4, iron for 5–7, diamond for 8, and netherite for 9. Ordinary mining speed and attack still progress with the digit; mining stone does not bypass an ore's harvesting requirement.

Native full-set armor is 16/18/20/22 and toughness 4/8/12/16. Bare armor has no extra custom damage multiplier. Each slot uses its weighted share of the full values.

## Enhancement and Manuscripts

Unavailable items are dimmed in the enhancement interface: an empty table emphasizes equipment; an occupied table or manuscript emphasizes currently insertable materials. Compatibility, grade, capacity and Totem rules all apply. “Effect preview” describes only the edited item, with +HP for normal-realm attack and ×factors in deep realms. Armor protection contributions already include slot weights and add across pieces; the manuscript shows its separate protection factor.


Enhancement tables have reversible sockets and highlight eligible cores in the inventory. Basic/Advanced/Ultimate tables accept core grades1/2/3; the ungraded Ordinal Crystal is also accepted. Ordinal Cores fit manuscripts only. Mineral equipment has2/4/6/8 sockets; manuscripts have2/3/4/6, all four tiers accepting core grades1–3. Removing cores preserves wear; broken equipment releases installed cores.

Manuscripts have no durability. With one in your offhand, open the inventory and click **Book** to edit its sockets; a main-hand manuscript can also be right-clicked. Offhand manuscripts do not intercept ordinary right-click actions. Passive effects keep their pre-edit values until the menu closes; the right panel previews the edited setup. They work in either hand; **the offhand takes priority when both hands hold manuscripts**. Ordinal Crystals and Ordinal Cores are manuscript-only, and Criticality cannot enter manuscripts. Ordinal equipment, bows and manuscripts reject enchantments; numeric and vanilla tools remain enchantable. Normal realms are Outer and the vanilla dimensions; deep realms are Inner and Underworld.

| Core | Equipment | Manuscript |
|---|---|---|
| Sequence | Pick mines 1/2/3 extra blocks, or 1/4/9 deep; root-sum-squares, rounded down | Flat mining +4/8/16 and Haste-bucket +25/50/100%; each aggregates by root-sum-squares |
| Power Tower | Sword/bow attack | Attack, calculated separately from the weapon |
| Hydra Bud | Fortune II/IV/VI or switchable Silk Touch from Lv1; Looting II/IV/VI; bow infinity/multishot/piercing | Reach +1/2/3 blocks, root-sum-squares |
| Empty Set | Wear protection 4/16/64, summed | Ordinary targeting range 30% / no proactive targeting / no ordinary retaliation; Lv2+ night vision; no durability effect |
| Laver Condensation | Helmet oxygen consumption 1/4, 1/16, zero; Aqua Affinity; boots Depth Strider I/II/III | Heal 1/2/3 HP per 4 seconds, root-sum-squares; food floor 10 at Lv2, 19 at Lv3 |
| Criticality | Sword/bow burst within 3 blocks including the target; 15%/22.5%/30%, root-sum-squares | Incompatible |
| Boundary | Armor protection | Protection; Lv1 jump/fall protection, Lv2 slow flight, Lv3 free flight; Lv2+ fire resistance |
| Heart of Guogao | Sword/bow harmful effects, highest copy | Health +4/8/12 HP, root-sum-squares; immunity; Lv2+ real inventory Totem rescue |
| Ordinal Core (including the base crystal) | Incompatible | Attack, protection, multiplies Laver healing, extends Guogao effects; no mining/wear/reach/health bonuses |

Identical attack/protection cores within an item use root-sum-squares in both realm types. Each armor piece calculates its Boundary amount independently, then head/chest/legs/feet combine with weights 20%/40%/25%/15%. Native armor and toughness apply first; custom protection then divides damage, including fall, fire, sonic and void damage. Bare armor has no extra custom multiplier.

All manuscript tiers also accept vanilla Totems of Undying, one per socket. They activate when the editing screen closes and consume one real item on a lethal hit; they add no core stats. Active Guogao Lv2/3 uses inventory totems first, then socketed ones. A newly inserted totem is inactive until closing. A previously active totem moved to the cursor or inventory can still be consumed during editing; closing or dying never restores a spent item. Empty Set in a manuscript does not protect the durability of tools, weapons or armor.

Normal attack is weapon base + bare-manuscript 0.5/1/1.5/2 HP + weapon Tower + manuscript Tower + manuscript Ordinal. Tower grades add 1/2/3 HP; Ordinal adds 0.5/1/1.5/2.5 HP. Deep attack keeps the same weapon base and multiplies independent bare-book, book-Ordinal, book-Tower and weapon-Tower factors. Bare book percentages are 5%/10%/15%/20%; Tower is 25%/50%/100%; Ordinal is 5%/10%/20%/40%. Same-source copies still use root-sum-squares.

Let A be armor's weighted Boundary amount, Q the manuscript Boundary amount, U its Ordinal amount, and b the bare manuscript amount. Protection is `(1+2A)*(1+b+Q+U)` normally, or `(1+2A)*(1+b)*(1+Q)*(1+U)` deep. Damage is divided by this factor. Original armor wear points are independently consumed with probability `1/(protection * Empty-Set-wear-factor)`.

Healing is `sqrt(sum(level²))*(1+bare-book-healing+Ordinal-healing)` HP per 4 seconds, requiring Laver. Bare-book healing is 10%/20%/30%/40%; Ordinal is 10%/20%/30%/50%, combined by root-sum-squares. Guogao health comes only from the active manuscript, identically in both realms. Lv2 prevents Poison/Hunger/Weakness; Lv3 additionally prevents Nausea/Slowness/Wither/Blindness/Darkness. Lv2+ rescue consumes a real inventory Totem; no free revivals.

Guogao weapon effects last two seconds before bonuses: Lv1 Slowness I/Poison II; Lv2 Slowness II/Wither II/Weakness I; Lv3 Slowness III/Wither III/Weakness II/Nausea, plus 20% Blindness. Bare manuscript duration bonuses are 25%/50%/75%/100%; Ordinal 25%/50%/75%/125%, root-sum-squares.

Boundary Lv1 adds one jump-height block per copy. Having Lv1 halves fall and elytra-impact damage and doubles safe fall distance once; these bonuses coexist with higher grades. Every Lv1/2/3 core additionally grants 50% walking/running speed, combined by root-sum-square, without changing flight speed. The Inner realm separately halves fall/elytra damage and doubles safe fall distance, so Inner Lv1 means 25% damage and 4× safe distance. The Outer realm and Underworld do not receive this environmental bonus.

Lv2 grants slow flight and fall immunity, with no sprint boost. Both horizontal and vertical speeds are one sixth of Lv3's non-sprinting base speeds in ordinary realms, including the Outer realm, and one third in deep realms (Inner and Underworld). Lv3 grants free flight and collision immunity. The sprint key multiplies both horizontal and vertical speeds by two in ordinary realms and by four in deep realms. Manuscript flight uses its own speeds, independent of creative-flight speed controls. Double-jump toggles flight, jump ascends, and sneak descends; the Lv3 sprint key also works when strafing or moving only vertically. Book removal permits a safe landing without overriding creative/spectator or external permissions.

Empty Set affects ordinary hostile proactive/revenge goals only: appearance and existing targets stay unchanged; special anger/brain mechanics are untouched. Branch yield copies use max; applicable native Fortune/Looting adds to it. The table's Silk Touch switch replaces Fortune. Extra excavation selects eligible blocks nearest first in a 9×9 plane perpendicular to the dominant viewing axis, never below the manually broken block's Y. It respects tool and region permissions, excludes containers/relics, and stops while sneaking.

Core inventory icons are 64×64 native-pixel perspective sketches of each family's actual first-grade form. Family colors and silhouettes remain identical across grades; top-right numerals 1/2/3 identify grades. The ungraded Ordinal Crystal keeps its original item model and white name. All nine core families use yellow/aqua/purple rarity names for Lv1/2/3. World models remain three-dimensional, animated within 64 blocks, cached static beyond that, and approximated statically by Voxy.

[Enhancement simulator](enhancement-simulator.html?lang=en) · [Core effects](core-effects-reference.html?lang=en) · [Numeric audit](enhancement-numeric-audit.html?lang=en)

## Ordinal bows and sanctuary rewards

Four bows use the vanilla bow layout with three matching minerals replacing sticks, plus three strings. Launch-speed multipliers are 1.1/1.2/1.3/1.5; durability is 768/1152/1536/2304. Native draw time, actual-velocity damage, full-draw random critical bonus, gravity and drag remain. There is no invented tier-specific fixed HP base. Normal core HP bonuses scale with charge; deep factors are snapshotted at release.

Hydra Bud Lv1 provides Infinity in Survival, requiring one ordinary arrow; Lv2 fires three arrows without duplicate direct damage on one target; Lv3 allows each arrow to hit at most two creatures, passing through the first to hit the second. Special ammunition costs one per volley and only the center arrow retains its effects. Critical bursts include the direct target without destroying terrain or harming allies; a target takes at most one direct hit plus one burst per volley.

Every main chest in the eight giant sanctuaries guarantees two matching regional Lv2 cores and two Ordinal Core Lv1, plus thematic goods. Secondary chests contain2–3matching Lv1 cores,8–15Ordinal Crystals, and fewer lower-grade thematic goods. Interior ordinary displays and treasure ensembles are designed separately, with valuables at fixed recognizable installations.

## Core fusion and existing interactions

All nine core families display Lv1–3; the natural Ordinal Crystal is ungraded. Old Ordinal Crystal Lv2–4 is renamed Ordinal Core Lv1–3 without changing effects or ingredient counts. Regional Lv2 costs eight Lv1 plus one Ordinal Core Lv1, except Power Tower (five tower + one of each TREE color + Ordinal Core Lv1) and Empty Set (two each Empty Set/fffZ/FOS, one ψ, one Z, Ordinal Core Lv1). Regional Lv3 costs eight matching Lv2 plus Ordinal Core Lv2. Four shards ↔ one Ordinal Crystal; nine crystals ↔ one Ordinal Core Lv1; nine cores of the preceding grade ↔ Lv2 or Lv3. Ordinal fusion is reversible. Base-crystal harvesting follows glowstone's 2–4 shard/Fortune/Silk Touch behavior.

Player-placed regional and Ordinal Cores can be recovered with any tool; the base Ordinal Crystal still drops shards. Natural cores remain restricted to Inner and Underworld. Donor ordinary surface tables keep their original wool patterns; the Inner iBLP tables use the shared Laver Condensation Core and live pattern playback. Numbers, laver wood, Guogao sweets and lamps use shared items. The Outer world's furniture includes the Shock-Slump Chair. Turing tape, anchored LHO fragments, giant landmarks and the final chamber remain available.

Right-click Matrix Number Bricks to cycle 0–15, or colored number lamps to cycle 0–32. Blank lamps are separate blocks and remain blank when clicked. Number Stones do not cycle. Mined items preserve their edited number and color. The three portal designs follow their actual destination, including return trips.

Christmas-tree scenery uses matching vanilla woods; the two custom Christmas carpentry families are removed. Loquat and Underworld Loquat carpentry also use vanilla oak counterparts; Loquat foliage, saplings and fruit drops remain. Small Outer Christmas trees keep their height and layered conical crowns, using vanilla spruce logs and their dedicated original Christmas leaves. Dread Fir logs produce their own planks, with stairs, slabs, doors, trapdoors, fences, gates, buttons, pressure plates, signs and boats. Dread Fir products are nonflammable and do not convert into spruce; other woods burn normally. Numeric Stone Pickaxes/Swords keep their existing digit levels and attributes.

A server rack costs four Compute Chips, four iron ingots and one redstone dust. A terminal costs two chips, three glass, two iron ingots and two redstone dust. Each recipe produces one block; harvesting yields the block itself, and unpacking either block returns two chips.

See the [current gameplay rules](MERGE-REVIEW-CHECKLIST.en.md) and [import architecture](OUTER-IMPORT.md).

## 0.4.3 mining and landmark update

Mining speed is `(native tool + applicable native Efficiency + RSS of Sequence flat bonuses) × (1 + bare manuscript10/20/30/40% + RSS of Sequence percentage bonuses + native Haste)`, followed by native fatigue, water and airborne penalties. Book bonuses also apply to empty hands and unsuitable tools. Native Efficiency retains its original raw-speed>1 condition. Both realms use the same formula.

All displayed core grades1/2/3 have hardness25/50/100, harvestable by hand or any tool; picks are faster. The ungraded Ordinal Crystal has hardness2 and unchanged glowstone-style loot. Natural LHO anchoring conditions still apply.

Landmark interiors are redesigned as distinct ordinary displays and recognizable treasure ensembles, retaining similar visual density. Single-family interiors contain12 local grade1 cores; Power and LHO interiors contain20 total mixed-family materials; each has40 natural Ordinal Crystals. Underworld exterior lamp treasures and the final collection stay unchanged. The Power Pagoda arena and main chest occupy its genuine highest floor. Main chests give2local grade2 and2OrdinalCore grade1; side chests2–3local grade1 and8–15natural crystals. Theme products are approximately quartered. Newly generated buildings use this arrangement; old buildings are not rebuilt.

Any of the ten Numbered Stones crafts one-way into one cobblestone. The basic enhancement table uses one ungraded Ordinal Crystal instead of the previous omega mineral; its other ingredients and the advanced/ultimate tables remain unchanged.

## 0.4.5 restored landmark treasures and lower density

Interior models and complete regional-core/Ordinal Crystal quantities return to their pre-0.4.3 design, superseding the 12/20/40 budgets above. The usual quota is 50 regional materials and 50 crystals; Power has 50 Tower cores and 30 of each TREE color. Additional original lighting, railing and functional-center treasures also return. Inner landmarks occur at about 1/km², Underworld landmarks at 0.5/km². Power's highest-floor arena, current chest loot and unrelated gameplay changes remain. These changes affect newly generated buildings only.

## 0.4.6 Underworld regions

The Guogao Underworld remains one dimension, with Guogao Dreadwood, Misaligned Escarpments and Silent Mire on the surface. The forest retains its large upright/inverted conifers and vines. The cliffs pair recognizable structures whose corresponding levels fail to line up. The mire has water at Y0 and sparse, bottom-rooted cone skeletons. Descending Chain Caverns inside mountains connect repeated, increasingly nested chamber motifs through downhill passages. These are finite explorable landscapes, not an infinite generation system.

The final giant tree still stands in natural large lakes, averaging roughly one landmark per2km² across the dimension. Existing landmark collections and loot policy remain. New terrain applies to newly generated chunks only.

Core inventory icons remain simplified; held, dropped and framed cores use their actual static3D shapes. Lv3 manuscript sprint flight now uses4x normal speed on both axes in Inner/Underworld realms, and2x outside.

## 0.4.9 High-cliff giant descent chains

Underworld cliffs carry giant diagonal chains from actual rims at Y250 or higher down to the valley floor. Misaligned Escarpments have higher natural terraces; existing lake and giant-tree site rules remain. Alternating hollow links interlock, with pointed ends suggesting a descending sequence of greater-than signs. Both ends anchor into rock; the first link may be partly embedded in the rim while the remaining body stays exposed. Neighboring chains, large trees, vines and paired tier structures keep clear of each complete chain. Underground Descending Chain Caverns remain. This scenery applies only to newly generated chunks; existing chains are not rebuilt.
