# Block harvesting reference · 0.3.3

[简体中文](BLOCK-BALANCE.md) | English · [Survival guide](SURVIVAL-GUIDE.en.md)

This table uses the same `block_balance.json` as the running mod. “Tool” is the tool that speeds up mining; “Minimum tier” describes natural-block harvesting requirements. Appearance variants retain their saved number, color, and lantern tile when picked up and placed again. Collision shapes are separate from item drops.

Player-placed regional cores, TREE nodes, landmark relics, rare LHO materials, and higher-grade Ordinal Crystals can be recovered intact by hand or with any tool. Player-placed LHO materials do not vanish. Lv1 Ordinal Crystals instead follow glowstone rules: normally 2–4 shards, a Fortune cap of four, intact blocks with Silk Touch, and vanilla explosion decay.

Natural LHO fragments and ψ/Z branches require an Ordinal Crystal anchor in the offhand or within a 5×5×5 neighborhood. Their natural harvesting also requires a pickaxe.

Four ore tiers now use ordinary hardness 3/3.5/4/4.5 and require an iron pickaxe. They naturally generate only in Outer Googology. This table lists the shared monumental-world terrain, plant, decoration and relic profiles; the four ore/storage families, enhancement tables and imported Outer wood families are registered separately. See the survival guide for equipment and core fusion.

