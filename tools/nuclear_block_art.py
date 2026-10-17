"""Crisp native-pixel nuclear material artwork for the existing cloud_pearl ID.

The trefoil is drawn at native32 resolution with a deliberate safe margin.
Broad ice/teal planes and a high-contrast ivory emblem follow the original
Astra nucleus palette. Every pixel is opaque; the model is an ordinary cube.
"""
import math
from PIL import Image

SIZE=32
TEAL=(48,112,122)
DARK=(22,64,78)
EDGE=(83,155,162)
LIGHT=(212,250,244)
WHITE=(246,255,250)


def nuclear_texture():
    image=Image.new('RGBA',(SIZE,SIZE))
    glyph=set()
    for y in range(SIZE):
        for x in range(SIZE):
            dx,dy=x-15.5,y-15.5
            radius=math.hypot(dx,dy)
            angle=(math.atan2(dy,dx)+math.pi/2)%(math.tau/3)
            if radius<=2.25 or (4.25<=radius<=10.6 and (angle<=math.pi/6 or angle>=math.tau/3-math.pi/6)):
                glyph.add((x,y))
    outline={(x+dx,y+dy) for x,y in glyph for dx,dy in ((-1,0),(1,0),(0,-1),(0,1))}-glyph
    for y in range(SIZE):
        for x in range(SIZE):
            color=TEAL
            if min(x,y,31-x,31-y)<2:color=DARK
            elif x==2 or y==2:color=LIGHT
            elif x==29 or y==29:color=EDGE
            elif x+y<15:color=(59,130,138)
            elif x+y>47:color=(36,92,107)
            if (x,y) in outline:color=DARK
            if (x,y) in glyph:color=WHITE if y<15 else LIGHT
            image.putpixel((x,y),(*color,255))
    return image


def nuclear_cube_model():
    return {'parent':'minecraft:block/cube_all',
            'textures':{'all':'guogaology:block/cloud_pearl'}}
