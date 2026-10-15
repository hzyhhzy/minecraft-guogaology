"""0.5.5 source-controlled raw materials, crystal mining, recipes and bilingual copy.

Textures and small vanilla models are deterministic and have no runtime geometry
generator. Run --check after historical generators to detect stale resources.
"""
from pathlib import Path
import argparse
import copy
import io
import json
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
A = RES / 'assets/guogaology'
D = RES / 'data/guogaology'
MATERIALS = {
    'matrix_sheet': ('矩阵薄片', 'Matrix Sheet', ('#344858','#6e9db2','#a9d8e0','#eaf8f3')),
    'arrow_knot': ('箭纹木节', 'Arrowgrain Knot', ('#593626','#9e6135','#d49a4b','#f9db98')),
    'scg_node': ('SCG结点', 'SCG Junction', ('#243956','#47617c','#81abc6','#d7edf0')),
    'hydra_sprig': ('Hydra芽穗', 'Hydra Sprig', ('#164d40','#31886e','#6fd49d','#d9f5b2')),
    'laver_gel': ('紫菜胶质', 'Laver Gel', ('#1d665c','#4e9f89','#91d9b0','#e9f8d7')),
    'cloud_pearl': ('凝云珠', 'Condensed Cloud Pearl', ('#2a7b93','#6bc0cb','#bfebe6','#f4fffa')),
    'set_membrane': ('集合膜', 'Set Membrane', ('#277c77','#60bbb0','#abebd3','#efffe5')),
    'dread_resin': ('冥杉树脂', 'Dread Fir Resin', ('#452340','#78465b','#b77866','#eed0a1')),
}
ORES = ('ordinal_crystal_ore', 'empty_crystal_ore', 'dread_crystal_ore')
REGIONAL = ('sequence_core', 'power_tower_core', 'hydra_bud', 'lho_trace',
            'laver_core', 'astra_critical_core', 'boundary_core', 'guogao_heart')
