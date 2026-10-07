"""Verify modern display routing while retaining all authored GUI sprites/mesh materials."""
from contextlib import nullcontext
import json
from pathlib import Path
import shutil
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from generate_core_item_views import ASSETS, core_names, display_model, generate, select_model
from prepare_port_resources import convert_items


def main():
    generate(check=True)
    names = core_names()
    assert len(names) == 27
    checks = 0
    with nullcontext(ROOT / 'build/core-item-046-resource-check') as folder:
        out = Path(folder)
        assets = out / 'assets/guogaology'
        for directory in ('models/item', 'core_meshes'):
            (assets / directory).mkdir(parents=True, exist_ok=True)
        for name in names + ['ordinal_crystal']:
            for directory in ('models/item', 'core_meshes'):
                shutil.copy2(ASSETS / directory / (name + '.json'), assets / directory / (name + '.json'))
        convert_items(out)
        for name in names:
            item = json.loads((assets / f'items/{name}.json').read_text('utf8'))
            assert item['model'] == select_model(name)
            gui = json.loads((assets / f'models/item/{name}.json').read_text('utf8'))
            assert gui['parent'] == 'minecraft:item/generated'
            assert gui['textures']['layer0'] == 'guogaology:item/' + name
            alias = json.loads((ASSETS / f'models/item/core_views/{name}.json').read_text('utf8'))
            assert alias == display_model(name) and alias['parent'] == 'guogaology:block/' + name
            mesh = json.loads((ASSETS / f'core_meshes/{name}.json').read_text('utf8'))
            assert mesh['quads']
            assert all(q['t'] in mesh['textures'] for q in mesh['quads'])
            # Do not flatten C1 relief or replace translucent world materials.
            zs = {vertex[2] for q in mesh['quads'] for vertex in q['v']}
            assert max(zs) - min(zs) > 1
            for texture in mesh['textures'].values():
                if texture.startswith('guogaology:'):
                    assert (ASSETS / ('textures/' + texture.split(':', 1)[1] + '.png')).is_file()
            checks += 8
        natural = json.loads((assets / 'items/ordinal_crystal.json').read_text('utf8'))
        assert natural == {'model': {'type': 'minecraft:model', 'model': 'guogaology:item/ordinal_crystal'}}
        checks += 1
    print(f'CORE_ITEM_VIEWS_RESOURCES_OK checks={checks} GUI=27 static_mesh_aliases=27 natural_original=1')


if __name__ == '__main__':
    main()
