"""Audit imported content in the single host namespace and reject retired IDs."""
from pathlib import Path
import json,re,argparse,gzip
from audit_localization import arguments
ROOT=Path(__file__).resolve().parents[1]
NS='googology'
RETIRED='googology_outer'

def audit_namespace(resources):
    for root in ('assets','data'):
        assert not (resources/root/RETIRED).exists(),f'Retired namespace directory: {root}/{RETIRED}'
        for path in (resources/root).rglob('*'):
            if not path.is_file() or path.suffix not in ('.json','.nbt'):continue
            raw=path.read_bytes()
            if path.suffix=='.nbt':raw=gzip.decompress(raw)
            assert RETIRED.encode() not in raw,f'Retired namespace reference: {path}'

def audit_data(resources):
    """Validate surviving recipe/loot/tag references against shipped item roots."""
    data=resources/'data'/NS
    item_ids={namespace+':'+p.stem for namespace in (NS,)
              for p in (resources/'assets'/namespace/'models/item').glob('*.json')}
    def check(value,where):
        if isinstance(value,list):
            for child in value:check(child,where)
        elif isinstance(value,dict):
            if 'item' in value:check(value['item'],where)
            elif 'tag' in value:check('#'+value['tag'],where)
        elif isinstance(value,str):
            tag=value.startswith('#');identifier=value.lstrip('#');namespace,_,path=identifier.partition(':')
            if namespace!=NS:return
            if tag:assert (resources/f'data/{namespace}/tags/item/{path}.json').is_file(),(where,value)
            else:assert identifier in item_ids,(where,value)
    recipes=set()
    for namespace in (NS,):
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
    audit_namespace(ROOT/'src/main/resources')
    recipes=audit_data(ROOT/'src/main/resources')
    assets=ROOT/'src/main/resources/assets'/NS
    langs={code:json.loads((assets/f'lang/{code}.json').read_text('utf8')) for code in ('zh_cn','en_us')}
    assert langs['zh_cn'].keys()==langs['en_us'].keys(),'outer language keys differ'
    for key in langs['en_us']:
        assert arguments(langs['zh_cn'][key])==arguments(langs['en_us'][key]),key
        assert not re.search(r'[\u3400-\u9fff]',langs['en_us'][key]),key
    # All imported and original assets are now part of the same graph.
    from audit_resources import audit as shared_audit
    graph=shared_audit(ROOT,prune)
    assert not graph['missing_references'],graph['missing_references']
    return dict(language_keys=len(langs['en_us']),recipes=recipes,retained=graph['retained_assets'],unused=graph['unused_paths'])

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--prune',action='store_true');parser.add_argument('--report',type=Path);args=parser.parse_args()
    result=audit(args.prune)
    if args.report:args.report.write_text(json.dumps(result,indent=2)+'\n','utf8')
    print({k:(len(v) if k=='unused' else v) for k,v in result.items()})
