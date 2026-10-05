# 0.3.5 — unified namespace, portal recipes, material art and Voxy visibility

User requests: remove the `googology_outer` namespace completely, refine the empty-set core/portal/epsilon art, rename equipment and give all four manuscripts dedicated icons. No save migration.

- [x] Move all remaining entity, biome, worldgen, template, loot, recipe, tag, translation and art IDs into `googology`.
- [x] Resolve the outer/inner dimension-type and noise-settings name collision; preserve both terrains.
- [x] Make source/resource generation produce only the host namespace, including all four Minecraft targets.
- [x] Add source/JAR/runtime guards against reintroducing the retired namespace.
- [x] Outer→Inner: twelve mineral storage blocks, any mixture of all four tiers, plus one Ω mineral. Validate the route before accepting frames; do not consume rejected offerings.
- [x] Narrow return materials to two families per layer. Inner→Underworld uses twelve Guogao blocks plus one Guogao offering. Overworld cake/apple remains.
- [x] Old single-faced empty-set glyph inside the discontinuous cubic cage at Lv1; original translucent palette on the existing Lv2/3 moving geometry.
- [x] ε₀ on both ore skins, mineral block and material icon only; names still ε.
- [x] All four mineral equipment sets omit 石 / Stone; four distinct high-quality scroll/folio manuscript sprites.
- [x] Three portal styles have distinct palettes, sigils and details; model and outline lowered from 8.5–9.5 to 4.5–5.5 pixels.
- [x] Fix Voxy's empty core models across all 28 grades; expose the static shell and interior with preserved colors and transparency, omitting distant animation and outer ornaments.
- [x] Inspect all registered block models, including plants, lights, numeral/color variants and imported content.
- [x] Four-version compilation and hidden isolated fo262 runtime/visual checks.
- [x] Install in fo262 if Minecraft is closed, preserving the previous JAR.

Java source packages named `dev.googology.outer` remain a maintainable module boundary. They are not a Minecraft resource namespace. The outer world's dimension remains `googology:outer`.

Generation is ownership-based: `outer_generated_resources.json` removes only explicitly imported files before regeneration. It cannot remove the Inner or Underworld dimension/noise definitions. Optional donor reimport stages art in `build/outer-import`; it does not overwrite live host assets.

Current art is reproducible through `refresh_common_art.py` and `refine_material_art.py`. Native pixel silhouettes distinguish the four manuscripts: narrow sealed scroll, loose silver-clasped folio, open jade-bound pages, and broad gold/lapis charter. No book-item fallback remains. Epsilon's existing substrate/silhouette is retained.

The ambiguous “double-sided to single-sided” core request is implemented as one old glyph face, without a second rear inscription showing through it. The old mask is retained as a tiny source image; the relief illusion uses one quad rather than hundreds of duplicate voxel faces. Higher-level movement remains unchanged.

## Voxy static model compatibility

The installed Voxy 0.2.19-beta reads vanilla model parts/quads. The modern core wrapper previously returned no parts because its fixed shell used Fabric's mesh path and its interior used the animated renderer. Voxy consequently baked empty cells. All 28 grades now provide a separate cached static body via that API. The 1.21.1 adapter similarly separates the static vanilla fallback from nearby Fabric emission; no duplicate animated interior is added nearby.

The installed baker also ignores vertex tint and writes depth while blending transparent surfaces. A single 512×512 padded atlas preserves 369 quantized tint/texture combinations, and interior-first ordering keeps the glass shell from hiding the motif. The atlas is generated offline; runtime builds no new textures and allocates no models per frame. Nearby geometry, animation and colors are unchanged. Outer ornaments are intentionally omitted in LOD, as permitted by the user. This is a vanilla model interface fix, with no required Voxy dependency or modification of its JAR.

`ports/qa26/.../VoxyChecks.java` loads the installed software baker reflectively in a disposable hidden fo262 copy. It checks registry states, renders real six-face outputs and records a nearby screenshot. `tools/render_voxy_audit.py` makes a contact sheet from those captured pixels. QA helpers remain outside all release JARs. Ordinary Voxy data, normal saves, settings and other mods are not modified.

Final evidence: `build/runtime-qa/voxy-035-final` passed with Voxy 0.2.19-beta, 241 blocks / 3,725 states / 2,833 distinct models, no empty or invisible model variants, and visible output on all six faces of each of the 28 core grades. Nearby screenshots retain the animated interiors and outer ornaments. The earlier namespace/material runtime passed 676 assertions and actual feature placement in `namespace-035-a`. Four release JARs compiled and passed package/resource/localization checks; only 26.2 was launched.

Installed 26.2 into normal fo262 after confirming the game was closed. Installed and runtime-tested SHA-256: `4b619fa7ff61852c6e63df7780ab6937148ebc2bf7a0e068f0202ce9f96216dd`. The previous 0.3.4 is backed up in `build/backups/fo262-before-0.3.5`; exactly one main Mod remains and all 55 other files are unchanged.
