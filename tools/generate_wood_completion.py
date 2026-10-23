"""Complete the two existing wood families using native items, tags and UV layouts.

Artwork is derived only from this repository's own wood/boat textures. Runtime
models reference vanilla templates; legacy signs get an explicit atlas adapter.
"""
from pathlib import Path
import argparse,json
from PIL import Image,ImageDraw,ImageEnhance
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/guogaology'
FAMILIES=('laver','dread')

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def tag(kind,name,values):
    path=RES/f'data/minecraft/tags/{kind}/{name}.json'
    value=json.loads(path.read_text('utf-8')) if path.exists() else {'replace':False,'values':[]}
    value['values']=list(dict.fromkeys(value['values']+values));write(path,value)

def image(path,value):
    path.parent.mkdir(parents=True,exist_ok=True);value.save(path)

def item(name,texture=None):
    model={'parent':'minecraft:item/generated','textures':{'layer0':texture or 'guogaology:item/'+name}}
    write(ASSETS/f'models/item/{name}.json',model)
    write(ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':'guogaology:item/'+name}})

def loot(block,drop):
    write(RES/f'data/guogaology/loot_table/blocks/{block}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'guogaology:'+drop,'conditions':[{'condition':'minecraft:survives_explosion'}]}]}],'random_sequence':'guogaology:'+block})

def recipe(name,value):
    write(RES/f'data/guogaology/recipe/{name}.json',value)
    ingredient='guogaology:'+name.split('_')[0]+'_boat' if name.endswith('chest_boat') else 'guogaology:'+name.split('_')[0]+'_stripped_log'
    write(RES/f'data/guogaology/advancement/recipes/{name}.json',{'parent':'minecraft:recipes/root','criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':ingredient}]}},'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'guogaology:'+name}}},'requirements':[['has_material','has_recipe']],'rewards':{'recipes':['guogaology:'+name]}})

def hanging_atlas(family):
    sign=Image.open(ASSETS/f'textures/block/{family}_sign.png').convert('RGBA')
    front=sign.crop((0,2,24,14)).resize((14,10),Image.Resampling.NEAREST)
    back=ImageEnhance.Brightness(front).enhance(.89)
    plank=Image.open(ASSETS/f'textures/block/{family}_planks.png').convert('RGBA')
    atlas=Image.new('RGBA',(32,32))
    for box in ((0,0,20,6),(0,7,20,9),(0,14,32,28)):
        for y in range(box[1],box[3]):
            for x in range(box[0],box[2]):atlas.putpixel((x,y),plank.getpixel((x%16,y%16)))
    atlas.paste(front,(2,16));atlas.paste(back,(18,16))
    d=ImageDraw.Draw(atlas)
    # Authored iron links in the native compact atlas's chain rectangles.
    for left,top,width,height in ((22,7,3,6),(28,8,3,4)):
        for y in range(height):
            for x in range(width):
                if x in (0,width-1) or y%3==0:d.point((left+x,top+y),fill=(168,179,184,255) if (x+y)%2 else (103,117,127,255))
    for x in range(12):
        y=abs(x-6)//2
        d.point((20+x,y),fill=(164,178,185,255));d.point((20+x,min(5,y+1)),fill=(100,116,127,255))
    return atlas,front

