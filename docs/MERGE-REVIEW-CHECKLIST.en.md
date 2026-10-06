# Current gameplay rules · 0.4.0

[简体中文](MERGE-REVIEW-CHECKLIST.md) | English

This replaces the older enhancement rules. Current roles and verification are recorded in [WORK-0312](WORK-0312.md); use the [offline simulator](enhancement-simulator.html) to inspect calculated effects.

| Area | Current rule |
|---|---|
| Worlds | Outer supplies four mineral tiers and ordinary creatures; Inner and Guogao Underworld supply cores and landmarks |
| Equipment | Four tiers of picks, swords, bows, armor and manuscripts; numeric stone picks/swords remain |
| Numeric harvesting | 0–4 stone, 5–7 iron, 8 diamond, 9 netherite; ordinary progressive stats remain |
| Manuscripts | No durability; either hand, offhand priority; inventory Book button for offhand, main-hand right-click; passive edits apply on close; 2/3/4/6 slots |
| Enhancement | Same-item attack/protection uses root-sum-squares; armor is weighted; normal additive attack / multiplicative protection, separate deep factors |
| Durability | Empty Set 4/16/64 summed; wear probability changes, armor also benefits from pre-hit protection; no manuscript wear |
| Core roles | Sequence excavation/efficiency; Branch yield/reach; Laver aquatic/healing; Empty Set wear/stealth; Ordinal book-only; Criticality excludes books |
| Flight / rescue | Boundary provides flight, jumping and fire resistance; deep sprint is eightfold on both axes; Guogao consumes a real inventory Totem |
| Void / protection | Sonic booms and void use protection; Outer Y≤−1000 enters Inner, Inner Y≤−120 enters Underworld at Y500 |
| Enchanting | Four ordinal gear/manuscript tiers cannot enchant; numeric and vanilla items retain enchanting |
| Forward portals | Overworld cake + apple; Outer twelve mixed mineral blocks + Ω material; Inner twelve Guogao blocks + Guogao |
| Return portals | Twelve matching dedicated frames + one Return Token; each frame recipe yields one; token recipe is plank over stick |
| Arrival | Bounded async preparation, nearby safe portal reuse, safe-site search and silent fallback; prewarming never creates portals |
| Coordinates | Overworld:Outer:Inner:Underworld = 1:1:4:16 |
| Core rendering | Animation within 64 blocks; cached static inner form farther away within ordinary chunk visibility; Voxy static view remains |
| Creatures / bosses / progress | Seven ordinary creatures confined to Outer; no Hydra creature, bosses or exploration progression |
| Save migration | No old-save migration during early development |

Details: [survival guide](SURVIVAL-GUIDE.en.md), [import architecture](OUTER-IMPORT.md).