# Bounds deliberately match each material's readable, physically distinct model.
BOXES = {
    'matrix_sheet': [((1,0,1),(13,2,13)),((3,4,3),(15,6,15)),((1,8,1),(13,10,13))],
    'arrow_knot': [((2,0,2),(14,12,14)),((5,12,5),(11,14,11))],
    'scg_node': [((5,3,5),(11,11,11)),((0,5,6),(5,9,10)),((11,5,6),(16,9,10)),((6,11,6),(10,14,10)),((6,5,0),(10,9,5))],
    'hydra_sprig': [((6,0,6),(10,8,10)),((2,7,5),(7,10,10)),((10,7,5),(14,10,10)),((1,10,4),(5,15,8)),((6,9,9),(10,14,13)),((11,10,3),(15,15,7))],
    'laver_gel': [((2,0,3),(14,5,13)),((3,5,4),(12,8,12)),((5,8,6),(10,9,10))],
    'cloud_pearl': [((4,0,4),(12,2,12)),((2,2,3),(14,8,13)),((4,8,4),(12,12,12)),((1,4,5),(4,7,10))],
    'set_membrane': [((1,0,1),(15,1,15)),((2,1,2),(3,8,14)),((13,1,2),(14,8,14)),((4,3,4),(12,4,12)),((4,4,4),(5,10,12)),((11,4,4),(12,10,12)),((6,7,6),(10,8,10))],
    'dread_resin': [((4,0,4),(12,3,12)),((3,3,4),(13,8,12)),((5,8,5),(11,11,11)),((7,11,6),(10,14,9))],
}
TAG_VALUES = {
    'ancient_notation_materials': ['beaf_marble', 'bird_shale', 'conway_link'],
    'ordinal_flowers': ['epsilon_bloom','emoji_flower','omega_bloom','great_omega_bloom','zeta_vine','psi_fern','phi_bloom','graham_flower'],
    'numbered_lamps': [f'{c}_sequence_light' for c in ('amber','cyan','rose','lime','violet','scarlet')],
    'guogao_lamps': ['guogao_lantern'] + [f'{c}_guogao_lantern' for c in ('cyan','rose','lime','violet','scarlet')],
}
FIRST = {
    'sequence_core': ['matrix_sheet']*2 + ['ridge_lamina']*2 + ['projection_glass']*2 + ['y_leaves']*2,
    'power_tower_core': ['arrow_knot']*2 + ['scg_node']*2 + ['tree_node_red','tree_node_green','tree_node_blue','#ancient_notation_materials'],
    'hydra_bud': ['hydra_sprig']*2 + ['veblen_petal']*2 + ['psi_symbol','great_omega_symbol'] + ['#ordinal_flowers']*2,
    'lho_trace': ['lho_letter_l','lho_letter_h','lho_letter_o','fffz_trace','fos_trace','lho_hydra_psi','lho_hydra_z','absence_glass'],
    'laver_core': ['laver_gel']*2 + ['iblp_node']*2 + ['iblp_marked','iblp_blank','#fruit_guogao','tianyi_fiber'],
    'astra_critical_core': ['cloud_pearl']*2 + ['compute_chip']*2 + ['astra_cloud']*2 + ['astra_marble','astra_mint'],
    'boundary_core': ['set_membrane']*2 + ['turing_tape']*2 + ['proof_stone']*2 + ['formula_stone','rayo_rose'],
    'guogao_heart': ['dread_resin']*2 + ['dread_leaves']*2 + ['#fruit_guogao','star_gold','#numbered_lamps','#guogao_lamps'],
}
NEW_COPY = {
    'block.guogaology.ordinal_crystal_ore': ('序数晶矿', 'Ordinal Crystal Ore'),
    'block.guogaology.empty_crystal_ore': ('空境序数晶矿', 'Voidglass Ordinal Crystal Ore'),
    'block.guogaology.dread_crystal_ore': ('冥岩序数晶矿', 'Understone Ordinal Crystal Ore'),
    'block.guogaology.turing_endcap': ('纸带端帽', 'Tape Endcap'),
    'block.guogaology.guogao_lamp_shaper': ('果糕灯塑形台', 'Guogao Lamp Shaper'),
    'lamp.guogaology.input_hint': ('放入同色果糕灯，选择形态', 'Insert a Guogao lamp and choose its shape'),
    'lamp.guogaology.emotion': ('表情：%s', 'Face: %s'),
    'lamp.guogaology.full': ('完整 1×1×1', 'Full 1×1×1'),
    'lamp.guogaology.tile': ('3×3×3 分块：%s', '3×3×3 part: %s'),
    'lamp.guogaology.previous': ('上一页', 'Previous'),
    'lamp.guogaology.next': ('下一页', 'Next'),
    'mining.guogaology.enchantment_socket_conflict': ('附魔与镶嵌不能共存', 'Enchantments and sockets cannot coexist'),
    'mining.guogaology.enchanted_no_sockets': ('已附魔：去除附魔后可镶嵌', 'Enchanted: remove enchantments before socketing'),
    'mining.guogaology.socketed_no_enchantments': ('已镶嵌：拆空后可附魔', 'Socketed: remove all contents before enchanting'),
    'mining.guogaology.install.enchanted': ('已附魔的装备无法镶嵌', 'Enchanted equipment cannot accept sockets'),
}


def read(path):
    return json.loads(path.read_text(encoding='utf8'))


def self_loot(name):
    return {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'guogaology:'+name}],
                                            'conditions':[{'condition':'minecraft:survives_explosion'}]}]}


