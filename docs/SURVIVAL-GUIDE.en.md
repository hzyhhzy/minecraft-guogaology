# Survival guide · 0.3.11

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

Return frames have fixed recipes: 4 cobblestone + 1 plank for Outer, 4 Ordinal Shards + 1 plank for Inner, and 4 Guogao Dreadsoil + 1 Dread Fir Plank for Underworld. Each recipe yields one frame, so craft twelve times for one ring. Use any planks where unspecified. The three frame types cannot be mixed. One plank above one stick crafts one Return Token, and activation consumes one token. All materials are locally obtainable; Lv1 Ordinal Crystals drop shards when broken by hand. No mineral, intact core or carried apple is required. Ordinary terrain blocks and wood no longer qualify as return frames or offerings. Outer-to-Inner specifically requires the nine-mineral storage blocks, not ores, and an Ω offering. Normal arrivals also create a reverse gate. Breaking a frame disables the entire gate.

Destination styles are ivory/gold Omega for surface/home, indigo matrix-and-mountain for Inner, and dark jade Guogao for Underworld. The portal surface sits approximately 0.3 blocks above its cell's bottom.

Horizontal scales are Overworld:Outer:Inner:Underworld = 1:1:4:16. Falling to Y≤−1000 in Outer leads to Inner Y=500; void damage remains active during the descent, so insufficient protection can be fatal. Falling to Y≤−120 in Inner leads to Underworld Y=500; quarter gravity and no fall damage apply there. New destinations still require chunk generation. Approaching a portal only prepares chunks. Entering first reuses a nearby complete, safe portal, then searches for safe land, and finally tries a small platform built only in air. Loading and search have time and per-tick limits. An unsuccessful trip is silently cancelled and the player is returned outside the source gate; leaving allows another attempt. Containers and structures are not overwritten. `/googology` shows help; administrators have `visit`, `inner`, `guogao`, and `return`.

## Four mineral tiers

Only Outer naturally generates ω, ε, Γ and Ω ores, including understone variants in its underground biome. The two deeper dimensions no longer generate ores. All four drop usable minerals directly, support Fortune and Silk Touch, and can be mined with an ordinary iron pickaxe. There is no core-dependent processing or exponential ore-mining gate.

Each tier crafts a pickaxe, sword, four armor pieces, a passive offhand Manuscript, and a reversible nine-mineral storage block. Names omit “Stone”, for example ω Pickaxe or Ω Chestplate. Each Manuscript has a distinct scroll/folio icon. Epsilon mineral art uses ε₀ to distinguish rotated symbols; its name remains ε. Number pickaxes/swords remain: three Number Stones give the floor of their average digit, with ordinary stone-to-netherite stats at levels 0–9.

| Tier | Mining | Pick attack | Sword attack | Gear slots | Manuscript slots | Regional core limit |
|---|---:|---:|---:|---:|---:|---:|
| ω | 9 | 6 | 8 | 2 | 2 | Lv1 |
| ε | 11 | 8 | 10 | 4 | 3 | Lv2 |
| Γ | 14 | 10 | 12 | 6 | 4 | Lv2 |
| Ω | 18 | 12 | 16 | 8 | 6 | Lv3 |

Numeric pickaxe harvesting tiers are stone for digits 0–4, iron for 5–7, diamond for 8, and netherite for 9. Ordinary mining speed and attack still progress with the digit; mining stone does not bypass an ore's harvesting requirement.

Unenhanced full armor provides approximately 2/2.5/3/4 times effective damage capacity through custom reduction. Attacks, fall damage and fire/lava are covered; void damage and Warden sonic booms are covered too. Final-tier base equipment is approximately twice the first tier.

## Enhancement and Manuscripts

Gear uses enhancement tables with reversible direct sockets and highlighted compatible inventory cores. Basic/Advanced/Ultimate tables accept regional Lv1/2/3 and ordinal Lv2/3/4, subject to the gear tier. The four mineral tiers have 2/4/6/8 gear sockets and 2/3/4/6 manuscript sockets. Removing cores preserves wear; broken gear releases installed cores.

Manuscripts have no durability. Right-click opens their own socket menu without a table; passive effects require the offhand. If both hands hold manuscripts, right-click edits the offhand one; the books do not stack. All four ordinal gear/manuscript tiers reject enchantments; numeric stone tools and vanilla gear remain enchantable. Tool and book effects are combined once. Normal realms are the Outer world and three vanilla dimensions; deep realms are Inner and Underworld.

| Core | Equipment | Offhand manuscript |
|---|---|---|
| Sequence | Fortune-style pick yield / Looting-style sword yield | Adds directly to tool yield levels |
| Power Tower | Sword attack +1/2/3 HP | Same attack increments, added to sword |
| Hydra Bud | Pick area mining; helmet breathing | Mining speed |
| Empty Set | Tool reach | Jump, fall protection and flight |
| Laver Condensation | Wear protection | Healing every 4 seconds; Lv2+ night vision |
| Criticality | Target and nearby 3-block impact burst | Projectile burst; Lv2+ fire resistance |
| Boundary | Combined armor protection | Same protection budget |
| Heart of Guogao | Sword slowness / weakness | Health, immunity and real inventory Totems |
| Ordinal Crystal | Small bonuses to several properties | Small bonuses; amplifies deep specialized increments |

