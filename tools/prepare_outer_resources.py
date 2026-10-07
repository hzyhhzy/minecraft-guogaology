"""Maintain the donor's world content independently of host progression.

26.3 consumes the original world-generation contracts. Earlier engines receive
format/API translations, using their built-in Overworld noise router (the donor
also delegates to Overworld noise). Climate thresholds, template coordinates and cave settings are retained.
0.3.6 wraps the climate source to restrict the LHO edge to a 48 m coast.
"""
import json,re
from pathlib import Path
from import_outer_content import ROOT,NS,SOURCE_NS,ALIASES,DATA_NAMES,walk,write,remap_id,nbt_transform
from generate_ore_distribution import apply_policy as ore_policy
SOURCE=ROOT/'content/outer-1.0.0'
GENERATED=json.loads((ROOT/'tools/outer_generated_resources.json').read_text('utf8'))

def clear_generated(out):
    """Remove precisely the imported generated files, never the host data tree."""
    out=Path(out).resolve()
    assert out.is_relative_to(ROOT.resolve())
    for name in GENERATED:
        path=(out/name).resolve()
        assert name.startswith('data/guogaology/') and path.is_relative_to(out/'data/guogaology')
        assert name not in ('data/guogaology/dimension/guogaology.json','data/guogaology/dimension/guogao.json',
                            'data/guogaology/dimension_type/guogaology.json','data/guogaology/dimension_type/guogao.json',
                            'data/guogaology/worldgen/noise_settings/guogaology.json','data/guogaology/worldgen/noise_settings/guogao.json')
        path.unlink(missing_ok=True)

def load(path):return json.loads(path.read_text(encoding='utf8'))
def state(v):
    if isinstance(v,dict):return {'Name':v['id'],**({'Properties':v['properties']} if v.get('properties') else {})} if 'id' in v else v
    m=re.fullmatch(r'([^\[]+)(?:\[(.*)\])?',v)
    return {'Name':m[1],**({'Properties':dict(p.split('=') for p in m[2].split(','))} if m[2] else {})}

def provider(v):
    if v=='minecraft:soil_beneath_tree':v='minecraft:dirt'
    if isinstance(v,str) or 'id' in v:return {'type':'minecraft:simple_state_provider','state':state(v)}
    return v

def old_feature(v,target):
    if not isinstance(v,dict):return v
    kind=v.get('type');cfg={k:w for k,w in v.items() if k!='type'}
    if kind=='minecraft:template':kind=NS+':template'
    for k in ['trunk_provider','foliage_provider','below_trunk_provider','to_place','ground_state','fluid','barrier']:
        if k in cfg:cfg[k]=provider(cfg[k])
    if 'minimum_size' in cfg:
        cfg['minimum_size'].setdefault('limit',1);cfg['minimum_size'].setdefault('lower_size',0);cfg['minimum_size'].setdefault('upper_size',1)
    if 'state' in cfg:cfg['state']=state(cfg['state'])
    if kind=='minecraft:ore':
        for t in cfg['targets']:t['state']=state(t['state'])
    if kind=='minecraft:disk':
        sp=provider(cfg['state_provider']);cfg['state_provider']=sp if target=='26.2' else {'fallback':sp,'rules':[]}
    if kind=='minecraft:spring_feature':
        cfg.setdefault('requires_block_below',True);cfg.setdefault('rock_count',4);cfg.setdefault('hole_count',1)
    if kind=='minecraft:vegetation_patch':
        if target!='26.2':cfg['replaceable']=cfg['replaceable'].lstrip('#')
        cfg['vegetation_feature']=old_placed(cfg['vegetation_feature'],target)
    return {'type':kind,'config':cfg}

def old_predicate(v):
    if isinstance(v,list):return [old_predicate(x) for x in v]
    if not isinstance(v,dict):return v
    v={k:old_predicate(x) for k,x in v.items()}
    if v.get('type')=='minecraft:would_survive':v['state']=state(v['state'])
    return v

