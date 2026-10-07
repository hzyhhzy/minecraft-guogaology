"""Trim permanent coplanar core overlaps without changing the visible silhouette.

The operation is offline, uses only Python's standard library, and preserves the
GPU's original 012/023 triangles and their UV/color/normal interpolation. Faces
on different moving parts, opposite-facing sheets, and different materials are
never merged. The canonical art entry points run this before the LOD UV pass.
"""
from __future__ import annotations

from collections import defaultdict
from copy import deepcopy
import argparse
import json
import math
from pathlib import Path

from audit_core_surfaces import cross, cross2, dot, length, signed_area, sub

ROOT = Path(__file__).resolve().parents[1]
EPS = 1e-9
AREA_EPS = 1e-8


def clean_polygon(points):
    result = []
    for p in points:
        if not result or length(sub(p, result[-1])) > EPS:
            result.append(tuple(p))
    if len(result) > 1 and length(sub(result[0], result[-1])) <= EPS:
        result.pop()
    return result if len(result) >= 3 and abs(signed_area(result)) > AREA_EPS else []


def half_plane(poly, a, b, inside):
    """Clip one convex polygon; retain either side of an oriented edge."""
    if not poly:
        return []
    edge = sub(b, a)
    result = []
    previous = poly[-1]
    previous_d = cross2(edge, sub(previous, a))
    previous_in = previous_d >= 0 if inside else previous_d <= 0
    for point in poly:
        distance = cross2(edge, sub(point, a))
        point_in = distance >= 0 if inside else distance <= 0
        if point_in != previous_in:
            t = previous_d / (previous_d - distance)
            result.append(tuple(previous[k] + t * (point[k] - previous[k]) for k in range(2)))
        if point_in:
            result.append(point)
        previous, previous_d, previous_in = point, distance, point_in
    return clean_polygon(result)


def difference(subject, clip):
    """Return disjoint convex pieces of subject minus a convex clip polygon."""
    if signed_area(clip) < 0:
        clip = list(reversed(clip))
    overlap = subject
    for i, a in enumerate(clip):
        overlap = half_plane(overlap, a, clip[(i + 1) % len(clip)], True)
        if not overlap:
            return [subject]
    if abs(signed_area(overlap)) <= AREA_EPS:
        return [subject]
    pieces = []
    remaining = subject
    for i, a in enumerate(clip):
        b = clip[(i + 1) % len(clip)]
        outside = half_plane(remaining, a, b, False)
        if outside:
            pieces.append(outside)
        remaining = half_plane(remaining, a, b, True)
        if not remaining:
            break
    return pieces


def triangle(q, indices):
    points = [tuple(q['v'][i]) for i in indices]
    n = cross(sub(points[1], points[0]), sub(points[2], points[0]))
    size = length(n)
    if size < EPS:
        return None
    normal = tuple(x / size for x in n)
    # Determine orientation from the common quantized plane, not a numerically
    # tiny nominally-zero component of a newly clipped slender triangle.
    sign = next(1 if x > 0 else -1 for x in (round(v, 3) for v in normal) if x)
    canonical = tuple(x * sign for x in normal)
    rounded = tuple(round(x, 5) for x in canonical)
    axis = max(range(3), key=lambda k: abs(rounded[k]))
    polygon = [tuple(p[k] for k in range(3) if k != axis) for p in points]
    if abs(signed_area(polygon)) < AREA_EPS:
        return None
    d = dot(canonical, points[0])
    return {'indices': indices, 'points': points, 'n': normal, 'plane': canonical,
            'd': d, 'axis': axis, 'poly': polygon,
            # Broad normal bucket, then strict symmetric coplanarity below.
            # Five-decimal asset rounding can put one real plane on opposite
            # sides of a decimal bucket (especially 45-degree branches).
            'key': (q.get('part', 1), q['t'], sign, tuple(round(x, 3) for x in canonical))}


def compatible(a, b):
    return (abs(dot(a['plane'], b['plane']) - 1) < 1e-7
            and all(abs(dot(a['plane'], p) - a['d']) <= 3e-5 for p in b['points'])
            and all(abs(dot(b['plane'], p) - b['d']) <= 3e-5 for p in a['points']))


