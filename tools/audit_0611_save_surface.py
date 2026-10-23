"""Compare exact 0.6.10/0.6.11 archives; this is not a native save-load test.

All saved identities, blockstates, data, geometry, pixels and unrelated classes
must remain byte-identical. Only the declared combat/UI changes are permitted.
Nested legacy JARs are compared recursively with version metadata normalized.
"""
from __future__ import annotations

import argparse
from hashlib import sha256
import json
from pathlib import Path

from audit_069_save_compat import archive, states, identity_surfaces, string_constants
from audit_jei_patch_delta import TARGETS

ROOT = Path(__file__).resolve().parents[1]
COMBAT = {'EquipmentRules', 'BowEffects', 'GearData', 'OrdinalGear', 'ManuscriptAttackOverflowMixin'}
REVISION_ONLY = {'EnhancementMenu', 'OrdinalBowItem', 'OrdinalArmor'}
LANGUAGES = {f'assets/guogaology/lang/{locale}.json' for locale in ('zh_cn', 'en_us')}
PROTECTED_STEMS = (
    '/MiningContent', '/BowShotData', '/BowVolleyData', '/BowVolleyLedger',
    '/CoreProjectileMixin', '/BowEntityRemovalMixin', '/Portal',
    '/Lho', '/LHO', '/Absence', '/Anchored', '/OrdinalAnchoring',
    '/VoidFragment', '/Stability', '/Jei', '/RecipeSyncCompat',
)


def leaf_class(name):
    return name.rsplit('/', 1)[-1].removesuffix('.class').split('$', 1)[0]


def revision_only(before, after):
    """Unedited carriers inline REVISION as a bipush/int constant, 49 -> 50."""
    if len(before) != len(after):
        return False
    changes = [(a, b) for a, b in zip(before, after) if a != b]
    return bool(changes) and all(a == 49 and b == 50 for a, b in changes)


