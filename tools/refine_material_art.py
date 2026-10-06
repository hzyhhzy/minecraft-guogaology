"""Committed, reproducible pixel-art sources for shared mineral / manuscript art.

The tiny source images preserve Alice's substrate and the original empty-set
mask; no dependency on the donor JAR or historical workspace is needed.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import json, math, random

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/googology'
T=A/'textures';S=ROOT/'tools/art_sources'
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,separators=(',',':'))+'\n','utf8')

def epsilon():
    # Preserve the material silhouette and substrate, replace only the sigil.
    for name in ('epsilon_ore','nether_epsilon_ore','epsilon_block','epsilon_material'):
        im=Image.open(S/(name+'.png')).convert('RGBA')
        for y in range(5,12):
            for x in range(5,12):
                c=im.getpixel((x,y))
                if min(c[:3])>240 or max(c[:3])<50:
                    fill=(148,121,174,255) if name.endswith('material') else im.getpixel((2,y)) if name=='epsilon_block' else im.getpixel((14,y))
                    im.putpixel((x,y),fill)
        eps=['01111','10000','01110','10000','01111']
        sub=['010','101','101','010']
        ink={(4+x,5+y) for y,row in enumerate(eps) for x,v in enumerate(row) if v=='1'}
        ink|={(10+x,9+y) for y,row in enumerate(sub) for x,v in enumerate(row) if v=='1'}
        for x,y in ink:
            for dx,dy in ((-1,0),(1,0),(0,-1),(0,1)):
                if (x+dx,y+dy) not in ink:im.putpixel((x+dx,y+dy),(252,250,255,255))
        for xy in ink:im.putpixel(xy,(37,26,49,255))
        im.save(T/('item' if name.endswith('material') else 'block')/(name+'.png'))

GLYPHS={
 'omega':['10001','10001','10101','10101','01010'],
 'epsilon':['01111','10000','01110','10000','01111'],
 'gamma':['11111','10000','10000','10000','10000'],
 'true_omega':['01110','10001','10001','10001','01010','11011']}
def glyph(d,name,x,y,color):
    for yy,row in enumerate(GLYPHS[name]):
        for xx,c in enumerate(row):
            if c=='1':d.point((x+xx,y+yy),fill=color)
def script(d,x,y,width,rows,color):
    # Handwritten strokes with visible gaps, not miniature typed characters.
    for r in range(rows):
        yy=y+r*3
        for k in range(0,width-1,4):
            length=min(width-k-1,2+(r+k)%2)
            d.line((x+k,yy,x+k+length,yy),fill=color)
            if (r+k)%3==0:d.point((x+k+1,yy-1),fill=color)

def manuscripts():
    for tier,name in enumerate(GLYPHS):
        im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
        dark='#302c40';paper='#f4eacc';edge='#b79c70';gold='#d3ad55';shine='#fff4bf'
        if tier==0: # A narrow illuminated scroll with actual curled ends.
            d.rectangle((7,4,24,27),fill=dark);d.rectangle((8,5,23,26),fill=edge)
            d.rectangle((9,5,22,25),fill=paper);d.rectangle((10,6,21,24),fill='#fff6db')
            d.rectangle((6,2,25,5),fill=gold);d.line((7,2,24,2),fill=shine)
            d.rectangle((5,3,7,5),fill=edge);d.rectangle((24,3,26,5),fill=edge)
            d.rectangle((6,25,24,29),fill=gold);d.line((7,25,23,25),fill=shine)
            d.line((8,26,21,26,21,28,8,28),fill='#8d653e');d.line((9,27,20,27),fill=paper)
            d.line((10,7,10,22),fill='#d2b96b');glyph(d,name,14,7,'#967544')
            script(d,12,15,9,3,'#8c7860')
            d.rectangle((19,20,22,26),fill='#934651');d.polygon([(18,19),(22,18),(24,21),(22,23),(19,22)],fill='#bc6771');d.point((20,19),fill='#f2b4a4')
        elif tier==1: # Loose, staggered vellum sheets under a silver clasp.
            d.polygon([(8,2),(27,5),(24,26),(4,23)],fill='#665473')
            d.polygon([(7,3),(26,6),(23,25),(5,22)],fill='#cdbed9')
            d.polygon([(5,7),(24,3),(28,25),(9,29)],fill=dark)
            d.polygon([(6,8),(23,4),(27,24),(10,28)],fill='#ddd2eb')
            d.rectangle((7,5,24,26),fill='#8f83a6');d.rectangle((8,6,23,25),fill='#f3edf6')
            d.line((9,7,22,7),fill='#c1a0d7');d.line((9,24,21,24),fill='#c1a0d7')
            glyph(d,name,13,9,'#86639c');script(d,10,16,11,2,'#9b8aa3')
            d.rectangle((19,22,21,29),fill='#9874b4');d.polygon([(18,19),(22,19),(24,22),(21,25),(18,23)],fill='#655488')
            d.polygon([(20,20),(22,21),(21,23),(19,22)],fill='#e7d9fc')
            for x,y in [(7,5),(21,5),(7,23)]:d.line((x,y,x+2,y),fill='#eff5fc');d.line((x,y,x,y+2),fill='#eff5fc')
        elif tier==2: # An open, stitched double folio with a jade bookmark.
            d.polygon([(2,7),(13,5),(16,7),(19,5),(30,7),(29,27),(19,25),(16,27),(12,25),(2,27)],fill='#254a47')
            d.polygon([(3,6),(12,4),(15,6),(15,24),(12,23),(3,25)],fill='#d0ba75')
            d.polygon([(17,6),(20,4),(29,6),(29,25),(20,23),(17,24)],fill='#d0ba75')
            d.polygon([(4,7),(12,5),(14,7),(14,23),(12,22),(4,24)],fill='#f4f0d8')
            d.polygon([(18,7),(20,5),(28,7),(28,24),(20,22),(18,23)],fill='#fffae5')
            d.line((16,7,16,24),fill='#739b81');d.line((15,7,15,24),fill='#ede3ab')
            glyph(d,name,7,9,'#528571');script(d,5,17,8,2,'#809780');script(d,20,10,7,4,'#809780')
            d.line((25,6,25,27),fill='#498d7e',width=2);d.polygon([(24,26),(27,26),(27,30),(25,29),(24,30)],fill='#6db7a0')
            d.point((26,7),fill='#d4f6d3');d.point((3,7),fill=shine);d.point((29,7),fill=shine)
        else: # A broad royal charter, lapis enamel rods and gold-ink seal.
            d.rectangle((3,8,28,26),fill='#313b58');d.rectangle((4,7,27,24),fill='#c4a365')
            d.rectangle((5,6,26,23),fill='#fff1d6');d.rectangle((7,7,24,21),fill='#f3e6cb')
            for x in (2,26):
                d.rectangle((x,3,x+3,25),fill='#39496e');d.line((x,4,x,24),fill='#a9c3d9')
                d.rectangle((x-1,2,x+4,4),fill=gold);d.line((x-1,2,x+4,2),fill=shine)
                d.rectangle((x-1,24,x+4,26),fill=gold);d.point((x,25),fill=shine)
            d.line((7,7,24,7),fill='#bd9653');d.line((7,22,24,22),fill='#bd9653')
            glyph(d,name,13,9,'#5b7194');script(d,8,17,15,2,'#aa8f6b')
            d.rectangle((14,21,16,29),fill='#586fb0');d.rectangle((17,22,18,28),fill='#334b88')
            d.polygon([(13,21),(18,21),(20,24),(17,27),(13,26),(11,23)],fill=gold)
            d.polygon([(14,22),(17,22),(18,24),(16,26),(13,24)],fill='#7292bb')
            d.line((14,22,17,22,18,24),fill='#ddf5f2');d.point((15,23),fill='#ffffff')
        im.save(T/f'item/{name}_manuscript.png')
        write(A/f'models/item/{name}_manuscript.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'googology:item/{name}_manuscript'}})

def quad(v,t,part=0,normal=None,color=0xffffffff):
    if normal is None:
        a=[v[1][i]-v[0][i] for i in range(3)];b=[v[2][i]-v[0][i] for i in range(3)]
        n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];l=math.sqrt(sum(x*x for x in n));normal=[x/l for x in n]
    return {'v':v,'n':[normal]*4,'c':[color]*4,'uv':[[0,0],[1,0],[1,1],[0,1]],'t':t,'part':part}
def mesh_box(a,b,t,part=0):
    x,y,z=a;X,Y,Z=b
    return [quad(v,t,part) for v in [
        [[x,y,z],[x,Y,z],[X,Y,z],[X,y,z]],[[X,y,Z],[X,Y,Z],[x,Y,Z],[x,y,Z]],
        [[x,y,Z],[x,Y,Z],[x,Y,z],[x,y,z]],[[X,y,z],[X,Y,z],[X,Y,Z],[X,y,Z]],
        [[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],[[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]]]]

def absence():
    # The old C1 relief is the source of truth. Do not replace it with a font
    # mask or recolor the animated higher-grade rings independently.
    from generate_absence_core import generate
    generate()

def main():
    epsilon();manuscripts();absence()
    from sanitize_core_shells import generate as sanitize_core_surfaces
    sanitize_core_surfaces()
    from generate_core_lod import generate
    generate()
if __name__=='__main__':main()