def repair_crossed_quad(q):
    """Correct planar beam sides with accidentally crossed perimeter order.

    A sphere's genuinely warped faces must retain their original GPU triangles.
    Reordering the arrays together leaves every UV/color attached to its vertex.
    """
    a, b = triangle(q, (0, 1, 2)), triangle(q, (0, 2, 3))
    if a is None or b is None or dot(a['n'], b['n']) >= 0 or not compatible(a, b):
        return q, False
    axis = a['axis']
    projected = [tuple(p[k] for k in range(3) if k != axis) for p in q['v']]
    center = [sum(p[k] for p in projected)/4 for k in range(2)]
    order = sorted(range(4), key=lambda i: math.atan2(projected[i][1]-center[1], projected[i][0]-center[0]))
    # Keep the old first vertex and outward material normal for winding/UV origin.
    offset = order.index(0)
    order = order[offset:] + order[:offset]
    points = [q['v'][i] for i in order]
    n = cross(sub(points[1], points[0]), sub(points[2], points[0]))
    if dot(n, q['n'][0]) < 0:
        order = [order[0]] + list(reversed(order[1:]))
    result = deepcopy(q)
    for field in ('v', 'uv', 'n', 'c', 'lod_uv'):
        if field in q:
            result[field] = [q[field][i] for i in order]
    result.pop('lod_t', None)
    result.pop('lod_uv', None)
    return result, True


def interpolate(q, source, point):
    a, b, c = source['poly']
    denominator = cross2(sub(b, a), sub(c, a))
    wb = cross2(sub(point, a), sub(c, a)) / denominator
    wc = cross2(sub(b, a), sub(point, a)) / denominator
    weights = (1 - wb - wc, wb, wc)
    indices = source['indices']
    def vector(field):
        return [round(sum(weights[j] * q[field][indices[j]][k] for j in range(3)), 10)
                for k in range(len(q[field][indices[0]]))]
    vertex = {'v': vector('v'), 'uv': vector('uv'), 'n': vector('n')}
    color = 0
    for shift in (24, 16, 8, 0):
        channel = round(sum(weights[j] * ((q['c'][indices[j]] >> shift) & 255) for j in range(3)))
        color |= max(0, min(255, channel)) << shift
    vertex['c'] = color
    if 'lod_uv' in q:
        vertex['lod_uv'] = vector('lod_uv')
    return vertex


def emit_piece(q, source, poly):
    # Convex clipping keeps the source triangle winding. Triangulating a clipped
    # triangle also retains the GPU's original diagonal interpolation exactly.
    output = []
    for i in range(1, len(poly) - 1):
        points = [poly[0], poly[i], poly[i + 1], poly[i + 1]]
        if abs(signed_area(points)) <= AREA_EPS:
            continue
        vertices = [interpolate(q, source, p) for p in points]
        result = deepcopy(q)
        # The next canonical LOD pass rebakes tint tiles from the newly
        # interpolated material. Do not leave obsolete fallback atlas mappings.
        result.pop('lod_t', None)
        result.pop('lod_uv', None)
        for field in ('v', 'uv', 'n', 'c'):
            result[field] = [v[field] for v in vertices]
        output.append(result)
    return output