def generate():
    for family in FAMILIES:
        # Full two-block door silhouette instead of the lower block's square texture.
        top=Image.open(ASSETS/f'textures/block/{family}_door_top.png').convert('RGBA')
        bottom=Image.open(ASSETS/f'textures/block/{family}_door_bottom.png').convert('RGBA')
        tall=Image.new('RGBA',(16,32));tall.paste(top,(0,0));tall.paste(bottom,(0,16))
        icon=Image.new('RGBA',(16,16));icon.paste(tall.resize((7,14),Image.Resampling.NEAREST),(5,1))
        image(ASSETS/f'textures/item/{family}_door.png',icon)
        write(ASSETS/f'models/block/{family}_door_inventory.json',{'parent':'minecraft:item/generated','textures':{'layer0':'guogaology:item/'+family+'_door'}})
        # The edit GUI now shows exactly the outward/light face pixels.
        sign=Image.open(ASSETS/f'textures/block/{family}_sign.png').convert('RGBA')
        gui=Image.new('RGBA',(24,26));gui.paste(sign.crop((0,2,24,14)),(0,0));gui.paste(sign.crop((28,0,30,14)),(11,12))
        image(ASSETS/f'textures/gui/signs/{family}.png',gui)
        atlas,front=hanging_atlas(family);image(ASSETS/f'textures/block/{family}_hanging_sign.png',atlas)
        gui=Image.new('RGBA',(16,16));gui.paste(front,(1,3));image(ASSETS/f'textures/gui/hanging_signs/{family}.png',gui)
        icon=gui.copy();draw=ImageDraw.Draw(icon)
        for x in (3,12):draw.line((x,0,x,3),fill=(158,172,181,255),width=1)
        image(ASSETS/f'textures/item/{family}_hanging_sign.png',icon)
        item(family+'_hanging_sign')
        variants={}
        for attached in (False,True):
            for rotation in range(16):
                shape=('hanging_sign_attached_rot_' if attached else 'hanging_sign_rot_')+str(rotation%4)
                model=family+'_'+shape
                value={'model':'guogaology:block/'+model}
                if rotation//4:value['y']=90*(rotation//4)
                variants[f'attached={str(attached).lower()},rotation={rotation}']=value
                template=('template_attached_hanging_sign_rot_' if attached else 'template_hanging_sign_rot_')+str(rotation%4)
                write(ASSETS/f'models/block/{model}.json',{'parent':'minecraft:block/'+template,'textures':{'all':'guogaology:block/'+family+'_hanging_sign','particle':'guogaology:block/'+family+'_stripped_log'}})
        write(ASSETS/f'blockstates/{family}_hanging_sign.json',{'variants':variants})
        write(ASSETS/f'blockstates/{family}_wall_hanging_sign.json',{'variants':{f'facing={direction}':{'model':'guogaology:block/'+family+'_wall_hanging_sign',**({'y':angle} if angle else {})} for direction,angle in [('south',0),('west',90),('north',180),('east',270)]}})
        write(ASSETS/f'models/block/{family}_wall_hanging_sign.json',{'parent':'minecraft:block/template_wall_hanging_sign','textures':{'all':'guogaology:block/'+family+'_hanging_sign','particle':'guogaology:block/'+family+'_stripped_log'}})
        for shape in ('hanging_sign','wall_hanging_sign'):loot(family+'_'+shape,family+'_hanging_sign')
        recipe(family+'_hanging_sign',{'type':'minecraft:crafting_shaped','category':'misc','group':'hanging_sign','pattern':['C C','###','###'],'key':{'C':{'item':'minecraft:chain'},'#':{'item':'guogaology:'+family+'_stripped_log'}},'result':{'id':'guogaology:'+family+'_hanging_sign','count':6}})
        recipe(family+'_chest_boat',{'type':'minecraft:crafting_shapeless','category':'misc','group':'chest_boat','ingredients':[{'item':'guogaology:'+family+'_boat'},{'item':'minecraft:chest'}],'result':{'id':'guogaology:'+family+'_chest_boat','count':1}})
        boat=Image.open(ASSETS/f'textures/item/{family}_boat.png').convert('RGBA');icon=boat.copy();d=ImageDraw.Draw(icon)
        d.rectangle((6,3,12,9),fill=(126,81,39,255),outline=(60,43,31,255));d.line((7,5,11,5),fill=(189,144,70,255));d.rectangle((9,6,10,7),fill=(215,213,177,255))
        image(ASSETS/f'textures/item/{family}_chest_boat.png',icon);item(family+'_chest_boat')
        # Native chest-boat UVs reserve an extra64rows; hull pixels remain unchanged.
        hull=Image.open(ASSETS/f'textures/entity/boat/{family}_boat.png').convert('RGBA')
        tex=Image.new('RGBA',(128,128));tex.paste(hull,(0,0));d=ImageDraw.Draw(tex)
        for y in range(64,128):
            for x in range(128):
                shade=11 if y%8==0 or x%12==0 else (x*3+y)%9
                tex.putpixel((x,y),(126+shade,83+shade,43+shade,255))
        # The native chest base also uses the final5rows of the upper half.
        d.rectangle((0,59,48,63),fill=(126,83,43,255));d.rectangle((0,59,2,63),fill=(202,203,174,255))
        image(ASSETS/f'textures/entity/chest_boat/{family}.png',tex)
    for kind in ('block','item'):tag(kind,'wooden_fences',['guogaology:laver_fence','guogaology:dread_fence'])
    tag('block','fences',['guogaology:laver_fence','guogaology:dread_fence'])
    tag('block','ceiling_hanging_signs',[f'guogaology:{f}_hanging_sign' for f in FAMILIES])
    tag('block','wall_hanging_signs',[f'guogaology:{f}_wall_hanging_sign' for f in FAMILIES])
    tag('block','mineable/axe',[f'guogaology:{f}_{shape}' for f in FAMILIES for shape in ('hanging_sign','wall_hanging_sign')])
    tag('item','hanging_signs',[f'guogaology:{f}_hanging_sign' for f in FAMILIES])
    tag('item','boats',[f'guogaology:{f}_chest_boat' for f in FAMILIES])
    tag('item','chest_boats',[f'guogaology:{f}_chest_boat' for f in FAMILIES])
    tag('item','non_flammable_wood',['guogaology:dread_hanging_sign','guogaology:dread_chest_boat'])
    print('WOOD_COMPLETION_RESOURCES_OK families=2')


def prepare(out,target):
    """Only the four new native recipes/advancements need this chain spelling."""
    if target=='1.21.1':return
    out=Path(out)
    for family in FAMILIES:
        for kind in ('recipe','advancement/recipes'):
            path=out/f'data/guogaology/{kind}/{family}_hanging_sign.json'
            if path.exists():path.write_text(path.read_text('utf-8').replace('minecraft:chain','minecraft:iron_chain'),encoding='utf-8')

if __name__=='__main__':
    generate()
