"""Pixel art for the solid Guogao block, generated once with the resources.

The four walls share an upright square face. The forehead and lower face wrap
to dedicated blue/gold caps rather than repeating a sideways face overhead.
Facial geometry and the original emoji palette follow ``MosaicMotifs.face``
and ``generate_brand.globe_face``; the old circular silhouette is deliberately
absent. Every texel is opaque and each complete feature fits inside the UV.
"""
import math

from PIL import Image


SIZE = 32
PALETTE = (
    (244, 250, 242),  # sweat highlight
    (255, 239, 150),  # yellow under the cold forehead
    (255, 205, 45),   # main gold face
    (236, 161, 22),   # warm lower face
    (199, 117, 16),
    (93, 60, 13),     # original brown eyes and open mouth
    (31, 40, 54),     # anxious brows
    (42, 115, 214),   # sweat shadow
    (67, 167, 243),   # blue forehead and sweat
    (137, 203, 250),
    (204, 231, 251),
)


def _shade(rgb, delta):
    return tuple(max(0, min(255, channel + delta)) for channel in rgb)


def _skin(x, y):
    """Broad square color planes; no circular rim or pasted-on background."""
    # Keep the original globe's cool forehead and three warm face bands.
    color = PALETTE[10 if y < 5 else 9 if y < 9 else 8 if y < 12
                    else 1 if y < 16 else 2 if y < 26 else 3]
    # Quiet one- and two-texel facets run to the block edges. Their small value
    # changes make the material legible without outlining a sticker or a gem.
    delta = 5 if x < 2 else -7 if x > 29 else 0
    if y == 0:
        delta += 3
    elif y == 31:
        delta -= 4
    # Sparse two-pixel mineral marks, placed away from the expression.
    if (x, y) in {(3, 3), (4, 3), (24, 6), (25, 6),
                  (27, 20), (28, 20), (7, 29), (8, 29), (23, 28)}:
        delta += 6
    return _shade(color, delta)


def guogao_face_texture():
    """Return the opaque 32x32 RGBA wall texture with a complete Guogao face.

    The expression retains the reference's raised inner brow ends, separate
    narrow eyes, small open mouth and a single long blue sweat drop. Scaling
    the feature coordinates to a 30-pixel region leaves a safe edge margin;
    the square skin itself fills the complete block face.
    """
    im = Image.new('RGBA', (SIZE, SIZE))
    center = (SIZE - 1) * .5
    radius = 15.0
    for y in range(SIZE):
        for x in range(SIZE):
            u, v = (x - center) / radius, (center - y) / radius
            a = abs(u)
            color = _skin(x, y)
            # Same sloping worried brows as the approved globe/logo.
            brow = .48 - .63 * (a - .20)
            if .18 < a < .67 and abs(v - brow) < .065:
                color = PALETTE[6]
            if ((a - .30) / .115) ** 2 + ((v + .04) / .155) ** 2 < 1:
                color = PALETTE[5]
            if a < .32 and -.64 + .30 * u * u < v < -.40 - .75 * u * u:
                color = PALETTE[5]
            t = (.11 - v) / .79
            if 0 <= t <= 1:
                width = .22 * math.sin(math.pi * t) ** .70
                if abs(u + .72) < width:
                    color = (PALETTE[0] if -.82 < u < -.72 and .34 < t < .77
                             else PALETTE[7] if u > -.66 else PALETTE[8])
            im.putpixel((x, y), (*color, 255))
    return im


def guogao_cap_texture(top=True):
    """Return the solid blue crown or warm gold base, also opaque 32x32."""
    color = PALETTE[10 if top else 3]
    im = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            # Two broad stepped facets, not a circular medallion. Keep the
            # perimeter close to the top/bottom wall colors for a solid cube.
            delta = 0
            if 4 <= x <= 26 and 4 <= y <= 26:
                delta = 3 if top else 5
            if x + y > 43 and x < 30 and y < 30:
                delta = -6
            if (x, y) in {(8, 7), (9, 7), (22, 13), (23, 13),
                          (12, 24), (13, 24)}:
                delta += 5
            im.putpixel((x, y), (*_shade(color, delta), 255))
    return im


def guogao_cube_model():
    """Return a vanilla full cube; all four faces show the complete expression.

    ``cube_bottom_top`` supplies the standard six complete 0..16 face UVs,
    correct outward orientation and ordinary adjacent-block face culling.
    There is no sliced union geometry and no transparent or emissive overlay.
    Luminance is the block's native property, set by the block registration.
    """
    return {
        'parent': 'minecraft:block/cube_bottom_top',
        'textures': {
            'side': 'guogaology:block/guogao_drop',
            'top': 'guogaology:block/guogao_drop_top',
            'bottom': 'guogaology:block/guogao_drop_bottom',
        },
    }
