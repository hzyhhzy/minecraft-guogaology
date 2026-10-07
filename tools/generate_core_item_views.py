"""Static held/dropped core views, reusing the actual world mesh at phase zero.

GUI sprites remain the authored 64px icons. This creates only model aliases,
not copied geometry, entities, ticking renderers or additional item IDs.
"""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/googology'


def core_names(assets=ASSETS):
    return sorted(p.stem for p in (assets / 'core_meshes').glob('*.json')
                  if p.stem != 'ordinal_crystal')


def display_model(name):
    return {
        'parent': 'googology:block/' + name,
        'display': {
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.28, .28, .28]},
            'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.28, .28, .28]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [-2, 4, 0], 'scale': [.30, .30, .30]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [-2, 4, 0], 'scale': [.30, .30, .30]},
            'ground': {'translation': [0, 3, 0], 'scale': [.22, .22, .22]},
            'fixed': {'rotation': [0, 180, 0], 'scale': [.4, .4, .4]},
            'head': {'translation': [0, 8, 0], 'scale': [.55, .55, .55]},
        },
    }


def select_model(name):
    return {'type': 'minecraft:select', 'property': 'minecraft:display_context',
            'cases': [{'when': 'gui', 'model': {'type': 'minecraft:model', 'model': 'googology:item/' + name}}],
            'fallback': {'type': 'minecraft:model', 'model': 'googology:item/core_views/' + name}}


def generate(check=False):
    changed = []
    for name in core_names():
        path = ASSETS / f'models/item/core_views/{name}.json'
        expected = json.dumps(display_model(name), ensure_ascii=False, indent=2) + '\n'
        if not path.exists() or path.read_text('utf8') != expected:
            changed.append(path.name)
            if not check:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(expected, 'utf8')
    if check and changed:
        raise SystemExit('Stale portable core models: ' + ', '.join(changed))
    print(f'CORE_ITEM_VIEWS: {len(core_names())} static 3D aliases; GUI art unchanged; updated={len(changed)}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    generate(parser.parse_args().check)
