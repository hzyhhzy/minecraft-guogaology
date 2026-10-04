# Guogaology development

- Current release: 0.3.0. Chinese name 果糕逻辑; English name Guogaology. Preserve internal `googology` IDs, commands and existing save semantics.
- Maintain four independent Fabric targets: Minecraft 1.21.1, 1.21.11, 26.2, 26.3. All changes must compile across the four. Modern APIs belong in `ports/common` or explicit target overrides, not generated output.
- Runtime checks are currently limited to Minecraft 26.2 in an isolated hidden instance; do not capture the user's mouse or change normal saves/configuration. Other versions are compile/resource checked only unless the user expands test scope.
- Keep the repository free of historical art alternatives, retired systems, temporary add-ons, build caches, launcher data and game saves.
- Preserve all live blockstate/item variants. Run `python tools/audit_resources.py --check`; numeric filenames are not proof that a resource is obsolete.
- The pixel logo contains only capital Ω and the Guogao expression over indigo background. The entire face is 80% of the previous size and centred in Omega's upper loop. Its contour and palette borrow the giant Christmas-tree globe's `MosaicMotifs`; the user then requested fewer pixels. Sample the face at 30×30 and enlarge with nearest-neighbour to 60×60 in the 128×128 icon. Source: `tools/generate_brand.py`.
- Current gameplay and naming are documented in `docs/SURVIVAL-GUIDE.md`, `docs/BLOCK-BALANCE.md` and `docs/TERMINOLOGY.md`. Do not restore retired creatures, bosses or systems from old prototypes.
