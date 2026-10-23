"""Separate the Lv1 Guogao Heart's opaque relief from its transparent glass.

Only an optional per-face render-layer hint changes. Vertices, colors, UVs,
materials, the glass shell, portable scale and static LOD quads stay intact.
All existing meshes without a hint retain their established translucent path.
"""
from pathlib import Path
import argparse
import copy
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/guogaology'
NAME = 'guogao_heart'


def repaired(mesh, assets=ASSETS):
    result = copy.deepcopy(mesh)
    opaque = {}
    for key, identifier in mesh['textures'].items():
        namespace, path = identifier.split(':', 1)
        texture = assets.parent / namespace / 'textures' / (path + '.png')
        with Image.open(texture) as image:
            opaque[key] = image.convert('RGBA').getchannel('A').getextrema() == (255, 255)
    for quad in result['quads']:
        # The whole Lv1 relief is static. Do not introduce a solid hint into a
        # BE-animated part, whose separate moving draw pass remains unchanged.
        if quad.get('part', 1) != 0:
            raise ValueError('Lv1 Guogao Heart unexpectedly acquired a moving part')
        if opaque[quad['t']] and all((color >> 24) == 255 for color in quad['c']):
            quad['render_layer'] = 'solid'
        else:
            quad.pop('render_layer', None)
    return result


def generate(check=False, assets=ASSETS):
    path = assets / 'core_meshes' / (NAME + '.json')
    before = json.loads(path.read_text(encoding='utf-8'))
    after = repaired(before, assets)
    if before != after:
        if check:
            raise SystemExit('Stale Lv1 Guogao Heart opaque/glass layer partition')
        path.write_text(json.dumps(after, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
    solid = sum(quad.get('render_layer') == 'solid' for quad in after['quads'])
    print(f'GUOGAO_RENDER_LAYERS_OK solid={solid} translucent={len(after["quads"])-solid} check={check}')
    return after


def verify_archive(archive, target):
    path = f'assets/guogaology/core_meshes/{NAME}.json'
    actual = json.loads(archive.read(path))
    assert actual == repaired(actual), f'Stale Guogao opaque/glass layers: {target}'
    assert sum(q.get('render_layer') == 'solid' for q in actual['quads']) == 736
    assert sum('render_layer' not in q for q in actual['quads']) == 114


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    generate(parser.parse_args().check)
