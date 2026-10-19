"""Generate only the sixteen giant-landmark chest tables for the 0.6.5 economy.

Main: one local Lv2, each Lv1 ingredient averaging 3x its recipe cost,
and 2–4 capital-Omega materials. Side: one local Lv1 and each ingredient
averaging its recipe cost. Independent ingredient pools do not guarantee an
individual chest contains that many complete craftable sets. Every chest
also has 16–24 non-ingredient decorative blocks (mean 20).
"""
from pathlib import Path
from collections import Counter
import argparse
import json
import math
import random

ROOT=Path(__file__).resolve().parents[1]
TABLES=ROOT/'src/main/resources/data/guogaology/loot_table/chests'
THEMES={
    'matrix':{'core':'sequence_core','decoration':'matrix_archive_ceramic'},
    'power':{'core':'power_tower_core','decoration':'recursive_bronze'},
    'hydra':{'core':'hydra_bud','decoration':'hydra_jade'},
    'absence':{'core':'lho_trace','decoration':'lho_letter_l'},
    'weaver':{'core':'laver_core','decoration':'resonant_silk'},
    'astra':{'core':'astra_critical_core','decoration':'astra_marble'},
    'guogao':{'core':'guogao_heart','decoration':'guogao_heart_resin'},
    'frontier':{'core':'boundary_core','decoration':'axiom_porcelain'},
}
OMEGA_MATERIAL='true_omega_material'  # CapitalΩ processed mineral from the existing registry.


def pool(token,minimum,maximum):
    count=minimum if minimum==maximum else {'type':'minecraft:uniform','min':minimum,'max':maximum}
    entry={'type':'minecraft:tag' if token.startswith('#') else 'minecraft:item',
           'name':'guogaology:'+token.lstrip('#')}
    if token.startswith('#'):entry['expand']=True
    # Expanded TagEntry choices bypass entry functions. Apply the quantity to
    # the chosen stack at pool scope so an alternative group still gives its
    # full recipe budget, rather than one default item.
    return {'rolls':1,'entries':[entry],
            'functions':[{'function':'minecraft:set_count','count':count}]}


def tag_values(tag,seen=()):
    if tag in seen:raise ValueError('Cyclic recipe tag '+tag)
    ns,name=tag.split(':',1)
    data=json.loads((ROOT/f'src/main/resources/data/{ns}/tags/item/{name}.json').read_text('utf8'))
    result=[]
    for entry in data['values']:
        value=entry if isinstance(entry,str) else entry['id']
        result.extend(tag_values(value[1:],(*seen,tag)) if value.startswith('#') else [value])
    return list(dict.fromkeys(result))


def alternatives(token):
    return tag_values('guogaology:'+token[1:]) if token.startswith('#') else ['guogaology:'+token]


def raw_recipe(theme):
    """Count every actual recipe slot, including the central base crystal.

    A tag remains one alternative group; giving every alternative its full count
    would multiply its effective contribution by the number of tag members.
    """
    core=THEMES[theme]['core']
    recipe=json.loads((ROOT/f'src/main/resources/data/guogaology/recipe/{core}.json').read_text('utf8'))
    ingredients=([recipe['key'][symbol] for row in recipe['pattern'] for symbol in row if symbol!=' ']
                 if recipe['type']=='minecraft:crafting_shaped' else recipe['ingredients'])
    items=[]
    for value in ingredients:
        token=value.get('item') or '#'+value['tag']
        items.append(('#' if token.startswith('#') else '')+token.lstrip('#').split(':',1)[1])
    assert len(items)==9,(theme,items)
    assert items.count('ordinal_crystal')==1
    return Counter(items)


def ingredient_bounds(count,main):
    # All groups remain present. A positive integer with mean1 must be fixed1.
    return (count*2,count*4) if main else ((1,1) if count==1 else (count-1,count+1))


def reward_bounds(theme,main):
    spec=THEMES[theme]
    result={spec['core']+('_lv2' if main else ''):(1,1)}
    result.update((token,ingredient_bounds(count,main)) for token,count in raw_recipe(theme).items())
    if main:result[OMEGA_MATERIAL]=(2,4)
    result[spec['decoration']]=(16,24)
    return result


def table(theme,main):
    return {'type':'minecraft:chest','pools':[pool(token,*limits) for token,limits in reward_bounds(theme,main).items()]}


def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n','utf8')


