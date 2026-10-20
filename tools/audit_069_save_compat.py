"""Read-only four-target 0.6.8 -> 0.6.9 patch/save-surface guard.

This inspects exact archives, including the 1.21.1 nested bridge. It cannot prove
world loading: use the separate Save069 native writer/upgrade/reopen fixture.
"""
from __future__ import annotations

import argparse
from collections import Counter
from hashlib import sha256
import json
from pathlib import Path
import struct

from audit_jei_patch_delta import TARGETS, normalized_entries

ROOT = Path(__file__).resolve().parents[1]
ASSETS = 'assets/guogaology/'
FACING = ('server_rack', 'office_monitor', 'chair', 'nuke_chair', 'snake_head')
ENTITY_KEY = 'entity.guogaology.laver_boat'
COMMON_CLASSES = {
    'dev/guogaology/GuogaologyBlocks.class',
    'dev/guogaology/block/HorizontalDecorBlock.class',
    'dev/guogaology/outer/registry/ModBlocks.class',
    'dev/guogaology/outer/block/ChairBlock.class',
    'dev/guogaology/outer/block/NukeChairBlock.class',
    'dev/guogaology/outer/block/SnakeHeadBlock.class',
}
LEGACY_CLASSES = {
    'dev/guogaology/outer/registry/ModBoats.class',
    'dev/guogaology/outer/registry/ModBoats$WoodenBoat.class',
    'dev/guogaology/outer/item/OuterBoatItem.class',
}
COMBAT_CLASSES = {'dev/guogaology/mining/MiningEffects.class', 'dev/guogaology/mining/BowEffects.class'}


def flat(entries, prefix=''):
    result = {}
    for name, value in entries.items():
        if isinstance(value, dict):
            result.update(flat(value, prefix + name + '!/'))
        else:
            result[prefix + name] = value
    return result


def string_constants(data):
    """Read only CONSTANT_String literals, not method/type metadata."""
    assert data[:4] == b'\xca\xfe\xba\xbe', 'Expected a class file'
    count = int.from_bytes(data[8:10], 'big'); offset = 10; pool = {}; strings = []
    index = 1
    while index < count:
        tag = data[offset]; offset += 1
        if tag == 1:
            size = int.from_bytes(data[offset:offset+2], 'big'); offset += 2
            pool[index] = data[offset:offset+size].decode('utf8', errors='replace'); offset += size
        elif tag == 8:
            strings.append(int.from_bytes(data[offset:offset+2], 'big')); offset += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): offset += 4
        elif tag in (5, 6): offset += 8; index += 1
        elif tag in (7, 16, 19, 20): offset += 2
        elif tag == 15: offset += 3
        else: raise AssertionError('Unknown class constant tag: ' + str(tag))
        index += 1
    return Counter(pool[key] for key in strings)


def scope_allowed(target):
    result = {ASSETS + f'lang/{locale}.json' for locale in ('zh_cn', 'en_us')}
    result |= {ASSETS + f'blockstates/{name}.json' for name in FACING}
    for name in COMMON_CLASSES:
        prefix = 'META-INF/jars/guogaology-outer-legacy-<release>.jar!/' if target == '1.21.1' and '/outer/' in name else ''
        result.add(prefix + name)
    if target == '1.21.1':
        result |= {'META-INF/jars/guogaology-outer-legacy-<release>.jar!/' + name for name in LEGACY_CLASSES}
    if target == '26.3': result |= COMBAT_CLASSES
    return result


def states(entries):
    prefix = ASSETS + 'blockstates/'
    return {name[len(prefix):-5]: json.loads(data) for name, data in entries.items()
            if name.startswith(prefix) and name.endswith('.json')}


