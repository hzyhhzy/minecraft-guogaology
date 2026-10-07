"""Fixed, locally craftable return frames and their common wooden token.

The inactive pedestals keep the existing portal silhouettes, while their local
substrates distinguish the three return routes. Their seal follows the actual
destination: home/Outer for the first two, the Inner matrix for the third.
Pixel art is generated offline; ordinary builds use the committed resources.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
A = RES / 'assets/guogaology'
D = RES / 'data/guogaology'
FRAMES = ('outer_return_frame', 'inner_return_frame', 'guogao_return_frame')


def read(path):
    return json.loads(path.read_text(encoding='utf8'))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')


def cube(start, end, texture, top=None):
    faces = {side: {'texture': '#' + texture, 'uv': [0, 0, 16, 16]}
             for side in ('north', 'south', 'east', 'west', 'up', 'down')}
    if top:
        faces['up']['texture'] = '#' + top
    return {'from': start, 'to': end, 'faces': faces}


def generate_art():
    from PIL import Image, ImageDraw
    textures = A / 'textures/block'
    textures.mkdir(parents=True, exist_ok=True)
    # Cool cobble, clear ordinal facets, and locally compacted Dreadsoil.
    palettes = (
        ((100, 111, 119), (150, 161, 166), (67, 76, 81), (186, 218, 191)),
        ((84, 78, 120), (160, 175, 228), (55, 51, 86), (222, 233, 250)),
        ((35, 54, 46), (79, 96, 68), (21, 34, 29), (152, 178, 199)),
    )
    for style, name in enumerate(FRAMES):
        base, light, dark, ink = palettes[style]
        im = Image.new('RGBA', (16, 16), base + (255,))
        draw = ImageDraw.Draw(im)
        if style == 0:
            # Interlocking cobble courses, with broad pixel planes instead of noise.
            for y, edges in ((0, (0, 6, 15)), (5, (0, 4, 10, 15)), (11, (0, 8, 15))):
                end = 4 if y == 0 else 10 if y == 5 else 15
                draw.line((0, y, 15, y), fill=dark + (255,))
                for x in edges:
                    draw.line((x, y, x, end), fill=dark + (255,))
                    if x < 14:
                        draw.line((x + 1, y + 1, min(x + 4, 14), y + 1), fill=light + (255,))
        elif style == 1:
            draw.polygon(((0, 4), (7, 0), (10, 7), (3, 11)), fill=(109, 113, 164, 255))
            draw.polygon(((10, 0), (15, 0), (15, 10), (10, 7)), fill=(75, 78, 125, 255))
            draw.polygon(((3, 11), (10, 7), (15, 12), (9, 15), (0, 15)), fill=(95, 98, 153, 255))
            draw.line((0, 4, 7, 0, 10, 7, 3, 11, 0, 4), fill=light + (255,))
            draw.line((3, 11, 9, 15), fill=dark + (255,))
            draw.line((11, 10, 13, 11), fill=(181, 219, 239, 255))
        else:
            im = Image.open(textures / 'guogao_loam.png').convert('RGBA').resize((16, 16), Image.Resampling.NEAREST)
            draw = ImageDraw.Draw(im)
            draw.line((0, 0, 15, 0), fill=light + (255,))
            draw.line((0, 15, 15, 15), fill=dark + (255,))
        im.save(textures / f'{name}_base.png')

        # Wooden binding is readable on all three, and recognizably Dread Fir
        # in the Underworld where that exact plank is the crafting ingredient.
        if style == 2:
            rim = Image.open(textures / 'dread_planks.png').convert('RGBA').resize((16, 16), Image.Resampling.NEAREST)
        else:
            rim = Image.new('RGBA', (16, 16), (113, 85, 57, 255))
            grain = ImageDraw.Draw(rim)
            for y in (0, 5, 10, 15):
                grain.line((0, y, 15, y), fill=(63, 48, 37, 255))
                if y < 15:
                    grain.line((0, y + 1, 15, y + 1), fill=(153, 121, 81, 255))
            grain.line((3, 2, 6, 2), fill=(94, 67, 47, 255))
            grain.line((10, 8, 13, 8), fill=(94, 67, 47, 255))
        draw = ImageDraw.Draw(rim)
        draw.rectangle((1, 1, 14, 14), outline=light + (255,))
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            draw.point((x, y), fill=ink + (255,))
        rim.save(textures / f'{name}_rim.png')

        seal = Image.new('RGBA', (32, 32), dark + (255,))
        draw = ImageDraw.Draw(seal)
        draw.rectangle((1, 1, 30, 30), outline=light + (255,), width=2)
        draw.rectangle((4, 4, 27, 27), outline=base + (255,))
        if style < 2:
            # The same quiet home arch used by the destination-style-0 portals.
            draw.line((11, 14, 16, 10, 21, 14), fill=ink + (255,), width=2)
            draw.line((12, 14, 12, 21, 20, 21, 20, 14), fill=ink + (255,), width=2)
        else:
            # Matrix destination, rather than an Underworld face for a return.
            for x in (12, 18):
                for y in (10, 16):
                    draw.rectangle((x, y, x + 3, y + 3), outline=ink + (255,))
        # Large curved, left-pointing return arrow shared with the token.
        draw.line((23, 21, 25, 18, 25, 11, 22, 7, 10, 7, 7, 10, 7, 16), fill=light + (255,), width=2)
        draw.polygon(((3, 13), (7, 19), (11, 13)), fill=light + (255,))
        for x, y in ((4, 4), (27, 4), (4, 27), (27, 27)):
            draw.point((x, y), fill=ink + (255,))
        seal.save(textures / f'{name}_seal.png')

        parts = [cube([0, 0, 0], [16, 3, 16], 'base'),
                 cube([1, 3, 1], [15, 5, 15], 'rim'),
                 cube([3, 5, 3], [13, 8, 13], 'base'),
                 cube([3, 8, 3], [13, 10, 13], 'rim', 'seal')]
        for x in (1, 12):
            for z in (1, 12):
                if style == 0:
                    parts.extend((cube([x, 5, z], [x + 3, 9, z + 3], 'base'),
                                  cube([x, 9, z], [x + 3, 11, z + 3], 'rim')))
                elif style == 1:
                    parts.extend((cube([x + .5, 5, z + .5], [x + 2.5, 12, z + 2.5], 'base'),
                                  cube([x, 10, z], [x + 3, 11, z + 3], 'rim')))
                else:
                    parts.extend((cube([x, 5, z], [x + 3, 11, z + 3], 'base'),
                                  cube([x + .5, 11, z + .5], [x + 2.5, 14, z + 2.5], 'rim')))
        mapped = {key: f'guogaology:block/{name}_{key}' for key in ('base', 'rim', 'seal')}
        mapped['particle'] = mapped['base']
        write(A / f'models/block/{name}.json', {
            'parent': 'minecraft:block/block', 'textures': mapped, 'elements': parts})
        write(A / f'blockstates/{name}.json', {'variants': {'': {'model': f'guogaology:block/{name}'}}})
        write(A / f'models/item/{name}.json', {'parent': f'guogaology:block/{name}'})
        write(A / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'guogaology:block/{name}'}})

    # A small carved wooden travel tag: clipped corners, a return arrow and
    # the crafting stick as a short handle. Keep the native 16px silhouette.
    token = Image.new('RGBA', (16, 16))
    draw = ImageDraw.Draw(token)
    draw.rectangle((6, 10, 9, 15), fill=(62, 45, 32, 255))
    draw.rectangle((7, 10, 8, 14), fill=(153, 111, 65, 255))
    draw.line((7, 11, 7, 14), fill=(194, 151, 88, 255))
    draw.polygon(((5, 1), (11, 1), (13, 3), (13, 10), (11, 12), (3, 12), (2, 10), (2, 4)), fill=(62, 45, 32, 255))
    draw.polygon(((5, 2), (10, 2), (12, 4), (12, 9), (10, 11), (4, 11), (3, 9), (3, 5)), fill=(158, 122, 77, 255))
    draw.line((5, 3, 10, 3, 11, 4), fill=(204, 167, 107, 255))
    draw.line((4, 11, 10, 11), fill=(113, 81, 51, 255))
    draw.line((5, 6, 9, 6, 10, 7, 10, 9, 9, 10, 6, 10), fill=(233, 223, 170, 255))
    draw.polygon(((3, 7), (6, 5), (6, 9)), fill=(233, 223, 170, 255))
    (A / 'textures/item').mkdir(parents=True, exist_ok=True)
    token.save(A / 'textures/item/return_token.png')
    write(A / 'models/item/return_token.json', {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': 'guogaology:item/return_token'}})
    write(A / 'items/return_token.json', {
        'model': {'type': 'minecraft:model', 'model': 'guogaology:item/return_token'}})


def generate_data():
    recipes = (
        ('outer_return_frame', ['minecraft:cobblestone'] * 4 + ['#minecraft:planks'], 1),
        ('inner_return_frame', ['guogaology:ordinal_shard'] * 4 + ['#minecraft:planks'], 1),
        ('guogao_return_frame', ['guogaology:guogao_loam'] * 4 + ['guogaology:dread_planks'], 1),
    )
    paths = []
    for name, materials, count in recipes:
        path = D / f'recipe/{name}.json'
        write(path, {'type': 'minecraft:crafting_shapeless',
                     'category': 'misc' if name == 'return_token' else 'building',
                     'ingredients': [({'tag': item[1:]} if item.startswith('#') else {'item': item})
                                     for item in materials],
                     'result': {'id': 'guogaology:' + name, 'count': count}})
        paths.append(path)
    # A plank above one stick makes a handled token directly in the inventory
    # grid, with neither the ingredients nor layout of a vanilla wood recipe.
    token_path = D / 'recipe/return_token.json'
    write(token_path, {'type': 'minecraft:crafting_shaped', 'category': 'misc',
                       'pattern': ['P', 'S'],
                       'key': {'P': {'tag': 'minecraft:planks'}, 'S': {'item': 'minecraft:stick'}},
                       'result': {'id': 'guogaology:return_token', 'count': 1}})
    paths.append(token_path)
    from prepare_outer_resources import recipe_unlocks
    recipe_unlocks(D, paths)
    for name in FRAMES:
        # Inert frames are recoverable by hand and never require a tool tier.
        write(D / f'loot_table/blocks/{name}.json', {
            'type': 'minecraft:block', 'pools': [{'rolls': 1,
            'entries': [{'type': 'minecraft:item', 'name': 'guogaology:' + name}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

    balance_path = RES / 'guogaology/block_balance.json'
    balance = read(balance_path)
    for name in FRAMES:
        balance[name] = {'hardness': .8, 'resistance': 3, 'tool': 'hand',
                         'tier': 0, 'light': 0, 'drop': 'self', 'use': '返程门专用框架',
                         'friction': .6, 'collision': '底座、中央印台及四角立柱', 'renewable': '合成'}
    write(balance_path, dict(sorted(balance.items())))

    names = {
        'block.guogaology.outer_return_frame': ('表界归途框', 'Outer Return Frame'),
        'block.guogaology.inner_return_frame': ('里界归途框', 'Inner Return Frame'),
        'block.guogaology.guogao_return_frame': ('冥林归途框', 'Dread Forest Return Frame'),
        'item.guogaology.return_token': ('归途签', 'Return Token'),
    }
    guide = (
        '返程：表界归途框＝4 圆石＋1 任意木板；里界归途框＝4 序数晶屑＋1 任意木板；冥林归途框＝4 果糕冥土＋1 冥杉木板；每次得 1 块。同种归途框摆十二格，投入 1 张归途签（1 任意木板在上、1 木棍在下）返回上一层；三种框架不能混搭。',
        'Return: 4 cobblestone + 1 plank -> 1 Outer Return Frame; 4 Ordinal Shards + 1 plank -> 1 Inner Return Frame; 4 Guogao Dreadsoil + 1 Dread Fir Plank -> 1 Dread Forest Return Frame. Use any planks where unspecified. Place 12 matching frames and throw in 1 Return Token (1 plank above 1 stick) to return one layer; do not mix frame types.',
    )
    catalog_path = ROOT / 'tools/copy_catalog.json'
    catalog = read(catalog_path)
    for index, locale in enumerate(('zh_cn', 'en_us')):
        values = catalog['translations'][locale]
        values.update({key: pair[index] for key, pair in names.items()})
        guide_key = 'message.guogaology.guide'
        prefix = '返程：' if index == 0 else 'Return:'
        lines = values[guide_key].split('\n')
        assert sum(line.startswith(prefix) for line in lines) == 1
        values[guide_key] = '\n'.join(guide[index] if line.startswith(prefix) else line for line in lines)
        path = A / f'lang/{locale}.json'
        language = read(path)
        language.update({key: values[key] for key in (*names, guide_key)})
        write(path, language)
    catalog['removed'] = [key for key in catalog['removed'] if key not in names]
    write(catalog_path, catalog)


def generate():
    generate_art()
    generate_data()
    print('Three fixed return frames, Return Token, recipes, hand drops and bilingual guide generated')


if __name__ == '__main__':
    generate()
