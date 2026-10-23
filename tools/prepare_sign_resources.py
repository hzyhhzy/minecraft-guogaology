"""Use native sign renderers on each engine without changing the source artwork.

1.21.x renders the board as an entity model with a 64x32 atlas. 26.x renders
the board from template_sign JSON and uses a compact 32x32 atlas. These exact
face rectangles also map native 26.2 oak pixels back to native 1.21.1 oak.
"""
from io import BytesIO
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
FAMILIES = ('laver', 'dread')
LEGACY_TARGETS = ('1.21.1', '1.21.11')
MODELS = ('sign_rot_0', 'sign_rot_1', 'sign_rot_2', 'sign_rot_3', 'wall_sign')
# Modern pixel rectangle, legacy destination, vertical flip. No pixels are scaled.
FACES = (
    ((0, 2, 24, 14), (2, 2), False),       # board front
    ((0, 16, 24, 28), (28, 2), False),     # board back
    ((24, 16, 26, 28), (0, 2), False),     # board west
    ((24, 2, 26, 14), (26, 2), False),     # board east
    ((0, 0, 24, 2), (2, 0), False),        # board top
    ((0, 28, 24, 30), (26, 0), True),      # board bottom
    ((28, 0, 30, 14), (2, 16), False),     # post front
    ((28, 16, 30, 30), (6, 16), False),    # post back
    ((30, 16, 32, 30), (0, 16), False),    # post west
    ((30, 0, 32, 14), (4, 16), False),     # post east
    ((28, 30, 30, 32), (4, 14), True),     # post bottom
    ((28, 30, 30, 32), (2, 14), False),    # hidden post top, under the board
)


def legacy_sign_model(value):
    textures = value.get('textures', {})
    if (value.get('parent', '').startswith('minecraft:block/template_')
            and textures.get('all') in tuple('guogaology:block/'+family+suffix for family in FAMILIES for suffix in ('_sign','_hanging_sign'))):
        # Native 1.21.x oak_sign has only a particle texture: its BE draws the board.
        return {'textures': {'particle': textures['particle']}}
    return value


def legacy_sign_texture(source):
    if source.size != (32, 32):
        raise ValueError('Expected the existing compact32x32 sign atlas, got ' + str(source.size))
    source = source.convert('RGBA')
    result = Image.new('RGBA', (64, 32))
    for rectangle, position, flip_y in FACES:
        pixels = source.crop(rectangle)
        if flip_y:
            pixels = pixels.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
        result.paste(pixels, position)
    return result


def legacy_hanging_texture(source):
    """Map native32x32 hanging faces onto the older64x32 entity atlas."""
    if source.size != (32,32): raise ValueError('Expected32x32 hanging-sign atlas')
    source=source.convert('RGBA'); result=Image.new('RGBA',(64,32))
    mappings=(
        ((18,16,32,26),(2,14),False), ((2,16,16,26),(18,14),False),
        ((16,16,18,26),(0,14),False), ((0,16,2,26),(16,14),False),
        ((2,14,16,16),(2,12),False), ((2,26,16,28),(16,12),True),
        ((0,4,16,6),(4,4),False), ((0,7,16,9),(24,4),False),
        ((16,4,20,6),(0,4),False), ((16,7,20,9),(20,4),False),
        ((0,0,16,4),(4,0),False), ((0,9,16,13),(20,0),True))
    for rect,pos,flip in mappings:
        pixels=source.crop(rect)
        if flip:pixels=pixels.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
        result.paste(pixels,pos)
    # Two crossed chain strips and a V strip. These are alpha-cut native planes.
    chain=source.crop((22,7,25,13))
    for x in (0,3,6,9):result.paste(chain,(x,6))
    v=source.crop((20,0,32,6));result.paste(v,(14,6));result.paste(v,(26,6))
    return result


def prepare(out, target):
    if target not in LEGACY_TARGETS:
        return
    assets = Path(out) / 'assets/guogaology'
    for family in FAMILIES:
        texture_source = assets / f'textures/block/{family}_sign.png'
        if not texture_source.exists() and not any(
                (assets / 'models/block' / f'{family}_{shape}.json').exists() for shape in MODELS):
            continue  # The legacy adapter also supports small partial resource fixtures.
        for shape in MODELS:
            path = assets / 'models/block' / f'{family}_{shape}.json'
            before = json.loads(path.read_text('utf8'))
            after = legacy_sign_model(before)
            if before != after:
                path.write_text(json.dumps(after, indent=2) + '\n', encoding='utf8')
        with Image.open(texture_source) as source:
            texture = legacy_sign_texture(source)
        destination = assets / f'textures/entity/signs/{family}.png'
        destination.parent.mkdir(parents=True, exist_ok=True)
        texture.save(destination)
        hanging=assets/f'textures/block/{family}_hanging_sign.png'
        if hanging.exists():
            for shape in ('hanging_sign_rot_0','hanging_sign_rot_1','hanging_sign_rot_2','hanging_sign_rot_3',
                          'hanging_sign_attached_rot_0','hanging_sign_attached_rot_1','hanging_sign_attached_rot_2','hanging_sign_attached_rot_3','wall_hanging_sign'):
                path=assets/'models/block'/f'{family}_{shape}.json'
                value=json.loads(path.read_text('utf8'));path.write_text(json.dumps(legacy_sign_model(value),indent=2)+'\n',encoding='utf8')
            with Image.open(hanging) as source:texture=legacy_hanging_texture(source)
            destination=assets/f'textures/entity/signs/hanging/{family}.png';destination.parent.mkdir(parents=True,exist_ok=True);texture.save(destination)



def verify_archive(archive, target):
    for family in FAMILIES:
        for shape in MODELS:
            name = f'assets/guogaology/models/block/{family}_{shape}.json'
            expected = json.loads((ROOT / 'src/main/resources' / name).read_text('utf8'))
            if target in LEGACY_TARGETS:
                expected = legacy_sign_model(expected)
            assert json.loads(archive.read(name)) == expected, f'Stale native sign model: {target}/{name}'
        if target in LEGACY_TARGETS:
            with Image.open(ROOT / f'src/main/resources/assets/guogaology/textures/block/{family}_sign.png') as source:
                expected = legacy_sign_texture(source)
            name = f'assets/guogaology/textures/entity/signs/{family}.png'
            with Image.open(BytesIO(archive.read(name))) as actual:
                assert actual.size == (64, 32) and actual.convert('RGBA').tobytes() == expected.tobytes(), \
                    f'Stale native sign atlas: {target}/{name}'
