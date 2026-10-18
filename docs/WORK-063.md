# 0.6.3 — building material codes and giant reservations

## Scope

2026-10-18: the user authorizes fixing the fixed-number1 / Guogao blueprint material collision. Separately investigate giant buildings overlapping other objects, and repair only confirmed defects. A subsequent clarification specifically calls out historical reports of the Astra giant and ordinary Astra office buildings overlapping. Ordinary scenery/scenery collisions and building resource balance remain deferred.

Baseline: published `1ca71ed70762513178a953da8959aac5a8c52b7e`, backed up at `codex/backup-0.6.2-before-building-fixes`. Development branch: `codex/0.6.3-building-fixes`. No main merge, push or Release is authorized for this round.

## Compatibility boundary

Keep all registered IDs, item/block states and saved-world formats. Blueprint material numbers are internal transient generation data, not persisted registry IDs. Repairing generation does not scan or rewrite incorrectly generated old blocks. Preserve the intended Guogao blocks at the24 giant exterior emoji globe centers. Preserve0.6.2portal behavior and0.6.1optional26.3JEI compatibility.

## Material collision repair

`FIXED_NUMBER=48` reserves48–63 for the sixteen digits. The additional `GUOGAO_DROP=49` collided with digit1, and the final palette assignment replaced that digit with the luminous Guogao block in every theme. The actual affected authored digits were2 at the Power tower doorway,17 in LHO and12 in the Guogao number garlands:31 total.

Remove that redundant blueprint constant and its palette overwrite in both legacy/common `SurvivalStructures`. The24 legitimate Guogao exterior emoji globe centers now use the existing `PRIMARY_RAW=47`; this already resolves to the same `CoreResources.GUOGAO_DROP` in the Guogao theme. Do not change the separate `NaturalForms.GUOGAO_DROP=93` namespace.

Comparison against the released0.6.2 JAR shows that the only changed blueprint bytes are those24 centers,49→47; their actual registered block is unchanged. All other blueprint bytes and furnishing/resource metadata are identical. The31 intended digit1 positions regain the correct material through the palette repair. Evidence: `build/sanctuary-material-063/proof.json` and `compare.py`.

The regression now expands the fixed-number and pixel ranges and asserts128 unique material slots. It fails on the original duplicate49 and passes after the fix. No widening of the blueprint byte format or new block is needed.

## Astra giant versus ordinary offices

**No current counterexample was found, so production placement geometry remains unchanged.** `AstraCity` already validates the complete ordinary-office bounds, including lower plinth, entrance stairs and rooftop. A conflicting office is rejected as a whole or tested at alternate positions. `SanctuarySpace` includes rotated non-KEEP blueprint voxels, intentionally carved AIR, chests, foundation piers and padded entrance space. Thus preserving an empty room is also part of the reservation.

Focused pure-production geometry regression (`tools/check_astra_giant_063.py`) passed19,855,858 assertions:

- All8 giant themes ×4 rotations:16,938,288 non-KEEP voxels and actual chest positions covered by reservations.
- Actual accepted giant fixtures:547 intersecting chunk queries, no missing reservation.
- Seeds515515,512,620630:36 accepted Astra giant sites and46 nearby accepted ordinary offices, zero actual voxel overlaps and zero accepted-office/reservation intersections. Street/lamp painters also do not intrude into the tested giant footprints.
-23 original office proposals touched a giant reservation:17 rejected,6 relocated. This exercises both branches of the existing guard.
- Whole-window painting equals shuffled chunk painting around one giant per seed; no chunk-generation-order discrepancy in those3 cases. Actual office painter voxels stay inside their declared bounds.

Additional bounded reach probes used unrestricted ±512-block brushes and actual pure painters. LHO Hydra20,000 salts:maximum23/declared24; each of the following2,048 salts: Laver7/10, Y tree23/32, TREE39/64, SCG30/64, undergrowth8/9, yarn18/36. No writes beyond the declared reach. TREE/SCG include the configured minimum24 and maximum58 heights. Other wrappers were reviewed statically: Astra ground fir maximum24<72, individual knot conservative<39, cloud with attached knot conservative<128. Those static bounds are not a claim of exhaustively generating every scenery variant. Evidence: `build/astra-giant-063/checks.log` and `evidence.json`.