| Name | Block ID | Hardness | Blast resistance | Tool | Minimum tier | Light | Collision | Drop |
|---|---|---:|---:|---|---|---:|---|---|
| Absence Glass | `absence_glass` | 0.35 | 0.5 | Hand | None | 2 | Full block | Block itself |
| Amber Apple Guogao | `amber_guogao` | 0.5 | 1 | Hand | None | 3 | Inset Guogao shape | Block itself |
| Amber Inlay Bricks | `amber_inlay` | 1.5 | 5 | Pickaxe | None | 10 | Full block | Block itself |
| Astra Condensed Cloud | `astra_cloud` | 0.4 | 0.5 | Shears | None | 2 | Full block | Block itself |
| Astra Cloud Billow | `astra_cloud_shade` | 0.4 | 0.5 | Shears | None | 2 | Full block | Block itself |
| Astra Compute Crystal | `astra_compute_crystal` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| Criticality Core · Lv1 | `astra_critical_core` | 3 | 7 | Pickaxe | Iron | 10 | Full block | Block itself |
| Astra Light | `astra_light` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Astra Marble | `astra_marble` | 1.8 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Astra Pale-Green Jade | `astra_mint` | 1.8 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Braided Crystal Porcelain | `astra_weave` | 1.3 | 4 | Pickaxe | None | 5 | Full block | Block itself |
| Azure Guogao | `azure_guogao` | 0.5 | 1 | Hand | None | 3 | Inset Guogao shape | Block itself |
| Laver Vein Panel | `basic_laver_pattern` | 1.6 | 3 | Axe | None | 3 | Full block | Block itself |
| Berry Guogao | `berry_guogao` | 0.5 | 1 | Hand | None | 3 | Inset Guogao shape | Block itself |
| Guogao Lantern · Cyan | `cyan_guogao_lantern` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Dread Fir Leaves | `dread_leaves` | 0.2 | 0.2 | Hoe | None | 0 | Full block | Block itself |
| Dread Fir Log | `dread_log` | 2 | 3 | Axe | None | 0 | Full block | Block itself |
| Anxious Emoji Flower | `emoji_flower` | 0 | 0 | Hand | None | 3 | None | Block itself |
| Epsilon Bloom | `epsilon_bloom` | 0 | 0 | Hand | None | 3 | None | Block itself |
| ε Symbol Branchstone | `epsilon_symbol` | 1.2 | 3 | Pickaxe | Stone | 5 | Full block | Block itself |
| Symbol Turf | `epsilon_turf` | 0.5 | 0.5 | Hoe | None | 0 | Full block | Block itself |
| fffZ Missing Definition | `fffz_trace` | 0.15 | 0.1 | Pickaxe | None | 5 | None | Intact when anchored |
| FOS Undefined Fragment | `fos_trace` | 0.15 | 0.1 | Pickaxe | None | 5 | None | Intact when anchored |
| Guogao Underworld Portal | `fruit_portal` | -1 | 3600000 | None | None | 8 | None | No normal drop |
| Giant Laver Blade | `giant_laver` | 0.35 | 0.35 | Hoe | None | 0 | Full block | Block itself |
| Great Omega Crown | `great_omega_bloom` | 0 | 0 | Hand | None | 3 | None | Block itself |
| Ω Symbol Branchstone | `great_omega_symbol` | 1.2 | 3 | Pickaxe | Stone | 5 | Full block | Block itself |
| Heart of Guogao · Lv1 | `guogao_heart` | 3 | 7 | Pickaxe | Iron | 12 | Full block | Block itself |
| Dread Fir Heart Resin | `guogao_heart_resin` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| Guogao Lantern · Amber | `guogao_lantern` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Guogao Dreadsoil | `guogao_loam` | 0.6 | 0.6 | Shovel | None | 0 | Full block | Block itself |
| Guogaology Star Portal | `guogao_portal` | -1 | 3600000 | None | None | 8 | None | No normal drop |
| Ordinal Portal Frame | `guogao_portal_frame` | 3.5 | 12 | Pickaxe | Stone | 7 | Stepped frame, up to 0.75 blocks high | Block itself |
| Hydra Bud · Lv1 | `hydra_bud` | 3 | 7 | Pickaxe | Iron | 7 | Full block | Block itself |
| Branching Jade | `hydra_jade` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| iBLP Empty Cell | `iblp_blank` | 1.6 | 3 | Axe | None | 3 | Full block | Block itself |
| iBLP Marked Point · * | `iblp_marked` | 1.6 | 3 | Axe | None | 3 | Full block | Block itself |
| iBLP Ordinary Point · ○ | `iblp_node` | 1.6 | 3 | Axe | None | 3 | Full block | Block itself |
| Laver Core · Lv1 | `laver_core` | 2.5 | 5 | Axe | None | 8 | Full block | Block itself |
| Laver Plain Jade | `laver_court_blank` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| Laver Ring Jade | `laver_court_node` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| Laver Inlay | `laver_inlay` | 1.5 | 6 | Pickaxe | None | 2 | Full block | Block itself |
| Laver Mat | `laver_mat` | 0.5 | 0.5 | Hoe | None | 0 | Full block | Block itself |
| Laver Table Planks | `laver_planks` | 1.6 | 3 | Axe | None | 3 | Full block | Block itself |
| Laver Vein | `laver_vein` | 2 | 3 | Axe | None | 0 | Full block | Block itself |
| Absent Hydra Psi | `lho_hydra_psi` | 0.15 | 0.1 | Pickaxe | None | 5 | Full block | Intact when anchored |
| Absent Hydra Z | `lho_hydra_z` | 0.15 | 0.1 | Pickaxe | None | 5 | Full block | Intact when anchored |
| LHO Glass · H | `lho_letter_h` | 0.35 | 0.5 | Hand | None | 2 | Full block | Block itself |
| LHO Glass · L | `lho_letter_l` | 0.35 | 0.5 | Hand | None | 2 | Full block | Block itself |
| LHO Glass · O | `lho_letter_o` | 0.35 | 0.5 | Hand | None | 2 | Full block | Block itself |
| Empty Set Core · Lv1 | `lho_trace` | 0.15 | 0.1 | Pickaxe | None | 5 | None | Intact when anchored |
| Lime Guogao | `lime_guogao` | 0.5 | 1 | Hand | None | 3 | Inset Guogao shape | Block itself |
| Guogao Lantern · Lime | `lime_guogao_lantern` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| LTY Tianyi Yarn | `lty_yarn` | 0.5 | 0.5 | Shears | None | 2 | Full block | Block itself |
| Matrix Archive Ceramic | `matrix_archive_ceramic` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| Mosaic Lamp Pixel | `mosaic_light` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Astra Office Desk | `office_desk` | 1.8 | 6 | Pickaxe | Stone | 0 | Full block (furniture model) | Block itself |
| Astra Terminal | `office_monitor` | 3 | 8 | Pickaxe | Iron | 3 | Full block (furniture model) | Block itself |
| Omega Twinbell | `omega_bloom` | 0 | 0 | Hand | None | 3 | None | Block itself |
| ω Symbol Branchstone | `omega_symbol` | 1.2 | 3 | Pickaxe | Stone | 5 | Full block | Block itself |
| Ordinal Crystal · Lv1 | `ordinal_crystal` | 3 | 5 | Hand | None | 15 | Full block | Silk Touch: block; otherwise shards |
| Number Stone 0 | `ordinal_stone` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Phi Ringflower | `phi_bloom` | 0 | 0 | Hand | None | 3 | None | Block itself |
| φ Symbol Branchstone | `phi_symbol` | 1.2 | 3 | Pickaxe | Stone | 5 | Full block | Block itself |
| Power Tower Wood | `power_bricks` | 2 | 3 | Axe | None | 0 | Full block | Block itself |
| Power Sand | `power_sand` | 0.6 | 0.6 | Shovel | None | 0 | Full block | Block itself |
| Power Tower Core · Lv1 | `power_tower_core` | 3 | 7 | Pickaxe | Iron | 9 | Full block | Block itself |
| Psi Trident Fern | `psi_fern` | 0 | 0 | Hand | None | 3 | None | Block itself |
| ψ Symbol Branchstone | `psi_symbol` | 1.2 | 3 | Pickaxe | Stone | 5 | Full block | Block itself |
| Recursive Bronze | `recursive_bronze` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| Resonant Silk | `resonant_silk` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| Guogao Dreadstone | `rootbound_stone` | 2.5 | 8 | Pickaxe | Stone | 0 | Full block | Block itself |
| Guogao Lantern · Rose | `rose_guogao_lantern` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Guogao Lantern · Scarlet | `scarlet_guogao_lantern` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Sequence Core · Lv1 | `sequence_core` | 3 | 7 | Pickaxe | Iron | 7 | Full block | Block itself |
| Astra Server Rack | `server_rack` | 3 | 8 | Pickaxe | Iron | 3 | Full block | Block itself |
| Starlight Glass | `star_gold` | 1.2 | 4 | Pickaxe | None | 15 | Full block | Block itself |
| Sun-Pattern Tiles | `sun_pattern_tiles` | 1.6 | 5 | Pickaxe | None | 0 | Full block | Block itself |
| Tianyi Cyan Fiber | `tianyi_fiber` | 0.5 | 0.5 | Shears | None | 2 | Full block | Block itself |
| TREE Blue Node | `tree_node_blue` | 2.5 | 6 | Pickaxe | Iron | 5 | Full block | Block itself |
| TREE Green Node | `tree_node_green` | 2.5 | 6 | Pickaxe | Iron | 5 | Full block | Block itself |
| TREE Red Node | `tree_node_red` | 2.5 | 6 | Pickaxe | Iron | 5 | Full block | Block itself |
| Guogao Lantern · Violet | `violet_guogao_lantern` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Moonwhite Fiber | `white_fiber` | 0.5 | 0.5 | Shears | None | 2 | Full block | Block itself |
| Y-branch Frost Leaves | `y_leaves` | 0.2 | 0.2 | Hoe | None | 0 | Full block | Block itself |
| Y-branch Log | `y_log` | 2 | 3 | Axe | None | 0 | Full block | Block itself |
| Y-Sequence Mountain Stone | `y_sequence_stone` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| ζ Symbol Branchstone | `zeta_symbol` | 1.2 | 3 | Pickaxe | Stone | 5 | Full block | Block itself |
| Zeta Curlvine | `zeta_vine` | 0 | 0 | Hand | None | 3 | None | Block itself |
| Mountain Lamina | `ridge_lamina` | 1.3 | 6 | Pickaxe | Stone | 1 | Full block | Block itself |
| Projectible Glass | `projection_glass` | 0.7 | 3 | Pickaxe | None | 2 | Full block | Block itself |
| BEAF Marbled Stone | `beaf_marble` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Bird Feather Shale | `bird_shale` | 1.2 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Conway Chain Stone | `conway_link` | 2.0 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Veblen Petal | `veblen_petal` | 0.4 | 3 | Pickaxe | None | 3 | Full block | Block itself |
| Boundary Stone | `limit_stone` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Rank Lamina | `limit_lamina` | 1.6 | 6 | Pickaxe | Stone | 1 | Full block | Block itself |
| Set Glass | `set_glass` | 0.8 | 3 | Pickaxe | None | 2 | Full block | Block itself |
| Formula Stone | `formula_stone` | 2.0 | 6 | Pickaxe | Stone | 3 | Full block | Block itself |
| Proof Strata Stone | `proof_stone` | 2.0 | 6 | Pickaxe | Stone | 2 | Full block | Block itself |
| Turing Tape | `turing_tape` | 1.4 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Boundary Core · Lv1 | `boundary_core` | 3.5 | 8 | Pickaxe | Iron | 9 | Full block | Block itself |
| Rank Amberstone | `rank_amber` | 1.8 | 6 | Pickaxe | Stone | 1 | Full block | Block itself |
| Logic Ivory Stone | `logic_ivory` | 1.7 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Rayo Roseglass | `rayo_rose` | 0.8 | 3 | Pickaxe | None | 3 | Full block | Block itself |
| Set Jadestone | `set_jade` | 1.8 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Axiom Porcelain | `axiom_porcelain` | 3.5 | 9 | Pickaxe | Iron | 4 | Full block | Block itself |
| Ordinal Digit Brick | `ordinal_bricks` | 1.8 | 6 | Pickaxe | Stone | 3 | Full block | Block itself |
| Amber Sequence Light | `amber_sequence_light` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Cyan Sequence Light | `cyan_sequence_light` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Rose Sequence Light | `rose_sequence_light` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Lime Sequence Light | `lime_sequence_light` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Violet Sequence Light | `violet_sequence_light` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Scarlet Sequence Light | `scarlet_sequence_light` | 0.6 | 1.5 | Pickaxe | None | 15 | Full block | Block itself |
| Sequence Core · Lv2 | `sequence_core_lv2` | 4.5 | 12 | Pickaxe | Iron | 8 | Full block | Block itself |
| Sequence Core · Lv3 | `sequence_core_lv3` | 6 | 18 | Pickaxe | Iron | 9 | Full block | Block itself |
| Power Tower Core · Lv2 | `power_tower_core_lv2` | 4.5 | 12 | Pickaxe | Iron | 10 | Full block | Block itself |
| Power Tower Core · Lv3 | `power_tower_core_lv3` | 6 | 18 | Pickaxe | Iron | 11 | Full block | Block itself |
| Hydra Bud · Lv2 | `hydra_bud_lv2` | 4.5 | 12 | Pickaxe | Iron | 8 | Full block | Block itself |
| Hydra Bud · Lv3 | `hydra_bud_lv3` | 6 | 18 | Pickaxe | Iron | 9 | Full block | Block itself |
| Empty Set Core · Lv2 | `lho_trace_lv2` | 4.5 | 12 | Pickaxe | Iron | 6 | Full block | Block itself |
| Empty Set Core · Lv3 | `lho_trace_lv3` | 6 | 18 | Pickaxe | Iron | 7 | Full block | Block itself |
| Laver Core · Lv2 | `laver_core_lv2` | 4.5 | 12 | Pickaxe | Iron | 9 | Full block | Block itself |
| Laver Core · Lv3 | `laver_core_lv3` | 6 | 18 | Pickaxe | Iron | 10 | Full block | Block itself |
| Criticality Core · Lv2 | `astra_critical_core_lv2` | 4.5 | 12 | Pickaxe | Iron | 11 | Full block | Block itself |
| Criticality Core · Lv3 | `astra_critical_core_lv3` | 6 | 18 | Pickaxe | Iron | 12 | Full block | Block itself |
| Boundary Core · Lv2 | `boundary_core_lv2` | 4.5 | 12 | Pickaxe | Iron | 10 | Full block | Block itself |
| Boundary Core · Lv3 | `boundary_core_lv3` | 6 | 18 | Pickaxe | Iron | 11 | Full block | Block itself |
| Heart of Guogao · Lv2 | `guogao_heart_lv2` | 4.5 | 12 | Pickaxe | Iron | 13 | Full block | Block itself |
| Heart of Guogao · Lv3 | `guogao_heart_lv3` | 6 | 18 | Pickaxe | Iron | 14 | Full block | Block itself |
| Ordinal Crystal · Lv2 | `ordinal_crystal_lv2` | 4.5 | 12 | Pickaxe | Iron | 15 | Full block | Block itself |
| Ordinal Crystal · Lv3 | `ordinal_crystal_lv3` | 6 | 18 | Pickaxe | Iron | 15 | Full block | Block itself |
| Ordinal Crystal · Lv4 | `ordinal_crystal_lv4` | 8 | 24 | Pickaxe | Iron | 15 | Full block | Block itself |
| Number Stone 1 | `ordinal_stone_1` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Number Stone 2 | `ordinal_stone_2` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Number Stone 3 | `ordinal_stone_3` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Number Stone 4 | `ordinal_stone_4` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Number Stone 5 | `ordinal_stone_5` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Number Stone 6 | `ordinal_stone_6` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Number Stone 7 | `ordinal_stone_7` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Number Stone 8 | `ordinal_stone_8` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
| Number Stone 9 | `ordinal_stone_9` | 1.5 | 6 | Pickaxe | Stone | 0 | Full block | Block itself |