Let l be a regional core level. Sequence levels from tool and book add directly, never multiply. Yield participates in native loot calculation; cores, storage blocks, containers and marked player-placed relics are excluded. Existing Fortune/Looting and core yield use the higher effective level, not two separate loot calculations.

Hydra Bud picks mine `floor(sqrt(sum(l²)))` extra blocks normally or `sum(l²)` in deep realms: a single core gives 1/2/3 or 1/4/9. Candidates are the 9×9 plane normal to the view, excluding displays, containers and unharvestable blocks. Sneaking disables area mining. Manuscript mining values are 1/1.5/2; multiple cores use root-sum-squares normally and the squared sum in deep realms. No core means 1×; vanilla tool speed, Haste, Fatigue, water and airborne penalties remain.

Laver wear protection is 16/64/256 per core, summed; Ordinal contributes a smaller universal benefit. Each original wear point is consumed with probability 1/protection in every realm. Maximum durability never increases and no automatic repair occurs. Manuscript healing is `sqrt(sum(l²))` HP every 4 seconds everywhere. Empty Set tool reach is `sqrt(sum(l²))` blocks. The highest Hydra helmet level gives 1/4, 1/16 or zero oxygen consumption.

An Empty Set Lv1 manuscript multiplies fall damage by 0.25 once and adds one jump-height block per core, coexisting with higher flight grades. Lv2 grants slow flight and fall immunity. Lv3 overrides Lv2 with creative-style flight and fall/collision immunity; deep sprint multiplies ordinary non-sprinting creative horizontal and vertical flight by eight. Double-jump toggles flight. Removing the book permits a safe landing and preserves vanilla creative/spectator permissions.

Criticality coefficients are 15%/22.5%/30%, combined by root-sum-squares. The main target also receives burst damage; terrain, allies and recursive bursts are excluded. Guogao swords apply Slowness I/II/III, plus Weakness I at Lv2 or II at Lv3. A single core lasts three seconds; multiple cores extend this to at most eight.

Guogao manuscript health is `sqrt(sum((4l)²))` HP normally and `sum(10l)` in deep realms. Lv2 prevents poison, hunger, nausea, weakness and slowness; Lv3 also prevents wither, blindness and darkness. Lv2+ rescue consumes one real inventory Totem; it does not create free revivals.

Boundary protection combines head/chest/legs/feet shares of 20%/40%/25%/15% once. Bare full mineral sets take 1/2, 1/2.5, 1/3 or 1/4 damage. Q sums armor-share-weighted squared core levels and manuscript squared levels; the capacity gains `1+0.25sqrt(Q)` normally or `1+0.25Q` deep. Fire, falls, sonic booms and void damage are covered, together with applicable vanilla mitigation layers.

Ordinal universal values are 0.06/0.12/0.25/0.5. Normal copies use root-sum-squares and add small bonuses; deep copies add and amplify specialized increments. Reach, healing and wear stay consistent across realms. The offline [enhancement simulator](enhancement-simulator.html) calculates individual gear and complete player loadouts.

Core animation remains within 64 blocks, with cached static interiors farther away in normally rendered chunks and a static approximation for Voxy.

## Core fusion and existing interactions

Eight regional families have Lv1–3; Ordinal Crystal has Lv1–4. Regional Lv2 costs eight Lv1 plus one ordinal Lv2, except Power Tower (five tower + one of each TREE color + ordinal Lv2) and Empty Set (two each Empty Set/fffZ/FOS, one ψ, one Z, ordinal Lv2). Regional Lv3 costs eight matching Lv2 plus ordinal Lv3. Ordinal tiers fuse/split nine-to-one, with four shards ↔ Lv1. Lv1 mining follows glowstone's 2–4 shard/Fortune/Silk Touch behavior.

Player-placed regional cores and higher ordinal tiers can be recovered with any tool; ordinal Lv1 still uses its shard drops. Natural cores remain restricted to Inner and Underworld. Donor ordinary surface tables keep their original wool patterns; the Inner iBLP tables use the shared Laver Condensation Core and live pattern playback. Numbers, laver wood, Guogao sweets and lamps use shared items. Turing tape, anchored LHO fragments, giant landmarks and the final chamber remain available.

Right-click Matrix Number Bricks to cycle 0–15, or colored number lamps to cycle 0–32. A blank lamp changes to 0 on its first click. Number Stones do not cycle. Mined items preserve their edited number and color. The three portal designs follow their actual destination, including return trips.

Christmas-tree scenery uses matching vanilla woods; the two custom Christmas carpentry families are removed. Loquat and Underworld Loquat carpentry also use vanilla oak counterparts; Loquat foliage, saplings and fruit drops remain. Small Outer Christmas trees keep their height and layered conical crowns, using vanilla spruce logs and their dedicated original Christmas leaves. Dread Fir logs produce their own planks, with stairs, slabs, doors, trapdoors, fences, gates, buttons, pressure plates, signs and boats. Dread Fir products are nonflammable and do not convert into spruce; other woods burn normally. Numeric Stone Pickaxes/Swords keep their existing digit levels and attributes.

A server rack costs four Compute Chips, four iron ingots and one redstone dust. A terminal costs two chips, three glass, two iron ingots and two redstone dust. Each recipe produces one block; harvesting yields the block itself, and unpacking either block returns two chips.

See the [current gameplay rules](MERGE-REVIEW-CHECKLIST.en.md) and [import architecture](OUTER-IMPORT.md).