def check_four_way(name, data):
    expected = {f'facing={facing}': {'model': 'guogaology:block/' + name, **({'y': y} if y else {})}
                for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
    assert data == {'variants': expected}, f'Unexpected four-way model or changed geometry: {name}'


def archive(path, version):
    return flat(normalized_entries(path.read_bytes(), version))


def identity_surfaces(entries):
    prefixes = (ASSETS + 'blockstates/', ASSETS + 'models/item/', 'data/guogaology/dimension/',
                'data/guogaology/dimension_type/', 'data/guogaology/worldgen/biome/')
    return {prefix: sorted(name[len(prefix):] for name in entries if name.startswith(prefix)) for prefix in prefixes}


def historical(old, current, target):
    a, b = archive(old, '0.6.0'), archive(current, '0.6.8')
    ia, ib = identity_surfaces(a), identity_surfaces(b)
    deleted = {key: sorted(set(ia[key]) - set(ib[key])) for key in ia}
    assert not any(deleted.values()), f'0.6.0 identity surface removed: {target}/{deleted}'
    dimensions = [name for name in a if name.startswith(('data/guogaology/dimension/', 'data/guogaology/dimension_type/'))]
    assert all(a[name] == b[name] for name in dimensions), f'0.6.0 dimension codec data drift: {target}'
    codecs = ['dev/guogaology/mining/MiningContent.class', 'dev/guogaology/portal/PortalState.class',
              'dev/guogaology/mining/BowVolleyLedger.class']
    unchanged = []
    for name in codecs:
        assert a[name] == b[name], f'0.6.0 persisted component/ledger class changed: {target}/{name}'
        unchanged.append(name)
    return {'minecraft': target, 'oldVersion': '0.6.0', 'oldSha256': sha256(old.read_bytes()).hexdigest(),
            'removedIdentitySurfaces': deleted, 'blockStateResourcesIdentical': states(a) == states(b),
            'dimensionCodecResourcesIdentical': True, 'persistentComponentAndLedgerClassesIdentical': unchanged,
            'scope': 'Static same-Minecraft-version compatibility surface only; does not claim a fresh 0.6.0 native upgrade run.'}


def audit(baseline, candidate, old_version='0.6.8', new_version='0.6.9', historical_root=None):
    rows = []; historical_rows = []
    for target in TARGETS:
        old = baseline / f'guogaology-dimension-{target}-{old_version}.jar'
        new = candidate / f'guogaology-dimension-{target}-{new_version}.jar'
        a, b = archive(old, old_version), archive(new, new_version)
        allowed = scope_allowed(target); changed = []; json_formatting = []
        for name in sorted(a.keys() | b.keys()):
            if a.get(name) == b.get(name): continue
            # World/item geometry, source pixels, data IDs, recipes, loot, tags,
            # metadata defaults and all codecs are byte-exact, even whitespace.
            if name not in allowed:
                raise AssertionError(f'Unexpected production entry changed: {target}/{name}')
            changed.append(name)
        assert ASSETS + 'lang/zh_cn.json' in changed and ASSETS + 'lang/en_us.json' in changed, 'Boat entity translations missing'
        for locale, expected in (('zh_cn', '紫菜木船'), ('en_us', 'Laver Boat')):
            name = ASSETS + f'lang/{locale}.json'; before, after = json.loads(a[name]), json.loads(b[name])
            assert ENTITY_KEY not in before and after.pop(ENTITY_KEY) == expected, f'Wrong new boat translation: {target}/{locale}'
            assert before == after, f'Unrelated localization changed: {target}/{locale}'
        old_states, new_states = states(a), states(b)
        assert old_states.keys() == new_states.keys(), f'Block ID set changed: {target}'
        for name in FACING: check_four_way(name, new_states[name])
        for name in old_states.keys() - set(FACING):
            assert old_states[name] == new_states[name], f'Unrelated block state changed: {target}/{name}'
        # Changing the registry implementation must not remove any old literal ID.
        registry_strings = []
        for name in (p for p in a if p.endswith('/GuogaologyBlocks.class') or p.endswith('/outer/registry/ModBlocks.class')):
            lost = set(string_constants(a[name])) - set(string_constants(b[name]))
            assert not lost, f'Registry string literal removed: {target}/{name}/{sorted(lost)}'
            registry_strings.append(name)
        assert 'dev/guogaology/GuogaologyBlocks.class' in changed, 'Device constructor repair is absent'
        prefix = 'META-INF/jars/guogaology-outer-legacy-<release>.jar!/' if target == '1.21.1' else ''
        assert prefix + 'dev/guogaology/outer/registry/ModBlocks.class' in changed, 'Stripping mappings absent'
        # NukeChairBlock inherits the repaired placement path from ChairBlock;
        # recompilation is allowed but a binary change is not itself required.
        for name in ('ChairBlock', 'SnakeHeadBlock'):
            assert prefix + 'dev/guogaology/outer/block/' + name + '.class' in changed, 'Directional placement repair absent: ' + name
        if target == '26.3': assert COMBAT_CLASSES <= set(changed), 'Both extra-hit cooldown repairs are required'
        else: assert not (COMBAT_CLASSES & set(changed)), '26.3 cooldown rewrite leaked to another native version'
        if target == '1.21.1':
            assert prefix + 'dev/guogaology/outer/registry/ModBoats$WoodenBoat.class' in changed, 'Legacy hull fall repair absent'
            assert prefix + 'dev/guogaology/outer/item/OuterBoatItem.class' in changed, 'Legacy named placement repair absent'
        assert identity_surfaces(a) == identity_surfaces(b), f'Persistent/resource ID sets drifted: {target}'
        rows.append({'minecraft': target, 'baselineVersion': old_version, 'version': new_version,
                     'baselineSha256': sha256(old.read_bytes()).hexdigest(), 'sha256': sha256(new.read_bytes()).hexdigest(),
                     'reviewedChangedEntries': changed, 'registeredBlockIdsRetained': len(old_states),
                     'registryLiteralClassesPreserved': registry_strings, 'modelsTexturesRecipesLootTagsAndPersistedCodecsUnchanged': True,
                     'legacyMissingFacingPolicy': 'Old rack/monitor palettes omit facing; the new block must default NORTH. Native fixture must prove this on disk.'})
        if historical_root:
            historical_rows.append(historical(historical_root / f'guogaology-dimension-{target}-0.6.0.jar', old, target))
    return {'success': True, 'scope': 'Narrow recursive archive guard, no game execution or world modification.',
            'nativeEvidenceRequired': 'Use exact068 writer ->069 upgrade ->069 second reopen with clean server stops; static data retention is not native save certification.',
            'targets': rows, 'historical060': historical_rows,
            'irrecoverableOldDataBoundary': 'A legacy boat name already lost before saving cannot be reconstructed; saved names and new named item placements must survive.'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--candidate', type=Path, required=True)
    parser.add_argument('--old-version', default='0.6.8')
    parser.add_argument('--new-version', default='0.6.9')
    parser.add_argument('--historical-060', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = audit(args.baseline, args.candidate, args.old_version, args.new_version, args.historical_060)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(json.dumps({'success': True, 'targets': [{'minecraft': row['minecraft'], 'entries': len(row['reviewedChangedEntries'])} for row in result['targets']]}))
