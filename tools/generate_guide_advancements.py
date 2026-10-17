"""Maintain the visible survival guide; recipe-book unlocks stay independent."""
import argparse
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NS='guogaology:'
REGIONAL=('sequence_core','power_tower_core','hydra_bud','lho_trace','laver_core','astra_critical_core','boundary_core','guogao_heart')
MINERALS=('omega','epsilon','gamma','true_omega')
THEMES=('matrix','power','hydra','absence','weaver','astra','frontier','guogao')
RAW=('matrix_sheet','arrow_knot','hydra_sprig','laver_gel','cloud_pearl','scg_node','dread_resin','set_membrane',
     'lho_letter_l','lho_letter_h','lho_letter_o','fffz_trace','fos_trace','lho_hydra_psi','lho_hydra_z')
RETIRED_NODES=('guide/portal_outer','guide/portal_inner','guide/portal_underworld')
RETIRED_COPY={f'advancement.guogaology.{name.replace("/",".")}.{field}' for name in RETIRED_NODES for field in ('title','description')}

def inventory(*groups):
    return {'collected':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[NS+n for n in group]} for group in groups]}}}

def travel(to,source=None):
    return {'travelled':{'trigger':'minecraft:changed_dimension','conditions':{'to':to,**({'from':source} if source else {})}}}

