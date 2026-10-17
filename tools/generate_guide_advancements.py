"""Maintain the visible survival guide; recipe-book unlocks stay independent."""
import argparse
import json
from pathlib import Path
from guide_biomes import append_biome_guides, BIOME_ICONS
from generate_landmark_icons import models as landmark_models
from guide_layout import positions, reading_order

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NS='guogaology:'
REGIONAL=('sequence_core','power_tower_core','hydra_bud','lho_trace','laver_core','astra_critical_core','boundary_core','guogao_heart')
MINERALS=('omega','epsilon','gamma','true_omega')
THEMES=('matrix','power','hydra','absence','weaver','astra','frontier','guogao')
RETIRED_NODES=('guide/portal_outer','guide/portal_inner','guide/portal_underworld','guide/raw_material','guide/first_core','guide/core2','guide/core3','guide/landmark_first')
RETIRED_COPY={f'advancement.guogaology.{name.replace("/",".")}.{field}' for name in RETIRED_NODES for field in ('title','description')}

def inventory(*groups):
    return {'collected':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[NS+n for n in group]} for group in groups]}}}

def travel(to,source=None):
    return {'travelled':{'trigger':'minecraft:changed_dimension','conditions':{'to':to,**({'from':source} if source else {})}}}

def build():
    result={};copy={};rows=[]
    names={locale:json.loads((RES/f'assets/guogaology/lang/{locale}.json').read_text(encoding='utf8')) for locale in ('zh_cn','en_us')}
    def node(name,parent,icon,zh,en,description,en_description,criteria,requirements=None,frame='task'):
        key='advancement.guogaology.'+name.replace('/','.')
        display={'icon':{'id':icon if ':' in icon else NS+icon},'title':{'translate':key+'.title'},
                 'description':{'translate':key+'.description'},'frame':frame,'show_toast':name!='arrival','announce_to_chat':False,'hidden':False}
        if parent is None:display['background']='minecraft:textures/gui/advancements/backgrounds/stone.png'
        value={'display':display,'criteria':criteria,'requirements':requirements or [[k for k in criteria]]}
        if parent:value['parent']=NS+parent
        result[name]=value;copy[key+'.title']=(zh,en);copy[key+'.description']=(description,en_description)
        rows.append((name,zh,description))
    def item(name,parent,icon,zh,en,description,en_description,*groups,frame='task'):
        node('guide/'+name,parent,icon,zh,en,description,en_description,inventory(*groups),frame=frame)
    node('arrival',None,'brand_emblem','果糕逻辑','Guogaology',
         '从蛋糕门出发，探索表界、里界与果糕地府。','Begin with a cake gate and explore the Outer, Inner and Guogao Underworld.',
         {'begin':{'trigger':'minecraft:tick'}})
    node('guide/enter_outer','arrival','guide_emblem','初入表界','Into the Outer Realm',
         '用12个蛋糕摆成水平5×5去四角的门框，向内投入一个苹果激活，再穿门进入表界。','Build a horizontal 5×5 ring of 12 cakes without corners, throw an apple inside to activate it, then enter the Outer realm.',travel(NS+'outer'))
    item('first_ore','guide/enter_outer','omega_material','序矿初采','The First Ordinal Mineral',
         '用铁镐或更好的镐获得任一种序数矿物；四种矿都直接掉落材料。','Obtain an ordinal mineral with an iron or better pickaxe. All four ores drop usable materials directly.',list(n+'_material' for n in MINERALS))
    item('first_omega','guide/first_ore','true_omega_material','通往里界的Ω','Omega Opens the Way',
         '获得第一份Ω序数结晶或一块Ω矿石。前往里界的矿物门，需要投入一份Ω材料激活。','Obtain your first Ω mineral or ore. The mineral gate to Inner requires one Ω mineral as its offering.',
         ['true_omega_material','true_omega_ore','nether_true_omega_ore'])
    item('first_tool','guide/first_ore','omega_pickaxe','手中的序数','An Ordinal in Hand',
         '制作或获得一把序数镐、剑或弓。','Craft or obtain an ordinal pickaxe, sword or bow.',[n+'_'+kind for n in MINERALS for kind in ('pickaxe','sword','bow')])
    item('first_book','guide/first_tool','omega_manuscript','随身手稿','A Manuscript to Carry',
         '制作扽西手稿。副手持有时，可从背包里的手稿按钮打开镶嵌界面。','Craft a Denxi Manuscript. While it is in your offhand, use the Book button in your inventory to edit it.',[n+'_manuscript' for n in MINERALS])
    item('full_set','guide/first_tool','epsilon_chestplate','备齐护甲','Ready to Wear',
         '备齐头盔、胸甲、护腿与靴子，可以混用四种序数材料。','Obtain a helmet, chestplate, leggings and boots; the four ordinal materials may be mixed.',
         *[[n+'_'+kind for n in MINERALS] for kind in ('helmet','chestplate','leggings','boots')])
    item('return_ready','guide/enter_outer','return_token','出发，也要能回来','A Way Back',
         '备好归途签和归途框。12个同种归途框搭门，投入归途签可返回上一层。','Obtain a Return Token and frames. Activate a ring of 12 matching Return Frames with the token to return one realm.',
         ['return_token'],['outer_return_frame','inner_return_frame','guogao_return_frame'])
    node('guide/portal_return','guide/return_ready','outer_return_frame','搭好归途','A Return Gate',
         '亲手投入归途签，激活任一种归途门。','Throw a Return Token to activate a return gate.',{'done':{'trigger':'minecraft:impossible'}})
    node('guide/return_home','guide/portal_return','minecraft:compass','回到主世界','Home Again',
         '从表界返回主世界。','Return from the Outer realm to the Overworld.',travel('minecraft:overworld',NS+'outer'))
    node('guide/return_up','guide/portal_return','inner_return_frame','返回上一层','One Realm Up',
         '从里界返回表界，或从果糕地府返回里界。','Return from Inner to Outer, or from the Guogao Underworld to Inner.',
         {'inner':travel(NS+'outer',NS+'guogaology')['travelled'],'underworld':travel(NS+'guogaology',NS+'guogao')['travelled']})
    node('guide/enter_inner','guide/first_omega','ordinal_stone','走进里界','Into the Inner Realm',
         '在表界用12个矿物结晶块搭水平5×5去四角的门框，ω、ε、Γ、Ω可混搭；投入一份Ω材料激活，再穿门进入里界。','In Outer, build a horizontal 5×5 ring of 12 mineral blocks without corners, mixing ω, ε, Γ and Ω. Throw one Ω mineral inside, then enter Inner.',travel(NS+'guogaology'))
    item('number_tools','guide/enter_inner','number_pickaxe','数字也能成器','Tools from Numbers',
         '在里界采集数字刻石，制作数字石镐或数字石剑；等级取材料数字的平均值向下取整。','Gather Number Stones in Inner to craft a Number Stone pickaxe or sword. Its digit is the floor of the material digits’ average.',['number_pickaxe','number_sword'])
    item('compute_setup','guide/material_astra','server_rack','自己的算力','A Computing Setup',
         '在Astra推演原收集算力芯片，制作机柜或终端。','Collect Compute Chips in Astra and craft a server rack or terminal.',['server_rack','office_monitor'])
    item('shards','guide/enter_inner','ordinal_shard','晶矿的碎片','Crystal from the Rock',
         '开采序数晶矿，获得序数晶屑；徒手也能采集，镐更快。','Mine Ordinal Crystal Ore for shards. Hands work; pickaxes are faster.',['ordinal_shard'])
    item('crystal','guide/shards','ordinal_crystal','稳定的一块','A Stable Crystal',
         '四片晶屑合成序数晶体，也能拆回。放置后稳定周围两圈的LHO材料。','Craft an Ordinal Crystal from four shards, or unpack it again. A placed crystal stabilizes nearby LHO materials.',['ordinal_crystal'])
    item('station','guide/crystal','enhancement_table','强化台','The Enhancement Table',
         '制作强化台，可装入或取回晶核；装备附魔和晶核镶嵌互斥。','Craft an Enhancement Table to insert or recover cores. Enchantments and sockets are mutually exclusive.',['enhancement_table'])
    node('guide/socketed','guide/station','power_tower_core','镶入一枚','A Core Installed',
         '完成一次带晶核的装备或手稿编辑并关闭界面。','Close an equipment or manuscript editor with a core installed.',{'done':{'trigger':'minecraft:impossible'}})
    item('higher_station','guide/station','enhancement_table_2','更高的强化','Advanced Enhancement',
         '制作高级强化台，使用二阶晶核。','Craft an Advanced Enhancement Table for second-grade cores.',['enhancement_table_2'])
    item('ultimate_station','guide/higher_station','enhancement_table_3','终极强化','Ultimate Enhancement',
         '制作终极强化台，使用三阶晶核。','Craft an Ultimate Enhancement Table for third-grade cores.',['enhancement_table_3'],frame='goal')
    item('ordinal1','guide/crystal','ordinal_crystal_lv2','晶体中的结构','Structure within Crystal',
         '九块普通序数晶体凝为一级序数晶核。装入手稿可加强攻击、防护、已有治疗和果糕状态持续时间。','Fuse nine base crystals into an Ordinal Core Lv1. In a manuscript it improves attack, protection, existing healing and Guogao effect duration.',['ordinal_crystal_lv2'])
    item('ordinal2','guide/ordinal1','ordinal_crystal_lv3','结构中的层次','Layers within Structure',
         '九合一取得二级序数晶核，继续加强手稿中的综合加成。','Fuse nine-to-one for an Ordinal Core Lv2 and stronger combined manuscript bonuses.',['ordinal_crystal_lv3'])
    item('ordinal3','guide/ordinal2','ordinal_crystal_lv4','序数的凝聚','Condensed Ordinals',
         '序数晶体与序数晶核逐级九合一，获得三级序数晶核；各级均可拆分。','Fuse crystals and Ordinal Cores nine-to-one until reaching an Ordinal Core Lv3; every step is reversible.',['ordinal_crystal_lv4'],frame='challenge')
    item('lamp_shaper','guide/enter_outer','guogao_lamp_shaper','表情塑形','Shaping Expressions',
         '制作果糕灯塑形台，在两种表情、完整灯与3×3×3分块之间转换。','Craft a Guogao Lamp Shaper to choose expressions, whole lamps and 3×3×3 exterior tiles.',['guogao_lamp_shaper'])
    node('guide/enter_underworld','guide/enter_inner',BIOME_ICONS['guogao'],'进入果糕地府','The Guogao Underworld',
         '在里界用12个果糕方块搭水平5×5去四角的门框，可混搭颜色；再投入1个果糕方块激活，穿门进入果糕地府。','In Inner, build a horizontal 5×5 ring of 12 Guogao blocks without corners; colors may be mixed. Throw one more Guogao block inside, then enter the Guogao Underworld.',travel(NS+'guogao'),frame='goal')
    append_biome_guides(node,item,names,inventory,travel,NS)
    node('guide/landmarks_all','guide/enter_inner','landmark_emblem_power','八种巨构','All Eight Megastructures',
         '进入八种巨构各自的建筑空间；无需开启箱子。','Enter the built space of all eight megastructure types; opening chests is not required.',
         {theme:{'trigger':'minecraft:impossible'} for theme in THEMES},[[theme] for theme in THEMES],frame='challenge')
    node('guide/eat_guogao','guide/enter_outer','guogao_slice','尝一口果糕','A Taste of Guogao',
         '吃下一片果糕切片。','Eat a Guogao Slice.',{'eaten':{'trigger':'minecraft:consume_item','conditions':{'item':{'items':['guogaology:guogao_slice']}}}})
    node('guide/play_laver','guide/biome_weaver','iblp_marked','桌边的一声','A Note at the Table',
         '双手空着、不潜行，右键一张合法紫菜桌的图案格，实际开始演奏。','With both hands empty and without sneaking, right-click a valid Laver table pattern to begin playing.',{'done':{'trigger':'minecraft:impossible'}})
    node('guide/omega_mastery','guide/ultimate_station','true_omega_chestplate','Ω，全部展开','Omega, Fully Unfolded',
         '穿戴四件Ω护甲并手持Ω手稿；五件共38槽均镶入兼容的满级晶核。图腾、空槽与未生效的编辑不计。','Wear all four Ω armor pieces and hold an Ω Manuscript. Fill all 38 sockets with compatible maximum-grade cores; Totems, empty sockets and uncommitted edits do not count.',{'done':{'trigger':'minecraft:impossible'}},frame='challenge')
    copy['item.guogaology.guide_emblem']=('果糕逻辑探索指引','Guogaology Exploration Guide')
    copy['item.guogaology.brand_emblem']=('果糕逻辑标志','Guogaology Emblem')
    copy['item.guogaology.lho_guide_emblem']=('LHO空无之境标志','LHO Realm Emblem')
    for theme in THEMES:
        copy['item.guogaology.landmark_emblem_'+theme]=(names['zh_cn']['structure.guogaology.'+theme]+'探索标志',names['en_us']['structure.guogaology.'+theme]+' Exploration Emblem')
    order=reading_order(result);lookup={row[0]:row for row in rows}
    return {name:result[name] for name in order},copy,[lookup[name] for name in order]

