"""Audit the imported namespace, shared aliases, bilingual names and reachable art."""
from pathlib import Path
import json,re,argparse
from audit_localization import arguments
ROOT=Path(__file__).resolve().parents[1]
NS='googology_outer'

def audit_data(resources):
    """Validate surviving recipe/loot/tag references against shipped item roots."""
    data=resources/'data'/NS
    item_ids={namespace+':'+p.stem for namespace in ('googology',NS)
              for p in (resources/'assets'/namespace/'models/item').glob('*.json')}
    def check(value,where):
        if isinstance(value,list):
            for child in value:check(child,where)
        elif isinstance(value,dict):
            if 'item' in value:check(value['item'],where)
            elif 'tag' in value:check('#'+value['tag'],where)
        elif isinstance(value,str):
            tag=value.startswith('#');identifier=value.lstrip('#');namespace,_,path=identifier.partition(':')
            if namespace not in ('googology',NS):return
            if tag:assert (resources/f'data/{namespace}/tags/item/{path}.json').is_file(),(where,value)
            else:assert identifier in item_ids,(where,value)
    recipes=set()
    for namespace in ('googology',NS):
        for p in (resources/f'data/{namespace}/recipe').rglob('*.json'):recipes.add(namespace+':'+p.relative_to(resources/f'data/{namespace}/recipe').with_suffix('').as_posix())
    for p in (data/'recipe').rglob('*.json'):
        value=json.loads(p.read_text('utf8'))
        for ingredient in value.get('key',{}).values():check(ingredient,p)
        for key in ('ingredient','ingredients'):
            if key in value:check(value[key],p)
        result=value.get('result',{});check(result.get('id') if isinstance(result,dict) else result,p)
    for p in (data/'advancement').rglob('*.json'):
        value=json.loads(p.read_text('utf8'))
        for recipe in value.get('rewards',{}).get('recipes',[]):assert recipe in recipes,(p,recipe)
    for p in (data/'tags/item').rglob('*.json'):
        for value in json.loads(p.read_text('utf8'))['values']:check(value,p)
    def loot(v,p):
        if isinstance(v,list):
            for child in v:loot(child,p)
        if isinstance(v,dict):
            if v.get('type')=='minecraft:item':check(v['name'],p)
            for child in v.values():loot(child,p)
    for p in (data/'loot_table').rglob('*.json'):loot(json.loads(p.read_text('utf8')),p)
    assert len(list((data/'structure').rglob('*.nbt')))==61
    return len(list((data/'recipe').glob('*.json')))

def audit(prune=False):
    recipes=audit_data(ROOT/'src/main/resources')
    assets=ROOT/'src/main/resources/assets'/NS
    langs={code:json.loads((assets/f'lang/{code}.json').read_text('utf8')) for code in ('zh_cn','en_us')}
    assert langs['zh_cn'].keys()==langs['en_us'].keys(),'outer language keys differ'
    for key in langs['en_us']:
        assert arguments(langs['zh_cn'][key])==arguments(langs['en_us'][key]),key
        assert not re.search(r'[\u3400-\u9fff]',langs['en_us'][key]),key
    retained=set();pending=[]
    def add(p):
        assert p.is_file(),f'Missing imported asset {p.relative_to(ROOT)}'
        if p not in retained:
            retained.add(p)
            if p.suffix=='.json':pending.append(p)
    def visit(v):
        if isinstance(v,list):
            for child in v:visit(child)
        elif isinstance(v,dict):
            for k,x in v.items():
                if k in ('model','parent') and isinstance(x,str) and x.startswith(NS+':'):add(assets/'models'/f'{x.split(":",1)[1]}.json')
                elif k=='textures' and isinstance(x,dict):
                    for t in x.values():
                        if isinstance(t,dict):t=t['sprite']
                        if t.startswith(NS+':'):add(assets/'textures'/f'{t.split(":",1)[1]}.png')
                else:visit(x)
    blocks={p.stem for p in (assets/'blockstates').glob('*.json')}
    items={p.stem for p in (assets/'models/item').glob('*.json')}
    for kind,names in [('block',blocks),('item',items-blocks)]:
        for name in names:assert f'{kind}.{NS}.{name}' in langs['en_us'],(kind,name)
    for folder in ('blockstates','models/item','items'):
        for p in (assets/folder).glob('*.json'):add(p)
    # Entity/boat textures have programmatic renderer roots, not block models.
    for name in ('snake','deepseek_whale','busy_beaver','fly_y','fruit_cake_slime','fruit_slime','evil_pig'):add(assets/f'textures/entity/{name}.png')
    for wood in ('christmas','loquat','laver','hell_christmas','hell_loquat'):
        add(assets/f'textures/entity/boat/{wood}_boat.png')
        add(assets/f'textures/gui/signs/{wood}.png')
    while pending:visit(json.loads(pending.pop().read_text('utf8')))
    candidates={p for d in ('models','textures','items','blockstates','equipment') for p in (assets/d).rglob('*') if p.is_file()}
    unused=sorted(candidates-retained)
    if prune:
        for p in unused:
            assert p.resolve().is_relative_to(assets.resolve());p.unlink()
    return dict(language_keys=len(langs['en_us']),blocks=len(blocks),items=len(items-blocks),recipes=recipes,retained=len(retained),unused=[p.relative_to(ROOT).as_posix() for p in unused])

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--prune',action='store_true');parser.add_argument('--report',type=Path);args=parser.parse_args()
    result=audit(args.prune)
    if args.report:args.report.write_text(json.dumps(result,indent=2)+'\n','utf8')
    print({k:(len(v) if k=='unused' else v) for k,v in result.items()})