def ore_loot(name):
    return {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{
        'type':'minecraft:alternatives','children':[
            {'type':'minecraft:item','name':'guogaology:'+name,'conditions':[{
                'condition':'minecraft:match_tool','predicate':{'predicates':{'minecraft:enchantments':[
                    {'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}}]},
            {'type':'minecraft:item','name':'guogaology:ordinal_shard','functions':[
                {'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':4,'max':6}},
                {'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:ore_drops'},
                {'function':'minecraft:explosion_decay'}]}]}]}]}


def collectible_laver_loot(value):
    """Preserve Alice's ribbon probabilities, but also let ordinary shears collect leaves.

    Accepts either the original26.3 or shared legacy loot schema, before the
    existing per-version converter. Explicit enchantments remain in the recipe
    ecosystem, but the first Laver core never requires an existing core.
    """
    value = copy.deepcopy(value)
    first = value['pools'][0]['entries'][0]['children'][0]
    if first.get('conditions'):
        original = first['conditions'][0]
        if original.get('condition') == 'minecraft:any_of':
            return value
        first['conditions'] = [{'condition':'minecraft:any_of','terms':[original,
            {'condition':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}]}]
    elif first.get('condition'):
        original = first['condition']
        if original.get('type') == 'minecraft:any_of':
            return value
        first['condition'] = {'type':'minecraft:any_of','terms':[original,
            {'type':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}]}
    else:
        raise ValueError('Review the Laver source loot schema before regenerating')
    return value


def element(bounds, texture='body'):
    a,b=bounds
    return {'from':list(a),'to':list(b),'faces':{f:{'uv':[0,0,16,16],'texture':'#'+texture}
                                             for f in ('down','up','north','south','west','east')}}


def material_texture(name, palette):
    rng=random.Random(name)
    im=Image.new('RGBA',(32,32),palette[1]);d=ImageDraw.Draw(im)
    for _ in range(70):
        x,y=rng.randrange(32),rng.randrange(32)
        d.rectangle((x,y,min(x+1,31),min(y+1,31)),fill=palette[rng.choice((0,1,1,2))])
    if name=='matrix_sheet':
        d.rectangle((3,3,28,28),outline=palette[3],width=2)
        for x in (8,20):
            for y in (8,20): d.rectangle((x,y,x+4,y+4),fill=palette[0]);d.point((x,y),fill=palette[3])
    elif name=='arrow_knot':
        for x in range(1,32,5):d.line((x,0,x+2,31),fill=palette[0],width=2)
        for y in (9,20):d.line((7,y+5,15,y-3,23,y+5),fill=palette[3],width=3)
    elif name=='scg_node':
        d.rectangle((11,11,20,20),fill=palette[2],outline=palette[3],width=2)
        for line in ((0,15,11,15),(20,15,31,15),(15,0,15,11)):
            d.line(line,fill=palette[0],width=5);d.line(line,fill=palette[2],width=2)
    elif name=='hydra_sprig':
        d.line((15,31,15,15,5,4),fill=palette[3],width=3)
        d.line((15,18,26,5),fill=palette[3],width=3)
        d.line((15,20,23,27),fill=palette[2],width=3)
    elif name=='laver_gel':
        for y in (8,17,25):d.arc((1,y-6,30,y+6),10,170,fill=palette[3],width=2)
        d.rectangle((5,3,11,5),fill=palette[3]);d.rectangle((6,5,8,10),fill=palette[2])
    elif name=='cloud_pearl':
        d.rectangle((0,0,31,31),fill=palette[2])
        for box in ((2,14,17,25),(10,4,28,22),(15,14,31,31)):
            d.ellipse(box,fill=palette[3],outline=palette[1],width=2)
    elif name=='set_membrane':
        for box in ((2,2,29,29),(8,7,26,25),(12,12,22,22)):
            d.rectangle(box,outline=palette[3],width=2)
        d.line((3,28,12,21,21,21),fill=palette[0],width=2)
    else:
        d.line((16,31,16,15,7,5),fill=palette[0],width=3)
        d.line((16,16,25,4),fill=palette[0],width=3)
        d.line((16,23,26,17),fill=palette[2],width=2)
        d.rectangle((8,6,11,11),fill=palette[3]);d.rectangle((11,4,17,6),fill=palette[3])
    if name in ('laver_gel','set_membrane','dread_resin'):
        alpha=220 if name!='set_membrane' else 170
        im.putalpha(Image.eval(im.getchannel('A'),lambda a:alpha))
    return im


def crystal_ore_texture(name):
    rng=random.Random(name)
    if name=='dread_crystal_ore':
        im=Image.open(A/'textures/block/rootbound_stone.png').convert('RGBA')
    elif name=='empty_crystal_ore':
        im=Image.open(A/'textures/block/absence_glass.png').convert('RGBA')
    else:
        # Match numbered mother-rock's palette; omit the stone's numeral.
        source=Image.open(A/'textures/block/ordinal_stone_0.png').convert('RGBA')
        samples=[source.getpixel((x,y)) for x in range(2,9) for y in range(2,9)]
        im=Image.new('RGBA',(32,32));im.putdata([rng.choice(samples) for _ in range(1024)])
    im=im.resize((32,32),Image.Resampling.NEAREST);d=ImageDraw.Draw(im)
    shades=('#628da8','#a8d7e1','#eefbfa','#ad9bd0')
    for x,y in ((5,5),(20,3),(11,15),(25,23),(2,25)):
        d.polygon([(x,y+1),(x+3,y),(x+6,y+3),(x+4,y+7),(x,y+5)],fill=shades[0])
        d.polygon([(x+1,y+2),(x+3,y+1),(x+4,y+3),(x+3,y+5),(x+1,y+4)],fill=shades[1])
        d.line((x+1,y+2,x+3,y+1,x+4,y+3),fill=shades[2],width=1)
        d.point((x+2,y+4),fill=shades[3])
    return im


def ingredient(name):
    return {'tag':'guogaology:'+name[1:]} if name.startswith('#') else {'item':'guogaology:'+name}


def shapeless(materials, result, count=1):
    return {'type':'minecraft:crafting_shapeless','category':'misc',
            'ingredients':[ingredient(n) for n in materials],'result':{'id':'guogaology:'+result,'count':count}}


def generated_files():
    out={}
    def put(path,value):out[path]=value
    for name,(cn,en,palette) in MATERIALS.items():
        put(A/f'textures/block/{name}.png',material_texture(name,palette))
        put(A/f'blockstates/{name}.json',{'variants':{'':{'model':'guogaology:block/'+name}}})
        put(A/f'models/block/{name}.json',{'parent':'minecraft:block/block',
            'textures':{'body':'guogaology:block/'+name,'particle':'guogaology:block/'+name},
            'elements':[element(b) for b in BOXES[name]]})
        put(A/f'models/item/{name}.json',{'parent':'guogaology:block/'+name})
        put(D/f'loot_table/blocks/{name}.json',self_loot(name))
    for name in (*ORES,'turing_endcap'):
        if name in ORES: image=crystal_ore_texture(name)
        else:
            image=Image.new('RGBA',(32,32),'#4d455b');d=ImageDraw.Draw(image)
            d.rectangle((2,2,29,29),outline='#b69dcd',width=3)
            d.rectangle((7,7,24,24),fill='#d5d0dc',outline='#75647c',width=2)
            d.line((11,11,20,20),fill='#8f743e',width=3);d.line((20,11,11,20),fill='#8f743e',width=3)
        put(A/f'textures/block/{name}.png',image)
        put(A/f'blockstates/{name}.json',{'variants':{'':{'model':'guogaology:block/'+name}}})
        put(A/f'models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':'guogaology:block/'+name}})
        put(A/f'models/item/{name}.json',{'parent':'guogaology:block/'+name})
        put(D/f'loot_table/blocks/{name}.json',ore_loot(name) if name in ORES else self_loot(name))
    put(D/'loot_table/blocks/ordinal_crystal.json',self_loot('ordinal_crystal'))
    path=D/'loot_table/blocks/giant_laver.json'
    put(path,collectible_laver_loot(read(path)))
    for name,values in TAG_VALUES.items():
        put(D/f'tags/item/{name}.json',{'replace':False,'values':['guogaology:'+n for n in values]})
    recipes={}
    for core,materials in FIRST.items():
        assert len(materials)==8
        recipes[core]=shapeless(materials+['ordinal_crystal'],core)
        for grade in (2,3):
            source=core+('' if grade==2 else '_lv2')
            recipes[f'{core}_lv{grade}']=shapeless([source]*8+[f'ordinal_crystal_lv{grade}'],f'{core}_lv{grade}')
    recipes['ordinal_crystal']=shapeless(['ordinal_shard']*4,'ordinal_crystal')
    recipes['ordinal_shards_from_crystal']=shapeless(['ordinal_crystal'],'ordinal_shard',4)
    for stage in (2,3,4):
        source='ordinal_crystal'+('' if stage==2 else f'_lv{stage-1}')
        recipes[f'ordinal_crystal_lv{stage}']=shapeless([source]*9,f'ordinal_crystal_lv{stage}')
        recipes[f'ordinal_crystal_lv{stage}_unpack']=shapeless([f'ordinal_crystal_lv{stage}'],source,9)
    recipes['guogao_lamp_shaper']={'type':'minecraft:crafting_shaped','category':'misc','pattern':[' L ','QQQ','CIC'],
        'key':{'L':{'tag':'guogaology:guogao_lamps'},'Q':{'item':'minecraft:quartz'},'C':{'item':'minecraft:copper_ingot'},'I':{'item':'minecraft:iron_ingot'}},
        'result':{'id':'guogaology:guogao_lamp_shaper','count':1}}
    recipes['turing_endcap']={'type':'minecraft:crafting_shaped','category':'misc','pattern':[' P ','PSP',' P '],
        'key':{'P':{'item':'guogaology:proof_stone'},'S':{'tag':'minecraft:stone_crafting_materials'}},
        'result':{'id':'guogaology:turing_endcap','count':4}}
    for name,value in recipes.items():
        put(D/f'recipe/{name}.json',value)
        materials=set()
        for ing in (*value.get('ingredients',[]),*value.get('key',{}).values()):
            materials.add(ing.get('item','#'+ing['tag']) if 'tag' in ing else ing['item'])
        criteria={f'ingredient_{i}':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':v}]}}
                  for i,v in enumerate(sorted(materials))}
        criteria['has_recipe']={'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'guogaology:'+name}}
        put(D/f'advancement/recipes/{name}.json',{'parent':'minecraft:recipes/root','criteria':criteria,
             'requirements':[list(criteria)],'rewards':{'recipes':['guogaology:'+name]}})
    balance=read(RES/'guogaology/block_balance.json')
    for name in (*MATERIALS,*ORES,'turing_endcap'):
        balance[name]={'hardness':3 if name in ORES else 0.8,'resistance':3 if name in ORES else 1.5,
            'tool':'pickaxe','tier':0,'light':1 if name in ('cloud_pearl','dread_resin') else 0,
            'drop':'4～6晶屑；时运增产；精准采集矿石' if name in ORES else 'self',
            'use':'序数晶体的采矿来源；徒手可采集，镐加速' if name in ORES else '本群系晶核的自然原料；徒手／任意工具完整采集，镐加速',
            'friction':0.6,'collision':'完整方块' if name in ORES or name=='turing_endcap' else '自然材料形状',
            'renewable':'采集／合成' if name=='turing_endcap' else '自然生成'}
    for name in ('tree_node_red','tree_node_green','tree_node_blue'):
        balance[name].update(hardness=1.5,tool='pickaxe',tier=0,use='TREE三色普通节点原料；任意工具完整采集，镐加速')
    balance['ordinal_crystal'].update(drop='self',use='徒手／任意工具完整采集，镐加速；4晶屑可合成或反向拆解；稳定周围两圈LHO材料')
    for core in REGIONAL:
        for grade in (1,2,3):
            name=core+('' if grade==1 else f'_lv{grade}')
            balance[name]['use']='群系材料合成；巨构仅有少量一阶陈列，任意工具完整采集，镐加速'
    put(RES/'guogaology/block_balance.json',balance)
    new_ids={'guogaology:'+n for n in (*MATERIALS,*ORES,'turing_endcap')}
    for path in (RES/'data/minecraft/tags/block').rglob('*.json'):
        name=path.relative_to(RES/'data/minecraft/tags/block').with_suffix('').as_posix()
        if not name.startswith(('mineable/','needs_','incorrect_for_')):continue
        data=read(path)
        if name=='mineable/pickaxe':data['values']=list(dict.fromkeys(data['values']+sorted(new_ids)))
        else:data['values']=[v for v in data['values'] if (v if isinstance(v,str) else v.get('id')) not in new_ids]
        put(path,data)
    catalog=read(ROOT/'tools/copy_catalog.json')
    for locale,index in (('zh_cn',0),('en_us',1)):
        values=catalog['translations'][locale]
        for name,pair in MATERIALS.items():values['block.guogaology.'+name]=pair[index]
        for key,pair in NEW_COPY.items():values[key]=pair[index]
        for root,pair in {'sequence_core':('矩阵晶核','Matrix Core'),'lho_trace':('空无晶核','Absence Core'),'astra_critical_core':('核爆之心','Heart of Nuclear Awe')}.items():
            for grade in (1,2,3):values['block.guogaology.'+root+('' if grade==1 else f'_lv{grade}')]=pair[index]+f' · Lv{grade}'
        data=read(A/f'lang/{locale}.json');data.update(values)
        put(A/f'lang/{locale}.json',data)
    put(ROOT/'tools/copy_catalog.json',catalog)
    return out


def generate(check=False):
    changed=[]
    for path,value in generated_files().items():
        if isinstance(value,Image.Image):
            buffer=io.BytesIO();value.save(buffer,format='PNG');expected=buffer.getvalue()
        else:
            if path.exists() and read(path)==value:continue
            expected=(json.dumps(value,ensure_ascii=False,indent=2)+'\n').encode('utf8')
        if path.exists() and path.read_bytes()==expected:continue
        changed.append(str(path.relative_to(ROOT)))
        if not check:path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(expected)
    print(f'CORE_RESOURCES_055_OK changed={len(changed)} check={check}')
    if check and changed:raise SystemExit('\n'.join(changed))
    return changed


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--check',action='store_true')
    generate(parser.parse_args().check)