def generate(check=False):
    advancements,copy,rows=build();changed=[]
    def put(path,value):
        text=json.dumps(value,ensure_ascii=False,indent=2)+'\n' if not isinstance(value,str) else value
        if not path.exists() or path.read_text(encoding='utf8')!=text:
            changed.append(str(path.relative_to(ROOT)))
            if not check:path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf8',newline='\n')
    for name,value in advancements.items():put(RES/f'data/guogaology/advancement/{name}.json',value)
    put(RES/'guogaology/guide_layout.json',positions(advancements))
    for name in RETIRED_NODES:
        retired=RES/f'data/guogaology/advancement/{name}.json'
        if retired.exists():
            changed.append(str(retired.relative_to(ROOT)))
            if not check:retired.unlink()
    put(RES/'assets/guogaology/models/item/guide_emblem.json',{'parent':'minecraft:item/generated','textures':{'layer0':'guogaology:item/guide_emblem'}})
    put(RES/'assets/guogaology/models/item/brand_emblem.json',{'parent':'minecraft:item/generated','textures':{'layer0':'guogaology:item/brand_emblem'}})
    # Keep the actual L/H/O glyphs and broken glass edging, with sufficient
    # contrast for a small advancement icon. World block textures stay intact.
    from PIL import Image
    from io import BytesIO
    palette={62:(157,199,230,255),82:(208,229,245,255),89:(115,169,216,255),104:(47,99,175,255)}
    for letter in 'lho':
        with Image.open(RES/f'assets/guogaology/textures/block/lho_letter_{letter}.png') as source:
            icon=Image.new('RGBA',source.size);icon.putdata([palette[p[3]] for p in source.convert('RGBA').getdata()])
        output=BytesIO();icon.save(output,format='PNG');data=output.getvalue()
        path=RES/f'assets/guogaology/textures/item/lho_guide_{letter}.png'
        if not path.exists() or path.read_bytes()!=data:
            changed.append(str(path.relative_to(ROOT)))
            if not check:path.write_bytes(data)
    put(RES/'assets/guogaology/models/item/lho_guide_emblem.json',{
        'parent':'minecraft:block/cube','gui_light':'side',
        'textures':{'particle':'guogaology:item/lho_guide_l',
                    'north':'guogaology:item/lho_guide_l','south':'guogaology:item/lho_guide_l',
                    'east':'guogaology:item/lho_guide_h','west':'guogaology:item/lho_guide_h',
                    'up':'guogaology:item/lho_guide_o','down':'guogaology:item/lho_guide_o'}})
    source_icon=RES/'assets/guogaology/icon.png';item_icon=RES/'assets/guogaology/textures/item/brand_emblem.png'
    if not item_icon.exists() or item_icon.read_bytes()!=source_icon.read_bytes():
        changed.append(str(item_icon.relative_to(ROOT)))
        if not check:item_icon.write_bytes(source_icon.read_bytes())
    for name,value in landmark_models().items():put(RES/f'assets/guogaology/models/item/{name}.json',value)
    path=ROOT/'tools/copy_catalog.json';catalog=json.loads(path.read_text(encoding='utf8'))
    catalog['removed']=sorted(set(catalog.get('removed',()))|RETIRED_COPY)
    for locale,index in [('zh_cn',0),('en_us',1)]:
        for key,values in copy.items():catalog['translations'][locale][key]=values[index]
        path_lang=RES/f'assets/guogaology/lang/{locale}.json';lang=json.loads(path_lang.read_text(encoding='utf8'))
        for key,values in copy.items():lang[key]=values[index]
        for key in RETIRED_COPY:catalog['translations'][locale].pop(key,None);lang.pop(key,None)
        put(path_lang,lang)
    put(path,catalog)
    text='# 生存引导进度 · 0.5.10\n\n使用原版进度系统，保留原版各页与配方解锁。“果糕逻辑”是自动完成的目录根，首次进入三层世界的说明包含造门方法；返程门仍记录亲手激活。首次Ω矿物是里界的引导前置，不锁定传送。\n\n八条主题分枝分别从七个里界群系和地府维度开始；每枝为代表材料→一级→二级→三级晶核，以及独立的进入巨构路线。晶核名称侧重能力，获得后仍须镶嵌才产生效果。巨构按进入实际建筑空间判定，无需开箱。另有吃果糕、弹奏紫菜桌与38槽Ω满配进度。父子关系用于引导，不强制完成顺序，不发额外物品。\n\n| ID | 名称 | 引导与完成条件 |\n|---|---|---|\n'
    for name,title,description in rows:text+=f'| `{name}` | {title} | {description} |\n'
    put(ROOT/'docs/ADVANCEMENTS-057.md',text)
    text='# Survival guide advancements · 0.5.10\n\nThe directory root completes automatically. First entering each realm explains its gate; return gates credit their activator. First Ω ore/mineral precedes Inner in the guide without locking travel. Vanilla progress and recipe unlocks remain.\n\nEight branches start with the seven Inner biomes and the Underworld dimension. Each has representative materials → core Lv1 → Lv2 → Lv3, plus a separate megastructure visit. Core titles reflect their capabilities; possession alone does not activate them. Entering the actual built space counts without opening a chest. Eating Guogao, playing a Laver table and a fully socketed 38-slot Ω loadout are additional milestones. Parents guide rather than enforce order; no extra items are awarded.\n\n| ID | Title | Guidance and condition |\n|---|---|---|\n'
    for name in advancements:
        key='advancement.guogaology.'+name.replace('/','.')
        text+=f'| `{name}` | {copy[key+".title"][1]} | {copy[key+".description"][1]} |\n'
    put(ROOT/'docs/ADVANCEMENTS-057.en.md',text)
    if check and changed:raise SystemExit('Stale guide: '+', '.join(changed))
    print(f'GUIDE_ADVANCEMENTS_057_OK nodes={len(advancements)} changed={len(changed)} check={check}')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');generate(parser.parse_args().check)
