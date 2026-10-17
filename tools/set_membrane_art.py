"""Symmetric violet/ivory artwork and union-only model for three set membranes.

The six existing boxes and their collision envelope remain unchanged. Only
the union exterior is emitted, so intersections have no buried or coplanar
duplicate faces. UVs use the full model coordinates continuously: a clipped
surface gets the matching subsection rather than a stretched complete glyph.
"""
from collections import defaultdict
from PIL import Image

BOXES=(
    ((2,4,7),(14,12,9)),((4,2,7),(12,14,9)),
    ((7,2,4),(9,14,12)),((7,4,2),(9,12,14)),
    ((2,7,4),(14,9,12)),((4,7,2),(12,9,14)),
)


def membrane_texture():
    image=Image.new('RGBA',(32,32))
    cross=lambda x,y:(4<=x<28 and 8<=y<24) or (8<=x<24 and 4<=y<28)
    for y in range(32):
        for x in range(32):
            color=(87,68,120)
            if cross(x,y):
                border=any(not cross(x+dx,y+dy) for dx,dy in ((-1,0),(1,0),(0,-1),(0,1)))
                color=(87,68,120) if border else (240,233,224)
                if not border:
                    # Other membranes physically cross the middle strips
                    # x/y14..18. Keep those areas quiet; each exposed quadrant
                    # gets one small, complete member dot rather than a glyph
                    # whose middle would be hidden by the crossing plane.
                    if not 14<=x<=18 and not 14<=y<=18:
                        distance=min((x-cx)**2+(y-cy)**2 for cx in (10.5,20.5) for cy in (10.5,20.5))
                        if distance<=3:color=(99,73,135)
                        if distance<=.6:color=(177,155,206)
            image.putpixel((x,y),(*color,255))
    return image


def membrane_model():
    occupied={(x,y,z) for a,b in BOXES for x in range(a[0],b[0])
              for y in range(a[1],b[1]) for z in range(a[2],b[2])}
    surfaces=defaultdict(set)
    directions=(('west',0,-1,2,1),('east',0,1,2,1),('down',1,-1,0,2),
                ('up',1,1,0,2),('north',2,-1,0,1),('south',2,1,0,1))
    for p in sorted(occupied):
        for name,axis,sign,u,v in directions:
            neighbor=list(p);neighbor[axis]+=sign
            if tuple(neighbor) not in occupied:
                surfaces[(name,axis,p[axis]+(sign>0),u,v)].add((p[u],p[v]))
    elements=[]
    for (name,axis,plane,u,v),cells in sorted(surfaces.items()):
        while cells:
            x,y=min(cells,key=lambda p:(p[1],p[0]));width=height=1
            while (x+width,y) in cells:width+=1
            while all((xx,y+height) in cells for xx in range(x,x+width)):height+=1
            cells.difference_update((xx,yy) for xx in range(x,x+width) for yy in range(y,y+height))
            lo=[0,0,0];hi=[0,0,0];lo[axis]=hi[axis]=plane
            lo[u]=x;hi[u]=x+width;lo[v]=y;hi[v]=y+height
            uv=[x,y,x+width,y+height] if axis==1 else [x,16-y-height,x+width,16-y]
            elements.append({'from':lo,'to':hi,'faces':{name:{'uv':uv,'texture':'#body'}}})
    return {'parent':'minecraft:block/block',
            'textures':{'body':'guogaology:block/set_membrane','particle':'guogaology:block/set_membrane'},
            'elements':elements}
