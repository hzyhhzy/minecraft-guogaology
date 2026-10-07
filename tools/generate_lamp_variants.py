"""Generate only the six plain/numbered lamp families and their exact shared substrates.

Every digit is painted on a copy of the same color's base bitmap. The original
0..32 glyphs and pixel dimensions are retained; plain lamps have independent IDs.
No acquisition recipes are added: these decorations retain their scenery source.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import argparse
import copy
import gzip
import json
import random

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT/'src/main/resources'
ASSET = RES/'assets/guogaology'
COLORS = ('amber', 'cyan', 'rose', 'lime', 'violet', 'scarlet')
RGB = ((255,174,48),(47,224,239),(255,112,182),(160,239,55),(188,130,255),(255,87,76))
DIGITS = ('111 101 101 101 111','010 110 010 010 111','111 001 111 100 111',
          '111 001 111 001 111','101 101 111 001 001','111 100 111 001 111',
          '111 100 111 101 111','111 001 010 010 010','111 101 111 101 111','111 101 111 001 111')
INK = (35,39,53,255)


def base(color):
    rgb = RGB[color]
    rng = random.Random(40300+color)
    image = Image.new('RGBA',(32,32))
    for y in range(32):
        for x in range(32):
            grain = rng.randrange(-3,4)
            image.putpixel((x,y),tuple(max(0,min(255,c+grain)) for c in rgb)+(255,))
    ImageDraw.Draw(image).rectangle((0,0,31,31),outline=tuple(min(255,c+45) for c in rgb)+(255,),width=2)
    return image


def glyph_mask(value):
    image=Image.new('1',(32,32));draw=ImageDraw.Draw(image)
    text=str(value);scale=4 if value<10 else 3
    left,top=(32-(len(text)*4-1)*scale)//2,(32-5*scale)//2
    for i,char in enumerate(text):
        for y,row in enumerate(DIGITS[int(char)].split()):
            for x,bit in enumerate(row):
                if bit=='1':draw.rectangle((left+(i*4+x)*scale,top+y*scale,left+(i*4+x+1)*scale-1,top+(y+1)*scale-1),fill=1)
    return image


def generate(check=False):
    changed=[]
    def write(path,value):
        raw=(json.dumps(value,ensure_ascii=False,indent=2)+'\n').encode('utf8')
        if not path.exists() or path.read_bytes()!=raw:
            changed.append(str(path.relative_to(ROOT)))
            if not check:path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
    def png(path,image):
        if not path.exists() or Image.open(path).convert('RGBA').tobytes()!=image.tobytes():
            changed.append(str(path.relative_to(ROOT)))
            if not check:image.save(path)
    for color,name in enumerate(COLORS):
        substrate=base(color)
        png(ASSET/f'textures/block/sequence_light_{color}.png',substrate)
        write(ASSET/f'blockstates/{name}_light.json',{'variants':{'':{'model':f'guogaology:block/sequence_light_{color}'}}})
        write(ASSET/f'models/item/{name}_light.json',{'parent':f'guogaology:block/sequence_light_{color}'})
        numeric={'variants':{}}
        overrides=[]
        for value in range(33):
            number=substrate.copy();number.paste(INK,(0,0,32,32),glyph_mask(value))
            model=f'guogaology:block/christmas_digit_{color}_{value}'
            png(ASSET/f'textures/block/christmas_digit_{color}_{value}.png',number)
            numeric['variants'][f'digit={value}']={'model':model}
            overrides.append({'predicate':{'guogaology:appearance':value/256.0},'model':model})
        write(ASSET/f'blockstates/{name}_sequence_light.json',numeric)
        write(ASSET/f'models/item/{name}_sequence_light.json',{'parent':f'guogaology:block/christmas_digit_{color}_0','overrides':overrides})
        write(RES/f'data/guogaology/loot_table/blocks/{name}_light.json',{
            'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'guogaology:{name}_light'}],
                                             'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    profile_path=RES/'guogaology/block_balance.json'
    profiles=json.loads(profile_path.read_text('utf8'))
    for color in COLORS:
        profiles[color+'_light']=copy.deepcopy(profiles[color+'_sequence_light'])
        profiles[color+'_light']['use']='无字照明；独立方块，右键不变成数字'
    write(profile_path,dict(sorted(profiles.items())))
    # Plain colors have the same harvesting/tool classification as their numbered partner.
    for path in (RES/'data/minecraft/tags/block').rglob('*.json'):
        data=json.loads(path.read_text('utf8'));values=data.get('values',[])
        additions=[f'guogaology:{color}_light' for color in COLORS
                   if f'guogaology:{color}_sequence_light' in values and f'guogaology:{color}_light' not in values]
        if additions:data['values']=values+additions;write(path,data)
    # Only re-encode imported templates that actually contain the donor's plain lamp.
    # Their immutable canonical source preserves coordinates and every unrelated state.
    from import_outer_content import nbt_transform
    for path in (ROOT/'content/outer-1.0.0').rglob('*.nbt'):
        if b'guogaology:christmas_light' not in gzip.decompress(path.read_bytes()):continue
        relative=path.relative_to(ROOT/'content/outer-1.0.0')
        target=RES/'data/guogaology'/relative
        if not target.exists():continue
        raw=nbt_transform(path.read_bytes(),legacy=True)
        if target.read_bytes()!=raw:
            changed.append(str(target.relative_to(ROOT)))
            if not check:target.write_bytes(raw)
    print(('CHECK' if check else 'GENERATED')+f': six plain IDs, 198 numbered textures on six exact substrates; {len(changed)} changed files')
    if check and changed:raise SystemExit('Lamp resources have drifted: '+', '.join(changed))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--check',action='store_true')
    generate(parser.parse_args().check)