def loot(themes):
    return {theme+'_'+kind:{'trigger':'minecraft:player_generates_container_loot','conditions':{'loot_table':NS+'chests/'+theme+'_'+kind}}
            for theme in themes for kind in ('sanctum','ruin')}

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
    node('arrival',None,'guide_emblem','果糕逻辑','Guogaology',
         '从蛋糕门出发，探索表界、里界与果糕地府。','Begin with a cake gate and explore the Outer, Inner and Guogao Underworld.',
         {'begin':{'trigger':'minecraft:tick'}})
    node('guide/enter_outer','arrival','minecraft:apple','初入表界','Into the Outer Realm',
         '用12个蛋糕摆成水平5×5去四角的门框，向内投入一个苹果激活，再穿门进入表界。','Build a horizontal 5×5 ring of 12 cakes without corners, throw an apple inside to activate it, then enter the Outer realm.',travel(NS+'outer'))
    item('first_ore','guide/enter_outer','omega_material','序矿初采','The First Ordinal Mineral',
         '用铁镐或更好的镐获得任一种序数矿物；四种矿都直接掉落材料。','Obtain an ordinal mineral with an iron or better pickaxe. All four ores drop usable materials directly.',list(n+'_material' for n in MINERALS))
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
    node('guide/enter_inner','guide/enter_outer','ordinal_stone','走进里界','Into the Inner Realm',
         '在表界用12个矿物结晶块搭水平5×5去四角的门框，ω、ε、Γ、Ω可混搭；投入一份Ω材料激活，再穿门进入里界。','In Outer, build a horizontal 5×5 ring of 12 mineral blocks without corners, mixing ω, ε, Γ and Ω. Throw one Ω mineral inside, then enter Inner.',travel(NS+'guogaology'))
    item('number_tools','guide/enter_inner','number_pickaxe','数字也能成器','Tools from Numbers',
         '在里界采集数字刻石，制作数字石镐或数字石剑；等级取材料数字的平均值向下取整。','Gather Number Stones in Inner to craft a Number Stone pickaxe or sword. Its digit is the floor of the material digits’ average.',['number_pickaxe','number_sword'])
    item('compute_setup','guide/enter_inner','server_rack','自己的算力','A Computing Setup',
         '在里界收集算力芯片，制作机柜或终端。','Collect Compute Chips in Inner and craft a server rack or terminal.',['server_rack','office_monitor'])
    item('raw_material','guide/enter_inner','matrix_sheet','寻找群系原料','Biome Ingredients',
         '从自然景物中收集一种本地晶核原料。成品晶核不在野外直接生成。','Collect a core ingredient from a natural landmark. Finished cores do not spawn in the wilderness.',list(RAW))
    item('shards','guide/enter_inner','ordinal_shard','晶矿的碎片','Crystal from the Rock',
         '开采序数晶矿，获得序数晶屑；徒手也能采集，镐更快。','Mine Ordinal Crystal Ore for shards. Hands work; pickaxes are faster.',['ordinal_shard'])
    item('crystal','guide/shards','ordinal_crystal','稳定的一块','A Stable Crystal',
         '四片晶屑合成序数晶体，也能拆回。放置后稳定周围两圈的LHO材料。','Craft an Ordinal Crystal from four shards, or unpack it again. A placed crystal stabilizes nearby LHO materials.',['ordinal_crystal'])
    item('first_core','guide/raw_material','sequence_core','第一颗晶核','The First Core',
         '获得一颗晶核。区域晶核由多种群系原料合成；序数晶核由序数晶体逐级融合。','Obtain a core. Regional cores combine biome ingredients; Ordinal Cores fuse from Ordinal Crystals.',list(REGIONAL)+['ordinal_crystal_lv2'])
    item('station','guide/first_core','enhancement_table','强化台','The Enhancement Table',
         '制作强化台，可装入或取回晶核；装备附魔和晶核镶嵌互斥。','Craft an Enhancement Table to insert or recover cores. Enchantments and sockets are mutually exclusive.',['enhancement_table'])
    node('guide/socketed','guide/station','power_tower_core','镶入一枚','A Core Installed',
         '完成一次带晶核的装备或手稿编辑并关闭界面。','Close an equipment or manuscript editor with a core installed.',{'done':{'trigger':'minecraft:impossible'}})
    item('higher_station','guide/station','enhancement_table_2','更高的强化','Advanced Enhancement',
         '制作高级强化台，使用二阶晶核。','Craft an Advanced Enhancement Table for second-grade cores.',['enhancement_table_2'])
    item('ultimate_station','guide/higher_station','enhancement_table_3','终极强化','Ultimate Enhancement',
         '制作终极强化台，使用三阶晶核。','Craft an Ultimate Enhancement Table for third-grade cores.',['enhancement_table_3'],frame='goal')
    item('core2','guide/first_core','sequence_core_lv2','二阶融合','Second-Grade Fusion',
         '用8颗同系一级晶核和1颗一级序数晶核，合成一颗二级晶核。','Combine eight first-grade regional cores with one first-grade Ordinal Core into a second-grade core.',[n+'_lv2' for n in REGIONAL])
    item('core3','guide/core2','sequence_core_lv3','三阶融合','Third-Grade Fusion',
         '用8颗同系二级晶核和1颗二级序数晶核，合成一颗三级晶核。','Combine eight second-grade regional cores with one second-grade Ordinal Core into a third-grade core.',[n+'_lv3' for n in REGIONAL],frame='goal')
    item('ordinal3','guide/crystal','ordinal_crystal_lv4','序数的凝聚','Condensed Ordinals',
         '序数晶体与序数晶核逐级九合一，获得三级序数晶核；各级均可拆分。','Fuse crystals and Ordinal Cores nine-to-one until reaching an Ordinal Core Lv3; every step is reversible.',['ordinal_crystal_lv4'],frame='challenge')
    item('lamp_shaper','guide/enter_outer','guogao_lamp_shaper','表情塑形','Shaping Expressions',
         '制作果糕灯塑形台，在两种表情、完整灯与3×3×3分块之间转换。','Craft a Guogao Lamp Shaper to choose expressions, whole lamps and 3×3×3 exterior tiles.',['guogao_lamp_shaper'])
    node('guide/enter_underworld','guide/enter_inner','guogao_heart','进入果糕地府','The Guogao Underworld',
         '在里界用12个果糕方块搭水平5×5去四角的门框，可混搭颜色；再投入1个果糕方块激活，穿门进入果糕地府。','In Inner, build a horizontal 5×5 ring of 12 Guogao blocks without corners; colors may be mixed. Throw one more Guogao block inside, then enter the Guogao Underworld.',travel(NS+'guogao'),frame='goal')
    node('guide/landmark_first','guide/enter_inner','minecraft:chest','巨构初探','Into a Megastructure',
         '在任意巨构中开启第一只奖励箱，继续探索楼层和主题陈列。','Open a reward chest in any megastructure, then explore its floors and themed exhibits.',loot(THEMES))
    titles=[(names['zh_cn']['structure.guogaology.'+theme],names['en_us']['structure.guogaology.'+theme]) for theme in THEMES]
    icons=['sequence_core','power_tower_core','hydra_bud','lho_trace','laver_core','astra_critical_core','boundary_core','guogao_heart']
    for theme,(zh,en),icon in zip(THEMES,titles,icons):
        node('guide/landmark_'+theme,'guide/landmark_first',icon,'探索'+zh,'Explore '+en,
             '在'+zh+'中开启一只主箱或副箱。','Open a main or secondary reward chest in the '+en+'.',loot([theme]))
    node('guide/landmarks_all','guide/landmark_first','ordinal_crystal_lv4','八种巨构','All Eight Megastructures',
         '探索八种巨构，并在每一种中开启至少一只奖励箱。','Explore all eight megastructure types and open at least one reward chest in each.',loot(THEMES),
         [[theme+'_sanctum',theme+'_ruin'] for theme in THEMES],frame='challenge')
    copy['item.guogaology.guide_emblem']=('果糕逻辑探索指引','Guogaology Exploration Guide')
    return result,copy,rows

