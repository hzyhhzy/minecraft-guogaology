# 0.3.3 — outer / inner world merger

Baseline: `bec510e`, preserved on `codex/backup-0.3.2-before-world-merge`.
Implementation branch: `codex/0.3.3-outer-inner-worlds`.

The user's 2026-10-05 implementation request supersedes the earlier proposal's active manuscript, focus meter, quotas, separate Den tool, six ores and exponential mining gate.

## Delivery checklist

- [x] Preserve baseline and create implementation branch.
- [x] Add the natural-scale outer world, keeping the existing inner world and Guogao underworld.
- [x] Import the authorized four ore/material/equipment appearances and characteristic outer scenery; unify shared materials and number blocks.
- [x] Keep ordinary imported creatures in the outer world; omit Hydra and all bosses.
- [x] Four accessible ore tiers, ordinary mining; no natural ores in inner world / underworld.
- [x] Four modest equipment tiers; dimension-dependent, additive-input core bonuses.
- [x] Passive offhand Denxi Manuscript: no charges, focus meter, quota, active guard or extra action key.
- [x] Reversible core installation, passive flight / protection / rescue, safe removal of effects.
- [x] Three-world travel using the existing responsive portal implementation.
- [x] Recipes, loot, creative tabs, English / Chinese, and player documentation agree.
- [x] Compile all four supported versions; runtime checks only in an isolated hidden Minecraft 26.2 instance.
- [x] Record implemented choices requiring user review, plus actual verification results.

Do not modify the user's normal saves or launcher configuration. Do not claim a completed checkbox until verified.

## Latest portal requirement

Overworld cake + apple is retained. Other layers accept local common-block rings;
one additional local block activates a return to the previous layer. No return
path needs a crystal, mineral or an apple. Local material tags are the maintenance
entry points. Ring collapse, immediate visual synchronization and asynchronous
destination preparation remain in place.

## Delivery references

- `OUTER-IMPORT.md`: source provenance, aliases, API adapters and retained terrain/template parameters.
- `SURVIVAL-GUIDE.md` / `.en.md`: actual four-tier recipes, equipment and passive effects.
- `MERGE-REVIEW-CHECKLIST.md`: implemented provisional choices.
- `VALIDATION.md`: four-version compilation, package checks and isolated fo262 evidence.

This branch removes retired ore/item IDs without a migration layer. The existing
inner/underworld dimension IDs and monumental structures remain. The user later
authorized publishing this preview on its own GitHub branch; it is not a main
branch merge or a tagged release.

## Installation

On 2026-10-05 at 09:00 (Asia/Shanghai), the user authorized installation and future
automatic replacement after validation whenever Minecraft is closed. The tested
26.2 JAR was installed into `fo262`; the previous 0.2.26 JAR was backed up first.
Installed SHA-256: `e269a2b85e79e372ebbde303a0d7a3724c909eb884dc6586f03636367d55bf82`.
Exactly one main Mod remains and all 55 other Mod JAR hashes are unchanged.
The local record is `build/installation-0.3.3-26.2-fo262.json`; backups are under
`build/backups/installed-before-0.3.3-26.2-fo262-20261005-090031/`.
