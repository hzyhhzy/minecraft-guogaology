"""Generate the lamp-shaping bench with its exact union exterior.

The authored cuboids remain the source of the silhouette and face artwork.
Only exposed faces are emitted; intersecting trestles and the die handle do
not retain hidden sheets or coplanar duplicates. Clipped faces retain the
matching part of their original UVs rather than stretching the artwork.
"""
from collections import defaultdict
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RESOURCE=ROOT/'src/main/resources'
ID='guogao_lamp_shaper'


def element(a,b,texture,top=None):
    faces={side:{'texture':'#'+texture,'uv':[0,0,16,16]}
           for side in ('up','down','north','south','east','west')}
    if top:faces['up']['texture']='#'+top
    return {'from':a,'to':b,'faces':faces}


def bench_boxes():
    elements=[]
    # Dark copper trestles, a clean quartz worktop and the two face dies.
    for x in (1,12):
        elements.append(element([x,0,2],[x+3,10,14],'frame'))
    elements.extend([
        element([3,2,5],[13,4,11],'frame'),
        element([0,10,0],[16,13,16],'body','top'),
        element([1,13,1],[15,14,15],'top'),
        element([2,14,3],[8,15,9],'die'),
        element([9,14,3],[15,15,9],'other'),
        element([3,14,12],[13,15,13],'trim'),
        element([11,14,11],[13,16,13],'frame'),
    ])
    return elements


def face_uv(box,side,lo,hi):
    """Subsection of an original full-face UV, in native face orientation."""
    a,b=box['from'],box['to']
    if side=='up':
        values=(lo[0]-a[0],lo[2]-a[2],hi[0]-a[0],hi[2]-a[2])
        spans=(b[0]-a[0],b[2]-a[2])
    elif side=='down':
        values=(lo[0]-a[0],b[2]-hi[2],hi[0]-a[0],b[2]-lo[2])
        spans=(b[0]-a[0],b[2]-a[2])
    elif side=='north':
        values=(b[0]-hi[0],b[1]-hi[1],b[0]-lo[0],b[1]-lo[1])
        spans=(b[0]-a[0],b[1]-a[1])
    elif side=='south':
        values=(lo[0]-a[0],b[1]-hi[1],hi[0]-a[0],b[1]-lo[1])
        spans=(b[0]-a[0],b[1]-a[1])
    elif side=='west':
        values=(lo[2]-a[2],b[1]-hi[1],hi[2]-a[2],b[1]-lo[1])
        spans=(b[2]-a[2],b[1]-a[1])
    else:
        values=(b[2]-hi[2],b[1]-hi[1],b[2]-lo[2],b[1]-lo[1])
        spans=(b[2]-a[2],b[1]-a[1])
    return [round(value*16/spans[i%2],8) for i,value in enumerate(values)]


def exterior_elements(boxes):
    # Later authored details own a shared exposed patch, such as the handle
    # crossing the copper trim. This is deterministic and emits it only once.
    occupied={}
    for index,box in enumerate(boxes):
        a,b=box['from'],box['to']
        for x in range(a[0],b[0]):
            for y in range(a[1],b[1]):
                for z in range(a[2],b[2]):occupied[x,y,z]=index
    directions=(('west',0,-1,2,1),('east',0,1,2,1),('down',1,-1,0,2),
                ('up',1,1,0,2),('north',2,-1,0,1),('south',2,1,0,1))
    surfaces=defaultdict(set)
    for p,index in sorted(occupied.items()):
        for side,axis,sign,u,v in directions:
            neighbor=list(p);neighbor[axis]+=sign
            if tuple(neighbor) not in occupied:
                surfaces[index,side,axis,p[axis]+(sign>0),u,v].add((p[u],p[v]))
    result=[]
    for (index,side,axis,plane,u,v),cells in sorted(surfaces.items()):
        while cells:
            x,y=min(cells,key=lambda p:(p[1],p[0]));width=height=1
            while (x+width,y) in cells:width+=1
            while all((xx,y+height) in cells for xx in range(x,x+width)):height+=1
            cells.difference_update((xx,yy) for xx in range(x,x+width) for yy in range(y,y+height))
            lo=[0,0,0];hi=[0,0,0];lo[axis]=hi[axis]=plane
            lo[u]=x;hi[u]=x+width;lo[v]=y;hi[v]=y+height
            box=boxes[index]
            face={'texture':box['faces'][side]['texture'],'uv':face_uv(box,side,lo,hi)}
            result.append({'from':lo,'to':hi,'faces':{side:face}})
    return result


def resources():
    model={'parent':'minecraft:block/block','textures':{
        'frame':'minecraft:block/oxidized_copper',
        'body':'minecraft:block/quartz_block_side',
        'top':'minecraft:block/quartz_block_top',
        'trim':'minecraft:block/copper_block',
        'die':'guogaology:block/guogao_lantern',
        'other':'guogaology:block/guogao_lantern_sad',
        'particle':'minecraft:block/quartz_block_side'},'elements':exterior_elements(bench_boxes())}
    return {
        f'assets/guogaology/blockstates/{ID}.json':{'variants':{'':{'model':f'guogaology:block/{ID}'}}},
        f'assets/guogaology/models/block/{ID}.json':model,
        f'assets/guogaology/models/item/{ID}.json':{'parent':f'guogaology:block/{ID}'},
        f'data/guogaology/loot_table/blocks/{ID}.json':{
            'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'guogaology:{ID}'}],
                                             'conditions':[{'condition':'minecraft:survives_explosion'}]}]},
    }


def generate(check=False):
    for name,value in resources().items():
        path=RESOURCE/name
        encoded=json.dumps(value,ensure_ascii=False,indent=2)+'\n'
        if check:
            if not path.is_file() or path.read_text('utf8')!=encoded:raise ValueError('Stale lamp shaper resource: '+name)
        else:
            path.parent.mkdir(parents=True,exist_ok=True);path.write_text(encoded,'utf8')
    pickaxe=RESOURCE/'data/minecraft/tags/block/mineable/pickaxe.json'
    value=json.loads(pickaxe.read_text('utf8'));entry='guogaology:'+ID
    if check:
        if entry not in value['values']:raise ValueError('Lamp shaper is missing from pickaxe tag')
    elif entry not in value['values']:
        value['values'].append(entry);pickaxe.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n','utf8')


if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--check',action='store_true')
    generate(parser.parse_args().check)
