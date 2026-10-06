"""Check the restored C1 cuboid relief against its actual historical geometry."""
from pathlib import Path
from collections import Counter
import json
import math

from generate_absence_core import A, REF, main_layer, relief, split_strokes
from audit_core_surfaces import analyze


def cross(a,b):
    return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])


def area(q):
    points=q['v']; u=[points[1][a]-points[0][a] for a in range(3)]
    v=[points[3][a]-points[0][a] for a in range(3)]
    return math.sqrt(sum(x*x for x in cross(u,v)))


def volume(quads):
    result=0
    for q in quads:
        for i,j,k in ((0,1,2),(0,2,3)):
            a,b,c=(q['v'][n] for n in (i,j,k))
            result+=sum(x*y for x,y in zip(a,cross(b,c)))/6
    return result


def main():
    reference=json.loads(REF.read_text('utf8')); original=main_layer(reference)
    assert len(original)==74
    assert Counter(e['faces']['north']['texture'] for e in original)=={'#ink':59,'#soft':15}
    restored=relief(original)
    north=[q for q in restored if q['n'][0]==[0,0,-1]]
    south=[q for q in restored if q['n'][0]==[0,0,1]]
    sides=[q for q in restored if q['n'][0][2]==0]
    assert north and south and sides
    assert {q['t'] for q in sides}=={'c1_soft'}
    assert {q['t'] for q in south}=={'c1_ghost'}
    assert {p[2] for q in north for p in q['v']}=={7.4}
    assert {p[2] for q in south for p in q['v']}=={8.6}
    source_area=sum((e['to'][0]-e['from'][0])*(e['to'][1]-e['from'][1]) for e in original)
    assert abs(sum(area(q) for q in north)-source_area)<1e-8
    assert abs(sum(area(q) for q in south)-source_area)<1e-8
    assert abs(volume(restored)-source_area*1.2)<1e-8
    # Pixel-centre membership verifies holes as well as silhouette and area.
    for x in range(32):
        for y in range(32):
            px,py=2+(x+.5)*.375,2+(y+.5)*.375
            expected=next((e['faces']['north']['texture'][1:] for e in original
                           if e['from'][0]<px<e['to'][0] and e['from'][1]<py<e['to'][1]),None)
            actual=next((q['t'][3:] for q in north
                         if min(p[0] for p in q['v'])<px<max(p[0] for p in q['v'])
                         and min(p[1] for p in q['v'])<py<max(p[1] for p in q['v'])),None)
            assert actual==expected,(x,y,actual,expected)
    strokes=split_strokes(original)
    assert all(volume(relief(stroke))>0 for stroke in strokes)
    report={'source_sha256':reference['source_sha256'],'original_main_cuboids':74,
            'original_cell_count':180,'main_depth_model_units':1.2,'xy_area':source_area,
            'volume_model_units3':volume(restored),'source_six_face_materials_preserved':True,
            'side_quads':len(sides),'grades':[]}
    for name in ('lho_trace','lho_trace_lv2','lho_trace_lv3'):
        path=A/f'core_meshes/{name}.json';data=json.loads(path.read_text('utf8'))
        assert not any(q['t'] in ('c1_sigil','c1_ring','c1_slash') for q in data['quads'])
        parts=sorted({q['part'] for q in data['quads']} - {0,10})
        for part in parts:
            shape=[q for q in data['quads'] if q['part']==part]
            assert volume(shape)>0.1,(name,part,'no real extrusion')
            assert len({tuple(n) for q in shape for n in q['n']})==6
        audit=analyze(path,data)
        same=[c for c in audit['persistent_conflicts'] if c['same_direction']]
        assert not same,(name,same)
        report['grades'].append({'name':name,'quads':len(data['quads']),
                                'moving_solid_parts':parts,'same_part_coplanar_overlaps':len(same),
                                'reverse_contact_candidates':sum(not c['same_direction'] for c in audit['persistent_conflicts'])})
    out=Path(__file__).resolve().parents[1]/'build/absence-core-038-audit.json'
    out.write_text(json.dumps(report,indent=2)+'\n','utf8')
    print(json.dumps(report,indent=2))


if __name__=='__main__':
    main()
