# 0.5.4.1 JAR source reconstruction

The user requested a separate source branch and all four Minecraft targets, without merging into `main`.

- Branch: `codex/import-0.5.4.1-jar`.
- Baseline: `main`, `923dfb9698e2b3fa9e1415218e4b581bf4e1ad7e` (0.5.3).
- Input: user-supplied `guogaology-dimension-26.3-0.5.4.1.jar`.
- SHA-256: `056f959d2fa0c60a7686075fe9b971b78655fb7bb9f64eaf4cec96bc5e17b18d`.
- The received JAR retains the project's GPL-3.0-only WITH GPL-3.0-linking-exception notices. Original attribution is retained. This is a reconstruction, not the donor's unavailable original source.
- No push, Release or merge into `main` is authorized for this task.

## Reconstruction scope

1. Deterministic Outer spiral shafts: 113-block candidate cells, 5×5 stone rim, 3×3 interior, clockwise Conway steps. Keep the original hashes, positions, LHO exclusion and chunk boundaries. **The received design deliberately has no bottom floor and carves through bedrock to the world minimum.** This branch reproduces it rather than silently redesigning it.
2. Conway Link has four horizontal facing states and faces opposite the placing player. Existing scenery defaults east, preserving its previous appearance.
3. New translucent Gummy Jar, collision bounds 1..15 in X/Z, full block height, light 3. Outer Laver tabletop cells independently contain a jar with 1% probability. A jar drops 1–3 ordinary gummies. No new recipe. The donor's non-player-facing balance note incorrectly said random colored gummies; this documentation describes the actual loot.
4. Giant Laver drops itself with Silk Touch; otherwise its ribbon chance is 5%, 6.25%, 8.3333336%, 10% by Fortune level (capped at the final entry), with explosion survival.
5. Four embossed Guogao recipes use Fruit Cake Gel instead of slime balls; recipe unlocks follow the new ingredient.
6. Graham Vine is climbable.
7. Seven mineral textures imported byte-for-byte, including plain ε instead of ε₀. The epsilon art source/exporter is synchronized so rebuilding art does not revert it.
8. 26.3 recipe unlocks and Dread Fir block-state loot predicates use the correct version-specific schema. Earlier versions retain their own schemas.
9. 26.3 atmosphere data access invokes FabricRenderState's interface.

The 61 structures in the donor JAR decode identically to 0.5.3; only compression differed. Other apparent class changes were compiler/debug metadata. No structure templates, equipment/flight rules, giant-landmark loot, portals, ore distributions or mob behavior are replaced.

## Source organization

The host has Yarn 1.21.1 block classes and common Mojang-mapped modern equivalents. The shaft field is shared by all four versions, including the separately remapped 1.21.1 Outer bridge. Only the generation API entry points differ. There is no nested donor binary or runtime decompiler, and builds do not need the received JAR.

## Validation

- [x] Four target builds and package audit: 1.21.1, 1.21.11, 26.2, 26.3.
- [x] Resource, bilingual, Outer-content and Python regression checks: 47 Python tests; 2,301 retained resources, no missing/unused resources; 461 language keys in each language; 137 imported recipes.
- [x] Shaft behavior compared against the received implementation: five seeds, negative/positive coordinates, LHO exclusions, fluids and open bottom; 1,415 shafts and 7,240,757 ordered block writes match exactly. Shared SHA-256 fingerprint: `a64cfc139efa65b86e3f74ee8e54a4773132a80cf82579a833daa84411802e04`. This comparison runs only the pure algorithm, not a 26.3 client/server.
- [x] Rebuilt 26.3 resources compared semantically with the received archive: 3,404 resources, none missing/extra/different. The check accounts explicitly for equivalent item-model indirection and the corrected developer loot note, while ignoring JSON formatting/NBT compression.
- [x] Hidden isolated fo262 runtime: 15,175 checks, successful shutdown. Actual Fortune rates, Silk Touch, 512 jar-loot draws, replacement recipe ingredients, 11,200 eligible tabletop cells (97 jars), climbable vine, four facing states and two naturally generated shafts passed. The shafts at `(42,36,-72)` and `(-83,51,39)` reach the open minimum; both lower spiral segments checked. Two native screenshots inspected: no missing texture, jars transparent, shaft arrows follow the spiral.

The user's `fo263_2` keeps the exact received JAR for their own testing. Runtime verification of reconstructed source uses an isolated hidden fo262 instance; 26.3 is compile/resource checked only.

Evidence is under ignored `build/compare-0541/` and `build/runtime-qa/import-0541-a/`. Reproducible checks are `tools/audit_import_0541.py`, `tools/tests/SurfaceShaftFingerprint.java`, `tools/tests/test_import_0541_resources.py`, and the development-only `JarImport0541Checks` selected by `tools/run_merge_qa.py --fresh-world --jar-import-only`. No QA helper is in release JARs. Existing 0.5.3 Connector fixes remain; Connector was not rerun for this world-content import.

## Build outputs

All binaries are in `build/releases/0.5.4.1/`, with manifest and checksums:

| Minecraft | SHA-256 |
| --- | --- |
| 1.21.1 | `1e258cfe23ef30061ec529b2d16705f6588b091b38a8589ca0ff0c234537c483` |
| 1.21.11 | `94c7036fa7264828ea8b8604e4d660fa1bf10df1b4b2eb4f2bf6ef0aae8419f5` |
| 26.2 | `d9b7e242bf61e7b0e9ec5e95102507b8e9a3cf0806c45c9e784872dc8438253e` |
| 26.3 | `ea89c1cd56d515488958cad9927c78b21c4089ea13270740990eea3e50cc9ebd` |

The reconstructed 26.3 JAR is a new build and intentionally has a different archive hash from the received binary. Its reviewed behavior and resources match; formatting, source names/debug information and packaging do not need byte identity.

## Local installation

After checking that Minecraft was closed, the standing installation authorization was used to install the verified 26.2 build into normal `fo262`. The previous 0.5.3 JAR is saved under `build/backups/fo262-before-0.5.4.1/`. Installed SHA-256 matches both the runtime-tested JAR and release manifest; exactly one main Mod is present and all 55 other mod files are unchanged. Receipt: `build/install-0541.json`. The exact donor JAR in `fo263_2` is untouched.
