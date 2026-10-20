"""Bound both repair lines to the reviewed JEI, item-display and native wood fixes."""
from __future__ import annotations

import argparse
from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile

from audit_jei_patch_delta import COMPAT, TARGETS, normalized_entries
from generate_item_displays import ITEMS, DISPLAY
from package_multiversion import verify_wood_loot_policy
from prepare_sign_resources import FAMILIES, MODELS, LEGACY_TARGETS, verify_archive

WOOD_CLASS = 'dev/guogaology/outer/registry/ModWoodDecorBlocks.class'
ASSETS = 'assets/guogaology/'


def allowed_changes(target):
    names = {ASSETS + f'models/item/{item}.json' for item in ITEMS}
    names |= {'data/guogaology/loot_table/blocks/laver_' + shape + '.json' for shape in ('door', 'slab')}
    names.add(('META-INF/jars/guogaology-outer-legacy-<release>.jar!/' if target == '1.21.1' else '') + WOOD_CLASS)
    if target == '1.21.1':
        names |= {ASSETS + f'items/{item}.json' for item in ITEMS if item != 'gummy_jar'}
    if target in LEGACY_TARGETS:
        names |= {ASSETS + f'models/block/{family}_{model}.json' for family in FAMILIES for model in MODELS}
        names |= {ASSETS + f'textures/entity/signs/{family}.png' for family in FAMILIES}
    if target == '26.3':
        names |= {COMPAT + name + '.class' for name in ('JeiTagCompatPlugin', 'JeiTagCompatShape', 'JeiTagCompatShape$Flow')}
    return names


def compare(old, new, prefix='', formatting=None):
    changes = []
    for name in sorted(old.keys() | new.keys()):
        before, after = old.get(name), new.get(name)
        path = prefix + name
        if isinstance(before, dict) and isinstance(after, dict):
            changes.extend(compare(before, after, path + '!/', formatting))
        elif before != after:
            if name.endswith('.json') and before is not None and after is not None:
                # Metadata is already normalized by normalized_entries. Record
                # semantically identical JSON separately instead of whitelisting
                # whole resource directories or silently accepting new fields.
                if json.loads(before) == json.loads(after):
                    if formatting is not None:
                        formatting.append(path)
                    continue
            changes.append(path)
    return changes


def verify_item_resources(old, new, target):
    for item in ITEMS:
        path = ASSETS + f'models/item/{item}.json'
        before, after = json.loads(old[path]), json.loads(new[path])
        for context, expected in DISPLAY.items():
            assert after['display'][context] == expected, f'Wrong item transform: {target}/{item}/{context}'
        preserved = json.loads(json.dumps(after))
        preserved['display'] = {key: value for key, value in preserved['display'].items() if key not in DISPLAY}
        if not preserved['display']:
            preserved.pop('display')
        original = json.loads(json.dumps(before))
        if 'display' in original:
            original['display'] = {key: value for key, value in original['display'].items() if key not in DISPLAY}
            if not original['display']:
                original.pop('display')
        assert original == preserved, f'Item geometry or GUI changed: {target}/{item}'
        if target == '1.21.1' and item != 'gummy_jar':
            definition = ASSETS + f'items/{item}.json'
            expected = json.loads(old[definition])
            expected['model']['model'] = 'guogaology:item/' + item
            assert json.loads(new[definition]) == expected


def audit(baseline, candidate, old_version, new_version):
    result = []
    for target in TARGETS:
        old_path = baseline / f'guogaology-dimension-{target}-{old_version}.jar'
        new_path = candidate / f'guogaology-dimension-{target}-{new_version}.jar'
        old = normalized_entries(old_path.read_bytes(), old_version)
        new = normalized_entries(new_path.read_bytes(), new_version)
        formatting = []
        changed = compare(old, new, formatting=formatting)
        assert set(changed) == allowed_changes(target), {
            'target': target, 'unexpected': sorted(set(changed) - allowed_changes(target)),
            'missing': sorted(allowed_changes(target) - set(changed)),
        }
        verify_item_resources(old, new, target)
        with ZipFile(BytesIO(new_path.read_bytes())) as archive:
            verify_wood_loot_policy(archive, target)
            verify_archive(archive, target)
        result.append({'minecraft': target, 'baselineVersion': old_version, 'version': new_version,
                       'baselineSha256': sha256(old_path.read_bytes()).hexdigest(),
                       'sha256': sha256(new_path.read_bytes()).hexdigest(),
                       'reviewedChangedEntries': changed, 'jsonFormattingOnly': formatting})
    return {'success': True, 'onlyReviewedRepairsChanged': True,
            'scope': 'Exact entry allowlist; unchanged non-sign world models, source textures, recipes, IDs, loot outside Laver door/slab, and every unrelated Java class, including nested JARs',
            'targets': result}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--candidate', type=Path, required=True)
    parser.add_argument('--old-version', required=True)
    parser.add_argument('--new-version', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = audit(args.baseline, args.candidate, args.old_version, args.new_version)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(json.dumps({'success': True, 'targets': [{'minecraft': row['minecraft'], 'entries': len(row['reviewedChangedEntries'])} for row in result['targets']]}))
