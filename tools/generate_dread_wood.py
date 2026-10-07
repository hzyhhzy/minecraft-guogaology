"""Dedicated nonflammable Dread Fir carpentry, independent of vanilla spruce.

The dedicated models / dark green textures are committed artistic sources;
regeneration never requires the retired Loquat model family. Scenery aliases live
in import_outer_content.py; retired carpentry is removed exactly, never by clearing
the shared namespace. The unrelated Loquat fruit item is retained.
"""
from pathlib import Path
import json, re

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources';A=RES/'assets/guogaology';D=RES/'data/guogaology'
SHAPES=('log','wood','stripped_log','stripped_wood','planks','leaves','sapling','stairs','slab',
        'fence','fence_gate','door','trapdoor','button','pressure_plate','sign','wall_sign')
RETIRED=('christmas','hell_christmas','loquat','hell_loquat')
FOLIAGE={family+'_'+shape for family in ('loquat','hell_loquat') for shape in ('leaves','sapling')}|{'christmas_leaves'}
OLD_RECIPES={'cobblestone_from_ordinal_stone','cobblestone_from_ordinal_stone_smelting',
             'ordinal_stone_from_cobbled_ordinal_stone','ordinal_stone_from_cobblestone',
             'epsilon_block_unpack','gamma_block_unpack','omega_block_unpack','dread_log_planks','laver_log_to_planks','dread_log_charcoal'}

def read(p):return json.loads(p.read_text('utf8'))
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf8')
def remap(v):return json.loads(json.dumps(v).replace('loquat','dread'))
def retired_id(value):
    names={family+'_'+shape for family in RETIRED for shape in (*SHAPES,'boat','logs')}-FOLIAGE
    return any(identifier.split(':')[1] in names for identifier in re.findall(r'guogaology:[a-z_]+',value))
def retired_file(stem):
    # christmas_digit_* models are live numeral-lamp variants, not carpentry.
    if stem in FOLIAGE:return False
    return any(any(stem==family+'_'+shape or stem.startswith(family+'_'+shape+'_')
        for shape in (*SHAPES,'boat')) for family in RETIRED)

def shaped(name,pattern,key,result,count=1):
    write(D/f'recipe/{name}.json',{'type':'minecraft:crafting_shaped','category':'building','pattern':pattern,
        'key':{k:{'item':v} for k,v in key.items()},'result':{'id':result,'count':count}})

def tag(path,values):
    v=read(path) if path.exists() else {'replace':False,'values':[]}
    v['values']=list(dict.fromkeys(v['values']+values));write(path,v)

def clean_recipe_rewards(resources):
    """Clean references inside aggregate advancements as well as old filenames.

    Retarget the renamed Dread plank recipe, retain only recipes actually shipped,
    and remove empty recipe unlocks instead of leaving broken / useless rewards.
    Display advancements retain their other rewards and criteria.
    """
    data=Path(resources)/'data/guogaology';recipes=data/'recipe'
    current={'guogaology:'+p.relative_to(recipes).with_suffix('').as_posix()
             for p in recipes.rglob('*.json')}
    aliases={'guogaology:dread_log_planks':'guogaology:dread_planks'}
    def valid(identifier):return not identifier.startswith('guogaology:') or identifier in current
    for path in (data/'advancement').rglob('*.json'):
        value=read(path);before=json.dumps(value,sort_keys=True)
        rewards=value.get('rewards',{})
        if 'recipes' not in rewards:continue
        rewards['recipes']=list(dict.fromkeys(aliases.get(r,r) for r in rewards['recipes'] if valid(aliases.get(r,r))))
        if not rewards['recipes']:
            if 'display' not in value and set(rewards)<={'recipes'}:path.unlink();continue
            rewards.pop('recipes')
        criteria=value.get('criteria',{})
        for name,criterion in list(criteria.items()):
            if criterion.get('trigger')!='minecraft:recipe_unlocked':continue
            conditions=criterion.get('conditions',{});identifier=conditions.get('recipe')
            if identifier is None:continue
            conditions['recipe']=aliases.get(identifier,identifier)
            if not valid(conditions['recipe']):criteria.pop(name)
        if 'requirements' in value:
            value['requirements']=[[name for name in group if name in criteria] for group in value['requirements']]
            value['requirements']=[group for group in value['requirements'] if group]
        if before!=json.dumps(value,sort_keys=True):write(path,value)