def sanitize(data, name='', allowed_parts=None):
    """Return mesh plus changed-face count, preserving exact untouched quads."""
    repaired = [repair_crossed_quad(q) if allowed_parts is None or q.get('part', 1) in allowed_parts
                else (q, False) for q in data['quads']]
    quads = [q for q, _ in repaired]
    repaired_count = sum(fixed for _, fixed in repaired)
    # Reviewed solid-to-solid junctions only. Complete paired thin sheets are
    # intentionally kept; they have readable front/back UVs and culling enabled.
    contact_materials = {
        'hydra_bud': (0, frozenset(('c_stem', 'c_tip'))),
        'sequence_core_lv2': (1, frozenset(('optical_clean_255',))),
        'sequence_core_lv3': (1, frozenset(('optical_clean_255',))),
    }.get(name)
    contacts = defaultdict(list)
    contact_markers = 0
    contact_participants = set()
    if contact_materials:
        part, textures = contact_materials
        for index, q in enumerate(quads):
            if q.get('part', 1) != part or q['t'] not in textures or q.get('internal_cap_checked', False):
                continue
            for indices in ((0, 1, 2), (0, 2, 3)):
                source = triangle(q, indices)
                if source is None or max(abs(x) for x in source['n']) < .999999:
                    continue
                source['face'] = index
                source['shape'] = frozenset(tuple(p) for p in q['v'])
                contacts[source['key'][3]].append(source)
                contact_participants.add(index)
        for index in contact_participants:
            # After clipping, formerly equal front/back page outlines may have
            # unequal fragments. Preserve the reviewed original cap decision on
            # subsequent runs rather than reinterpret those as buried end caps.
            quads[index] = dict(quads[index], internal_cap_checked=True)
            contact_markers += 1
    covered = defaultdict(list)
    output = []
    changed = 0
    removed_area = 0.0
    contact_removed_area = 0.0
    for face_index, q in enumerate(quads):
        if allowed_parts is not None and q.get('part', 1) not in allowed_parts:
            output.append(q)
            continue
        parts = []
        face_changed = False
        for indices in ((0, 1, 2), (0, 2, 3)):
            source = triangle(q, indices)
            if source is None:
                continue
            pieces = [source['poly']]
            original_area = abs(signed_area(source['poly']))
            if face_index in contact_participants:
                shape = frozenset(tuple(p) for p in q['v'])
                for prior in contacts[source['key'][3]]:
                    if (prior['face'] == face_index or prior['shape'] == shape
                            or dot(source['n'], prior['n']) >= 0 or not compatible(source, prior)):
                        continue
                    clip = [tuple(p[k] for k in range(3) if k != source['axis']) for p in prior['points']]
                    pieces = [piece for remaining in pieces for piece in difference(remaining, clip)]
                contact_remaining = sum(abs(signed_area(p)) for p in pieces)
                contact_removed_area += (original_area-contact_remaining) / abs(source['plane'][source['axis']])
            for prior in covered[source['key']]:
                if not compatible(source, prior):
                    continue
                # Reproject in this triangle's stable common plane basis.
                clip = [tuple(p[k] for k in range(3) if k != source['axis']) for p in prior['points']]
                pieces = [piece for remaining in pieces for piece in difference(remaining, clip)]
                if not pieces:
                    break
            remaining_area = sum(abs(signed_area(p)) for p in pieces)
            if original_area - remaining_area > AREA_EPS:
                face_changed = True
                removed_area += (original_area - remaining_area) / abs(source['plane'][source['axis']])
            parts.append((source, pieces))
            # Original coverage equals earlier coverage plus the newly retained
            # pieces; recording it avoids fragmented clipping during later faces.
            covered[source['key']].append(source)
        # A self-crossing quad can contain GPU triangles with opposite winding.
        # Preserve both original one-sided triangles explicitly rather than
        # treating the invalid bow-tie polygon as an empty/convex surface.
        if len(parts) == 2 and parts[0][0]['key'][2] != parts[1][0]['key'][2]:
            face_changed = True
        if face_changed:
            changed += 1
            for source, pieces in parts:
                for piece in pieces:
                    output.extend(emit_piece(q, source, piece))
        else:
            output.append(q)
    result = dict(data)
    result['quads'] = output
    return result, {'changed_faces': changed + repaired_count + contact_markers, 'repaired_beam_sides': repaired_count,
                    'reviewed_cap_markers': contact_markers,
                    'before': len(data['quads']), 'after': len(output),
                    'redundant_area_removed': removed_area, 'internal_cap_area_removed': contact_removed_area}


def generate(meshes=None, include_absence=False):
    meshes = meshes or ROOT/'src/main/resources/assets/guogaology/core_meshes'
    report = {}
    for path in sorted(meshes.glob('*.json')):
        if path.stem.startswith('lho_trace') and not include_absence:
            continue  # Absence has its own independently edited canonical artist.
        data = json.loads(path.read_text('utf8'))
        result, stats = sanitize(data, path.stem, {10} if path.stem.startswith('lho_trace') else None)
        if stats['changed_faces']:
            path.write_text(json.dumps(result, ensure_ascii=False, separators=(',', ':')) + '\n', 'utf8')
            report[path.stem] = stats
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--include-absence', action='store_true')
    args = parser.parse_args()
    print(json.dumps(generate(include_absence=args.include_absence), indent=2))
