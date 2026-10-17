"""Offline32-pixel art and a small stepped rounded shell for Guogao.

Hand-drawn pixel geometry follows Apple's anxious face with sweat: wide-set
worried brows, short vertical oval eyes, a small rounded open mouth and one
left-cheek sweat drop. The pale blue/lavender forehead turns into a golden
lower face. Facial features stay in the central28x28 region; the outer two
pixels supply a single1/16-block, axis-aligned corner step of the shell.

Visual reference (analysis only, not a bundled or resampled source asset):
https://emojipedia.org/apple/ios-17.4/anxious-face-with-sweat
https://em-content.zobj.net/source/apple/391/anxious-face-with-sweat_1f630.png
"""
from PIL import Image, ImageDraw


SIZE = 32
# A small set of broad color steps keeps the cold-to-gold transition legible as
# Minecraft pixels. Unlike the old globe projection, no circular edge is drawn.
SKIN_STOPS = (
    (0, (76, 140, 244)),
    (2, (197, 211, 255)),
    (6, (177, 190, 248)),
    (10, (151, 171, 216)),
    (12, (175, 184, 149)),
    (15, (229, 211, 91)),
    (17, (252, 215, 61)),
    (22, (255, 203, 42)),
    (27, (246, 177, 23)),
    (31, (223, 141, 12)),
)
BROW = (48, 57, 50)


def _shade(rgb, delta):
    return tuple(max(0, min(255, channel + delta)) for channel in rgb)


