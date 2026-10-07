"""Maintain the shared Underworld biome/density resources; modern ports convert these files."""
from copy import deepcopy
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/guogaology'


def read(path):
    return json.loads(path.read_text('utf8'))


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', 'utf8')


def main():
    base = read(DATA / 'worldgen/biome/guogao_forest.json')
    variants = {
        'misaligned_strata': (0x403B58, 0x141428, 0x283947, 0x192532, 0.002),
        'silent_mire': (0x263D44, 0x111C27, 0x172E35, 0x0C171D, 0.003),
        'descending_caverns': (0x28223C, 0x101020, 0x243340, 0x0C151E, 0.005),
    }
    for name, (fog, sky, water, water_fog, ash) in variants.items():
        biome = deepcopy(base)
        biome['effects'].update(fog_color=fog, sky_color=sky, water_color=water,
                                water_fog_color=water_fog)
        biome['effects']['particle']['probability'] = ash
        # All four entries invoke exactly the same deterministic once-per-chunk
        # feature. Its region-aware pass planner decides which complete objects fit.
        write(DATA / f'worldgen/biome/{name}.json', biome)

    dimension_path = DATA / 'dimension/guogao.json'
    dimension = read(dimension_path)
    dimension['generator']['biome_source'] = {
        'type': 'guogaology:underworld', 'forest': 'guogaology:guogao_forest',
        'strata': 'guogaology:misaligned_strata', 'mire': 'guogaology:silent_mire',
        'descent': 'guogaology:descending_caverns',
    }
    write(dimension_path, dimension)
    noise_path = DATA / 'worldgen/noise_settings/guogao.json'
    noise = read(noise_path)
    noise['noise_router']['temperature']['underworld'] = True
    # Preserve the current Guogao substrate. The cave-surface restoration pass
    # removes loam only from carved internal floors, leaving true exterior skins.
    rules = noise['surface_rule']['sequence']
    forest = next(rule for rule in rules if 'guogaology:guogao_forest' in rule.get('if_true', {}).get('biome_is', []))
    forest['if_true']['biome_is'] = ['guogaology:guogao_forest', 'guogaology:misaligned_strata',
                                    'guogaology:silent_mire', 'guogaology:descending_caverns']
    write(noise_path, noise)


if __name__ == '__main__':
    main()
