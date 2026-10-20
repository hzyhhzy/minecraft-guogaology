"""Conserve the placed item count of the two registered wooden door/slab families.

The donor's unconditional self-drop tables are immutable provenance inputs. Host
tables own these fixes, and the final Outer overlay must keep them on all targets.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FAMILIES = ('laver', 'dread')
STRUCTURAL_SHAPES = ('door', 'slab')
TABLES = tuple(f'{family}_{shape}' for family in FAMILIES for shape in STRUCTURAL_SHAPES)


def wood_loot(family, shape):
    """Legacy schema, including the existing Dread carpentry/sign conventions."""
    if family not in FAMILIES:
        raise ValueError('Unknown registered wood family: ' + family)
    name = family + '_' + shape
    item = family + '_sign' if shape == 'wall_sign' else name
    conditions = [{'condition': 'minecraft:survives_explosion'}]
    if shape == 'door':
        conditions.append({'condition': 'minecraft:block_state_property',
                           'block': 'guogaology:' + name, 'properties': {'half': 'lower'}})
        conditions = [{'condition': 'minecraft:all_of', 'terms': conditions}]
    functions = []
    if shape == 'slab':
        functions = [{'function': 'minecraft:set_count', 'count': 2,
                      'conditions': [{'condition': 'minecraft:block_state_property',
                                      'block': 'guogaology:' + name, 'properties': {'type': 'double'}}]}]
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1,
            'entries': [{'type': 'minecraft:item', 'name': 'guogaology:' + item, 'functions': functions}],
            'conditions': conditions}]}


def generate(check=False):
    changed = []
    for family in FAMILIES:
        for shape in STRUCTURAL_SHAPES:
            path = ROOT / 'src/main/resources/data/guogaology/loot_table/blocks' / f'{family}_{shape}.json'
            expected = wood_loot(family, shape)
            actual = json.loads(path.read_text('utf8')) if path.exists() else None
            if actual == expected:
                continue
            changed.append(path.relative_to(ROOT).as_posix())
            if not check:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(expected, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    if check and changed:
        raise AssertionError('Stale wooden door/slab loot: ' + ', '.join(changed))
    return changed


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    print('WOOD_LOOT_OK changed=' + str(len(generate(args.check))) + ' check=' + str(args.check))
