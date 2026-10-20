"""Four-way furniture state selection; original north and item models stay intact."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/guogaology'
BLOCKS = ('chair', 'nuke_chair', 'snake_head', 'server_rack', 'office_monitor')
ROTATIONS = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def blockstate(name):
    if name not in BLOCKS:
        raise ValueError('Not horizontal furniture: ' + name)
    return {'variants': {
        'facing=' + direction: {'model': 'guogaology:block/' + name, **({'y': angle} if angle else {})}
        for direction, angle in ROTATIONS.items()
    }}


def generate(check=False, assets=ASSETS):
    changed = []
    for name in BLOCKS:
        path = assets / 'blockstates' / (name + '.json')
        value = blockstate(name)
        if json.loads(path.read_text('utf8')) == value:
            continue
        changed.append(path)
        if not check:
            path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    if check and changed:
        raise SystemExit('Stale horizontal furniture states: ' + ', '.join(str(p) for p in changed))
    print(f'DECOR_ORIENTATION_OK blocks={len(BLOCKS)} changed={len(changed)} check={check}')
    return changed


def verify_archive(archive, target):
    """Check actual final resources, including item geometry, after every importer."""
    for name in BLOCKS:
        stem = 'assets/guogaology/'
        actual = json.loads(archive.read(stem + 'blockstates/' + name + '.json'))
        assert actual == blockstate(name), f'{target}: stale horizontal furniture state {name}'
        # No model rotation/display is baked into the art: old states lacking
        # facing decode to north, and north still selects the old unrotated model.
        for kind in ('block', 'item'):
            relative = f'models/{kind}/{name}.json'
            expected = json.loads((ASSETS / relative).read_text('utf8'))
            if target == '1.21.1':
                from prepare_legacy_resources import adapt
                expected = adapt(expected)
            assert json.loads(archive.read(stem + relative)) == expected, f'{target}: changed furniture {kind} model {name}'
        if target != '1.21.1':
            item = json.loads(archive.read(stem + 'items/' + name + '.json'))
            assert item == {'model': {'type': 'minecraft:model', 'model': 'guogaology:item/' + name}}, f'{target}: changed furniture item selection {name}'


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    generate(parser.parse_args().check)
