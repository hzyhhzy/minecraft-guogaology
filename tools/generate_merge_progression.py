"""Four-tier common progression. Donor textures are imported separately.

No recipes for surface materials may require an inner-world crystal. Crystal
fusion itself remains the established eight-plus-one system.
"""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources';A=RES/'assets/googology';D=RES/'data/googology'
METALS=('omega','epsilon','gamma','true_omega')
SYMBOLS=('ω','ε','Γ','Ω')
PARTS=('pickaxe','sword','helmet','chestplate','leggings','boots')
OLD=('psi','strata','proof')
def read(p):return json.loads(p.read_text(encoding='utf8'))
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def model(name,parent,tex):write(A/f'models/item/{name}.json',{'parent':parent,'textures':{'layer0':tex}})
def shaped(name,pattern,keys,result=None,count=1):
    write(D/f'recipe/{name}.json',{'type':'minecraft:crafting_shaped','category':'equipment' if any(x in name for x in (*PARTS,'manuscript')) else 'misc','pattern':pattern,'key':{k:({'tag':v[1:]} if v.startswith('#') else {'item':v}) for k,v in keys.items()},'result':{'id':result or 'googology:'+name,'count':count}})
def simple(name,items,result,count=1):write(D/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':x} for x in items],'result':{'id':result,'count':count}})
def old_id(id):return bool(re.search(r'googology:(?:nether_)?(?:psi|strata|proof)_(?:ore|block|material|fragment|cluster|pickaxe|sword|helmet|chestplate|leggings|boots)\b',id))

def generate():
    # Exact retired progression files only; ψ landscape blocks remain live.
    for folder in [A/'blockstates',A/'models',A/'items',A/'equipment',A/'textures',D/'recipe',D/'loot_table',D/'advancement',D/'tags']:
        for p in folder.rglob('*'):
            if not p.is_file():continue
            if re.fullmatch(r'(?:nether_)?(?:psi|strata|proof)_(?:ore|block|material|fragment|cluster|pickaxe|sword|helmet|chestplate|leggings|boots|repair)(?:_.*)?',p.stem) or p.stem in OLD:
                assert p.resolve().is_relative_to(RES.resolve());p.unlink();continue
            if p.suffix=='.json':
                v=read(p)
                if '/tags/' in p.as_posix():
                    v['values']=[x for x in v.get('values',[]) if not old_id(x if isinstance(x,str) else x.get('id',''))];write(p,v)
                elif folder in (D/'recipe',D/'advancement') and old_id(json.dumps(v)):p.unlink()
    for metal in METALS:
        for suffix in ('material',*PARTS):model(metal+'_'+suffix,'minecraft:item/handheld' if suffix in ('pickaxe','sword') else 'minecraft:item/generated','googology:item/'+metal+'_'+suffix)
        for block in (metal+'_block',metal+'_ore','nether_'+metal+'_ore'):
            write(A/f'blockstates/{block}.json',{'variants':{'':{'model':'googology:block/'+block}}})
            write(A/f'models/block/{block}.json',{'parent':'minecraft:block/cube_all','textures':{'all':'googology:block/'+block}})
            write(A/f'models/item/{block}.json',{'parent':'googology:block/'+block})
            loot=read(D/'loot_table/blocks/omega_ore.json') if block.endswith('_ore') else {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'googology:omega_block'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]}
            text=json.dumps(loot).replace('googology:omega_ore','googology:'+block).replace('googology:omega_material','googology:'+metal+'_material').replace('googology:omega_block','googology:'+block)
            write(D/f'loot_table/blocks/{block}.json',json.loads(text))
        write(A/f'equipment/{metal}.json',{'layers':{k:[{'texture':'googology:'+metal}] for k in ('humanoid','humanoid_leggings')}})
        write(D/f'tags/item/{metal}_repair.json',{'replace':False,'values':['googology:'+metal+'_material']})
        for part,pattern in zip(PARTS,[['MMM',' S ',' S '],[' M ',' M ',' S '],['MMM','M M'],['M M','MMM','MMM'],['MMM','M M','M M'],['M M','M M']]):
            shaped(metal+'_'+part,pattern,{'M':'googology:'+metal+'_material',**({'S':'minecraft:stick'} if part in ('pickaxe','sword') else {})})
        shaped(metal+'_block',['MMM']*3,{'M':'googology:'+metal+'_material'})
        simple(metal+'_unpack',['googology:'+metal+'_block'],'googology:'+metal+'_material',9)
        # A book silhouette stays readable at native item resolution.
        model(metal+'_manuscript','minecraft:item/generated','googology:item/'+metal+'_manuscript')
        shaped(metal+'_manuscript',[' M ','MBM',' M '],{'M':'googology:'+metal+'_material','B':'minecraft:book'})
    shaped('enhancement_table',[' M ','BWB','SSS'],{'M':'googology:omega_material','B':'minecraft:book','W':'minecraft:crafting_table','S':'#minecraft:stone_crafting_materials'})
    for tier,metal in ((2,'gamma'),(3,'true_omega')):
        shaped('enhancement_table_'+str(tier),['MCM','STS','MCM'],{'M':'googology:'+metal+'_material','C':'googology:ordinal_crystal'+('_lv2' if tier==3 else ''),'T':'googology:enhancement_table'+('_2' if tier==3 else ''),'S':'googology:epsilon_block'})
    for name in ('mineable/pickaxe','needs_iron_tool'):
        p=RES/f'data/minecraft/tags/block/{name}.json';v=read(p)
        v['values']=list(dict.fromkeys(v['values']+[f'googology:{prefix}{m}_ore' for prefix in ('','nether_') for m in METALS]))
        write(p,v)
    for p in (RES/'data/minecraft/tags').rglob('*.json'):
        v=read(p);v['values']=[x for x in v.get('values',[]) if not old_id(x if isinstance(x,str) else x.get('id',''))]
        if p.stem=='needs_diamond_tool':v['values']=[x for x in v['values'] if x not in [f'googology:{n}{m}_ore' for n in ('','nether_') for m in METALS]]
        write(p,v)
    write(D/'tags/block/number_stones.json',read(D/'tags/item/number_stones.json'))
    # Recipe-book advancement for every recipe, including the fourth equipment tier.
    for p in (D/'recipe').glob('*.json'):
        recipe=read(p);ids=set(re.findall(r'"item":\s*"([\w:]+)"',json.dumps(recipe)))
        if not ids:continue
        write(D/f'advancement/recipes/{p.stem}.json',{'parent':'minecraft:recipes/root','criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':sorted(ids)}]}},'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'googology:'+p.stem}}},'requirements':[['has_material','has_recipe']],'rewards':{'recipes':['googology:'+p.stem]}})
    catalog=read(ROOT/'tools/copy_catalog.json')
    for locale in ('zh_cn','en_us'):
        zh=locale=='zh_cn';values=catalog['translations'][locale]
        for key in list(values):
            if old_id(key.replace('item.googology.','googology:').replace('block.googology.','googology:')):values.pop(key);catalog['removed'].append(key)
        for m,symbol in zip(METALS,SYMBOLS):
            for part in PARTS:
                name=dict(zip(PARTS,('镐','剑','头盔','胸甲','护腿','靴子')))[part]
                values['item.googology.'+m+'_'+part]=f'{symbol}{name}' if zh else f'{symbol} {part.title()}'
            values['item.googology.'+m+'_material']=f'{symbol} 序数结晶' if zh else f'{symbol} Ordinal Mineral'
            values['item.googology.'+m+'_manuscript']=f'{symbol} 扽西手稿' if zh else f'{symbol} Denxi Manuscript'
            for prefix in ('','nether_'):values['block.googology.'+prefix+m+'_ore']=('冥岩' if prefix else '')+f'{symbol}矿石' if zh else ('Understone ' if prefix else '')+f'{symbol} Ore'
            values['block.googology.'+m+'_block']=f'{symbol}结晶块' if zh else f'{symbol} Mineral Block'
        extra={
            'mining_speed':('挖掘速度：%s','Mining speed: %s'),
            'manuscript.passive':('副手持有：晶核效果被动生效','Held offhand: core effects activate passively'),
            'manuscript.totem':('果糕Ⅱ以上：致命伤害消耗背包中的一枚不死图腾','Guogao II+: lethal damage consumes one inventory Totem of Undying'),
            'profile.outer':('表界／原版维度：温和强化','Outer / vanilla dimensions: additive enhancement'),
            'profile.inner':('里界／地府：深层强化','Inner / underworld: deep enhancement')}
        for k,v in extra.items():values['mining.googology.'+k]=v[0 if zh else 1]
        for i,(cn,en) in enumerate([('挖掘加速','Mining speed'),('武器攻击','Weapon attack'),('被动恢复','Passive recovery'),('缓降／飞行','Slow falling / flight'),('经验加成','Experience gain'),('移动速度','Movement speed'),('被动防护','Passive protection'),('生命／不死图腾','Health / totems'),('装备自修复','Equipment repair')]):values['mining.googology.manuscript.effect.'+str(i)]=cn if zh else en
    catalog['removed']=sorted(set(catalog['removed']));write(ROOT/'tools/copy_catalog.json',catalog)
    from copy_catalog import normalize
    for locale in ('zh_cn','en_us'):
        p=A/f'lang/{locale}.json';v=read(p)
        for key in catalog['removed']:v.pop(key,None)
        v.update(catalog['translations'][locale]);write(p,v)
    print('Four-tier recipes, drops, models and translations updated')
if __name__=='__main__':generate()
