# 0.3.4 — common materials and three portal appearances

User-approved scope, 2026-10-05. Early development: no old-save migration.

- [x] Empty-set cores: fragmented old FOS/fffZ-style material on both inner and outer geometry; retain current shapes and animation.
- [x] Four equipment tiers use Alice's `ω石镐` / `Ω石胸甲` naming, bilingual.
- [x] Alice stone families use their vanilla counterparts; underworld ordinal stone becomes netherrack. Host numbered stone and Guogao embossed terrain stay distinct.
- [x] Matrix number bricks: host glyphs on Alice's white background; name 矩阵数字砖.
- [x] Right-click matrix bricks 0–15 and number lamps 0–32 to increment and wrap; drops retain state. No number-stone interaction.
- [x] One Alice-style Laver wood/plank/leaf family across both worlds; remove duplicate old art.
- [x] Alice Christmas logs become oak / dark oak; dread timber adopts oak grain and keeps its existing palette.
- [x] Refine the existing Guogao slice icon.
- [x] One 空境玻璃 with the host absence-glass art and behavior.
- [x] Three portal designs selected by destination: surface/home, inner, underworld. Return gates use their actual destination appearance.
- [x] Outer Christmas trees use host single-block blank colored lamps.
- [x] All retained Alice blocks/items register under the host `googology` namespace and appear in the host creative tabs; no `googology_outer` item attribution.
- [x] Resource/localization/registry tests and four-version builds.
- [x] Isolated hidden fo262 runtime/visual verification.
- [x] Installed into fo262 after checking Minecraft was closed; previous 0.3.3 backed up, other55 files unchanged.

Internal worldgen/template namespaces are implementation details, not separate player-facing mods. Do not retune terrain, progression or crystal effects in this pass.

Validation: four release builds; hidden 26.2 run with416 assertions,61 templates and three actual configured-feature placements;8 Python regression tests; bilingual/resource audits. See [VALIDATION.md](VALIDATION.md).

Installed `googology-dimension-26.2-0.3.4.jar`, SHA-256 `57cbf60ad38a99aab55ee630ee91cf04c6dee32bb065868fc799b6c19bf988dc`. Previous JAR is in the local ignored directory `build/backups/fo262-before-0.3.4`. Development branch: `codex/0.3.4-unified-materials`; not published to GitHub.
