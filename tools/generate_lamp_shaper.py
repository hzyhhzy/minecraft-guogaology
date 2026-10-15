"""Generate the lamp-shaping bench from compact Minecraft-style model elements."""
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


def resources():
    elements=[]
    # Dark copper trestles, a clean quartz worktop and the two face dies.
    for x in (1,12):
        elements.append(element([x,0,2],[x+3,10,14],'frame'))
    elements.extend([
        element([3,2,5],[13,4,11],'frame'),
        element([0,10,0],[16,13,16],'body','top'),
        element([1,13,1],[15,14,15],'top'),
        element([2,14,3],[8,15,9],'die'),
        element([9,14,3],[14,15,9],'other'),
        element([3,14,12],[13,15,13],'trim'),
        element([11,14,11],[13,16,13],'frame'),
    ])
    model={'parent':'minecraft:block/block','textures':{
        'frame':'minecraft:block/oxidized_copper',
        'body':'minecraft:block/quartz_block_side',
        'top':'minecraft:block/quartz_block_top',
        'trim':'minecraft:block/copper_block',
        'die':'guogaology:block/guogao_lantern',
        'other':'guogaology:block/guogao_lantern_sad',
        'particle':'minecraft:block/quartz_block_side'},'elements':elements}
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