def generate(check=False):
    advancements,copy,rows=build();changed=[]
    def put(path,value):
        text=json.dumps(value,ensure_ascii=False,indent=2)+'\n' if not isinstance(value,str) else value
        if not path.exists() or path.read_text(encoding='utf8')!=text:
            changed.append(str(path.relative_to(ROOT)))
            if not check:path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf8',newline='\n')
    for name,value in advancements.items():put(RES/f'data/guogaology/advancement/{name}.json',value)
    for name in RETIRED_NODES:
        retired=RES/f'data/guogaology/advancement/{name}.json'
        if retired.exists():
            changed.append(str(retired.relative_to(ROOT)))
            if not check:retired.unlink()
    put(RES/'assets/guogaology/models/item/guide_emblem.json',{'parent':'minecraft:item/generated','textures':{'layer0':'guogaology:item/guide_emblem'}})
    path=ROOT/'tools/copy_catalog.json';catalog=json.loads(path.read_text(encoding='utf8'))
    catalog['removed']=sorted(set(catalog.get('removed',()))|RETIRED_COPY)
    for locale,index in [('zh_cn',0),('en_us',1)]:
        for key,values in copy.items():catalog['translations'][locale][key]=values[index]
        path_lang=RES/f'assets/guogaology/lang/{locale}.json';lang=json.loads(path_lang.read_text(encoding='utf8'))
        for key,values in copy.items():lang[key]=values[index]
        for key in RETIRED_COPY:catalog['translations'][locale].pop(key,None);lang.pop(key,None)
        put(path_lang,lang)
    put(path,catalog)
    text='# 生存引导进度 · 0.5.9\n\n使用原版进度系统，原版各页与配方解锁不替换。“果糕逻辑”是登录自动完成的目录根节点，保证新存档显示引导页；表界、里界和地府分别以首次进入为实际进度，其说明包含对应造门方法，前进门激活本身不另设成就。返程门仍按成功投掷归途签激活归属到投掷者；其他节点按获得材料、跨维度或开启巨构奖励箱触发。不要求严格顺序，也不发额外物品。\n\n| ID | 名称 | 引导与完成条件 |\n|---|---|---|\n'
    for name,title,description in rows:text+=f'| `{name}` | {title} | {description} |\n'
    put(ROOT/'docs/ADVANCEMENTS-057.md',text)
    text='# Survival guide advancements · 0.5.9\n\nThe Guogaology directory root auto-completes on joining so new players can see the guide. First entering Outer, Inner and the Underworld grants their respective milestones; each description explains its gate. Forward-gate activation itself has no separate milestone. Return gates still credit the player who successfully offered the Return Token. These milestones keep vanilla progress and recipe unlocks, impose no required order and award no extra items. Megastructures count when their actual main or secondary reward chest generates loot.\n\n| ID | Title | Guidance and condition |\n|---|---|---|\n'
    for name in advancements:
        key='advancement.guogaology.'+name.replace('/','.')
        text+=f'| `{name}` | {copy[key+".title"][1]} | {copy[key+".description"][1]} |\n'
    put(ROOT/'docs/ADVANCEMENTS-057.en.md',text)
    if check and changed:raise SystemExit('Stale guide: '+', '.join(changed))
    print(f'GUIDE_ADVANCEMENTS_057_OK nodes={len(advancements)} changed={len(changed)} check={check}')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');generate(parser.parse_args().check)
