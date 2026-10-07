"""Current shared material art. Models and textures are committed build inputs.

Uses the existing pixel glyph masks and Alice's white substrate, not system fonts.
Run manually after changing these designs; ordinary builds need no image libraries.
"""
from pathlib import Path
from collections import Counter
from PIL import Image,ImageDraw
import json,math

ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/guogaology'
T=A/'textures/block';S=ROOT/'tools/art_sources'
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,separators=(',',':'))+'\n','utf8')
def save(im,name):im.save(T/(name+'.png'))

def matrices():
    base=Image.open(S/'matrix_ground.png').convert('RGBA').resize((32,32),Image.Resampling.NEAREST)
    masks=json.loads((S/'matrix_masks.json').read_text('utf8'))
    for name,pixels in masks.items():
        im=base.copy()
        for x,y in pixels:im.putpixel((x,y),(30,39,49,255))
        save(im,name)

def absence():
    from refine_material_art import absence as current_absence
    current_absence()

def slice_icon():
    # The original mint-and-violet jelly silhouette, with bevels, translucency
    # highlights, small fruit inclusions and a quiet Guogao face on the cut side.
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    top=[(5,10),(20,3),(29,10),(15,17),(4,13)]
    left=[(4,13),(15,17),(15,29),(4,24)]
    right=[(15,17),(29,10),(29,23),(15,29)]
    d.polygon(left,fill='#946697');d.polygon(right,fill='#b97ea9');d.polygon(top,fill='#b6d9b5')
    d.line([(5,10),(20,3),(29,10)],fill='#edfae5',width=2)
    d.line([(4,13),(15,18),(29,11)],fill='#ddf4d7',width=2)
    d.line([(5,15),(5,23),(13,27)],fill='#b382bc')
    d.line([(27,14),(27,22),(17,26)],fill='#d6a3c8')
    d.polygon([(8,9),(11,8),(14,10),(11,11)],fill='#d7efc5')
    d.rectangle((19,19,21,22),fill='#efc979');d.point((19,19),fill='#fff0b2')
    d.rectangle((25,15,26,17),fill='#a5d6d9');d.rectangle((8,19,9,21),fill='#d7a29b')
    d.line([(17,15),(19,15)],fill='#67436b');d.line([(24,12),(25,13)],fill='#67436b')
    d.point((19,17),fill='#67436b');d.point((24,15),fill='#67436b');d.line([(20,20),(22,19)],fill='#67436b')
    d.line([(16,16),(16,18)],fill='#cee7ea')
    im.save(A/'textures/item/guogao_slice.png')

def cube(a,b,tex):
    return {'from':a,'to':b,'faces':{f:{'texture':'#'+tex,'uv':[0,0,16,16]} for f in ('north','south','east','west','up','down')}}

