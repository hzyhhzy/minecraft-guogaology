# 0.3.9 — fixed local return frames

User confirmed the concrete return proposal. This release also contains the completed 0.3.8 original C1 volume and Outer Christmas-tree material corrections, which were validated but not yet installed.

- [x] Register three separate recoverable Return Frames and the shared Return Token in the original and modern common sources.
- [x] Exact local recipes: four cobblestone/shards/Guogao loam plus one plank produce one corresponding frame; Underworld specifically uses Dread Fir planks. One any plank above one stick produces one token, avoiding vanilla sticks/pressure-plate recipes.
- [x] Twelve matching frames and one token return exactly one layer; reject old terrain rings, old offerings and mixed frame types.
- [x] Keep forward progression, complete 5×5 validation without loading neighbors, immediate appearance packet, actual-destination styles, generated reverse gates and whole-gate collapse.
- [x] Dedicated native frame/token artwork, matching collision shapes, hand drops, recipe-book unlocks and creative groups.
- [x] Update the Chinese/English help, survival guides and approved proposal.
- [x] Static resource/localization/content checks and four target builds.
- [x] Actual loaded recipe/ritual/travel/drop regression and art inspection in a hidden disposable fo262 / 26.2.
- [x] Fresh normal-game process check; back up and install if closed, then verify hash, unique main Mod and unrelated files.

## Final validation

The four final binary/source targets build: 1.21.1, 1.21.11, 26.2 and 26.3. The resource graph has 2,199 reachable assets and no missing references or orphans; 369 bilingual keys and 132 recipes pass the localization/content audits. Fourteen existing Python regressions pass. Package inspection confirms all three Return Frames output exactly one, the token uses the final plank-above-stick layout, old permissive return tags are absent and no development QA classes are shipped.

`returns-039-b` uses the exact final release in a hidden disposable fo262 / 26.2 and exits normally after saving. It passes 185 mechanics/template assertions, 413 return-portal/recipe assertions, 436 material assertions, 145 maintenance assertions and 26 original Christmas-leaf/conifer assertions: 1,205 in total, plus the existing coastline/plain-gummy, real feature-placement and manuscript survival checks. All 14 actual plank-tag members craft the token; the old two-plank layouts are rejected as token recipes while vanilla sticks and pressure plates remain available. Exact-source frames, one-token consumption, actual travel destinations, automatic reverse gates, hand drops and whole-gate collapse pass.

Screenshot `merge-18.png` was inspected for all three inert frames, all three active destination styles and the final handled token. The existing C1 and Christmas-tree regressions are included. Other Minecraft targets were compile/resource checked only, with no client/server/GameTest launch.

The installed/tested 26.2 SHA-256 is `c906fb4b7fba62fb72434b3fea5ce3a543a76b73f0c50a58639b28471e548dd6`. A fresh process check found normal Minecraft closed. The old main JAR is backed up in `build/backups/fo262-before-0.3.9/`; normal fo262 now contains exactly one 0.3.9 main Mod, with all other 55 Mod-directory files unchanged. Full build hashes are in `build/releases/0.3.9/manifest.json`, runtime evidence in `build/runtime-qa/returns-039-b/` and installation evidence in `build/install-039.json`.

No old-save migration or GitHub publication is included.
