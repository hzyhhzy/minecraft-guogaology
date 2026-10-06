# 0.3.8 — original C1 reliefs and Outer Christmas materials

## Authorized scope

- [x] Research the original gallery C1 directly: source cuboids, exact palette, per-face material, opposing main reliefs and faint interlayers. Record evidence in [C1-STRUCTURE.md](C1-STRUCTURE.md).
- [x] Restore dedicated original Alice Christmas leaves on Outer Christmas trees and use vanilla spruce logs. Preserve conical crowns, original height and density; do not restore the retired carpentry family.
- [x] Restore real volumetric C1-based interiors for all three absence-core grades, retaining independent component animation and the current smooth Lv3 outer arcs.
- [x] Check original volume, thickness, sides and gaps; inspect oblique game views and multiple animation phases.
- [x] Regenerate static Voxy art and check all 28 core meshes for real overlaps.
- [x] Build all four targets.
- [x] Run and inspect only the isolated hidden fo262 / 26.2.
- [ ] Install when normal Minecraft is closed, with backup/hash/unique-Mod checks. **Deferred: the normal fo262 client remains running.**

Return travel is a design request. A concrete low-cost fixed-material proposal is saved in [RETURN-PORTAL-PROPOSAL.md](RETURN-PORTAL-PROPOSAL.md); no portal recipe or ritual change is included in this correction release.

## Current evidence

Original C1 consists of 332 cuboids and the archived gallery mesh has exactly 1,992 faces with the identical vertex set. Its main layers have real sidewalls and 1.2/16-block thickness. The 0.3.7 projected-texture approach flattened that volume and is being replaced; see the research record for exact main/ghost layers and directional alpha.

The Outer leaf texture is restored byte-for-byte from Alice, SHA-256 `0d1951454c7e737c31a38c0805d37c579c9571e3e61384f529b216a1d12030ac`. Only the leaf block is restored. Sapling drops use vanilla spruce saplings. Forest and sparse LHO placements retain their shared conical crown and original tree-height/placement distributions. Inner and Underworld tree materials are outside this correction.

## Offline and build validation

`audit_absence_core.py` confirms the original 74-cuboid front relief, all 180 occupied source cells and their exact ink/soft classification, both exposed areas, signed volume, 1.2-unit thickness, ghost backs and all six side directions. It confirms positive closed volumes for every moving ring/slash, and no same-part coplanar overlap in any of the three grades. After hidden-face merging the three models have 445 / 572 / 1,666 faces. Rest-pose contacts between separate motion components are reported separately; they are not permanent same-part duplicates, and the real renderer gives the diagonal a 17-degree initial offset.

All 28 meshes pass the existing triangle-level substantive-overlap audit; normal two-sided sheets remain. Four unused projected-mask/mist textures were pruned. The resource audit has 2,179 reachable assets, no missing reference or orphan; both languages contain 365 matching keys. Fourteen Python regressions pass. Voxy atlas generation retains 369 padded tint tiles in a 512-square sprite and updates the grade inventory to the actual face/motion metadata.

Four binary/source targets build and pass release verification. The final 26.2 release SHA-256 is `3e83f941a123112751b16e7109cff5c6e5a990e6e4730c1a89b7147d15f9bd4f`. Only the disposable 26.2 QA client is started.

## Packaged runtime inspection

`corrections-038-a` tests the exact final 26.2 JAR in a hidden disposable fo262 copy and exits successfully. It passes 203 mechanics/template checks, 436 material checks, 145 maintenance checks and 26 original-leaf/conifer checks: 810 assertions, plus the existing full coastline/plain-gummy regressions. Generated trunk samples are 7/8/9/10 blocks and total heights 9–12, preserving the original distribution.

Screenshots `merge-8` (actual trees), `merge-9` (phase 0), `merge-12` (phase 10), `merge-13` (close oblique north-side relief) and `merge-14` (phase 21) were inspected. Lv1 has the authored discontinuous raised glyph with real visible sides; higher rings and the slash change relative orientation without moving their fixed shell. `merge-11` rechecks the unchanged tables/material scene. The QA client saves and closes normally.

`voxy-038-a` uses the installed Voxy software baker with the same release hash. All 205 host blocks, 2,677 states, 2,262 unique models and all 28 core grades pass; there are no empty/invisible model variants or missing core faces. The near-core screenshot was inspected, and the client exits normally.

The installer performs a fresh process check and reports `deferred` for normal fo262 process 59588; no files are replaced or backed up during this deferral. The normal installation still has the unique 0.3.7 main JAR with SHA-256 `ce66c432916bc5b7ba9f20da2184040526f27796ca8c3c097bf9ed6a47e7bc43`. The validated 0.3.8 release is ready in `build/releases/0.3.8/`; the existing standing authorization applies after the user closes Minecraft. The installer will then back up and recheck hashes, other mods and uniqueness.
