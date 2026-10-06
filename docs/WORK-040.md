# 0.3.10 — distance models, combined effects and safe silent travel

User-approved scope, 2026-10-06. This is based on installed 0.3.9; no exploration progression or new boss is included.

- [x] Record the exact implementation checklist and update the patch version.
- [x] Keep 64-block animation; render a cached static core beyond it within ordinary chunk visibility, preserving the current nearby/Voxy artwork.
- [x] Numeric pickaxe harvest tiers: digits 0–4 stone, 5–7 iron, 8 diamond, 9 netherite.
- [x] Merge manuscript and actual tool mining effects without suppressing vanilla effects or speed on non-pickaxe blocks.
- [x] Fixed base maximum durability; combine wear-protection points and reduce consumption probability equally in every realm; preserve vanilla Unbreaking.
- [x] Convert ordinal manuscript repair into held/worn gear wear protection; preserve branch manuscript healing.
- [x] Remove Laver experience gain and defer its replacement; keep block/art/fusion/table playback, reject new installation, allow old sockets to be removed.
- [x] Improve portal search based on the Nether-portal approach: nearby valid gate, bounded safe location search, then controlled safe fallback.
- [x] Async bounded destination loading, per-tick search budget, cancellation/timeouts and silent safe rollback; no destination edits during prewarming.
- [x] Reject hazardous landing blocks and avoid overwriting containers/player structures.
- [x] Synchronize GUI/tooltips, English/Chinese catalog and current guides; clean obsolete checklists.
- [x] Four target builds, packaging/static checks and appropriate regression tests.
- [x] Hidden isolated fo262 / 26.2 actual checks, distance screenshots and clean exit.
- [x] Fresh process check, backup/install after normal Minecraft closed, hash/unique-main verification.

Scope deliberately leaves exploration progression deferred. No other mods, saves or normal settings are changed. No GitHub publication is authorized by this task.

## Validation

All four final binary/source targets build: 1.21.1, 1.21.11, 26.2 and 26.3. The final build also checks 2,199 reachable assets, 370 bilingual keys and 132 recipes; there are no missing/orphan references, parameter mismatches or hardcoded Chinese UI. Fourteen Python regressions pass, alongside the new standalone EquipmentRules040 arithmetic checks and 2,916 cached-pose comparisons. Other Minecraft targets are compile/resource checked only, with no client, server or GameTest launch.

`fixes-040-b` runs the exact final release in a disposable hidden fo262 / 26.2, with mouse capture disabled. It passes 185 mechanism/template, 413 return/recipe, 436 material, 145 maintenance, 26 Christmas-tree, 266 mining/equipment and 40 portal assertions: 1,511 counted assertions, plus existing coastline/gummy generation, actual features, manuscript rescue and core-distance checks. The first attempted test (`fixes-040-a`) correctly failed because its Soul Fire fixture extinguished on stone; the test alone was corrected to use Soul Soil and verify that each hazard actually exists before checking it. Production hazard rejection was already present.

The portal checks cover harmful foot/neighbor blocks, preserved containers, cancellation on leaving/changing dimensions/breaking frames, failed futures, timeout, shared tickets, a finite search budget, no destination construction during prewarming, and silent cancellation. A real round trip plus a genuinely unvisited destination completes in 105 server ticks in the test. Both positive and negative coordinates keep the 9×9 footprint and its direct neighbor-update rim within prepared chunks. Local Minecraft bytecode confirmed that neighbor updates could otherwise trigger synchronous chunk loading, so site checks use a one-block loaded margin.

All 28 core appearances retain their fallback body. Eighteen moving high-grade bodies submit the original animation at 12 blocks and a single cached static batch at 65 and 100 blocks. Ordinal Lv2 intentionally stays fully in its static chunk model. Actual screenshots `merge-19.png`, `merge-20.png` and `merge-21.png` were inspected; the glass shells and visible inner structures remain, without duplicate shell geometry. The client saves and exits normally.

Final tested 26.2 SHA-256: `63499b1dacf15b10ee21576c320658dda936bd0d458a31cb82e370166e46984b`. Four-target hashes are in `build/releases/0.3.10/manifest.json`; runtime evidence is in `build/runtime-qa/fixes-040-b/`.

The separate hidden `voxy-040-a` run uses the same final release and the installed Voxy baker: 208 blocks, 2,680 states, 2,265 models and all 28 core appearances pass, with no invisible models or bake failures. The near-core screenshot was inspected; the client saves and exits normally.

The fresh installation check confirmed normal Minecraft was closed. Version 0.3.10 is installed in normal `fo262`, matching the tested SHA-256 above. The old main JAR is backed up in `build/backups/fo262-before-0.3.10/`; there is exactly one main Mod, and the hashes of the other 55 files are unchanged. Installation evidence is in `build/install-040.json`.

