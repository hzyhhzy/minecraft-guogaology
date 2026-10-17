# 0.5.12: compatibility with Alice0.5.4.1(26.3)

This is a static addendum to `SAVE-COMPAT-0541-0511.md`, not a26.3runtime test.

No existing block/item, dimension, entity, component, recipe or advancement ID is removed. `guogao_drop` is additive. `hydra_sprig` gains a default-false`hanging`property; this raw material was introduced after0.5.4.1, so that target save has no old Sprig states to convert. Existing formatted core stages/components and outer imported registrations are unchanged.

Old saved terrain, stars, hanging cloud materials, office chairs and landmark blocks are not scanned or rewritten. New pearls/chairs/starlights/sprigs/hidden drops appear through new generation. Textures and AstraLv1appearance update for already placed blocks; unchanged IDs still resolve. New crafting and ungenerated chest loot use the current recipes/data after update.

The eight finale tributes and Laver palace layout remain. Giant emoji globes gain a raw Drop only on new blueprint generation, at the exact formerLv1Heart center. This does not fill or obstruct the top combat chamber.

No new difficult0.5.4.1save migration is introduced by this patch. All four targets compile; actual gameplay validation remains limited to isolated hiddenfo262(26.2).
