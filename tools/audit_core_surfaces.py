"""Read-only coplanar surface audit for baked crystal meshes (stdlib only).

Coordinates/areas use model units (16 units = one block). Positive intersection
area is required; shared edges and vertices never count as overlapping faces.
Exactly reversed copies sharing a texture are reported separately as explicit
two-sided sheets (backside UVs may intentionally differ to keep text readable).
Partial opposite contacts are reported but do not create a duplicate visible
surface in a backface-culling pipeline. Same-direction overlap is the hard error.
Different animation parts are a rest-pose finding,
not a permanent duplicate. Warped quads are tested as the two GPU triangles.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import json
import hashlib
import math
from pathlib import Path

EPS = 1e-6
AREA_EPS = 1e-7
PLANE_EPS = 3e-5  # source vertices are rounded to 5 decimals in model units
CLIP_EPS = 1e-12
ROUNDING_AREA_EPS = 1e-5
ROOT = Path(__file__).resolve().parents[1]


def sub(a, b): return tuple(x-y for x, y in zip(a, b))
def dot(a, b): return sum(x*y for x, y in zip(a, b))
def cross(a, b): return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
def length(v): return math.sqrt(dot(v, v))
def cross2(a, b): return a[0]*b[1]-a[1]*b[0]


def signed_area(poly):
    return sum(cross2(poly[i], poly[(i+1) % len(poly)]) for i in range(len(poly)))/2


def unique_vertices(points):
    result = []
    for p in points:
        if not result or length(sub(p, result[-1])) > EPS:
            result.append(tuple(p))
    if len(result) > 1 and length(sub(result[0], result[-1])) <= EPS:
        result.pop()
    return result


def clip_convex(subject, clip):
    if signed_area(clip) < 0: clip = list(reversed(clip))
    result = subject
    for i, a in enumerate(clip):
        b = clip[(i+1) % len(clip)]
        edge = sub(b, a)
        output = []
        if not result: break
        last = result[-1]
        last_d = cross2(edge, sub(last, a))
        for p in result:
            dist = cross2(edge, sub(p, a))
            inside, last_inside = dist >= -CLIP_EPS, last_d >= -CLIP_EPS
            if inside != last_inside:
                t = last_d/(last_d-dist)
                output.append(tuple(last[k]+t*(p[k]-last[k]) for k in range(2)))
            if inside: output.append(p)
            last, last_d = p, dist
        result = output
    return result


def face_polygons(q, index):
    points = unique_vertices(q['v'])
    if len(points) < 3: return [], 'degenerate'
    n = None
    for i in range(1, len(points)-1):
        candidate = cross(sub(points[i], points[0]), sub(points[i+1], points[0]))
        norm = length(candidate)
        if norm > EPS:
            n = tuple(v/norm for v in candidate)
            break
    if n is None: return [], 'degenerate'
    d = dot(n, points[0])
    warped = any(abs(dot(n, p)-d) > EPS*10 for p in points)
    initial_axis = max(range(3), key=lambda k: abs(n[k]))
    projection = [tuple(p[k] for k in range(3) if k != initial_axis) for p in points]
    turns = [cross2(sub(projection[(i+1) % len(points)], projection[i]),
                    sub(projection[(i+2) % len(points)], projection[(i+1) % len(points)]))
             for i in range(len(points))]
    nonconvex = any(t > EPS for t in turns) and any(t < -EPS for t in turns)
    # A concave/bow-tie QUADS primitive is not a single polygon on the GPU.
    # Its 0-1-2 and 0-2-3 triangles may overlap even within the same face.
    groups = [points] if not (warped or nonconvex) else [points[:3], [points[0], points[2], points[3]]]
    result = []
    for points in groups:
        c = cross(sub(points[1], points[0]), sub(points[2], points[0]))
        norm = length(c)
        if norm <= EPS: continue
        normal = tuple(v/norm for v in c)
        sign = next(1 if v > 0 else -1 for v in normal if abs(v) > EPS)
        canonical = tuple(v*sign for v in normal)
        plane_d = dot(canonical, points[0])
        axis = max(range(3), key=lambda k: abs(canonical[k]))
        projected = [tuple(p[k] for k in range(3) if k != axis) for p in points]
        if abs(signed_area(projected)) <= AREA_EPS: continue
        result.append({'index': index, 'points': points, 'normal': normal, 'plane': canonical,
                       'd': plane_d, 'key': tuple(round(v, 5) for v in canonical)+(round(plane_d, 5),),
                       'axis': axis, 'poly': projected,
                       'bounds3': tuple((min(p[k] for p in points), max(p[k] for p in points)) for k in range(3)),
                       'bounds': (min(p[0] for p in projected), max(p[0] for p in projected),
                                  min(p[1] for p in projected), max(p[1] for p in projected))})
    return result, 'warped' if warped else 'nonconvex_gpu_triangles' if nonconvex else None


def exact_shape(a, b):
    return sorted(tuple(round(v, 5) for v in p) for p in a['points']) == sorted(tuple(round(v, 5) for v in p) for p in b['points'])


def vertex_material(q):
    # For a normal backside the material should agree at the same world vertex,
    # not merely have the same texture name with a different/mirrored UV map.
    return {tuple(round(v, 5) for v in p): (tuple(round(x, 5) for x in uv), color)
            for p, uv, color in zip(q['v'], q['uv'], q['c'])}


def analyze(path, fixture=None):
    source = path.read_bytes() if fixture is None else json.dumps(fixture).encode('utf8')
    data = json.loads(source)
    quads = data['quads']; planes = defaultdict(list); malformed = defaultdict(list)
    for i, q in enumerate(quads):
        surfaces, error = face_polygons(q, i)
        if error: malformed[error].append(i)
        # Plane-hash quantization can separate near-identical planes on a bin
        # boundary. Sweep all 3D bounding boxes instead, then verify coplanarity.
        for face in surfaces: planes[None].append(face)
    found = {}
    edge_contacts = set()
    for group in planes.values():
        group.sort(key=lambda f: f['bounds3'][0][0])
        for ai, a in enumerate(group):
            for b in group[ai+1:]:
                if b['bounds3'][0][0] > a['bounds3'][0][1]+EPS: break
                if any(min(a['bounds3'][k][1], b['bounds3'][k][1]) < max(a['bounds3'][k][0], b['bounds3'][k][0])-EPS for k in (1,2)): continue
                parallel_dot = abs(dot(a['plane'], b['plane']))
                if abs(parallel_dot-1) > 1e-7: continue
                separation = max([abs(dot(a['plane'], p)-a['d']) for p in b['points']] +
                                 [abs(dot(b['plane'], p)-b['d']) for p in a['points']])
                if separation > PLANE_EPS: continue
                # Equal normal components can choose different major axes after
                # floating-point normalization. Both polygons must use one basis.
                bpoly = [tuple(p[k] for k in range(3) if k != a['axis']) for p in b['points']]
                aa = a['bounds']
                bb = (min(p[0] for p in bpoly), max(p[0] for p in bpoly),
                      min(p[1] for p in bpoly), max(p[1] for p in bpoly))
                if min(aa[1], bb[1]) < max(aa[0], bb[0])-EPS or min(aa[3], bb[3]) < max(aa[2], bb[2])-EPS: continue
                polygon = clip_convex(a['poly'], bpoly)
                area = abs(signed_area(polygon))/abs(a['plane'][a['axis']]) if len(polygon) >= 3 else 0
                pair = tuple(sorted((a['index'], b['index'])))
                if area <= AREA_EPS:
                    edge_contacts.add(pair); continue
                qa, qb = quads[a['index']], quads[b['index']]
                same_part = qa.get('part', 1) == qb.get('part', 1)
                same_direction = dot(a['normal'], b['normal']) > 0
                exact = exact_shape(a, b)
                same_material = qa['t'] == qb['t'] and vertex_material(qa) == vertex_material(qb)
                if a['index'] == b['index']:
                    kind = 'self_intersecting_quad'
                elif not same_part:
                    kind = 'different_parts_rest_pose'
                elif same_direction:
                    kind = 'exact_same_direction' if exact else 'partial_same_direction'
                elif exact and qa['t'] == qb['t']:
                    kind = 'explicit_two_sided_sheet' if same_material else 'explicit_two_sided_sheet_backside_uv'
                else:
                    kind = 'exact_opposite_material_conflict' if exact else 'partial_opposite_direction'
                original_kind = kind
                # Rounded source planes are slightly tilted rather than exactly
                # coplanar. A tiny projected remnant after their subtraction is
                # not a resolvable surface; keep its evidence, but separate it
                # from real duplicate silhouettes. Exact duplicates never pass.
                if kind == 'partial_same_direction' and area <= ROUNDING_AREA_EPS and (separation > 1e-8 or parallel_dot < 1-1e-12):
                    kind = 'near_coplanar_rounding_sliver'
                evidence = {'faces': list(pair), 'parts': [qa.get('part', 1), qb.get('part', 1)],
                            'textures': [qa['t'], qb['t']], 'kind': kind,
                            'original_kind': original_kind,
                            'same_direction': same_direction, 'exact': exact,
                            'overlap_area': round(area, 9),
                            'parallel_dot': parallel_dot, 'max_plane_separation': separation,
                            'plane': [round(v, 7) for v in a['plane']]+[round(a['d'], 7)],
                            'a_vertices': qa['v'], 'b_vertices': qb['v']}
                previous = found.get(pair)
                if previous is None: found[pair] = evidence
                else: previous['overlap_area'] += evidence['overlap_area']
    overlaps = list(found.values())
    for e in overlaps:
        if e['kind'] == 'near_coplanar_rounding_sliver' and e['overlap_area'] > ROUNDING_AREA_EPS:
            e['kind'] = e['original_kind']
    counts = Counter(e['kind'] for e in overlaps)
    fixed_counts = Counter(e['kind'] for e in overlaps if e['parts'] == [0, 0])
    persistent = [e for e in overlaps if not e['kind'].startswith('explicit_two_sided_sheet') and e['kind'] not in ('different_parts_rest_pose', 'near_coplanar_rounding_sliver')]
    slivers = [e for e in overlaps if e['kind'] == 'near_coplanar_rounding_sliver']
    return {'name': path.stem, 'sha256': hashlib.sha256(source).hexdigest(), 'quads': len(quads), 'part_counts': dict(Counter(q.get('part', 1) for q in quads)),
            'counts': dict(counts), 'fixed_part0_counts': dict(fixed_counts),
            'positive_area_pairs': len(overlaps), 'shared_edge_or_vertex_pairs': len(edge_contacts),
            'malformed': dict(malformed), 'persistent_conflicts': persistent,
            'rounding_slivers': slivers, 'max_rounding_sliver_area': max((e['overlap_area'] for e in slivers), default=0),
            'normal_backside_examples': [e for e in overlaps if e['kind'].startswith('explicit_two_sided_sheet')][:3],
            'different_part_examples': [e for e in overlaps if e['kind'] == 'different_parts_rest_pose'][:3]}


def self_check():
    a = [(0, 0), (2, 0), (2, 2), (0, 2)]
    b = [(1, 1), (3, 1), (3, 3), (1, 3)]
    assert abs(abs(signed_area(clip_convex(a, b)))-1) < EPS
    adjacent = [(2, 0), (4, 0), (4, 2), (2, 2)]
    assert abs(signed_area(clip_convex(a, adjacent))) < AREA_EPS
    assert abs(abs(signed_area(clip_convex(a, list(reversed(a)))))-4) < EPS
    triangle = [(0, 0), (2, 0), (0, 2)]
    assert abs(abs(signed_area(clip_convex(a, triangle)))-2) < EPS
    # A 45-degree plane can select X or Y as the major axis due to numerical
    # ties. Reprojection must not invent an intersection between disjoint rods.
    av = [[2.77107, 2.13693, 8.4484], [5.37769, 4.74355, 8.4484], [5.37769, 4.74355, 7.5516], [2.77107, 2.13693, 7.5516]]
    bv = [[5.76591, 5.13177, 8.4484], [8.53891, 7.90477, 8.4484], [8.53891, 7.90477, 7.5516], [5.76591, 5.13177, 7.5516]]
    for axis in (0, 1):
        pa = [tuple(p[k] for k in range(3) if k != axis) for p in av]
        pb = [tuple(p[k] for k in range(3) if k != axis) for p in bv]
        assert abs(signed_area(clip_convex(pa, pb))) < AREA_EPS
    def quad(vertices):
        return {'v': vertices, 'uv': [[0,0],[1,0],[1,1],[0,1]],
                'c': [0xffffffff]*4, 't': 'glass', 'part': 0}
    original = quad([[0,0,0],[2,0,0],[2,2,0],[0,2,0]])
    back = quad(list(reversed(original['v'])))
    back['uv'] = list(reversed(original['uv']))
    adjacent = quad([[2,0,0],[4,0,0],[4,2,0],[2,2,0]])
    overlap = quad([[.5,.5,0],[1.5,.5,0],[1.5,1.5,0],[.5,1.5,0]])
    triangle = quad([[7,0,0],[9,0,0],[7,2,0],[7,2,0]])
    result = analyze(Path('fixture.json'), {'quads': [original, adjacent, back, overlap, triangle]})
    assert result['counts']['partial_same_direction'] == 1
    assert result['counts']['explicit_two_sided_sheet'] == 1
    assert not result['malformed']  # repeated final vertex is a legal triangle
    bowtie = quad([[0,0,0],[2,2,0],[0,2,0],[2,0,0]])
    result = analyze(Path('fixture.json'), {'quads': [bowtie]})
    assert result['counts']['self_intersecting_quad'] == 1


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--meshes', type=Path, default=ROOT/'src/main/resources/assets/guogaology/core_meshes')
    parser.add_argument('--output', type=Path, default=ROOT/'build/core-surface-audit')
    parser.add_argument('--fail-on-same-direction', action='store_true')
    args = parser.parse_args(); self_check()
    results = [analyze(p) for p in sorted(args.meshes.glob('*.json'))]
    args.output.mkdir(parents=True, exist_ok=True)
    report = {'method': __doc__, 'eps_model_units': EPS, 'plane_eps_model_units': PLANE_EPS,
              'area_eps_model_units2': AREA_EPS, 'rounding_sliver_area_eps_model_units2': ROUNDING_AREA_EPS, 'meshes': results}
    (args.output/'audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', 'utf8')
    lines = ['# Crystal coplanar surface audit', '', '| Mesh | Quads | Fixed part0 same-direction | All same-part same-direction | Reverse contact candidates | Normal two-sided sheets | Rounding slivers | Different-part rest-pose pairs |',
             '|---|---:|---:|---:|---:|---:|---:|---:|']
    hard_errors = 0
    for row in results:
        conflicts = row['persistent_conflicts']
        same = sum(e['same_direction'] for e in conflicts)
        reverse = len(conflicts)-same
        fixed = sum(e['parts'] == [0, 0] and e['same_direction'] for e in conflicts)
        sheets = sum(n for kind, n in row['counts'].items() if kind.startswith('explicit_two_sided_sheet'))
        hard_errors += same
        lines.append(f"| {row['name']} | {row['quads']} | {fixed} | {same} | {reverse} | {sheets} | {len(row['rounding_slivers'])} | {row['counts'].get('different_parts_rest_pose',0)} |")
        print(f"{row['name']:28} fixed_same={fixed:3} all_same={same:3} reverse_contact={reverse:3} sheets={sheets:3} slivers={len(row['rounding_slivers']):3} rest={row['counts'].get('different_parts_rest_pose',0):3}")
    lines += ['', 'Positive polygon area is required; touching edges/vertices are excluded. Exact reversed geometry sharing one texture is treated as a normal two-sided sheet, including independently readable backside UVs. Different animation parts are separated from permanent conflicts. Partial reverse intersections may be buried touching solids and require visual interpretation. Source planes rounded to five decimals can leave tilted projection slivers; those below 1e-5 model units² are separately reported, never hidden. Detailed face indices, planes, vertices, residual areas and source SHA-256 are in audit.json.']
    (args.output/'audit.md').write_text('\n'.join(lines)+'\n', 'utf8')
    if args.fail_on_same_direction and hard_errors:
        raise SystemExit(f'{hard_errors} same-part, same-direction overlap pairs remain')


if __name__ == '__main__': main()