def _skin(y):
    # Native two-pixel bands, without supersampling, smoothing or dithering.
    y = min(31, (y // 2) * 2 + .5)
    for (a, first), (b, second) in zip(SKIN_STOPS, SKIN_STOPS[1:]):
        if y <= b:
            t = (y - a) / (b - a)
            return tuple(round(x + (z - x) * t) for x, z in zip(first, second))
    return SKIN_STOPS[-1][1]


def _curve(draw, points, fill, width):
    """Draw a cubic on the native pixel grid, without supersampling."""
    a, b, c, d = points
    path = []
    for i in range(33):
        t = i / 32
        path.append(tuple(round((1-t)**3*a[j] + 3*(1-t)**2*t*b[j]
                                + 3*(1-t)*t*t*c[j] + t**3*d[j])
                          for j in range(2)))
    draw.line(path, fill=fill, width=width, joint='curve')
    for x, y in (path[0], path[-1]):
        draw.point((x, y), fill=fill)


def guogao_face_texture():
    """Return native32x32 RGBA art, with complete features inside a28x28 area.

    Apple-style proportions keep the brows outside the eyes, the mouth small
    and low, and the sweat beside the left eye. These are authored pixel curves
    and polygons, not pixels resampled from the reference. All six faces use
    this complete image, with every feature safely inside the texture edges.
    """
    im = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        color = _skin(y)
        for x in range(SIZE):
            # Subtle straight edge facets belong to the square block itself.
            delta = 3 if x == 0 else -4 if x == 31 else 0
            if y == 0:
                delta += 2
            elif y == 31:
                delta -= 3
            im.putpixel((x, y), (*_shade(color, delta), 255))
    draw = ImageDraw.Draw(im)

    # Apple's brows are widely separated: each raised inner end sits above
    # its eye, rather than reaching toward the middle into a peaked roof.
    _curve(draw, ((5, 12), (7, 13), (10, 11), (11, 9)), BROW, 1)
    _curve(draw, ((21, 9), (22, 11), (25, 13), (27, 12)), BROW, 1)

    # Warm shaded oval eyes, with the restrained amber upper glint seen in
    # the Apple face; no wide white surprised eyes or long cartoon pupils.
    for x in (10, 19):
        draw.ellipse((x, 15, x+3, 20), fill=(181, 117, 15))
        draw.ellipse((x+1, 16, x+2, 19), fill=(99, 66, 13))
        draw.point((x+1, 16), fill=(148, 105, 22))

    # Apple's small open frown arches up in the middle on BOTH boundaries;
    # it is neither a straight capsule nor a screaming O. The lower lip is
    # a thin warm arch with the rounded corners continuing downward.
    draw.polygon(((10, 25), (11, 24), (13, 23), (15, 22), (18, 22),
                  (20, 23), (21, 24), (22, 25), (22, 26), (21, 28),
                  (19, 28), (17, 27), (15, 27), (13, 28), (11, 28),
                  (10, 27)), fill=(178, 109, 15))
    draw.polygon(((12, 25), (13, 24), (15, 23), (18, 23), (20, 24),
                  (21, 25), (21, 26), (20, 27), (18, 26), (15, 26),
                  (13, 27), (12, 26)), fill=(96, 57, 12))
    draw.line(((12, 27), (15, 26), (18, 26), (20, 27)),
              fill=(238, 172, 41), width=1)

    # Apple's left-cheek sweat begins beside the eye, with a narrow point
    # and broad rounded base. Its blue outline and pale center remain clear
    # at Minecraft inventory size, entirely inside the square face.
    draw.polygon(((5, 16), (6, 18), (8, 22), (9, 25), (8, 27),
                  (7, 28), (5, 28), (3, 27), (2, 25), (2, 23),
                  (4, 19)), fill=(15, 132, 192))
    draw.polygon(((5, 18), (6, 20), (7, 23), (8, 25), (7, 27),
                  (5, 27), (3, 25), (3, 23), (4, 21)),
                 fill=(86, 210, 244))
    draw.line(((5, 20), (4, 22), (4, 25), (5, 26)),
              fill=(229, 253, 255), width=1)
    return im


def guogao_cap_texture(top=True):
    """Compatibility API: top AND bottom now contain the same complete face."""
    return guogao_face_texture()


def guogao_cube_model():
    """Return a closed, lightly rounded vanilla model, with SIX complete faces.

    A14-unit inner body and six14-unit-wide, one-unit-thick face caps form a
    SINGLE1/16-block corner step. Offline union trimming emits only the30
    external rectangles, with integer model coordinates and no half-unit tier.
    UVs retain native pixel scale and sample the
    complete28x28 expression region on every broad face. No custom renderer,
    transparent corner mask, overlapping body faces or sphere is required.
    Collision/outline and native light15 remain the block's properties.
    """
    occupied={(x,y,z) for x in range(1,15) for y in range(1,15) for z in range(1,15)}
    for axis in range(3):
        others=[a for a in range(3) if a!=axis]
        for edge in (0,15):
            for u in range(1,15):
                for v in range(1,15):
                    p=[0,0,0];p[axis]=edge;p[others[0]]=u;p[others[1]]=v
                    occupied.add(tuple(p))
    directions=(('west',0,-1,2,1),('east',0,1,2,1),('down',1,-1,0,2),
                ('up',1,1,0,2),('north',2,-1,0,1),('south',2,1,0,1))
    planes={}
    for p in sorted(occupied):
        for name,axis,sign,u,v in directions:
            neighbor=list(p);neighbor[axis]+=sign
            if tuple(neighbor) in occupied:
                continue
            planes.setdefault((name,axis,p[axis]+(sign>0),u,v),set()).add((p[u],p[v]))
    elements=[]
    for (name,axis,plane,u,v),cells in sorted(planes.items()):
        while cells:
            x,y=min(cells,key=lambda q:(q[1],q[0]));width=1;height=1
            while (x+width,y) in cells:width+=1
            while all((xx,y+height) in cells for xx in range(x,x+width)):height+=1
            cells.difference_update((xx,yy) for xx in range(x,x+width) for yy in range(y,y+height))
            a=[0,0,0];b=[0,0,0];a[axis]=b[axis]=plane
            a[u]=x;b[u]=x+width;a[v]=y;b[v]=y+height
            if name=='down':uv=[a[0],16-b[2],b[0],16-a[2]]
            elif name=='up':uv=[a[0],a[2],b[0],b[2]]
            elif name=='north':uv=[16-b[0],16-b[1],16-a[0],16-a[1]]
            elif name=='south':uv=[a[0],16-b[1],b[0],16-a[1]]
            elif name=='west':uv=[a[2],16-b[1],b[2],16-a[1]]
            else:uv=[16-b[2],16-b[1],16-a[2],16-a[1]]
            face={'uv':uv,'texture':'#body'}
            if plane in (0,16):face['cullface']=name
            elements.append({'from':a,'to':b,'faces':{name:face}})
    return {'parent':'minecraft:block/block','textures':{'body':'guogaology:block/guogao_drop',
            'particle':'guogaology:block/guogao_drop'},'elements':elements}
