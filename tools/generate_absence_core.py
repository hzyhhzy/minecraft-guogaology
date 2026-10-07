"""Restore the gallery C1 broken, translucent empty-set material at all grades.

The old glyph was built from tiny cuboids, not a font or a textured plane.
Its actual front cuboids, six face materials and 1.2-unit extrusion are reused;
the second main relief and two ghost copies are deliberately not retained.
Three independently animated solid components form each higher-grade interior.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import copy, json, math
from collections import defaultdict

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'src/main/resources/assets/guogaology'
REF = ROOT / 'tools/reference/absence-c1.json'
ORBIT = ROOT / 'tools/reference/absence-orbit-glyphs.json'
WHITE = 0xffffffff


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(',', ':')) + '\n', 'utf8')


def face(points, texture, part=0, uv=None):
    a = [points[1][k] - points[0][k] for k in range(3)]
    b = [points[2][k] - points[0][k] for k in range(3)]
    n = [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]
    norm = math.sqrt(sum(x*x for x in n))
    return {'v': [[round(v, 5) for v in p] for p in points], 'n': [[round(x/norm, 5) for x in n]]*4,
            'c': [WHITE]*4, 'uv': uv or [[0, 0], [1, 0], [1, 1], [0, 1]], 't': texture, 'part': part}


def merged_boxes(elements, texture=None, part=0):
    """Union C1 cuboids, retaining directional face materials and real sides.

    Greedy merges only join the same material on the same physical plane.
    Touching pixels therefore lose their buried faces without flattening the
    relief, filling its gaps, or turning its ghost reverse into a second glyph.
    """
    cuts = [sorted({e[side][axis] for e in elements for side in ('from','to')}) for axis in range(3)]
    indices = [{v:i for i,v in enumerate(values)} for values in cuts]
    occupied = {}
    for e in elements:
        lo = [indices[a][e['from'][a]] for a in range(3)]
        hi = [indices[a][e['to'][a]] for a in range(3)]
        for x in range(lo[0],hi[0]):
            for y in range(lo[1],hi[1]):
                for z in range(lo[2],hi[2]):
                    occupied[(x,y,z)] = e
    surfaces = defaultdict(set)
    for p in occupied:
        for axis in range(3):
            others = [i for i in range(3) if i != axis]
            for sign in (-1,1):
                neighbour = list(p); neighbour[axis] += sign
                if tuple(neighbour) not in occupied:
                    direction = (("west","east"),("down","up"),("north","south"))[axis][int(sign>0)]
                    material = texture or 'c1_' + occupied[p]['faces'][direction]['texture'].lstrip('#')
                    surfaces[(axis,sign,p[axis]+(1 if sign>0 else 0),material)].add((p[others[0]],p[others[1]]))
    quads = []
    for (axis,sign,plane_index,material),mask in sorted(surfaces.items()):
        others = [i for i in range(3) if i != axis]
        while mask:
            u,v = min(mask); width = 1
            while (u+width,v) in mask:
                width += 1
            height = 1
            while all((u+i,v+height) in mask for i in range(width)):
                height += 1
            mask.difference_update((u+i,v+j) for i in range(width) for j in range(height))
            points = []
            for a,b in ((u,v),(u+width,v),(u+width,v+height),(u,v+height)):
                p = [0,0,0]; p[axis] = cuts[axis][plane_index]
                p[others[0]] = cuts[others[0]][a]; p[others[1]] = cuts[others[1]][b]; points.append(p)
            q = face(points,material,part)
            if q['n'][0][axis]*sign < 0:
                q = face(list(reversed(points)),material,part)
            quads.append(q)
    return quads


def main_layer(reference):
    """The exact 74 original relief cuboids, excluding unrelated floating dust."""
    return [copy.deepcopy(e) for e in reference['elements']
            if e['from'][2] == 5.2 and e['to'][2] == 6.4]


def split_strokes(main):
    """Select existing C1 pixels, never draw replacement geometry or a font.

    C1's original author made an ellipse (7,6)-(24,25) and a diagonal
    (5,28)-(27,3), both two pixels wide. Those same authored stroke masks are
    selectors only: intersection with the actual 74 source cuboids preserves
    every original missing pixel and original six directional materials.
    """
    selectors = [Image.new('L',(32,32)), Image.new('L',(32,32))]
    ImageDraw.Draw(selectors[0]).ellipse((7,6,24,25),outline=255,width=2)
    ImageDraw.Draw(selectors[1]).line((5,28,27,3),fill=255,width=2)
    groups = [[],[]]
    for e in main:
        y = round((e['from'][1]-2)/.375)
        for x in range(round((e['from'][0]-2)/.375),round((e['to'][0]-2)/.375)):
            for k,mask in enumerate(selectors):
                if mask.getpixel((31-x,31-y)):
                    cell = copy.deepcopy(e)
                    cell['from'][0] = 2+x*.375; cell['to'][0] = 2+(x+1)*.375
                    groups[k].append(cell)
    return groups


def relief(elements,scale=1,part=0,turn=False):
    boxes = copy.deepcopy(elements)
    for e in boxes:
        e['from'][2] += 2.2; e['to'][2] += 2.2
    quads = merged_boxes(boxes,part=part)
    for q in quads:
        q['v'] = [[round(8+(v-8)*scale,5) for v in p] for p in q['v']]
        if turn:
            # A proper +90-degree Y rotation, not a reflective axis swap.
            q['v'] = [[round(p[2],5),p[1],round(16-p[0],5)] for p in q['v']]
            q['n'] = [[n[2],n[1],-n[0]] for n in q['n']]
    return quads


def triad(quads,strokes,scale=1,first=1):
    # Each circle and the original pixel staircase is a genuine extruded solid.
    # Their independent part IDs are kept by the existing relative-motion path.
    ring,slash = strokes
    quads.extend(relief(ring,scale,first))
    quads.extend(relief(ring,scale*.9125,first+1,True))
    quads.extend(relief(slash,scale,first+2))


def smooth_orbits():
    """Same three arc sets, now 10 segments per arc instead of just three.

    Thin, two-sided C1-material ribbons preserve a round outline at a small
    face cost. Their two surfaces have real separation, not coplanar doubles.
    """
    result = []
    for axis,radius in enumerate((11.5,13,14.5)):
        others = [i for i in range(3) if i != axis]
        for arc in range(3):
            start = arc*math.tau/3+.22; end = arc*math.tau/3+1.36
            for n in range(10):
                a = start+(end-start)*n/10; b = start+(end-start)*(n+1)/10
                for sign in (-1,1):
                    points = []
                    for r,t in ((radius-.55,a),(radius+.55,a),(radius+.55,b),(radius-.55,b)):
                        p = [8,8,8]; p[axis] += sign*.13
                        p[others[0]] += r*math.cos(t); p[others[1]] += r*math.sin(t); points.append(p)
                    q = face(points,'c1_ink',10)
                    if q['n'][0][axis]*sign < 0:
                        q = face(list(reversed(points)),'c1_ink',10)
                    result.append(q)
    for old in json.loads(ORBIT.read_text('utf8'))['quads']:
        q = copy.deepcopy(old); q['t']='c1_ink'; q['c']=[WHITE]*4
        q.pop('lod_t',None); q.pop('lod_uv',None); result.append(q)
    return result


def generate():
    reference = json.loads(REF.read_text('utf8'))
    main = main_layer(reference); strokes = split_strokes(main)
    textures = {f'c1_{k}':f'guogaology:block/lho_c1_{k}' for k in ('frame','ink','soft','ghost')}
    textures['particle'] = 'guogaology:block/lho_c1'
    frames = [e for e in reference['elements'] if next(iter(e['faces'].values()))['texture']=='#frame']
    fixed = merged_boxes(frames,'c1_frame')
    # Only the real unioned frame remains. C1's extra translucent mist cube
    # gave another full box outline behind it and is unnecessary here.
    exterior = smooth_orbits()
    counts = {}
    for grade, suffix in ((1,''),(2,'_lv2'),(3,'_lv3')):
        quads = copy.deepcopy(fixed)
        if grade == 1:
            quads.extend(relief(main))
        else:
            triad(quads,strokes,1.18 if grade == 2 else 1.22,1)
            if grade == 3:
                triad(quads,strokes,.66,4); quads.extend(copy.deepcopy(exterior))
        data = {'textures':textures, 'motion':'still' if grade==1 else 'absence_orbits',
                'motion_scale':1, 'extended':grade==3, 'quads':quads,
                'design_source':{'reference':'C1 / 0.2.16', 'sha256':reference['source_sha256'],
                                 'main_layers':1 if grade<3 else 2, 'source_main_layer_z':[5.2,6.4],
                                 'source_main_cuboids':len(main), 'source_main_thickness':1.2,
                                 'relief_geometry':'unioned original six-face cuboids',
                                 'outer_arc_segments':10 if grade==3 else 0, 'mist_box_removed':True}}
        if grade == 3:
            # Keep the C1 interior and unioned shell intact; only trim permanent
            # joints in the inherited rotating exterior's lettering/ribbons.
            from sanitize_core_shells import sanitize
            data, _ = sanitize(data, 'lho_trace_lv3', allowed_parts={10})
        write(A/f'core_meshes/lho_trace{suffix}.json',data)
        model = json.loads((A/f'models/block/lho_trace{suffix}.json').read_text('utf8'))
        model['textures'] = textures; model['elements'] = []; model['ambientocclusion'] = False
        write(A/f'models/block/lho_trace{suffix}.json',model)
        counts[grade] = len(data['quads'])
    print('C1 absence-core quads:',counts,'independently moving interior parts: Lv2=3, Lv3=6')


if __name__ == '__main__':
    generate()
