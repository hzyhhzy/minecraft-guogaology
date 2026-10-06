"""Reproducible native-pixel inventory art, independent of the world core meshes.

Generate 27 perspective core sprites and four mineral bows (idle + three draw stages).
Ordinal Lv1 retains its original three-dimensional block-model item.
World block models, animation surfaces and Voxy atlases are never written here.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw
from generate_brand import globe_face

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


def cube(d, cx, top, width, height, colors, fill=True, line=1):
    dark, mid, light, edge = colors
    half = width//2
    peak=(cx,top); left=(cx-width,top+half); right=(cx+width,top+half)
    front=(cx,top+width); low=(cx,top+width+height)
    ll=(cx-width,top+half+height); rr=(cx+width,top+half+height)
    if fill:
        d.polygon([peak,right,front,left],fill=light)
        d.polygon([left,front,low,ll],fill=mid)
        d.polygon([right,front,low,rr],fill=dark)
    d.line([peak,left,ll,low,rr,right,peak],fill=edge,width=line)
    d.line([left,front,right],fill=edge,width=line)
    d.line([front,low],fill=edge,width=line)


def sphere(d, cx, cy, radius, colors):
    dark,mid,light,white=colors
    d.ellipse((cx-radius,cy-radius,cx+radius,cy+radius),fill=dark)
    d.ellipse((cx-radius+1,cy-radius+1,cx+radius-2,cy+radius-2),fill=mid)
    d.ellipse((cx-radius+2,cy-radius+2,cx+2,cy+1),fill=light)
    d.line((cx-radius+3,cy-radius+2,cx-1,cy-radius+2),fill=white,width=2)


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
    if family == 'sequence_core':
        # Four warm sage/ivory matrix cells suspended between solid brackets.
        for x, y in ((20, 15), (35, 15), (20, 31), (35, 31)):
            cube(d, x, y, 5, 5, colors, True, 1)
            d.line((x-4, y+3, x-4, y+6), fill=white)
        for path in (((14, 16), (10, 18), (10, 40), (14, 42)),
                     ((40, 16), (42, 18), (42, 40), (40, 42))):
            d.line([(x+1,y+1) for x,y in path], fill=dark, width=5)
            d.line(path, fill=light, width=3)
            d.line(path, fill=white, width=1)
    elif family == 'power_tower_core':
        cube(d, 29, 32, 13, 6, colors, True, 2)
        cube(d, 29, 22, 9, 12, colors, True, 2)
        cube(d, 29, 11, 5, 14, colors, True, 2)
        d.polygon([(29,9),(34,13),(29,17),(24,13)],fill=white)
        d.line((25,18,25,24),fill=light,width=2)
        d.line((21,30,21,37),fill=light,width=2)
        d.line((17,40,17,44),fill=white,width=2)
    elif family == 'hydra_bud':
        paths = (((17,17),(17,29),(29,38)), ((41,17),(41,29),(29,38)),
                 ((29,13),(29,47)))
        for path in paths:
            d.line([(x+2,y+2) for x,y in path],fill=dark,width=7)
            d.line(path,fill=mid,width=7)
            d.line([(x-1,y-1) for x,y in path],fill=light,width=3)
        for x,y in ((17,16),(41,16),(29,12)):
            d.polygon([(x-3,y),(x,y-2),(x+3,y),(x,y+2)],fill=white)
        d.line((28,38,28,46),fill=white,width=1)
    elif family == 'lho_trace':
        ring = [(16,23),(19,17),(27,13),(35,13),(41,17),(44,24),
                (43,34),(39,42),(31,47),(23,47),(17,42),(14,33),(16,23)]
        d.line([(x+2,y+2) for x,y in ring],fill=dark,width=7)
        d.line(ring,fill=mid,width=6)
        d.line([(x-1,y-1) for x,y in ring],fill=light,width=3)
        d.line((17,46,42,14),fill=dark,width=8)
        d.line((16,45,41,13),fill=mid,width=6)
        d.line((15,44,40,12),fill=white,width=2)
        d.line((21,16,27,13),fill=white,width=1)
    elif family == 'laver_core':
        cube(d,29,10,17,17,(dark,mid,mix(mid,light,.25),light),True,2)
        # Original circle/point semantics placed on separate perspective faces.
        ring=[(18,23),(22,24),(25,29),(25,35),(22,38),(18,35),(16,30),(18,23)]
        d.line(ring,fill=dark,width=5)
        d.line([(x-1,y-1) for x,y in ring],fill=white,width=3)
        d.polygon([(38,28),(41,30),(41,34),(38,36),(35,34),(35,30)],fill=dark)
        d.polygon([(37,27),(40,29),(40,32),(37,34),(34,32),(34,29)],fill=white)
        d.line((29,28,29,43),fill=light,width=2)
        d.line((14,20,14,37),fill=light,width=1)
    elif family == 'astra_critical_core':
        # A turquoise three-lobed radiation core, following the bright Lv1 body.
        d.ellipse((12,13,46,47),outline=mid,width=2)
        lobes = (((24,23),(21,15),(25,12),(33,12),(37,15),(34,23)),
                 ((36,29),(44,28),(47,34),(43,41),(37,44),(32,37)),
                 ((23,29),(15,28),(12,34),(16,41),(22,44),(27,37)))
        for poly in lobes:
            d.polygon([(x+1,y+2) for x,y in poly],fill=dark)
            d.polygon(poly,fill=light)
            d.line(poly[:3],fill=white,width=2)
        sphere(d,29,30,7,colors)
        d.rectangle((25,26,28,28),fill=white)
        d.line((16,19,19,16),fill=white,width=1)
    elif family == 'boundary_core':
        cube(d,29,10,18,17,(dark,mid,light,mid),False,3)
        cube(d,29,17,12,11,(dark,mid,light,light),False,3)
        cube(d,29,24,6,5,(dark,mid,light,'#ead595'),False,3)
        d.polygon([(29,29),(33,31),(33,35),(29,37),(25,35),(25,31)],fill=white)
        d.line((12,20,12,36),fill=light,width=1)
        d.line((18,25,18,33),fill=white,width=1)
    elif family == 'guogao_heart':
        # Reuse the original giant-globe/brand face geometry and palette: raised
        # inner brows, oval open eyes and the sweat drop inside the left cheek.
        d.ellipse((12,13,46,47),fill='#6b643c')
        face=globe_face(34)
        # Increase facial contrast at inventory size while keeping the motif's
        # actual positions; the warm globe brown is too close to its yellow face.
        for y in range(face.height):
            for x in range(face.width):
                if face.getpixel((x,y)) == (0x5d,0x3c,0x0d,255):
                    face.putpixel((x,y),(0x24,0x31,0x3b,255))
        im.alpha_composite(face,(12,13))
        # A slightly fuller worried opening remains visible in the 16px sample.
        d.ellipse((24,35,34,41),fill='#24313b')
        d.line((27,40,31,40),fill='#eca116',width=1)
    else:
        # Uneven cut shards keep the original crystal's teal/blue-violet facets.
        pieces = (
            [(17,19),(24,23),(23,39),(17,44),(12,37),(11,27)],
            [(30,10),(39,17),(37,39),(29,49),(23,36),(22,19)],
            [(43,28),(48,32),(45,43),(39,46),(37,35)])
        for points in pieces:
            d.polygon([(x+1,y+1) for x,y in points],fill=dark)
            d.polygon(points,fill=mid)
        d.polygon([(17,19),(24,23),(17,29),(11,27)],fill=light)
        d.polygon([(17,29),(23,24),(23,39),(17,44)],fill='#627699')
        d.polygon([(30,10),(39,17),(30,26),(22,19)],fill=light)
        d.polygon([(30,26),(37,19),(37,39),(29,49)],fill='#657692')
        d.polygon([(23,20),(29,26),(29,47),(23,36)],fill='#8cacc0')
        d.polygon([(43,28),(48,32),(42,37),(37,35)],fill=light)
        d.polygon([(42,37),(47,33),(45,43),(39,46)],fill='#5f678f')
        d.line((30,11,37,17),fill=white,width=2)
        d.line((13,26,16,22),fill=white,width=2)
        d.line((24,24,24,32),fill=white,width=1)
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
    sheet = Image.new('RGB',(4*176,4*192),'#202631'); d=ImageDraw.Draw(sheet)
    for row,metal in enumerate(METALS):
        d.text((12,row*192+4),metal,fill='#eeeeee')
        for col,stage in enumerate((-1,0,1,2)):
            im=bow_icon(row,stage).resize((144,144),Image.Resampling.NEAREST)
            sheet.paste(im,(col*176+12,row*192+24),im)
            d.text((col*176+12,row*192+172),'idle' if stage<0 else f'pull {stage}',fill='#eeeeee')
    sheet.save(out/'bows.png')
    print('Generated 27 perspective core sprites + original Ordinal Lv1 3D model + 16 bow images/models; world meshes untouched')


if __name__ == '__main__':
    generate()
