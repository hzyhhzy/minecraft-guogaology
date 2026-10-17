"""Author the Astra Lv1 nucleus, computing modules and continuous trefoil lead.

Preserve the existing glass/clasps exactly; higher grades are independent art.
All geometry is built offline, with no extra entity, animation or ticker.
The raw pearl keeps the original Lv1 radiation-emblem material.
"""
import copy
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw
from generate_absence_core import face, merged_boxes
from generate_core_resources import BOXES

ROOT = Path(__file__).resolve().parents[1]
A = ROOT/'src/main/resources/assets/guogaology'
NAME = 'astra_critical_core'


def add(a,b): return [a[k]+b[k] for k in range(3)]
def mul(a,v): return [k*v for k in a]
def unit(a): return mul(a,1/math.sqrt(sum(k*k for k in a)))
def cross(a,b): return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]


def knot(t):
    r=3.7+1.25*math.cos(3*t)
    return [8+r*math.cos(2*t),8+2.3*math.sin(3*t),8+r*math.sin(2*t)]


def conductor():
    rings=[]
    for i in range(24):
        t=i*math.tau/24
        p=knot(t);before=knot(t-.001);after=knot(t+.001)
        tangent=unit([after[k]-before[k] for k in range(3)])
        side=unit(cross(tangent,[0,1,0]));up=cross(side,tangent)
        rings.append([add(p,add(mul(side,a*.38),mul(up,b*.38)))
                      for a,b in ((1,1),(-1,1),(-1,-1),(1,-1))])
    quads=[]
    for i in range(24):
        for j in range(4):
            points=[rings[i][j],rings[i][(j+1)%4],rings[(i+1)%24][(j+1)%4],rings[(i+1)%24][j]]
            quads.append(face(points,'astra_lead'))
    return quads


def chip(t):
    center=knot(t)
    radial=unit([center[0]-8,0,center[2]-8]);right=cross([0,1,0],radial)
    # Real six-sided chips sit on the six conductor arcs, rather than drawing
    # glyphs over the shell or adding disconnected circuit particles.
    points=[add(center,add(mul(right,x),add(mul([0,1,0],y),mul(radial,z))))
            for x,y,z in ((-1,-1,-.32),(1,-1,-.32),(1,1,-.32),(-1,1,-.32),
                          (-1,-1,.32),(1,-1,.32),(1,1,.32),(-1,1,.32))]
    return [face([points[i] for i in indices],'astra_chip') for indices in
            ((3,2,1,0),(4,5,6,7),(0,1,5,4),(7,6,2,3),(0,4,7,3),(2,6,5,1))]


def generate():
    path=A/f'core_meshes/{NAME}.json'
    model=json.loads(path.read_text('utf8'))
    shell=[copy.deepcopy(q) for q in model['quads'] if q['t'] in ('selected_glass','selected_clasp')]
    for q in shell:
        q.pop('lod_t',None);q.pop('lod_uv',None)
    nucleus=[]
    for a,b in BOXES['cloud_pearl']:
        nucleus.append({'from':[8+(k-8)*.43 for k in a],
                        'to':[8+(k-8)*.43 for k in b]})
    model['quads']=shell+merged_boxes(nucleus,texture='selected_body')+conductor()
    for i in range(6):model['quads'].extend(chip((i+.25)*math.tau/6))
    model['textures'].pop('lod_tints',None)
    model['textures'].update(astra_lead='guogaology:block/core_d/astra_lead',
                             astra_chip='guogaology:block/core_d/astra_chip')
    from sanitize_core_shells import sanitize
    model,_=sanitize(model,NAME)
    path.write_text(json.dumps(model,separators=(',',':'))+'\n','utf8')
    chip_image=Image.new('RGBA',(32,32),'#24545e');d=ImageDraw.Draw(chip_image)
    d.rectangle((2,2,29,29),outline='#97dcd4',width=2)
    d.rectangle((9,9,22,22),fill='#397d85',outline='#d8ffff',width=2)
    d.rectangle((13,13,18,18),fill='#b4f9e5')
    for i in (6,12,19,25):
        d.line((i,0,i,5),fill='#d9c48b',width=2);d.line((i,26,i,31),fill='#d9c48b',width=2)
        d.line((0,i,5,i),fill='#d9c48b',width=2);d.line((26,i,31,i),fill='#d9c48b',width=2)
    for line in ((6,6,10,6,10,9),(25,6,21,6,21,9),(6,25,10,25,10,22),(25,25,21,25,21,22)):
        d.line(line,fill='#75c8c0',width=1)
    chip_image.save(A/'textures/block/core_d/astra_chip.png')
    lead=Image.new('RGBA',(16,16),'#c6f5eb');d=ImageDraw.Draw(lead)
    d.rectangle((0,0,15,2),fill='#65b7bf');d.rectangle((0,13,15,15),fill='#65b7bf')
    d.line((0,5,15,5),fill='#f5ffff',width=2)
    lead.save(A/'textures/block/core_d/astra_lead.png')
    manifest=ROOT/'src/main/resources/guogaology/core_design_sources.json'
    sources=json.loads(manifest.read_text('utf8'))
    for entry in sources:
        if entry['id']==NAME:entry['revision_source']='tools/generate_astra_core.py (0.5.12); original shell retained'
    manifest.write_text(json.dumps(sources,ensure_ascii=False,separators=(',',':'))+'\n','utf8')
    # Reproject this family's three GUI icons from the new centered Lv1 body.
    # Keep the existing grade badge design, palette and all other families.
    import core_item_projection
    core_item_projection.mesh.cache_clear();core_item_projection.project.cache_clear()
    from generate_inventory_art import core_icon
    for grade in (1,2,3):
        suffix='' if grade==1 else f'_lv{grade}'
        core_icon(NAME,grade).save(A/f'textures/item/{NAME}{suffix}.png')
    print(f'ASTRA_CORE_0512_OK quads={len(model["quads"])} glass={len(shell)} six_chips/trefoil/nucleus')


if __name__=='__main__':generate()