def audit(baseline, candidate):
    rows = []
    for target in TARGETS:
        old = baseline / f'guogaology-dimension-{target}-0.6.10.jar'
        new = candidate / f'guogaology-dimension-{target}-0.6.11.jar'
        a, b = archive(old, '0.6.10'), archive(new, '0.6.11')
        added = {'dev/guogaology/mixin/ManuscriptAttackOverflowMixin.class'}
        assert b.keys() - a.keys() == added and not a.keys() - b.keys(), f'Unexpected archive identity change: {target}'
        sa, sb = states(a), states(b)
        assert len(sa) == len(sb) == 230, f'Unexpected registered block count: {target}'
        assert sa == sb, f'Blockstate identity/properties/models changed: {target}'
        assert identity_surfaces(a) == identity_surfaces(b), f'Saved resource IDs changed: {target}'
        data = [name for name in a if name.startswith('data/') or '!/data/' in name]
        assert all(a[name] == b[name] for name in data), f'Data/recipe/loot/tag/worldgen drift: {target}'
        blockstates = [name for name in a if '/blockstates/' in name]
        assert all(a[name] == b[name] for name in blockstates), f'Blockstate byte drift: {target}'
        models_pixels = [name for name in a if any(part in name for part in
                         ('/models/', '/items/', '/textures/', '/core_meshes/'))]
        assert all(a[name] == b[name] for name in models_pixels), f'Geometry/item view/pixel drift: {target}'
        protected = [name for name in a if name.endswith('.class') and
                     any(stem in name for stem in PROTECTED_STEMS)]
        for name in protected:
            assert a[name] == b[name], f'Codec, arrow lifecycle, portal, LHO or JEI class changed: {target}/{name}'
        for family in ('MiningContent', 'BowShotData', 'CoreProjectileMixin', 'BowVolleyLedger'):
            assert any(name.endswith('/' + family + '.class') for name in protected), f'Missing protected class: {target}/{family}'
        changed = []
        inlined = []
        for name in sorted(a.keys() | b.keys()):
            if a.get(name) == b.get(name):
                continue
            if name in LANGUAGES:
                before, after = json.loads(a[name]), json.loads(b[name])
                assert before.keys() <= after.keys(), f'Old translation keys removed: {target}/{name}'
                assert after.keys() - before.keys() == {'mining.guogaology.bow_damage_coefficient'}, f'Unexpected new UI identity: {target}/{name}'
            elif name == 'guogaology.mixins.json':
                before, after = json.loads(a[name]), json.loads(b[name])
                assert after['mixins'].count('ManuscriptAttackOverflowMixin') == 1
                after['mixins'].remove('ManuscriptAttackOverflowMixin')
                assert before == after, f'Unrelated mixin registration changed: {target}'
            elif name.endswith('refmap.json'):
                before, after = json.loads(a[name]), json.loads(b[name])
                key = 'dev/guogaology/mixin/ManuscriptAttackOverflowMixin'
                assert key not in before['mappings'] and key in after['mappings']
                expected = after['mappings'].pop(key)
                assert set(expected) == {'attack', 'Lnet/minecraft/entity/player/PlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D'}
                for namespace in after.get('data', {}).values():
                    assert namespace.pop(key) == expected
                assert before == after, f'Old refmap entries changed: {target}/{name}'
            elif name.endswith('.class'):
                leaf = leaf_class(name)
                if leaf in REVISION_ONLY:
                    assert revision_only(a[name], b[name]), f'Carrier change is not solely inlined REVISION49 -> 50: {target}/{name}'
                    inlined.append(name)
                else:
                    assert leaf in COMBAT or leaf == 'EnhancementScreen', f'Unrelated production class changed: {target}/{name}'
            else:
                raise AssertionError(f'Unrelated production resource changed: {target}/{name}')
            changed.append(name)
        for family in ('EquipmentRules', 'BowEffects', 'GearData', 'EnhancementScreen'):
            assert any(name.endswith('/' + family + '.class') for name in changed), f'Expected combat/UI change absent: {target}/{family}'
        registry = [name for name in a if name.endswith((
            '/GuogaologyBlocks.class', '/outer/registry/ModBlocks.class',
            '/outer/registry/ModWoodDecorBlocks.class', '/outer/registry/ModBoats.class'))]
        for name in registry:
            assert a[name] == b[name], f'Registry implementation changed: {target}/{name}'
            assert string_constants(a[name]) == string_constants(b[name])
        rows.append({
            'minecraft': target, 'baselineSha256': sha256(old.read_bytes()).hexdigest(),
            'sha256': sha256(new.read_bytes()).hexdigest(), 'blockIds': len(sa),
            'dataEntriesByteIdentical': len(data), 'blockstatesByteIdentical': len(blockstates),
            'modelsTexturesItemViewsByteIdentical': len(models_pixels),
            'protectedClassesByteIdentical': protected, 'registryClassesByteIdentical': registry,
            'reviewedChangedEntries': changed, 'strictRevisionOnlyCarrierChanges': inlined,
            'addedLocalAttackReadMixin': sorted(added),
        })
    return {'passed': True, 'baseline': '0.6.10', 'candidate': '0.6.11', 'targets': rows,
            'scope': 'Static exact archive identity/data/codecs/arrow lifecycle/LHO/JEI retention and narrow combat/UI delta; not native save-load certification.',
            'oldArrowPolicy': 'Stored native base damage and existing BowShotData final bonuses remain readable unchanged; only newly launched arrows use the new split coefficient/book calculation.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', type=Path, default=ROOT / 'build/releases/0.6.10')
    parser.add_argument('--candidate', type=Path, default=ROOT / 'build/releases/0.6.11')
    parser.add_argument('--output', type=Path, default=ROOT / 'build/combat-0611/save-surface.json')
    args = parser.parse_args()
    try:
        result = audit(args.baseline, args.candidate)
    except Exception as error:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps({'passed': False, 'error': str(error)}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        raise
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('SAVE_SURFACE_0611_OK', len(result['targets']))


if __name__ == '__main__':
    main()
