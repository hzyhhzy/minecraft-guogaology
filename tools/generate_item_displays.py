"""Keep parentless plant/jar world models small in non-GUI item contexts.

Only item wrappers change. World geometry, textures and existing GUI transforms
remain authored sources; these contexts match vanilla block/block item displays.
"""
import argparse
import copy
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/guogaology'
ITEMS=('graham','graham_flower','graham_vine','dread_sapling','laver_sapling',
       'loquat_sapling','hell_loquat_sapling','gummy_jar')
DISPLAY={
    'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.25,.25,.25]},
    'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[.5,.5,.5]},
    'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]},
    'thirdperson_lefthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]},
    'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,0,0],'scale':[.4,.4,.4]},
    'firstperson_lefthand':{'rotation':[0,225,0],'translation':[0,0,0],'scale':[.4,.4,.4]},
}

def files(assets=ASSETS):
    result={}
    for name in ITEMS:
        path=assets/f'models/item/{name}.json';model=json.loads(path.read_text('utf8'))
        assert model['parent']=='guogaology:block/'+name,'Unexpected item geometry: '+name
        model['display']={**model.get('display',{}),**copy.deepcopy(DISPLAY)}
        result[path]=model
        definition=assets/f'items/{name}.json'
        if definition.exists():
            value=json.loads(definition.read_text('utf8'))
            assert value['model']['type']=='minecraft:model','Unexpected item selector: '+name
            value['model']['model']='guogaology:item/'+name
            # The old-engine importer must keep this wrapper instead of replacing
            # it with a bare block parent from the donor's modern definition.
            result[definition]=value
    return result

def generate(check=False,assets=ASSETS):
    changed=[]
    for path,value in files(assets).items():
        if json.loads(path.read_text('utf8'))==value:continue
        changed.append(path)
        if not check:path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    if check and changed:raise SystemExit('Stale non-GUI item displays: '+', '.join(str(p) for p in changed))
    print(f'ITEM_DISPLAYS_OK items={len(ITEMS)} changed={len(changed)} check={check}')
    return changed

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--check',action='store_true')
    generate(parser.parse_args().check)