def portals():
    # Ivory/gold home arch; indigo/amethyst matrix gate; aged dark jade Guogao.
    palettes=[((224,229,207),(211,179,97),(63,139,135)),((26,25,63),(124,146,244),(125,237,242)),((23,41,35),(133,128,76),(162,198,127))]
    for style,(base,trim,ink) in enumerate(palettes):
        for layer,col in [('base',base),('trim',trim)]:
            im=Image.new('RGBA',(16,16),col+(255,));d=ImageDraw.Draw(im)
            d.line([(0,15),(0,0),(15,0)],fill=tuple(min(255,c+20) for c in col)+(255,))
            d.line([(15,1),(15,15),(1,15)],fill=tuple(max(0,c-20) for c in col)+(255,))
            if layer=='base':
                for x,y in [(3,5),(11,12),(8,3),(6,10)]:d.line((x,y,x+2,y),fill=tuple(max(0,c-6) for c in col)+(255,))
                if style==0:
                    d.line((2,14,2,2,14,2),fill=trim+(255,));d.line((4,12,4,4,12,4),fill=(245,244,222,255))
                elif style==1:
                    d.line((1,11,5,11,5,5,10,5,10,1),fill=trim+(255,));d.rectangle((7,8,12,13),outline=ink+(230,))
                else:
                    for x in (4,11):d.line((x,14,x,5,x-2,7),fill=trim+(230,));d.line((x,9,x+2,11),fill=(70,88,58,255))
            save(im,f'gate_{style}_{layer}')
        im=Image.new('RGBA',(32,32),base+(255,));d=ImageDraw.Draw(im)
        d.rectangle((1,1,30,30),outline=trim+(255,),width=2)
        if style==0:
            d.arc((6,5,25,24),145,395,fill=ink+(255,),width=3)
            d.line((7,19,10,25,5,25),fill=ink+(255,),width=3);d.line((24,19,21,25,26,25),fill=ink+(255,),width=3)
            for x,y in [(4,4),(26,4),(4,26),(26,26)]:d.point((x,y),fill=(255,250,202,255))
        elif style==1:
            d.polygon([(16,3),(28,16),(16,28),(3,16)],outline=trim+(255,),width=2)
            for x in (10,18):
                for y in (9,17):d.rectangle((x,y,x+4,y+4),fill=(53,62,117,255),outline=ink+(255,))
            d.line((5,24,11,15,16,22,25,7),fill=(229,220,255,255),width=2)
        else:
            d.rectangle((5,5,26,26),outline=(62,80,54,255))
            d.line((8,12,11,10),fill=ink+(255,),width=2);d.line((20,10,23,12),fill=ink+(255,),width=2)
            d.rectangle((10,14,11,17),fill=ink+(255,));d.rectangle((21,14,22,17),fill=ink+(255,))
            d.rectangle((13,21,20,23),fill=(13,23,27,255));d.line((14,20,19,20),fill=ink+(255,))
            d.polygon([(7,14),(5,20),(7,22),(9,20)],fill=trim+(255,))
        save(im,f'gate_{style}_seal')
        parts=[cube([0,0,0],[16,3,16],'base'),cube([2,3,2],[14,5,14],'trim')]
        if style==0:
            parts += [cube([3,5,3],[13,9,13],'base'),cube([4,9,4],[12,11,12],'seal')]
            for x in (1,12):
                for z in (1,12):parts.append(cube([x,5,z],[x+3,8,z+3],'trim'))
        elif style==1:
            parts += [cube([3,5,3],[13,8,13],'base'),cube([4,8,4],[12,11,12],'trim'),cube([3,11,3],[13,12,13],'seal')]
            for x in (1,13):
                for z in (1,13):parts.append(cube([x,5,z],[x+2,13,z+2],'trim'))
        else:
            parts += [cube([2,5,2],[14,8,14],'base'),cube([4,8,4],[12,12,12],'seal')]
            for x in (1,12):
                for z in (1,12):
                    parts.append(cube([x,5,z],[x+3,11,z+3],'base'))
                    parts.append(cube([x+.5,11,z+.5],[x+2.5,14,z+2.5],'trim'))
        textures={k:f'guogaology:block/gate_{style}_{k}' for k in ('base','trim','seal')};textures['particle']=textures['base']
        write(A/f'models/block/gate_frame_{style}.json',{'parent':'minecraft:block/block','textures':textures,'elements':parts})
        # Seamless, dark animated portal surface with restrained colored tracery.
        sheet=Image.new('RGBA',(32,32*16))
        for frame in range(16):
            tile=Image.new('RGBA',(32,32));draw=ImageDraw.Draw(tile)
            for y in range(32):
                for x in range(32):
                    wave=(math.sin((x+y)/32*math.tau+frame/16*math.tau)+math.cos((x-y)/32*math.tau-frame/16*math.tau))*.5
                    field=((28,92,93),(41,20,92),(12,37,27))[style]
                    tile.putpixel((x,y),tuple(int(c*(.6+.3*max(0,wave))) for c in field)+(238,))
            if style==0:
                draw.arc((6,6,25,25),frame*6,frame*6+230,fill=(147,222,200,165),width=1)
                draw.arc((10,9,21,21),145,395,fill=trim+(205,),width=1);draw.line((10,19,12,23,8,23),fill=trim+(205,));draw.line((21,19,19,23,23,23),fill=trim+(205,))
                for x,y in [(3,3),(27,27),(4,24)]:draw.point((x,y),fill=(246,234,165,190))
            elif style==1:
                draw.polygon([(16,2),(29,16),(16,29),(2,16)],outline=trim+(155,))
                for x in (9,18):
                    for y in (9,18):draw.rectangle((x,y,x+4,y+4),outline=ink+(190,))
                draw.line((4,25,10,15,16,23,26,6),fill=(206,182,251,200))
            else:
                draw.arc((2,3,29,30),205,335,fill=(72,105,58,125),width=2)
                draw.line((8,12,11,10),fill=trim+(140,));draw.line((20,10,23,12),fill=trim+(140,));draw.point((11,15),fill=trim+(190,));draw.point((21,15),fill=trim+(190,));draw.line((13,22,19,22),fill=ink+(170,));draw.polygon([(7,14),(5,20),(8,20)],fill=trim+(160,))
            sheet.paste(tile,(0,32*frame))
        save(sheet,f'gate_field_{style}');write(T/f'gate_field_{style}.png.mcmeta',{'animation':{'frametime':3,'interpolate':True}})
        tex=f'guogaology:block/gate_field_{style}'
        write(A/f'models/block/gate_field_{style}.json',{'parent':'minecraft:block/block','ambientocclusion':False,'textures':{'field':tex,'particle':tex},'elements':[cube([0,4.5,0],[16,5.5,16],'field')]})
    write(A/'blockstates/guogao_portal_frame.json',{'variants':{f'style={i}':{'model':f'guogaology:block/gate_frame_{i}'} for i in range(3)}})
    write(A/'models/item/guogao_portal_frame.json',{'parent':'guogaology:block/gate_frame_0','overrides':[{'predicate':{'guogaology:appearance':i/256},'model':f'guogaology:block/gate_frame_{i}'} for i in range(3)]})
    for name in ('guogao_portal','inner_portal','fruit_portal'):
        write(A/f'blockstates/{name}.json',{'variants':{f'style={i}':{'model':f'guogaology:block/gate_field_{i}'} for i in range(3)}})

def main():
    matrices();absence();slice_icon();portals()
    from generate_return_portals import generate as return_portals
    return_portals()
    from generate_enhancement_art import generate as enhancement_art
    enhancement_art()
    from sanitize_core_shells import generate as sanitize_core_surfaces
    sanitize_core_surfaces()
    from generate_core_lod import generate
    generate()
if __name__=='__main__':main()
