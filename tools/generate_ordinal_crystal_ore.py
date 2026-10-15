"""Ordinal Crystal ore uses the three vanilla iron-placement batches.

Benchmark: Minecraft 26.2 data/minecraft/worldgen/{configured,placed}_feature/
ore_iron*.json. The vanilla ore feature, vein sizes, attempt counts, height
providers, biome filter and air exposure behavior are retained. Only host-rock
targets and ore output states differ. No outer biome is edited.
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
FEATURES = tuple('guogaology:ore_ordinal_crystal_' + batch for batch in ('upper', 'middle', 'small'))
HOSTS = {
    'ordinal_crystal_ore_replaceables': ['guogaology:ordinal_stone' + (f'_{i}' if i else '') for i in range(10)],
    'empty_crystal_ore_replaceables': ['guogaology:absence_glass'],
    'dread_crystal_ore_replaceables': ['guogaology:rootbound_stone'],
}
BATCHES = {
    'upper': (90, 'trapezoid', {'absolute': 80}, {'absolute': 384}, 9),
    'middle': (10, 'trapezoid', {'absolute': -24}, {'absolute': 56}, 9),
    'small': (10, 'uniform', {'above_bottom': 0}, {'absolute': 72}, 4),
}


def ore_config(size):
    return {'type': 'minecraft:ore', 'config': {
        'discard_chance_on_air_exposure': 0.0, 'size': size,
        'targets': [{'state': {'Name': 'guogaology:' + ore},
                     'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'guogaology:' + host}}
                    for ore, host in zip(ORE_IDS, HOSTS)]}}


def placed_config(batch):
    count, provider, low, high, size = BATCHES[batch]
    return {'feature': 'guogaology:ore_ordinal_crystal' + ('_small' if size == 4 else ''),
            'placement': [{'type': 'minecraft:count', 'count': count},
                          {'type': 'minecraft:in_square'},
                          {'type': 'minecraft:height_range', 'height': {
                              'type': 'minecraft:' + provider,
                              'max_inclusive': high, 'min_inclusive': low}},
                          {'type': 'minecraft:biome'}]}


def owned_resources():
    resources = {
        'worldgen/configured_feature/ore_ordinal_crystal.json': ore_config(9),
        'worldgen/configured_feature/ore_ordinal_crystal_small.json': ore_config(4),
        'tags/block/ordinal_crystal_ores.json': {'replace': False, 'values': ['guogaology:' + ore for ore in ORE_IDS]},
    }
    resources.update({f'tags/block/{name}.json': {'replace': False, 'values': values} for name, values in HOSTS.items()})
    resources.update({f'worldgen/placed_feature/ore_ordinal_crystal_{batch}.json': placed_config(batch) for batch in BATCHES})
    return resources


def add_biome_features(biome):
    value = deepcopy(biome)
    features = value.setdefault('features', [])
    while len(features) <= 6:
        features.append([])
    # An exact once-per-step list avoids accidental double ore density after
    # an Underworld variant is regenerated from its forest parent.
    features[6] = [feature for feature in features[6] if feature not in FEATURES] + list(FEATURES)
    return value


def generate(resource_root=None, *, check=False):
    resources = Path(resource_root) if resource_root is not None else ROOT / 'src/main/resources'
    data = resources / 'data/guogaology'
    files = {data / name: value for name, value in owned_resources().items()}
    for name in BIOMES:
        path = data / f'worldgen/biome/{name}.json'
        files[path] = add_biome_features(json.loads(path.read_text(encoding='utf-8')))
    ores = data / 'tags/block/ores.json'
    value = json.loads(ores.read_text(encoding='utf-8')) if ores.exists() else {'replace': False, 'values': []}
    value['values'] = list(dict.fromkeys(value.get('values', []) + ['#guogaology:ordinal_crystal_ores']))
    files[ores] = value
    changed = []
    for path, value in files.items():
        old = json.loads(path.read_text(encoding='utf-8')) if path.exists() else None
        if old == value:
            continue
        changed.append(path)
        if not check:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'ORDINAL_CRYSTAL_ORE_055_OK changed={len(changed)} check={check} biomes={len(BIOMES)} vanillaIronBatches=3')
    if check and changed:
        raise SystemExit('\n'.join(str(path) for path in changed))
    return changed


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    generate(check=parser.parse_args().check)
