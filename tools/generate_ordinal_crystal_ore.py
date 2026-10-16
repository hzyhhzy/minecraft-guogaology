"""Uniform Ordinal veins, calibrated against iron per volume of host rock.

Use the vanilla small-vein algorithm, not vanilla iron's absolute Y bands:
floating continents have useful rock at entirely different elevations. LHO
receives a separate placed feature with exactly one fifth as many attempts.
"""
from copy import deepcopy
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INNER_BIOMES = ('ordinal_crags', 'power_desert', 'epsilon_meadow', 'lho_absence',
                'laver_tablelands', 'astra_awakening', 'limit_highlands')
UNDERWORLD_BIOMES = ('guogao_forest', 'misaligned_strata', 'silent_mire', 'descending_caverns')
BIOMES = INNER_BIOMES + UNDERWORLD_BIOMES
ORE_IDS = ('ordinal_crystal_ore', 'empty_crystal_ore', 'dread_crystal_ore')
FEATURES = tuple('guogaology:ore_ordinal_crystal_' + batch for batch in ('uniform', 'lho'))
LEGACY_FEATURES = tuple('guogaology:ore_ordinal_crystal_' + batch for batch in ('upper', 'middle', 'small'))
HOSTS = {
    'ordinal_crystal_ore_replaceables': ['guogaology:ordinal_stone' + (f'_{i}' if i else '') for i in range(10)],
    'empty_crystal_ore_replaceables': ['guogaology:absence_glass'],
    'dread_crystal_ore_replaceables': ['guogaology:rootbound_stone'],
}
# Final attempt count is checked against the native fresh-world benchmark.
NORMAL_COUNT = 40
BATCHES = {batch: (count, 'uniform', {'above_bottom': 0}, {'below_top': 0}, 9)
           for batch, count in (('uniform', NORMAL_COUNT), ('lho', NORMAL_COUNT // 5))}
RETIRED = ('worldgen/configured_feature/ore_ordinal_crystal_small.json',) + tuple(
    f'worldgen/placed_feature/ore_ordinal_crystal_{batch}.json' for batch in ('upper', 'middle', 'small'))


def ore_config(size):
    return {'type': 'minecraft:ore', 'config': {
        'discard_chance_on_air_exposure': 0.0, 'size': size,
        'targets': [{'state': {'Name': 'guogaology:' + ore},
                     'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'guogaology:' + host}}
                    for ore, host in zip(ORE_IDS, HOSTS)]}}


def placed_config(batch):
    count, provider, low, high, size = BATCHES[batch]
    return {'feature': 'guogaology:ore_ordinal_crystal',
            'placement': [{'type': 'minecraft:count', 'count': count},
                          {'type': 'minecraft:in_square'},
                          {'type': 'minecraft:height_range', 'height': {
                              'type': 'minecraft:' + provider,
                              'max_inclusive': high, 'min_inclusive': low}},
                          {'type': 'minecraft:biome'}]}


def owned_resources():
    resources = {
        'worldgen/configured_feature/ore_ordinal_crystal.json': ore_config(9),
        'tags/block/ordinal_crystal_ores.json': {'replace': False, 'values': ['guogaology:' + ore for ore in ORE_IDS]},
    }
    resources.update({f'tags/block/{name}.json': {'replace': False, 'values': values} for name, values in HOSTS.items()})
    resources.update({f'worldgen/placed_feature/ore_ordinal_crystal_{batch}.json': placed_config(batch) for batch in BATCHES})
    return resources


def add_biome_features(biome, name):
    value = deepcopy(biome)
    features = value.setdefault('features', [])
    while len(features) <= 6:
        features.append([])
    # An exact once-per-step list avoids accidental double ore density after
    # an Underworld variant is regenerated from its forest parent.
    feature = FEATURES[1 if name == 'lho_absence' else 0]
    features[6] = [entry for entry in features[6] if entry not in FEATURES + LEGACY_FEATURES] + [feature]
    return value


def generate(resource_root=None, *, check=False):
    resources = Path(resource_root) if resource_root is not None else ROOT / 'src/main/resources'
    data = resources / 'data/guogaology'
    files = {data / name: value for name, value in owned_resources().items()}
    for name in BIOMES:
        path = data / f'worldgen/biome/{name}.json'
        files[path] = add_biome_features(json.loads(path.read_text(encoding='utf-8')), name)
    ores = data / 'tags/block/ores.json'
    value = json.loads(ores.read_text(encoding='utf-8')) if ores.exists() else {'replace': False, 'values': []}
    value['values'] = list(dict.fromkeys(value.get('values', []) + ['#guogaology:ordinal_crystal_ores']))
    files[ores] = value
    changed = []
    for relative in RETIRED:
        path = data / relative
        if path.exists():
            changed.append(path)
            if not check:
                path.unlink()
    for path, value in files.items():
        old = json.loads(path.read_text(encoding='utf-8')) if path.exists() else None
        if old == value:
            continue
        changed.append(path)
        if not check:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'ORDINAL_CRYSTAL_ORE_056_OK changed={len(changed)} check={check} biomes={len(BIOMES)} uniformAttempts={NORMAL_COUNT} lhoAttempts={NORMAL_COUNT//5}')
    if check and changed:
        raise SystemExit('\n'.join(str(path) for path in changed))
    return changed


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    generate(check=parser.parse_args().check)
