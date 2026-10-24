"""Restrict0.6.14 to approved generation and native wood/Silk repairs."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import io
import json

ROOT=Path(__file__).resolve().parents[1]
OLD,NEW='0.6.13','0.6.14'
TARGETS=('1.21.1','1.21.11','26.2','26.3')
SILK_MIXINS={'DecoratedPotSilkMixin','NativeSilkMiningMixin'}
WOOD_TAGS={'wooden_buttons':'button','wooden_doors':'door','wooden_trapdoors':'trapdoor',
           'wooden_pressure_plates':'pressure_plate','fence_gates':'fence_gate'}


def contents(z):
    return {n:z.read(n) for n in z.namelist() if not n.endswith('/')}


def equivalent(name,a,b):
    return json.loads(a)==json.loads(b) if name.endswith('.json') else a==b


def resource_equivalent(name,a,b):
    for kind in ('block','item'):
        for tag,shape in WOOD_TAGS.items():
            if name!=f'data/minecraft/tags/{kind}/{tag}.json':continue
            before,after=json.loads(a),json.loads(b)
            expected=json.loads(a)
            values=expected.setdefault('values',[])
            for family in ('laver','dread'):
                value=f'guogaology:{family}_{shape}'
                if value not in values:values.append(value)
            assert not after.get('replace',False),name
            return after==expected
    return equivalent(name,a,b)


def refmap_without_silk(value):
    if isinstance(value,dict):
        return {key:refmap_without_silk(child) for key,child in value.items()
                if key.rsplit('/',1)[-1] not in SILK_MIXINS}
    return value


def audit():
    rows=[]
    for target in TARGETS:
        old=ROOT/f'build/releases/{OLD}/guogaology-dimension-{target}-{OLD}.jar'
        new=ROOT/f'build/releases/{NEW}/guogaology-dimension-{target}-{NEW}.jar'
        with ZipFile(old) as z:a=contents(z)
        with ZipFile(new) as z:b=contents(z)
        resources={n for n in a.keys()|b.keys() if n.startswith(('assets/','data/'))}
        assert resources<=a.keys() and resources<=b.keys(), 'No resource or saved ID additions/removals'
        assert all(resource_equivalent(n,a[n],b[n]) for n in resources), 'Unexpected resource delta beyond additive native wood tags'
        changed=sorted(n for n in a.keys()|b.keys() if a.get(n)!=b.get(n))
        classes={n for n in changed if n.endswith('.class')}
        allowed={'dev/guogaology/world/HauntedVines.class','dev/guogaology/world/NaturalScenery.class',
                 'dev/guogaology/world/NaturalScenery$CloudFir.class', # source-line table shifts only
                 'dev/guogaology/mixin/SanctuaryGenerationMixin.class',
                 'dev/guogaology/world/UnderworldFloor.class',
                 'dev/guogaology/mining/MiningEffects.class',
                 'dev/guogaology/outer/registry/ModWoodDecorBlocks.class'}
        allowed|={f'dev/guogaology/mixin/{name}.class' for name in SILK_MIXINS}
        wood_class='dev/guogaology/outer/registry/ModWoodDecorBlocks.class'
        if target=='1.21.1':allowed.remove(wood_class) # registered by the legacy bridge
        allowed|={f'dev/guogaology/world/UnderworldFloor${n}.class' for n in ('RandomAt','AirAt','Sink','Result')}
        assert classes==allowed,(target,classes^allowed)
        assert json.loads(a['fabric.mod.json'].decode().replace(OLD,NEW))==json.loads(b['fabric.mod.json'])
        for name in a:
            if name.endswith('-refmap.json'):
                assert json.loads(a[name])==refmap_without_silk(json.loads(b[name])),'No change to existing Mixin targets'
            if name=='guogaology.mixins.json':
                newer=json.loads(b[name]);assert SILK_MIXINS<=set(newer['mixins'])
                newer['mixins']=[m for m in newer['mixins'] if m not in SILK_MIXINS]
                assert newer==json.loads(a[name]),'Only two native Silk hooks may be added'
            if name=='guogaology.client.mixins.json':
                assert json.loads(a[name])==json.loads(b[name]),'Client targets unchanged'
        nested=set()
        nested_changes=[]
        for name in [n for n in a if n.startswith('META-INF/jars/')]:
            newer=name.replace(OLD,NEW);nested|={name,newer}
            with ZipFile(io.BytesIO(a[name])) as x,ZipFile(io.BytesIO(b[newer])) as y:
                assert set(x.namelist())==set(y.namelist())
                for entry in x.namelist():
                    if entry=='fabric.mod.json':assert json.loads(x.read(entry).decode().replace(OLD,NEW))==json.loads(y.read(entry))
                    elif target=='1.21.1' and entry==wood_class:
                        assert x.read(entry)!=y.read(entry),'Legacy native wood property repair must reach bridge'
                        nested_changes.append(entry)
                    else:assert x.read(entry)==y.read(entry),entry
        allowed_meta={'fabric.mod.json','META-INF/MANIFEST.MF','guogaology.mixins.json'}|nested
        allowed_meta|={n for n in a if n.endswith('-refmap.json')}
        unexpected=[n for n in changed if n not in classes|resources|allowed_meta]
        assert not unexpected,unexpected
        rows.append({'minecraft':target,'success':True,'sha256':hashlib.sha256(new.read_bytes()).hexdigest(),
                     'baselineSha256':hashlib.sha256(old.read_bytes()).hexdigest(),
                     'preservedResourceIds':len(resources),'changedResources':sorted(n for n in resources if not equivalent(n,a[n],b[n])),
                     'changedClasses':sorted(classes),'changedBridgeClasses':nested_changes})
    output=ROOT/'build/tree-lights-0614/package-delta.json'
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps({'success':True,'targets':rows,'scope':'Static packaged data/ID and class delta; native generation/save observations are separate.'},indent=2)+'\n','utf8')
    print('TREE_LIGHTS0614_PACKAGE_DELTA_OK targets=4')


if __name__=='__main__':audit()