def generate():
    # Remove old family entrypoints and art. Shared lamp scenery is unrelated.
    for folder in (A/'blockstates',A/'models',A/'items',A/'textures',D/'recipe',D/'advancement',D/'loot_table'):
        for p in folder.rglob('*'):
            if not p.is_file():continue
            stem=p.stem
            if retired_file(stem) or (stem in RETIRED and p.parent.name not in ('item','items')):
                assert p.resolve().is_relative_to(RES.resolve());p.unlink()
    for name in OLD_RECIPES:
        (D/f'recipe/{name}.json').unlink(missing_ok=True)
        (D/f'advancement/recipes/{name}.json').unlink(missing_ok=True)
    for shape in SHAPES:
        if not (A/f'blockstates/dread_{shape}.json').exists():raise ValueError('Missing committed Dread wood state: '+shape)
    # Alice's original Outer Christmas leaf art is a committed source; only the
    # foliage block survives the removed wood family, with vanilla spruce trunks.
    for path in ('blockstates/christmas_leaves.json','models/block/christmas_leaves.json',
                 'models/item/christmas_leaves.json','items/christmas_leaves.json',
                 'textures/block/christmas_leaves.png'):
        if not (A/path).exists():raise ValueError('Missing committed Christmas foliage: '+path)
    # Plank conversion accepts exactly this family, avoiding duplicate log recipes.
    write(D/'recipe/dread_planks.json',{'type':'minecraft:crafting_shapeless','category':'building',
        'ingredients':[{'tag':'guogaology:dread_logs'}],'result':{'id':'guogaology:dread_planks','count':4}})
    # Normal carpentry recipes are independent of any imported wood family.
    board='guogaology:dread_planks';stick='minecraft:stick'
    for name,pattern,key,count in (
        ('stairs',['P  ','PP ','PPP'],{'P':board},4),('slab',['PPP'],{'P':board},6),
        ('door',['PP','PP','PP'],{'P':board},3),('trapdoor',['PPP','PPP'],{'P':board},2),
        ('fence',['PSP','PSP'],{'P':board,'S':stick},3),('fence_gate',['SPS','SPS'],{'P':board,'S':stick},1),
        ('pressure_plate',['PP'],{'P':board},1),('sign',['PPP','PPP',' S '],{'P':board,'S':stick},3),
        ('boat',['P P','PPP'],{'P':board},1)):
        shaped('dread_'+name,pattern,key,'guogaology:dread_'+name,count)
    write(D/'recipe/dread_button.json',{'type':'minecraft:crafting_shapeless','category':'redstone',
        'ingredients':[{'item':board}],'result':{'id':'guogaology:dread_button','count':1}})
    shaped('dread_wood',['LL','LL'],{'L':'guogaology:dread_log'},'guogaology:dread_wood',3)
    shaped('dread_stripped_wood',['LL','LL'],{'L':'guogaology:dread_stripped_log'},'guogaology:dread_stripped_wood',3)
    # Doors drop only their lower half; signs return the standing item.
    for shape in SHAPES:
        if shape in ('log','leaves'):continue
        name='dread_'+shape;item='dread_sign' if shape=='wall_sign' else name
        conditions=[{'condition':'minecraft:survives_explosion'}]
        if shape=='door':conditions.append({'condition':'minecraft:block_state_property','block':'guogaology:dread_door','properties':{'half':'lower'}})
        if len(conditions)>1:conditions=[{'condition':'minecraft:all_of','terms':conditions}]
        functions=[{'function':'minecraft:set_count','count':2,'conditions':[{'condition':'minecraft:block_state_property','block':'guogaology:dread_slab','properties':{'type':'double'}}]}] if shape=='slab' else []
        write(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'guogaology:'+item,'functions':functions}],'conditions':conditions}]})
    # Add normal crafting categories, while explicitly excluding logs_that_burn.
    for side in ('block','item'):
        base=RES/f'data/minecraft/tags/{side}'
        tag(base/'logs.json',['#guogaology:dread_logs'])
        tag(D/f'tags/{side}/dread_logs.json',['guogaology:dread_'+s for s in ('log','wood','stripped_log','stripped_wood')])
        for shape,label in [('planks','planks'),('stairs','wooden_stairs'),('slab','wooden_slabs'),('door','wooden_doors'),
                            ('trapdoor','wooden_trapdoors'),('fence','wooden_fences'),('fence_gate','fence_gates'),
                            ('button','wooden_buttons'),('pressure_plate','wooden_pressure_plates'),('sign','signs')]:
            tag(base/(label+'.json'),['guogaology:dread_'+shape])
        if side=='block':tag(base/'standing_signs.json',['guogaology:dread_sign']);tag(base/'wall_signs.json',['guogaology:dread_wall_sign'])
        if side=='item':tag(base/'boats.json',['guogaology:dread_boat'])
        tag(base/'non_flammable_wood.json',['guogaology:dread_'+s for s in SHAPES if s!='wall_sign']+(['guogaology:dread_boat'] if side=='item' else ['guogaology:dread_wall_sign']))
    tag(RES/'data/minecraft/tags/block/mineable/axe.json',['guogaology:dread_'+s for s in SHAPES if s not in ('leaves','sapling')])
    # Clean retired IDs from every active tag. The canonical donor templates and
    # tag source are remapped at import; their provenance remains untouched.
    for namespace in ('minecraft','guogaology'):
        for p in (RES/f'data/{namespace}/tags').rglob('*.json'):
            v=read(p);v['values']=[x for x in v.get('values',[]) if not retired_id(json.dumps(x))]
            if p.stem=='logs_that_burn':v['values']=[x for x in v['values'] if 'dread_' not in json.dumps(x)]
            write(p,v)
    # Synchronize only our keys: another task may be updating equipment names.
    catalog=read(ROOT/'tools/copy_catalog.json')
    for locale in ('zh_cn','en_us'):
        values=catalog['translations'][locale]
        for key in list(values):
            if key.split('.',2)[-1] not in FOLIAGE and any(re.match(r'(?:block|item|entity)\.guogaology\.'+family+'_',key) for family in RETIRED):
                values.pop(key);catalog['removed'].append(key)
        zh=locale=='zh_cn'
        labels={'wood':('木','Wood'),'stripped_log':('去皮原木','Stripped Log'),'stripped_wood':('去皮木','Stripped Wood'),
            'planks':('木板','Planks'),'sapling':('树苗','Sapling'),'stairs':('木楼梯','Stairs'),'slab':('木台阶','Slab'),
            'fence':('木栅栏','Fence'),'fence_gate':('木栅栏门','Fence Gate'),'door':('木门','Door'),'trapdoor':('木活板门','Trapdoor'),
            'button':('木按钮','Button'),'pressure_plate':('木压力板','Pressure Plate'),'sign':('木告示牌','Sign'),'wall_sign':('木墙告示牌','Wall Sign')}
        for shape,(cn,en) in labels.items():values['block.guogaology.dread_'+shape]='冥杉'+cn if zh else 'Dread Fir '+en
        values['item.guogaology.dread_boat']='冥杉木船' if zh else 'Dread Fir Boat'
        values['entity.guogaology.dread_boat']='冥杉木船' if zh else 'Dread Fir Boat'
        for family in ('loquat','hell_loquat'):
            prefix='地府枇杷' if family.startswith('hell_') else '枇杷'
            english='Underworld Loquat' if family.startswith('hell_') else 'Loquat'
            for shape,cn,en in (('leaves','树叶','Leaves'),('sapling','树苗','Sapling')):
                key='block.guogaology.'+family+'_'+shape;values[key]=prefix+cn if zh else english+' '+en
                if key in catalog['removed']:catalog['removed'].remove(key)
        key='block.guogaology.christmas_leaves';values[key]='圣诞树叶' if zh else 'Christmas Leaves'
        if key in catalog['removed']:catalog['removed'].remove(key)
    catalog['removed']=sorted(set(catalog['removed']));write(ROOT/'tools/copy_catalog.json',catalog)
    ids=read(ROOT/'tools/outer_content_ids.json')
    for kind,values in ids.items():
        values[:]=[v for v in values if v in FOLIAGE or not any(v.startswith(f+'_') for f in RETIRED)]
        if kind in ('blocks','items'):values.extend(FOLIAGE)
        if kind=='blocks':values.extend('dread_'+s for s in SHAPES)
        if kind=='items':values.extend('dread_'+s for s in SHAPES if s!='wall_sign');values.append('dread_boat')
        ids[kind]=sorted(set(values))
    write(ROOT/'tools/outer_content_ids.json',ids)
    # Rebuild recipe unlocks for the added family, using any material trigger.
    from prepare_outer_resources import recipe_unlocks
    recipe_unlocks(D,list((D/'recipe').glob('dread_*.json')))
    clean_recipe_rewards(RES)
    from unify_outer_content import run
    run()
    from copy_catalog import apply
    apply()
    print('Dread Fir wood family generated; retired Christmas carpentry and stone recipes removed')

if __name__=='__main__':generate()