def old_placed(v,target):
    if isinstance(v,dict) and isinstance(v.get('feature'),dict):v={**v,'feature':old_feature(v['feature'],target)}
    if isinstance(v,dict):
        v=old_predicate(v)
        for i,p in enumerate(v.get('placement',[])):
            if p['type']=='minecraft:offset':
                assert p.get('x',0)==p.get('z',0),p
                v['placement'][i]={'type':'minecraft:random_offset','xz_spread':p.get('x',0),'y_spread':p['y']}
    return v

def old_loot(v):
    if isinstance(v,list):return [old_loot(x) for x in v]
    if not isinstance(v,dict):return v
    result={k:old_loot(w) for k,w in v.items() if k not in ('condition','modifier')}
    if 'condition' in v:
        cond=v['condition'];conditions=cond if isinstance(cond,list) else [cond]
        result['conditions']=[condition(c) for c in conditions]
    if 'modifier' in v:
        fs=v['modifier'];fs=fs if isinstance(fs,list) else [fs]
        result['functions']=[{('function' if k=='type' else k):old_loot(w) for k,w in f.items()} for f in fs]
    return result

def condition(v):
    if isinstance(v,str):
        if v=='minecraft:tool/can_shear':return {'condition':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}
        if v=='minecraft:tool/can_silk_touch':return {'condition':'minecraft:match_tool','predicate':{'predicates':{'minecraft:enchantments':[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}}
        return {'condition':'minecraft:reference','name':v}
    result={('condition' if k=='type' else k):w for k,w in v.items()}
    for k in ('term','terms'):
        if k in result:result[k]=[condition(x) for x in result[k]] if isinstance(result[k],list) else condition(result[k])
    return result

def old_rule(v):
    if isinstance(v,str):
        original={new:old for old,new in DATA_NAMES.items()}.get(v.split(':')[-1],v.split(':')[-1])
        if v.startswith(NS+':') and (p:=SOURCE/'worldgen/material_rule'/f'{original}.json').exists():return old_rule(walk(load(p)))
        if v=='minecraft:bedrock_floor':return {'type':'minecraft:condition','if_true':{'type':'minecraft:vertical_gradient','random_name':'minecraft:bedrock_floor','true_at_and_below':{'above_bottom':0},'false_at_and_above':{'above_bottom':5}},'then_run':{'type':'minecraft:block','result_state':{'Name':'minecraft:bedrock'}}}
        if v in ('minecraft:on_floor','minecraft:under_floor'):return {'type':'minecraft:stone_depth','offset':0,'add_surface_depth':v.endswith('under_floor'),'secondary_depth_range':0,'surface_type':'floor'}
        return v
    if isinstance(v,list):return [old_rule(x) for x in v]
    if not isinstance(v,dict):return v
    result={k:old_rule(w) for k,w in v.items()}
    if result.get('type')=='minecraft:biome' and isinstance(result['biome_is'],str):result['biome_is']=[result['biome_is']]
    if 'result_state' in result:result['result_state']=state(result['result_state'])
    return result

def old_biome(v,target):
    attrs=v.pop('attributes',{});effects=v['effects']
    for k in ('sky_color','fog_color','water_fog_color'):
        effects[k]=int(attrs.get('minecraft:visual/'+k,{'sky_color':'#7ba4db','fog_color':'#c0d8f0','water_fog_color':'#050533'}[k]).lstrip('#'),16)
    for k in ('water_color','grass_color','foliage_color'):
        if isinstance(effects.get(k),str):effects[k]=int(effects[k].lstrip('#'),16)
    sp=attrs.get('minecraft:gameplay/natural_mob_spawns',{}).get('argument',{})
    v['spawners']=sp.get('spawns_by_category',{});v['spawn_costs']=sp.get('spawn_costs',{})
    for cat,entries in v['spawners'].items():
        entries[:]=[e for e in entries if 'hydra' not in e['type']]
        for e in entries:
            n=e.pop('count',1);e.update(minCount=n if isinstance(n,int) else n['min_inclusive'],maxCount=n if isinstance(n,int) else n['max_inclusive'])
    if target=='1.21.1':v['carvers']={'air':v.get('carvers',[])}
    return v

def old_ingredient(v):
    if isinstance(v,list):return [old_ingredient(x) for x in v]
    if isinstance(v,str):return {'tag':v[1:]} if v.startswith('#') else {'item':v}
    return v

def recipe_unlocks(data,recipe_paths):
    """Unlock surviving recipes from any ingredient, without stale donor rewards.

    The original recipe advancements refer to removed Den/axe/hoe items and
    require eight unrelated ingredients together. Rebuild only this UI metadata;
    recipe patterns, costs and results remain unchanged.
    """
    def ingredients(v):
        if isinstance(v, str):return {v}
        if isinstance(v, list):return set().union(*(ingredients(x) for x in v))
        if isinstance(v, dict):
            if 'item' in v:return {v['item']}
            if 'tag' in v:return {'#'+v['tag']}
        return set()
    for p in recipe_paths:
        recipe=load(p);values=set()
        for ingredient in recipe.get('key',{}).values():values |= ingredients(ingredient)
        for key in ('ingredient','ingredients'):
            if key in recipe:values |= ingredients(recipe[key])
        identifier=NS+':'+p.stem
        criteria={f'ingredient_{i}':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':value}]}}
                  for i,value in enumerate(sorted(values))}
        criteria['has_recipe']={'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':identifier}}
        write(data/f'advancement/recipes/{p.stem}.json',{
            'parent':'minecraft:recipes/root','criteria':criteria,
            'requirements':[list(criteria)],'rewards':{'recipes':[identifier]}})

def prepare(target,out):
    out=Path(out).resolve();assert out.is_relative_to(ROOT.resolve())
    if target=='1.21.1':
        # Before 1.21.4 an inventory item needs models/item/<id>.json, even
        # when the donor's modern item definition directly uses a block model.
        assets=out/'assets'/NS
        for p in (assets/'items').glob('*.json'):
            item=load(p)['model'];assert item['type']=='minecraft:model',(p,item)
            if item['model']!=NS+':item/'+p.stem:write(assets/'models/item'/p.name,{'parent':item['model']})
    # 1.21.x expects texture IDs; 26.x also accepts per-sprite translucency hints.
    glass=out/'assets'/NS/'models/block/lho_glass.json'
    if glass.exists():
        model=load(glass)
        for k,t in model.get('textures',{}).items():
            sprite=t.get('sprite') if isinstance(t,dict) else t
            model['textures'][k]={'sprite':sprite,'force_translucent':True} if target.startswith('26.') else sprite
        write(glass,model)
    data=out/'data'/NS;data.mkdir(parents=True,exist_ok=True)
    clear_generated(out)
    recipe_paths=[];biome_paths=[]
    for p in SOURCE.rglob('*'):
        if not p.is_file() or 'compat' in p.parts or 'minecraft' in p.relative_to(SOURCE).parts or p.name=='manifest.json':continue
        rel=p.relative_to(SOURCE)
        if p.suffix=='.nbt':write(data/rel,nbt_transform(p.read_bytes(),legacy=target!='26.3'));continue
        if p.suffix!='.json':continue
        v=walk(load(p));folder=rel.parts[0]
        if rel.parts==('worldgen','feature','trees_christmas.json'):
            v['layered_christmas_crown']=True
        if folder in ('dimension','dimension_type'):continue
        if folder=='advancement':continue
        if folder=='recipe':
            # Retired Christmas carpentry resolves to vanilla blocks for scenery,
            # never to a second recipe for vanilla planks. The old stone aliases
            # otherwise turn into free cobblestone/stone conversion recipes.
            if rel.stem.startswith(('christmas_','hell_christmas_','loquat_','hell_loquat_')) or rel.stem in {
                'cobblestone_from_ordinal_stone','cobblestone_from_ordinal_stone_smelting',
                'ordinal_stone_from_cobbled_ordinal_stone','ordinal_stone_from_cobblestone',
                'epsilon_block_unpack','gamma_block_unpack','omega_block_unpack','laver_log_to_planks'}:continue
            text=json.dumps(v)
            if re.search(r'(?:_den\b|_axe\b|_hoe\b|_shovel\b|ordinal_heart|raw_omega|portal_generator|guide_book|hydra)',text):continue
            # Our common equipment recipes replace duplicate donor gear and ore recipes.
            result=v.get('result',{});id=result.get('id','') if isinstance(result,dict) else result
            if id.startswith('guogaology:') and any(x in id for x in ('_material','_block','_pickaxe','_sword','_helmet','_chestplate','_leggings','_boots')):continue
            if target=='1.21.1':
                for k in ('ingredient','ingredients'):
                    if k in v:v[k]=old_ingredient(v[k])
                if 'key' in v:v['key']={k:old_ingredient(w) for k,w in v['key'].items()}
        if folder=='loot_table':
            if 'nine_headed_hydra' in str(rel) or 'raw_omega' in str(rel):continue
            if len(rel.parts)>1 and rel.parts[1]=='blocks' and rel.stem in ALIASES:continue
            if target!='26.3':v=old_loot(v)
        if rel.parts[:2]==('worldgen','feature') and target!='26.3':rel=Path('worldgen/configured_feature')/rel.name;v=old_feature(v,target)
        if rel.parts[:2]==('worldgen','carver') and target!='26.3':
            rel=Path('worldgen/configured_carver')/rel.name
            cfg={k:w for k,w in v.items() if k not in ('type','count','thickness','weird_thickness_bias')}
            cfg['lava_level']={'above_bottom':8};cfg['replaceable']='#minecraft:overworld_carver_replaceables'
            cfg['yScale']=cfg.pop('room_vertical_radius_multiplier',cfg.get('shape',{}).pop('y_scale',3.0))
            if v['type']=='minecraft:cave':
                v={'type':NS+':source_cave','config':{'base':cfg,'count':v['count'],'thickness':v['thickness'],'weird_thickness_bias':v['weird_thickness_bias'],'floor_level':v['floor_level']}}
            else:v={'type':v['type'],'config':cfg}
        if rel.parts[:2]==('worldgen','placed_feature') and target!='26.3':v=old_placed(v,target)
        if folder=='tags':
            if any(s in str(rel) for s in ('ordinal_hearts','repairs_den','mineable/den')):continue
            v['values']=[x for x in v['values'] if not any(s in json.dumps(x) for s in ('ordinal_heart','raw_omega','hydra','_den"'))]
        if rel.parts[:2]==('worldgen','biome'):
            if target!='26.3':v=old_biome(v,target)
            else:
                for es in v.get('attributes',{}).get('minecraft:gameplay/natural_mob_spawns',{}).get('argument',{}).get('spawns_by_category',{}).values():es[:]=[e for e in es if 'hydra' not in e['type']]
        if rel.parts[:2]==('worldgen','material_rule') and target!='26.3':continue
        if rel.parts[:2]==('worldgen','noise_settings') and target!='26.3':
            old=load(SOURCE/'compat'/target/'vanilla_overworld_noise.json')
            old['default_block']=state(v['default_block']);old['default_fluid']=state(v['default_fluid'])
            old['sea_level']=v['sea_level'];old['ore_veins_enabled']=False
            old['surface_rule']=old_rule(walk(load(SOURCE/'worldgen/material_rule/googology.json')))
            v=old
        destination=data/rel
        if rel.parts[:2]==('loot_table','blocks'):
            namespace,name=remap_id(SOURCE_NS+':'+rel.stem).split(':')
            destination=out/f'data/{namespace}/loot_table/blocks/{name}.json'
        elif rel.parts[0]=='worldgen':
            namespace,name=remap_id(SOURCE_NS+':'+rel.stem).split(':')
            destination=out/f'data/{namespace}'/rel.parent/(name+'.json')
        if rel.parts[0]=='worldgen':v=ore_policy(rel.parts[1],rel.stem,v)
        write(destination,v)
        if folder=='recipe':recipe_paths.append(destination)
        if rel.parts[:2]==('worldgen','biome'):biome_paths.append(destination)
    recipe_unlocks(data,recipe_paths)
    dimension=walk(load(SOURCE/'dimension/googology.json'))
    if target!='26.3':
        def climate(v):
            if isinstance(v,list):return [climate(x) for x in v]
            if isinstance(v,dict):return [v['min'],v['max']] if set(v)=={'min','max'} else {k:climate(x) for k,x in v.items()}
            return v
        dimension=climate(dimension)
    # Preserve Alice's climate regions, but only keep the barren edge close to
    # actual void. The filtered source restores proper inland biome features.
    source={k:v for k,v in dimension['generator']['biome_source'].items() if k!='type'}
    inland={**source,'biomes':[b for b in source['biomes'] if b['biome'] not in
            ('guogaology:lho_edge','guogaology:lho_void','guogaology:underworld')]}
    dimension['generator']['biome_source']={'type':'guogaology:lho_border','source':source,'inland':inland}
    write(out/'data/guogaology/dimension/outer.json',dimension)
    dim=walk(load(SOURCE/'dimension_type/googology.json'))
    if target=='1.21.1':
        dim.pop('attributes',None);dim.pop('timelines',None);dim.pop('default_clock',None);dim.pop('has_ender_dragon_fight',None)
        dim.update(effects='minecraft:overworld',natural=True,piglin_safe=False,respawn_anchor_works=False,bed_works=True,has_raids=True,ultrawarm=False)
    elif target=='1.21.11':
        dim.pop('default_clock',None);dim.pop('has_ender_dragon_fight',None);dim.update(has_fixed_time=False,skybox='overworld')
    write(data/'dimension_type/outer.json',dim)
    # 1.21.11 / 26.2 use environment attributes, but retained biome fields need conversion.
    if target not in ('1.21.1','26.3'):
        from prepare_port_resources import convert_biome
        for p in biome_paths:
            v=load(p);convert_biome(v);write(p,v)
    print(f'Prepared faithful outer-world resources for {target}')
    for p in (SOURCE/'minecraft/tags').rglob('*.json'):
        destination=out/'data/minecraft'/p.relative_to(SOURCE/'minecraft');v=walk(load(p))
        # The donor repeats full vanilla tags. Import only its additions: carrying
        # the vanilla lists across versions breaks tags for absent vanilla blocks.
        v['values']=[x for x in v['values'] if 'guogaology' in json.dumps(x) and not any(t in json.dumps(x) for t in ('raw_omega','hydra','_den"'))]
        existing=load(destination) if destination.exists() else {'replace':False,'values':[]}
        existing['values']=list({json.dumps(x,sort_keys=True):x for x in existing['values']+v['values']}.values());write(destination,existing)
    if target=='1.21.1':write(out/'data/minecraft/tags/block/bats_spawnable_on.json',{'values':['#minecraft:base_stone_overworld']})
    if target!='26.3':
        path=out/'data/minecraft/tags/block/overworld_carver_replaceables.json'
        tag=load(path) if path.exists() else {'replace':False,'values':[]}
        stones=['ordinal_stone','hell_ordinal_stone','andesite_ordinal_stone','diorite_ordinal_stone','granite_ordinal_stone','tuff_ordinal_stone','cobbled_ordinal_stone']
        tag['values']=list(dict.fromkeys(tag['values']+[remap_id(SOURCE_NS+':'+s) for s in stones]));write(path,tag)

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('target');p.add_argument('out',type=Path);a=p.parse_args();prepare(a.target,a.out)
