"""Small explicit recipe/loot repairs, applied after immutable donor imports."""
import argparse
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECIPE = Path('data/guogaology/recipe/tnt_from_nuke_mushroom.json')
LOOT = Path('data/guogaology/loot_table/blocks/gummy_jar.json')


def tnt_recipe():
    # Use the native sand tag: ordinary sand, red sand and their mixed grids.
    return {'type': 'minecraft:crafting_shaped', 'category': 'redstone',
            'key': {'N': {'item': 'guogaology:nuke_mushroom'}, '#': {'tag': 'minecraft:sand'}},
            'pattern': ['N#N', '#N#', 'N#N'], 'result': {'id': 'minecraft:tnt'}}


def gummy_jar_loot():
    silk = {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {
        'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}
    # Retain the previous ordinary 1-3 rolls, with an exclusive Silk Touch branch.
    return {'type': 'minecraft:block', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'guogaology:gummy_jar'}],
         'conditions': [copy.deepcopy(silk)]},
        {'rolls': {'type': 'minecraft:uniform', 'min': 1, 'max': 3},
         'entries': [{'type': 'minecraft:item', 'name': 'guogaology:gummy'}],
         'conditions': [{'condition': 'minecraft:inverted', 'term': silk}]}]}


def resources(target):
    recipe, loot = tnt_recipe(), gummy_jar_loot()
    if target != '1.21.1':
        from prepare_port_resources import ingredient
        recipe['key'] = {k: ingredient(v) for k, v in recipe['key'].items()}
    if target == '26.3':
        from prepare_port_resources import convert_263_loot
        loot = convert_263_loot(loot)
    return {RECIPE: recipe, LOOT: loot}


def apply_pack(out, target):
    from prepare_outer_resources import recipe_unlocks
    out = Path(out)
    for rel, value in resources(target).items():
        path = out / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    recipe_unlocks(out / 'data/guogaology', [out / RECIPE], target)


def generate(check=False):
    source = ROOT / 'src/main/resources'
    changed = []
    for rel, value in resources('1.21.1').items():
        path = source / rel
        if json.loads(path.read_text(encoding='utf-8')) != value:
            changed.append(str(rel))
    if check:
        if changed:
            raise AssertionError('Stale TNT/gummy-jar policy: ' + ', '.join(changed))
    else:
        apply_pack(source, '1.21.1')
    return changed


def verify_archive(archive, target):
    for rel, value in resources(target).items():
        assert json.loads(archive.read(rel.as_posix())) == value, f'Overwritten small gameplay policy: {target}/{rel}'


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    print('SMALL0610_RESOURCES_OK changed=' + str(len(generate(args.check))))
