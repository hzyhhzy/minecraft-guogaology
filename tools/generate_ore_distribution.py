"""Current four-ore placement policy, overlaid on the imported Alice resources.

Only candidate counts, air rejection and Gamma/Omega heights are owned here.
Vein sizes, replaceable blocks and biome membership stay in their source data.
The vanilla bottom-biased provider keeps every attempt inside the world instead
of discarding the below-bedrock half of a diamond-style triangle.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COUNTS = {'omega': 24, 'epsilon': 12, 'gamma': 6, 'true_omega': 2}
TOPS = {'gamma': 56, 'true_omega': 16}


def apply_policy(folder, name, value):
    """Also called after every version's donor-resource conversion."""
    mineral = name.removeprefix('ore_').removesuffix('_hell')
    if not name.startswith('ore_') or mineral not in COUNTS:
        return value
    if folder in ('configured_feature', 'feature'):
        value.get('config', value)['discard_chance_on_air_exposure'] = 0.5
    elif folder == 'placed_feature':
        counts = [p for p in value['placement'] if p['type'] == 'minecraft:count']
        assert len(counts) == 1, name
        counts[0]['count'] = COUNTS[mineral]
        if mineral in TOPS:
            heights = [p for p in value['placement'] if p['type'] == 'minecraft:height_range']
            assert len(heights) == 1, name
            heights[0]['height'] = {
                'type': 'minecraft:biased_to_bottom',
                'min_inclusive': {'absolute': -64},
                'max_inclusive': {'absolute': TOPS[mineral]},
                'inner': 1,
            }
    return value


def generate(root=ROOT, *, check=False):
    worldgen = Path(root) / 'src/main/resources/data/googology/worldgen'
    changed = []
    for mineral in COUNTS:
        for suffix in ('', '_hell'):
            name = f'ore_{mineral}{suffix}.json'
            for folder in ('placed_feature', 'configured_feature'):
                path = worldgen / folder / name
                old = json.loads(path.read_text(encoding='utf8'))
                value = json.loads(json.dumps(old))
                apply_policy(folder, Path(name).stem, value)
                if value != old:
                    changed.append(path)
                    if not check:
                        path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(f'Ore distribution: {len(changed)} files ' + ('would change' if check else 'changed'))
    return changed


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    changed = generate(check=args.check)
    if args.check and changed:
        raise SystemExit(1)
