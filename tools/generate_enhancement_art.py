"""Dedicated, reproducible enhancement-station block art.

Only committed models / textures are generated here. The stations retain their
ordinary block / menu implementation; no renderer, ticker or entity is needed.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/googology'
T=A/'textures/block'

PALETTES=[
    # A readable carpenter's bench: pale stone, warm wood, copper and cyan ink.
    dict(body=(117,86,61),shade=(71,55,45),edge=(188,149,88),light=(230,207,154),
         top=(211,209,187),ink=(55,111,115),gem=(116,215,224),dark=(43,68,72)),
    # A jade-and-slate precision station, powered by captured crystal sockets.
    dict(body=(48,73,80),shade=(24,43,55),edge=(130,184,163),light=(215,246,223),
         top=(112,152,148),ink=(29,66,79),gem=(146,237,219),dark=(24,50,66)),
    # An advanced instrument: indigo ceramic, pale gold and violet crystal.
    dict(body=(43,49,81),shade=(21,29,55),edge=(208,182,116),light=(255,243,196),
         top=(109,124,160),ink=(35,43,81),gem=(199,203,255),dark=(21,34,64)),
]

def write(p,v):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def texture(rank,name,p):
    im=Image.new('RGBA',(32,32),p['body']+(255,));d=ImageDraw.Draw(im)
    if name=='body':
        d.rectangle((0,0,31,31),outline=p['shade'],width=2)
        for y in (6,15,24):
            d.line((2,y,29,y),fill=p['shade']);d.line((2,y+1,29,y+1),fill=p['edge'])
        if rank==1:
            for x,y,w in [(5,3,5),(17,11,7),(8,19,8),(20,27,5)]:
                d.line((x,y,x+w,y),fill=p['edge']);d.point((x+2,y+1),fill=p['shade'])
        else:
            for x in (4,27):d.line((x,2,x,29),fill=p['edge'])
            for y in (4,13,22):
                d.rectangle((8,y,23,y+5),fill=p['dark'],outline=p['shade'])
                for x in (11,16,21):d.rectangle((x,y+2,x+1,y+3),fill=p['gem'])
            if rank==3:d.rectangle((14,14,17,17),fill=p['light'])
    elif name=='trim':
        im.paste(p['edge']+(255,),(0,0,32,32));d=ImageDraw.Draw(im)
        d.line((0,31,0,0,31,0),fill=p['light'],width=2)
        d.line((31,1,31,31,1,31),fill=p['shade'],width=2)
        d.rectangle((5,5,26,26),outline=p['body'],width=1)
        d.rectangle((7,7,24,24),outline=p['light'],width=1)
    elif name=='top':
        im.paste(p['top']+(255,),(0,0,32,32));d=ImageDraw.Draw(im)
        d.rectangle((1,1,30,30),outline=p['edge'],width=2)
        d.rectangle((4,4,27,27),outline=p['ink'])
        # Central equipment recess, not a temporary colored full-cube skin.
        d.rectangle((11,7,20,24),fill=p['dark'],outline=p['light'])
        d.rectangle((13,9,18,22),fill=p['body'])
        d.line((14,10,14,20),fill=p['gem'],width=2)
        for x,z in ((7,7),(24,7),(7,24),(24,24)):
            d.rectangle((x-2,z-2,x+2,z+2),fill=p['ink'],outline=p['edge'])
            d.point((x,z),fill=p['gem'])
        if rank>=2:
            for y in (12,19):d.line((5,y,10,y),fill=p['gem']);d.line((21,y,26,y),fill=p['gem'])
        if rank==3:
            for x in (5,26):d.rectangle((x-1,14,x+1,17),fill=p['light'])
            d.line((6,26,11,26,11,28,20,28,20,26,25,26),fill=p['light'])
    elif name=='gem':
        im.paste(p['gem']+(255,),(0,0,32,32));d=ImageDraw.Draw(im)
        d.polygon([(0,0),(31,0),(24,7),(7,7)],fill=p['light'])
        d.polygon([(0,0),(7,7),(7,24),(0,31)],fill=p['edge'])
        d.polygon([(31,0),(31,31),(24,24),(24,7)],fill=p['top'])
        d.polygon([(0,31),(7,24),(24,24),(31,31)],fill=p['ink'])
        d.line((7,7,24,7,24,24),fill=p['light'])
    else:raise ValueError(name)
    im.save(T/f'enhancement_{rank}_{name}.png')

def cube(a,b,side='body',up=None):
    faces={f:{'texture':'#'+(up if f=='up' and up else side),'uv':[0,0,16,16]}
           for f in ('up','down','north','south','east','west')}
    return {'from':a,'to':b,'faces':faces}

def station(rank):
    if rank==1:
        elements=[cube([1,0,1],[15,2,15]),cube([0,10,0],[16,13,16],'body','top'),
                  cube([2,13,2],[14,14,14],'trim','top')]
        for x in (2,11):
            for z in (2,11):elements.append(cube([x,2,z],[x+3,10,z+3]))
        for z in (2,12):elements.append(cube([4,3,z],[12,5,z+2],'trim'))
    elif rank==2:
        elements=[cube([0,0,0],[16,3,16]),cube([3,3,3],[13,10,13]),
                  cube([0,10,0],[16,13,16],'trim','top')]
        for x in (1,12):
            for z in (1,12):
                elements.append(cube([x,3,z],[x+3,10,z+3],'trim'))
                elements.append(cube([x,13,z],[x+3,16,z+3],'gem'))
        for a,b in [([4,13,4],[12,14,5]),([4,13,11],[12,14,12]),
                    ([4,13,5],[5,14,11]),([11,13,5],[12,14,11])]:
            elements.append(cube(a,b,'body','trim'))
    else:
        elements=[cube([0,0,0],[16,2,16],'trim'),cube([1,2,1],[15,4,15]),
                  cube([3,4,3],[13,11,13]),cube([0,11,0],[16,13,16],'trim','top'),
                  cube([4,13,4],[12,14.5,12],'body','top')]
        for x in (.5,12.5):
            for z in (.5,12.5):
                elements.append(cube([x,4,z],[x+3,11,z+3],'body'))
                elements.append(cube([x,13,z],[x+3,14,z+3],'trim'))
                elements.append(cube([x+.5,14,z+.5],[x+2.5,16,z+2.5],'gem'))
        for a,b in [([5,13,1],[11,14,3]),([5,13,13],[11,14,15]),
                    ([1,13,5],[3,14,11]),([13,13,5],[15,14,11])]:
            elements.append(cube(a,b,'gem'))
    name='enhancement_table'+('' if rank==1 else '_'+str(rank))
    textures={k:f'googology:block/enhancement_{rank}_{k}' for k in ('body','trim','top','gem')}
    textures['particle']=textures['body']
    write(A/f'models/block/{name}.json',{'parent':'minecraft:block/block','textures':textures,'elements':elements})
    write(A/f'models/item/{name}.json',{'parent':'googology:block/'+name})
    write(A/f'blockstates/{name}.json',{'variants':{'':{'model':'googology:block/'+name}}})

def generate():
    T.mkdir(parents=True,exist_ok=True)
    for rank,p in enumerate(PALETTES,1):
        for name in ('body','trim','top','gem'):texture(rank,name,p)
        station(rank)
    # Retire the three placeholder cube skins, now unused by any model.
    for name in ('enhancement_table','enhancement_table_2','enhancement_table_3'):
        (T/(name+'.png')).unlink(missing_ok=True)
    print('Three dedicated enhancement-station models / materials generated')

if __name__=='__main__':generate()
