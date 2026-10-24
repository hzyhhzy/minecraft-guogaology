"""Keep both wood families in native mechanism tags after donor resource overlays."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RESOURCE=ROOT/'src/main/resources'
FAMILIES=('laver','dread')
MECHANISMS={'wooden_buttons':'button','wooden_doors':'door','wooden_trapdoors':'trapdoor',
            'wooden_pressure_plates':'pressure_plate','fence_gates':'fence_gate'}


def apply_pack(root):
    changed=[]
    for kind in ('block','item'):
        for name,shape in MECHANISMS.items():
            path=Path(root)/f'data/minecraft/tags/{kind}/{name}.json'
            value=json.loads(path.read_text('utf8')) if path.exists() else {'replace':False,'values':[]}
            before=json.dumps(value,sort_keys=True)
            # Tags may also contain optional {id, required} records from datapacks.
            entries=value.get('values',[])+[f'guogaology:{family}_{shape}' for family in FAMILIES]
            value['values']=list({json.dumps(entry,sort_keys=True):entry for entry in entries}.values())
            if json.dumps(value,sort_keys=True)!=before:
                path.parent.mkdir(parents=True,exist_ok=True)
                path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n','utf8')
                changed.append(path)
    return changed


def verify_archive(archive):
    for kind in ('block','item'):
        for name,shape in MECHANISMS.items():
            path=f'data/minecraft/tags/{kind}/{name}.json'
            value=json.loads(archive.read(path))
            assert not value.get('replace',False),f'Wood mechanism replaces native tag: {path}'
            for family in FAMILIES:
                assert f'guogaology:{family}_{shape}' in value['values'],f'Missing native wood mechanism tag: {path}/{family}'


if __name__=='__main__':
    print('WOOD_MECHANISM0614_TAGS changed='+str(len(apply_pack(RESOURCE))))
