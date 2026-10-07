"""Offline, native-pixel projection of the authoritative world-core meshes.

Only the inventory generator uses this helper.  It reads mesh/texture resources
without altering them; no Minecraft renderer, model, animation or atlas changes.
"""
from functools import lru_cache
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageFilter

ASSETS = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/guogaology'
PROJECTION_CENTER = (29., 30.)  # Midpoint of opposite corners of the 64px case.


@lru_cache(maxsize=32)
def mesh(name):
    return json.loads((ASSETS / f'core_meshes/{name}.json').read_text('utf8'))


@lru_cache(maxsize=128)
def texture(resource):
    namespace, path = resource.split(':', 1)
    assets = ASSETS.parent / namespace
    return np.asarray(Image.open(assets / f'textures/{path}.png').convert('RGBA'), dtype=float)


def inner_quad(name, quad):
    t = quad['t']
    if name == 'sequence_core':
        # The matrix and brackets share material names with the outer case.
        # Its case lies at 0.6..1.12 / 14.88..15.4; the core is wholly inside.
        return t != 'optical_shell' and all(2 <= axis <= 14 for v in quad['v'] for axis in v)
    if name == 'lho_trace':
        return t != 'c1_frame'
    if name == 'boundary_core':
        return t != 'c1_glass'
    if name.startswith('ordinal_crystal'):
        return 'shell' not in t and 'glass' not in t
    if name == 'guogao_heart':
        return t.startswith('face_')
    return not any(word in t for word in ('glass', 'clasp', 'particle', 'case_'))


def triangle(canvas, xy, uv, colors, image, shade, simplify, opaque):
    """Nearest texture sampling with planar UVs and vertex tint interpolation."""
    size = canvas.shape[0]
    lo = np.maximum(np.floor(xy.min(axis=0)).astype(int), 0)
    hi = np.minimum(np.ceil(xy.max(axis=0)).astype(int), size-1)
    if np.any(lo > hi):
        return
    xs, ys = np.meshgrid(np.arange(lo[0], hi[0]+1)+.5,
                         np.arange(lo[1], hi[1]+1)+.5)
    a, b, c = xy
    den = (b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
    if abs(den) < 1e-7:
        return
    wa = ((b[1]-c[1])*(xs-c[0])+(c[0]-b[0])*(ys-c[1]))/den
    wb = ((c[1]-a[1])*(xs-c[0])+(a[0]-c[0])*(ys-c[1]))/den
    wc = 1-wa-wb
    mask = (wa >= -1e-7) & (wb >= -1e-7) & (wc >= -1e-7)
    if not mask.any():
        return
    weights = np.stack((wa, wb, wc), axis=-1)
    tex_uv = weights @ uv
    tint = weights @ colors
    tx = np.clip((tex_uv[...,0]*image.shape[1]).astype(int), 0, image.shape[1]-1)
    ty = np.clip((tex_uv[...,1]*image.shape[0]).astype(int), 0, image.shape[0]-1)
    sampled = image[ty,tx]
    source = sampled[:,:,:3] * tint[:,:,:3] / 255 * shade
    if simplify:
        source = np.clip(np.round(source/16)*16, 0, 255)
    alpha = sampled[:,:,3] * tint[:,:,3] / (255*255) * mask
    if opaque:
        # Clear material colour reads better than stacking several translucent
        # surfaces in one tiny sprite; retain the actual holes and silhouette.
        alpha = np.where(alpha > 0, 1., 0.)
    elif simplify:
        # LHO retains its real translucent breaks and ghosts.  Raise only their
        # readability against the dark glass; never fill holes or redraw ∅.
        alpha = np.minimum(alpha*1.45, .9)
        source = np.minimum(source*1.12+np.array([8,16,22]), 255)
    dest = canvas[lo[1]:hi[1]+1,lo[0]:hi[0]+1]
    out_alpha = alpha+dest[:,:,3]*(1-alpha)
    premul = source*alpha[:,:,None]+dest[:,:,:3]*dest[:,:,3,None]*(1-alpha[:,:,None])
    dest[:,:,:3] = premul / np.maximum(out_alpha[:,:,None], 1e-8)
    dest[:,:,3] = out_alpha


def project_points(name, vertices, magnify=1., size=64):
    """One model origin and one uniform scale; never compensate for a badge."""
    vs = np.asarray(vertices, dtype=float)
    sign = -1 if name == 'lho_trace' else 1
    center = np.asarray(PROJECTION_CENTER)
    relative = np.column_stack((sign*(vs[:,0]+vs[:,2]-16)*1.625,
                                -(vs[:,1]-8)*1.75+sign*(vs[:,0]-vs[:,2])*.8125))
    return (center+relative*magnify)*(size/64)


@lru_cache(maxsize=64)
def project(name, size=64, include_shell=False, simplify=True, magnify=1.):
    model = mesh(name)
    canvas = np.zeros((size,size,4), dtype=float)
    quads = [q for q in model['quads'] if include_shell or inner_quad(name,q)]
    # Same projection as the hand-drawn glass case: 2:1 diagonals, vertical Y.
    # View the positive X / negative Z corner.  This reveals the tower's diagonal
    # ascending boxes instead of hiding them along the camera's view direction.
    # The LHO letter is shown from its opposite readable face; no mirrored glyph.
    sign = -1 if name == 'lho_trace' else 1
    quads.sort(key=lambda q: np.mean([sign*(v[0]-v[2])+v[1]*1.2 for v in q['v']]))
    for quad in quads:
        normal = np.mean(quad['n'], axis=0)
        if normal @ np.array([float(sign),1.2,float(-sign)]) <= 1e-6:
            continue
        vs = np.asarray(quad['v'], dtype=float)
        xy = project_points(name, vs, magnify, size)
        colors = np.array([[(c>>16)&255,(c>>8)&255,c&255,(c>>24)&255]
                           for c in quad['c']], dtype=float)
        uv = np.asarray(quad['uv'], dtype=float)
        image = texture(model['textures'][quad['t']])
        # Mesh vertex colours already contain deliberate facet shading.  Keep
        # those colours legible instead of applying a second dark pass to icons.
        shade = 1. if simplify else .76+.24*max(0,normal[1])
        for indices in ((0,1,2),(0,2,3)):
            ix=list(indices)
            triangle(canvas,xy[ix],uv[ix],colors[ix],image,shade,simplify,
                     simplify and name != 'lho_trace')
    rgba = np.zeros((size,size,4), dtype=np.uint8)
    rgba[:,:,:3] = np.clip(canvas[:,:,:3],0,255).astype(np.uint8)
    rgba[:,:,3] = np.clip(canvas[:,:,3]*255,0,255).astype(np.uint8)
    result = Image.fromarray(rgba)
    if simplify and not include_shell:
        # One deliberate silhouette line; never invent internal branches/facets.
        mask = result.getchannel('A').point(lambda a: 255 if a > 128 else 0)
        outline = mask.filter(ImageFilter.MaxFilter(3))
        ink = Image.new('RGBA',result.size,'#1b2d37')
        ink.putalpha(outline)
        ink.alpha_composite(result)
        result = ink
    return result