def audit():
    """Exact expected-cost checks on the written JSON, plus native-stack bounds."""
    known={p.stem for p in (ROOT/'src/main/resources/assets/guogaology/models/item').glob('*.json')}
    known|={p.stem for p in (ROOT/'src/main/resources/assets/guogaology/blockstates').glob('*.json')}
    rng=random.Random(605);reports=[]
    for theme,spec in THEMES.items():
        recipe=raw_recipe(theme)
        all_raw={item for token in recipe for item in alternatives(token)}
        assert 'guogaology:'+spec['decoration'] not in all_raw,'Decoration repeats a recipe ingredient'
        for main in (True,False):
            name=theme+('_sanctum' if main else '_ruin')
            actual=json.loads((TABLES/(name+'.json')).read_text('utf8'))
            assert actual==table(theme,main),name+' drifted from generator'
            bounds={};group_rows=[]
            for p in actual['pools']:
                assert p['rolls']==1 and len(p['entries'])==1 and not p.get('conditions')
                entry=p['entries'][0]
                assert entry['type'] in ('minecraft:item','minecraft:tag')
                assert entry['name'].startswith('guogaology:') and not entry.get('conditions')
                token=('#' if entry['type']=='minecraft:tag' else '')+entry['name'].split(':',1)[1]
                if token.startswith('#'):assert entry['expand'] is True
                options=alternatives(token);assert options
                for identifier in options:assert identifier.split(':',1)[1] in known,identifier
                assert not entry.get('functions'),'Expanded tags require pool-level quantity functions'
                assert len(p['functions'])==1
                function=p['functions'][0];assert function['function']=='minecraft:set_count'
                count=function['count'];lo,hi=(count,count) if isinstance(count,int) else (count['min'],count['max'])
                assert isinstance(lo,int) and isinstance(hi,int) and 1<=lo<=hi<=64
                assert token not in bounds,'Duplicate reward group '+token
                bounds[token]=(lo,hi)
                if token in recipe:
                    expected=(lo+hi)/2
                    assert expected==recipe[token]*(3 if main else 1),(name,token,expected)
                    group_rows.append({'ingredient':token,'recipe_count':recipe[token],'range':[lo,hi],
                       'expected_count':expected,'alternatives':options,
                       'group_presence_probability':1,'individual_variant_probability':1/len(options)})
            assert bounds==reward_bounds(theme,main)
            core=spec['core']+('_lv2' if main else '')
            assert bounds[core]==(1,1) and bounds[spec['decoration']]==(16,24)
            assert (OMEGA_MATERIAL in bounds)==main
            expected_core_ids={core,'ordinal_crystal'}
            roots={v['core'] for v in THEMES.values()}|{'ordinal_crystal'}
            for token in bounds:
                for identifier in alternatives(token):
                    item=identifier.split(':',1)[1]
                    if any(item==root or item.startswith(root+'_lv') for root in roots):assert item in expected_core_ids,item
                    if item.endswith(('_ore','_material')):assert main and item==OMEGA_MATERIAL,item
                    assert item!='ordinal_shard'
            maximum_stacks=sum(math.ceil(hi/64) for lo,hi in bounds.values())
            assert maximum_stacks<=27
            sample_sums=Counter()
            for _ in range(10000):
                sample={token:rng.randint(lo,hi) for token,(lo,hi) in bounds.items()}
                assert sum(math.ceil(n/64) for n in sample.values())<=27
                sample_sums.update(sample)
            reports.append({'table':name,'core':core,'core_count':1,
               'raw_sets_expected':3 if main else 1,'raw_items_expected':27 if main else 9,
               'raw_expectation_scope':'Each ingredient independently averages this recipe-cost multiplier; not a complete-set guarantee.',
               'raw_groups':group_rows,'omega_material_range':[2,4] if main else [0,0],
               'decoration':spec['decoration'],'decoration_range':[16,24],'decoration_expected':20,
               'maximum_initial_stacks':maximum_stacks,'sampled_chests':10000,
               'sample_mean_by_group':{k:v/10000 for k,v in sample_sums.items()}})
    result={'tables':len(reports),'sampled_chests':sum(v['sampled_chests'] for v in reports),
       'maximum_initial_stacks':max(v['maximum_initial_stacks'] for v in reports),'results':reports,
       'notes':['Only eight giant sanctum/ruin tables are generated; unrelated building loot is untouched.',
                'Every ingredient group is guaranteed; an expanded tag chooses one member uniformly for that group.',
                'Tag alternatives are not extra ingredient sets. Numeral/state variants use ordinary default items.',
                'Main: local Lv2 + each ingredient averaging 3x Lv1 recipe cost (including the base crystal) + 2–4 true_omega_material + mean 20 decoration.',
                'Side: local Lv1 + each ingredient averaging 1x Lv1 recipe cost (including the base crystal) + mean 20 decoration.',
                'Ingredient groups roll independently: raw_sets_expected is a per-ingredient cost multiplier, not a guarantee of complete craftable sets in a chest.']}
    write(ROOT/'build/loot-065.json',result)
    return result


def generate():
    for theme in THEMES:
        for main in (True,False):write(TABLES/(theme+('_sanctum' if main else '_ruin')+'.json'),table(theme,main))
    result=audit()
    print(f"Generated/audited {result['tables']} tables; {result['sampled_chests']} samples; at most {result['maximum_initial_stacks']} initial stacks")


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--check',action='store_true');args=parser.parse_args()
    result=audit() if args.check else None
    if args.check:print(f"PASS: {result['tables']} tables, exact expected costs, {result['sampled_chests']} samples")
    else:generate()