Historical source review confirms that the earliest city generation lacked whole-building giant avoidance and could clip offices against a giant. The parent workspace's `docs/TESTING.md` records whole-object avoidance in0.2.4, and the0.2.8 source already contains the present office guard. The formal repository inherited it in0.3.0. Old generated chunks can retain old defects; this does **not** establish that the unspecified reported overlap came from an old chunk. Coordinates/seed/version would be needed to classify that particular report. The finite sample above is not a global zero-failure guarantee or a saved-world scan.

Ordinary scenery/scenery overlaps and resource abundance are outside this patch. No blanket clearing, wider exclusion zone or resource nerf was introduced.

## Validation on final artifacts

- `build-all.ps1 -Offline`: all four targets compile and package-check,1.21.1 /1.21.11 /26.2 /26.3. Resource, localization and Outer-content audits pass.
- Existing Python suite:84 tests pass.
- `tools/check_landmarks_043.py`:14,531 blueprint assertions;123,044 chest assertions across8,192 cases;16 loot tables with160,000 samples pass.
- The focused Astra geometry regression above passes. Its test-only sources do not ship in the production JAR.
- Final JAR comparisons against0.6.2 show exactly3 changed production classes (`SanctuaryLayout`, `MosaicTreeLights`, `SurvivalStructures`) plus version metadata. All assets/data resources are byte-identical. The legacy nested bridge differs only in version metadata/filename. No QA classes are packaged. Evidence: `build/payload-diff-063.json`.
- Real hidden, mouse-unlocked disposable26.2 client/server run `building063-native-a`:178 registered palette/center checks plus36 native blueprint/light checks and19 placed-block readback assertions pass. Actual0–15 and11242 scenes are rendered, both screenshots inspected; digits1 and legitimate Guogao blocks are visually distinct. Marker: `BUILDING063_RUNTIME_OK`, runner exit0. No other-version runtime was launched this round. No Voxy dependency or renderer behavior changed.

Native evidence: `build/runtime-qa/building063-native-a/`, including `launch-output.log`, `port-client-ok.txt`, `building063-native-checks.txt` and `screenshots/building063-0.png` / `building063-1.png`. Normal saves/configuration are never used as QA fixtures.

| Minecraft | Final0.6.3 SHA-256 |
|---|---|
|1.21.1|`1b73fcdc5cb2b9cf98fd22336273f2ef9f1122af62f4a2e8125e10b76c61a091`|
|1.21.11|`0a1de8433a8aa46afb7ec7d854cb02cc9ab781c770ccfef964ce82a8bfab3831`|
|26.2|`b84845a3fdab69fcfb52950aa2da3ffaf8851eef4054bfbf6860e96e85832a84`|
|26.3|`4e3f6703f0806ae613f35d792fa68d77cf8e6ee01c681791b995a6377a186b39`|

## Installation and delivery

Normal Minecraft was confirmed closed. Following standing authorization, install the final tested26.2 JAR into normalfo262, back up0.6.2 at `build/backups/fo262-before-0.6.3`, and verify the sole main-Mod ID and exact hash.55 other Mods,129 configuration files and48 normalfo263_2 Mod files remain unchanged. Both normal save trees' file size/mtime snapshots are unchanged (21,178fo262 records and356fo263_2 records). Receipt: `build/install-063.json`.

Normalfo263_2 remains0.6.1. Keep the fix on `codex/0.6.3-building-fixes` with a local commit only; no push, Release or merge into real `main` (`1900d1eaa821d80afe41b8588cbb0f61563cdfdd`). Existing saved wrong blocks are not silently rewritten; corrected authored digits apply to newly generated construction.

## Publication follow-up authorization

The user subsequently explicitly requests pushing0.6.3 and publishing a Release. This supersedes the previous no-publication scope for this version only; retain the independent branch and leave real `main` unchanged. Publish the exact already tested four binaries together with matching complete source, checksums and license/provenance files. Documentation-only release preparation does not require changing/rebuilding those tested binaries. The requested research on adding resource blocks to all giants is a proposal only and is excluded from this release.
