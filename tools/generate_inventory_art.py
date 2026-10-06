"""Reproducible native-pixel inventory art, independent of the world core meshes.

Generate 27 perspective core sprites and four mineral bows (idle + three draw stages).
The ungraded natural Ordinal Crystal retains its original 3D block-model item.
World block models, animation surfaces and Voxy atlases are never written here.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw
from generate_brand import globe_face
from core_item_projection import project

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'src/main/resources/assets/googology'
T = A / 'textures/item'
FAMILIES = ('sequence_core', 'power_tower_core', 'hydra_bud', 'lho_trace',
            'laver_core', 'astra_critical_core', 'boundary_core', 'guogao_heart',
            'ordinal_crystal')
METALS = ('omega', 'epsilon', 'gamma', 'true_omega')
# Family colours follow the existing Lv1 mesh/materials, not item rarity.  All
# three crafted grades use exactly the same sprite outside the number badge.
PALETTES = {
    'sequence_core': ('#424b37', '#929f76', '#c5c49b', '#f1edcb'),
    'power_tower_core': ('#693b21', '#c18435', '#f3cf78', '#fff5cf'),
    'hydra_bud': ('#13513e', '#42b47f', '#91e5a2', '#d4ffd0'),
    'lho_trace': ('#34484f', '#63858b', '#abc8cb', '#e8f2f1'),
    'laver_core': ('#174d3e', '#328b65', '#98d6a2', '#fff0bd'),
    'astra_critical_core': ('#24565f', '#4fbfc8', '#a4f1ec', '#f0fffd'),
    'boundary_core': ('#284d70', '#568ab0', '#9bd5df', '#faf8d1'),
    'guogao_heart': ('#2c4148', '#669396', '#edbe51', '#fff0be'),
    'ordinal_crystal': ('#354e69', '#628ca6', '#b3addc', '#e7f3fb'),
}
NUMBER_PIXELS = {
    1: ('010', '110', '010', '010', '111'),
    2: ('111', '001', '111', '100', '111'),
    3: ('111', '001', '111', '001', '111'),
}
BOW_PALETTES = (
    ('#24344a', '#547299', '#97b8d7', '#eaf5ff'),
    ('#40294f', '#8354a4', '#c49be0', '#f6e1ff'),
    ('#592544', '#b44982', '#f095bc', '#ffe5ec'),
    ('#583825', '#bd812f', '#f1c752', '#fff2ad'),
)


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', 'utf8')


def rgb(color):
    return tuple(int(color[i:i+2], 16) for i in (1, 3, 5))


def mix(a, b, weight):
    return tuple(round(x*(1-weight)+y*weight) for x,y in zip(rgb(a), rgb(b)))


def crystal_case(d, colors, foreground=False):
    """Three glass planes with restrained bevels, on a native 64px pixel grid."""
    dark, mid, light, white = colors
    top, left, right, front = (29, 3), (3, 16), (55, 16), (29, 29)
    ll, rr, low = (3, 44), (55, 44), (29, 57)
    ink = mix(dark, '#111a26', .40)
    if not foreground:
        d.polygon([top, right, front, left], fill=mix(mid, '#273442', .63))
        d.polygon([left, front, low, ll], fill=mix(dark, '#435964', .25))
        d.polygon([right, front, low, rr], fill=mix(dark, '#182532', .25))
        # The rear edge and floor can be seen through the case.
        rear = mix(mid, '#314755', .48)
        d.line((29, 5, 29, 29, 5, 43), fill=rear, width=1)
        d.line((29, 29, 53, 43), fill=rear, width=1)
        d.line((6, 20, 23, 11), fill=mix(light, '#506474', .56), width=2)
        d.line((9, 23, 16, 19), fill=mix(light, '#506474', .56), width=1)
        return
    rim = mix(mid, white, .38)
    d.line([top, left, ll, low, rr, right, top], fill=ink, width=6)
    d.line([top, left, ll, low, rr, right, top], fill=rim, width=4)
    # Avoid a bright front edge bisecting the object behind transparent glass.
    d.line((4, 16, 11, 20), fill=rim, width=2)
    d.line((48, 20, 54, 17), fill=light, width=2)
    d.line((29, 50, 29, 56), fill=rim, width=2)
    d.line((5, 44, 29, 56), fill=light, width=3)
    d.line((5, 17, 5, 42), fill=mix(light, white, .46), width=1)
    d.line((30, 4, 53, 16), fill=light, width=1)
    # Small corner clasps and two deliberate glass glints, not scattered noise.
    for x, y in ((29, 3), (3, 16), (3, 44), (29, 57), (55, 16), (55, 44)):
        d.rectangle((x-1, y-1, x+1, y+1), fill=rim)
    d.line((9, 31, 9, 35), fill=mix(light, white, .45), width=1)
    d.line((8, 32, 11, 32), fill=mix(light, white, .45), width=1)
    d.line((48, 36, 51, 34), fill=mix(mid, white, .22), width=1)


def grade_badge(d, grade):
    # 3x5 digits at scale 4 remain actual readable 3x5 pixels in a 16px slot.
    # Upper right leaves Minecraft's lower-right stack count unobstructed.
    d.rectangle((44, 0, 63, 27), fill='#16252d')
    d.rectangle((45, 1, 62, 26), fill='#29404b')
    d.line((45, 1, 62, 1), fill='#80a6b1')
    d.line((45, 1, 45, 26), fill='#80a6b1')
    for y, row in enumerate(NUMBER_PIXELS[grade]):
        for x, pixel in enumerate(row):
            if pixel == '1':
                d.rectangle((48+x*4, 4+y*4, 51+x*4, 7+y*4), fill='#fff5d8')


def core_icon(family, level):
    im = Image.new('RGBA', (64, 64)); d = ImageDraw.Draw(im)
    dark, mid, light, white = colors = PALETTES[family]
    crystal_case(d, colors)
    if family != 'guogao_heart':
        # Enlarge the real Lv1 geometry uniformly for inventory readability.
        # Shape, face UVs and intrinsic colour all come from the world mesh;
        # no theme-based replacement facets, branches or marks are invented.
        scale = {
            'sequence_core': 1.7,
            'power_tower_core': 1.0,
            'hydra_bud': 1.5,
            'lho_trace': 1.3,
            'laver_core': 1.3,
            'astra_critical_core': 1.2,
            'boundary_core': .92,
            'ordinal_crystal': 1.25,
        }[family]
        source = 'ordinal_crystal_lv2' if family == 'ordinal_crystal' else family
        im.alpha_composite(project(source, magnify=scale))
    else:
        # Reuse the original giant-globe/brand face geometry and palette: raised
        # inner brows, oval open eyes and the sweat drop inside the left cheek.
        d.ellipse((12,13,46,47),fill='#6b643c')
        # Odd diameter puts the sampled sphere's exact centre at (29,30), the
        # same model origin as every projected core and the surrounding case.
        face=globe_face(35)
        # Increase facial contrast at inventory size while keeping the motif's
        # actual positions; the warm globe brown is too close to its yellow face.
        for y in range(face.height):
            for x in range(face.width):
                if face.getpixel((x,y)) == (0x5d,0x3c,0x0d,255):
                    face.putpixel((x,y),(0x24,0x31,0x3b,255))
        im.alpha_composite(face,(12,13))
        # Keep the prior face; compress only the mouth vertically around its centre.
        d.ellipse((24,36,34,40),fill='#2c2627')
    crystal_case(d, colors, True)
    grade_badge(d, level-1 if family == 'ordinal_crystal' else level)
    return im


def bow_icon(tier, stage):
    im = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(im)
    dark, mid, light, white = BOW_PALETTES[tier]
    # The familiar diagonal bow silhouette, angular mineral limbs and bright facets.
    limb = [(2,14),(2,11),(3,8),(5,5),(8,3),(12,2),(14,2)]
    if stage >= 0:
        limb = [(2,14),(2,11),(3,8),(5,5),(8,4),(12,3),(15,3)]
    d.line(limb, fill=dark, width=3)
    d.line(limb, fill=mid, width=2)
    d.line([(2,13),(3,9),(5,6),(8,4),(12,3),(14,3)], fill=light)
    d.line((3,10,4,8), fill=white)
    d.line((9,4,12,3), fill=white)
    d.rectangle((4,6,6,8), fill=dark)
    d.line((5,6,6,7), fill=light)
    # A pale, taut string; draw stages pull its centre away from the mineral grip.
    if stage < 0:
        d.line((14,3,3,14), fill='#abb5c7')
        d.point((14,2), fill=white); d.point((2,14),fill=white)
    else:
        cx,cy = ((9,9),(11,10),(13,11))[stage]
        d.line((15,4,cx,cy,3,14), fill='#d0d9e5')
        # Clearly visible arrow and white fletching at every draw stage.
        d.line((cx-8,cy-8,cx+1,cy+1), fill='#a59473')
        d.line((cx-7,cy-8,cx+2,cy+1), fill='#f2e6cb')
        d.line((cx-8,cy-8,cx-7,cy-8,cx-7,cy-7), fill='#f1f7ff',width=2)
        d.polygon([(cx+1,cy),(min(15,cx+3),cy+1),(min(15,cx+2),cy+3)],fill=light)
        d.point((cx+2,cy+1),fill=white)
    if tier >= 1: d.point((7,4),fill=white)
    if tier >= 2: d.point((3,11),fill=white)
    if tier == 3: d.point((11,2),fill='#deabf5'); d.point((2,12),fill='#deabf5')
    return im


def generate():
    T.mkdir(parents=True, exist_ok=True)
    for family in FAMILIES:
        for level in range(1, 5 if family == 'ordinal_crystal' else 4):
            name = family + (f'_lv{level}' if level > 1 else '')
            if family == 'ordinal_crystal' and level == 1:
                # Exact original 0.3.11 item model; never create a replacement sprite.
                (A / f'models/item/{name}.json').write_text('{"parent":"googology:block/ordinal_crystal"}', 'utf8')
                texture = T / f'{name}.png'
                assert texture.resolve().is_relative_to(T.resolve())
                texture.unlink(missing_ok=True)
                continue
            core_icon(family, level).save(T / f'{name}.png')
            write(A / f'models/item/{name}.json', {
                'parent': 'minecraft:item/generated', 'textures': {'layer0': f'googology:item/{name}'}})
    for tier, metal in enumerate(METALS):
        name = metal + '_bow'
        bow_icon(tier, -1).save(T / f'{name}.png')
        write(A / f'models/item/{name}.json', {
            'parent': 'minecraft:item/bow', 'textures': {'layer0': f'googology:item/{name}'},
            'overrides': [
                {'predicate': {'pulling': 1}, 'model': f'googology:item/bows/{name}_pulling_0'},
                {'predicate': {'pulling': 1, 'pull': .65}, 'model': f'googology:item/bows/{name}_pulling_1'},
                {'predicate': {'pulling': 1, 'pull': .9}, 'model': f'googology:item/bows/{name}_pulling_2'}]})
        for stage in range(3):
            pull = f'{name}_pulling_{stage}'
            bow_icon(tier, stage).save(T / f'{pull}.png')
            # Variant models are not registered items; keep them out of the
            # inventory entrypoint directory and its localization/item-ID audits.
            write(A / f'models/item/bows/{pull}.json', {
                'parent': 'minecraft:item/bow', 'textures': {'layer0': f'googology:item/{pull}'}})
            legacy = A / f'models/item/{pull}.json'
            assert legacy.resolve().is_relative_to((A / 'models/item').resolve())
            legacy.unlink(missing_ok=True)
    # Contact sheets are developer previews, never runtime resources.
    out = ROOT / 'build/art-0312'; out.mkdir(parents=True, exist_ok=True)
    sheet = Image.new('RGB', (5*224, 9*152+48), '#202631'); d = ImageDraw.Draw(sheet)
    d.text((16,8),'64px native crystal cases / identical family colours and shapes / corner grade numbers',fill='#eeeeee')
    d.text((16,24),'Left: native sprite at 2x.  Right: actual 16px inventory sample at 4x + true size.',fill='#a8bdc8')
    small_sheet = Image.new('RGB', (680, 9*64+40), '#202631'); sd=ImageDraw.Draw(small_sheet)
    sd.text((12,8),'16px inventory samples (nearest), enlarged 3x for inspection',fill='#eeeeee')
    for row,family in enumerate(FAMILIES):
        y = 48+row*152
        d.text((12,y+12),family,fill='#eeeeee')
        sy=40+row*64
        sd.text((12,sy+18),family,fill='#eeeeee')
        for level in range(1,5 if family == 'ordinal_crystal' else 4):
            x=224*level
            sx=220+(level-1)*108
            if family == 'ordinal_crystal' and level == 1:
                d.text((x,y+32),'Original crystal',fill='#eef2f5')
                d.text((x,y+62),'3D model unchanged',fill='#dddddd')
                sd.text((sx,sy+8),'Original',fill='#dddddd')
                sd.text((sx,sy+24),'3D model',fill='#dddddd')
                continue
            im=core_icon(family,level)
            big=im.resize((128,128),Image.Resampling.NEAREST)
            sheet.paste(big,(x,y),big)
            small=im.resize((16,16),Image.Resampling.NEAREST)
            zoom=small.resize((64,64),Image.Resampling.NEAREST)
            sheet.paste(zoom,(x+136,y+24),zoom)
            sheet.paste(small,(x+158,y+99),small)
            grade=level-1 if family == 'ordinal_crystal' else level
            d.text((x+136,y+4),f'Core Lv{grade}',fill='#dddddd')
            d.text((x+142,y+120),'16px',fill='#a8bdc8')
            inventory_zoom=small.resize((48,48),Image.Resampling.NEAREST)
            small_sheet.paste(inventory_zoom,(sx,sy),inventory_zoom)
            small_sheet.paste(small,(sx+56,sy+17),small)
            sd.text((sx+76,sy+21),str(grade),fill='#dddddd')
    sheet.save(out/'core-icons.png')
    small_sheet.save(out/'core-icons-16px.png')
    draft=ROOT/'build/art-0312-next'; draft.mkdir(parents=True,exist_ok=True)
    sheet.save(draft/'core-icons.png')
    small_sheet.save(draft/'core-icons-16px.png')
    reference = Image.new('RGB', (816, 9*160+40), '#202631'); rd=ImageDraw.Draw(reference)
    rd.text((12,8),'Authoritative WORLD mesh / inner mesh / 64px inventory sketch + actual 16px sample',fill='#eeeeee')
    for row,family in enumerate(FAMILIES):
        y=40+row*160
        rd.text((12,y+12),family,fill='#eeeeee')
        source='ordinal_crystal_lv2' if family=='ordinal_crystal' else family
        native=core_icon(family,2 if family=='ordinal_crystal' else 1)
        samples=(project(source,128,True,False), project(source,128,False,False),
                 native.resize((128,128),Image.Resampling.NEAREST))
        for col,sample in enumerate(samples):
            reference.paste(sample,(200+col*184,y),sample)
        small=native.resize((16,16),Image.Resampling.NEAREST)
        zoom=small.resize((48,48),Image.Resampling.NEAREST)
        reference.paste(zoom,(748,y+40),zoom)
        for x,label in ((200,'WORLD full mesh'),(384,'WORLD inner only'),(568,'Inventory sketch')):
            rd.text((x,y+136),label,fill='#a8bdc8')
    reference.save(draft/'source-and-sketch.png')
    sheet = Image.new('RGB',(4*176,4*192),'#202631'); d=ImageDraw.Draw(sheet)
    for row,metal in enumerate(METALS):
        d.text((12,row*192+4),metal,fill='#eeeeee')
        for col,stage in enumerate((-1,0,1,2)):
            im=bow_icon(row,stage).resize((144,144),Image.Resampling.NEAREST)
            sheet.paste(im,(col*176+12,row*192+24),im)
            d.text((col*176+12,row*192+172),'idle' if stage<0 else f'pull {stage}',fill='#eeeeee')
    sheet.save(out/'bows.png')
    print('Generated 27 perspective core sprites + original Ordinal Crystal 3D model + 16 bow images/models; world meshes untouched')


if __name__ == '__main__':
    generate()
