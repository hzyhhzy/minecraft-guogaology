"""Validate version-specific release jars and collect them with hashes; never installs them."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import shutil
from zipfile import ZipFile
from io import BytesIO

ROOT=Path(__file__).resolve().parents[1]
CONFIG=json.loads((ROOT/'ports/targets.json').read_text())

def verify_namespace(archive):
    """Include nested legacy bridge classes and compressed NBT, not only JSON."""
    from audit_namespace import audit_archive
    return audit_archive(archive)

def verify_structure_template(actual, expected, name):
    # gzip headers (notably the OS byte) and compression output can differ
    # between Python/zlib versions. Minecraft loads the decompressed NBT.
    # Compare every NBT byte, and let gzip reject damaged streams/CRC values.
    assert gzip.decompress(actual) == gzip.decompress(expected), f'Structure content mismatch: {name}'


def verify_client_hooks(archive, metadata):
    active=set()
    for entry in metadata['mixins']:
        config=json.loads(archive.read(entry if isinstance(entry,str) else entry['config']))
        for kind in ('mixins','client','server'):
            for name in config.get(kind,[]):
                full=config['package']+'.'+name
                assert full.replace('.','/')+'.class' in archive.namelist(),f'Missing registered mixin: {full}'
                active.add(full)
    for required in ('ManuscriptVerticalFlightMixin','InventoryOriginAccessor'):
        assert 'dev.guogaology.client.mixin.'+required in active,f'Unregistered client hook: {required}'


def verify_ore_policy(archive, target):
    """Resource imports must not restore donor counts after the host overlay."""
    from generate_ore_distribution import COUNTS, TOPS
    folder='feature' if target=='26.3' else 'configured_feature'
    prefix='data/guogaology/worldgen/'
    for mineral,count in COUNTS.items():
        for suffix in ('','_hell'):
            name='ore_'+mineral+suffix+'.json'
            configured=json.loads(archive.read(prefix+folder+'/'+name))
            config=configured.get('config',configured)
            assert config['discard_chance_on_air_exposure']==0.5,f'Stale ore air policy: {target}/{name}'
            placed=json.loads(archive.read(prefix+'placed_feature/'+name))['placement']
            actual_counts=[p['count'] for p in placed if p['type']=='minecraft:count']
            assert actual_counts==[count],f'Stale ore count: {target}/{name}'
            if mineral in TOPS:
                heights=[p['height'] for p in placed if p['type']=='minecraft:height_range']
                assert heights==[{'type':'minecraft:biased_to_bottom','min_inclusive':{'absolute':-64},
                    'max_inclusive':{'absolute':TOPS[mineral]},'inner':1}],f'Stale ore height: {target}/{name}'


def package(targets, output_dir=None):
    from audit_localization import audit
    audit(ROOT)
    from audit_outer_content import audit as audit_outer
    assert not audit_outer()['unused'],'Unused outer-world assets remain'
    release=next(line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
    output=Path(output_dir) if output_dir is not None else ROOT/'build/releases'/release
    output.mkdir(parents=True,exist_ok=True)
    manifest=[]
    for target in targets:
        assert target in ('1.21.1',*CONFIG),target
        directory=ROOT if target=='1.21.1' else ROOT/'ports'/target
        jar=directory/'build/libs'/f'guogaology-dimension-{target}-{release}.jar'
        with ZipFile(jar) as archive:
            metadata=json.loads(archive.read('fabric.mod.json'))
            assert metadata['id']=='guogaology' and metadata['version']==release
            assert metadata['name']=='果糕逻辑 · Guogaology'
            assert metadata['description']==json.loads((ROOT/'src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))['description']
            assert metadata['license']=='GPL-3.0-only WITH GPL-3.0-linking-exception'
            license_name = 'LICENSE_'+jar.stem.rsplit('-'+release,1)[0] if target=='1.21.1' else 'LICENSE'
            assert archive.read(license_name)==(ROOT/'LICENSE').read_bytes()
            for notice in ('LICENSE-MINECRAFT-EXCEPTION','COPYRIGHT','THIRD_PARTY.md'):
                assert archive.read(notice)==(ROOT/notice).read_bytes(),f'Missing or stale license notice: {target}/{notice}'
            from copy_catalog import normalize
            for locale in ('zh_cn','en_us'):
                key=f'assets/guogaology/lang/{locale}.json'
                translated=json.loads(archive.read(key))
                assert translated==normalize(Path(key),translated),f'Stale copy in {target}: {locale}'
                assert archive.read(key)==(ROOT/'src/main/resources'/key).read_bytes()
            assert archive.read(metadata['icon'])==(ROOT/'src/main/resources'/metadata['icon']).read_bytes()
            assert metadata['depends']['minecraft']==target
            entries=archive.namelist()
            verify_client_hooks(archive, metadata)
            verify_namespace(archive)
            verify_ore_policy(archive, target)
            frame=json.loads(archive.read('guogaology/block_balance.json'))['guogao_portal_frame']
            assert frame['hardness']==200 and frame['drop']=='none',f'Stale activated portal frame: {target}'
            loot=json.loads(archive.read('data/guogaology/loot_table/blocks/guogao_portal_frame.json'))
            assert not loot.get('pools'),f'Activated frame must never drop: {target}'
            assert not any('PortalGroundChecks' in name for name in entries),'Ground-placement QA leaked into release'
            assert not any('/qa/' in n or '/mergeqa/' in n or '/connectorqa/' in n or 'connector-qa' in n or 'port-qa' in n or 'visualqa' in n for n in entries),'QA code leaked into release'
            assert not any('/Fusion' in n or '/SurvivalEntities' in n or '/OrdinalWand' in n for n in entries),'Retired systems returned'
            templates=[n for n in entries if n.startswith('data/guogaology/structure/') and n.endswith('.nbt')]
            assert len(templates)==61,(target,len(templates))
            from import_outer_content import nbt_transform
            for name in templates:
                original=ROOT/'content/outer-1.0.0'/name.removeprefix('data/guogaology/')
                verify_structure_template(archive.read(name),nbt_transform(original.read_bytes(),legacy=target!='26.3'),name)
            for name in entries:
                if name.startswith('data/guogaology/advancement/') and name.endswith('.json'):
                    for recipe in json.loads(archive.read(name)).get('rewards',{}).get('recipes',[]):
                        namespace,path=recipe.split(':',1)
                        assert f'data/{namespace}/recipe/{path}.json' in entries,(target,name,recipe)
            if target=='1.21.1':
                from prepare_legacy_resources import adapt
                for name in entries:
                    if name.endswith('.json') and name.startswith(('data/guogaology/worldgen/', 'assets/guogaology/models/')):
                        source=ROOT/'src/main/resources'/name
                        if source.exists():
                            assert json.loads(archive.read(name))==adapt(json.loads(source.read_text('utf8'))),f'Stale 1.21.1 resource adaptation: {name}'
                nested=metadata['jars'][0]['file']
                with ZipFile(BytesIO(archive.read(nested))) as bridge:
                    info=json.loads(bridge.read('fabric.mod.json'))
                    assert info['version']==release and info['depends']['minecraft']==target
                    assert info['entrypoints'].keys()=={'guogaology:outer','guogaology:outer_client'}
                    # Loom 1.17 remaps Mixin annotation selectors in-place, without a refmap.
                    noise=bridge.read('dev/guogaology/mixin/OuterNoiseSettingsMixin.class')
                    spawn=bridge.read('dev/guogaology/mixin/OuterSpawnPlacementInvoker.class')
                    assert b'Lnet/minecraft/class_7138;method_41556(' in noise
                    assert b'method_20637' in spawn
                    assert b'net/minecraft/world/level' not in noise+spawn
            else:assert not metadata.get('jars'),'Legacy bridge leaked into modern target'
            meshes=[n for n in entries if n.startswith('assets/guogaology/core_meshes/') and n.endswith('.json')]
            assert len(meshes)==28,(target,len(meshes))
            for name in meshes:
                source=ROOT/'src/main/resources'/name
                assert archive.read(name)==source.read_bytes(),f'{target} diverged from final D mesh: {name}'
            java=21 if target in ('1.21.1','1.21.11') else 25
            assert max(int.from_bytes(archive.read(n)[6:8],'big') for n in entries if n.startswith('dev/guogaology/') and n.endswith('.class'))==java+44
            if target=='26.3':
                assert 'data/guogaology/worldgen/feature/monumental_landmarks.json' in entries
                assert 'data/guogaology/worldgen/material_rule/guogaology.json' in entries
                for n in entries:
                    if n.startswith('data/guogaology/loot_table/') and n.endswith('.json'):
                        data=archive.read(n).decode()
                        assert '"functions"' not in data and '"conditions"' not in data,(target,n)
        destination=output/jar.name
        shutil.copy2(jar,destination)
        manifest.append({'minecraft':target,'mod':release,'java':java,'license':metadata['license'],'file':jar.name,'bytes':jar.stat().st_size,
                         'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'requires':metadata['depends']})
    (output/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (output/'SHA256SUMS.txt').write_text(''.join(f"{m['sha256']}  {m['file']}\n" for m in manifest),encoding='utf-8')
    lines=['Guogaology / 果糕逻辑 '+release,'','Choose exactly ONE JAR matching the Minecraft version. Install Fabric Loader and matching Fabric API.','Both server and client need the same matching Guogaology JAR. Do not mix Minecraft versions in one mods folder.','No gallery or flight-speed addon is required. The 0.5.0 namespace change requires a new world; keep earlier saves with their previous Mod build.','']
    for m in manifest:lines.append(f"{m['minecraft']}: Java {m['java']}+, Fabric Loader {m['requires']['fabricloader']}, Fabric API {m['requires']['fabric-api']}")
    lines.extend(['', 'Minecraft 1.21.1 only: the SAME 1.21.1 JAR also runs on NeoForge 21.1.248 with Sinytra Connector 2.0.0-beta.16 or beta.17 and Forgified Fabric API 0.116.15+2.3.1+1.21.1.',
                  'Use Forgified Fabric API instead of the ordinary Fabric API in that NeoForge setup. Do not install both API distributions.',
                  'Connector is optional. There is no official Connector 26.2 build in this validation; 26.2 is tested on native Fabric.',
                  'Validation scope and reproducible checks: docs/CONNECTOR-COMPATIBILITY.md in the source repository.'])
    (output/'INSTALL.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    for notice in ('LICENSE','LICENSE-MINECRAFT-EXCEPTION','COPYRIGHT','THIRD_PARTY.md'):
        shutil.copy2(ROOT/notice,output/notice)
    (output/'SOURCE.txt').write_text(f'Guogaology {release} is GPL-3.0-only WITH GPL-3.0-linking-exception.\n'
        f'The matching complete source is Guogaology-v{release}-source.zip. Share that source archive with these JARs, or make the corresponding source available as required by GPLv3.\n',encoding='utf8')
    print(json.dumps({'output':str(output),'releases':manifest},ensure_ascii=False,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--targets',default='1.21.1,1.21.11,26.2,26.3')
    parser.add_argument('--output',type=Path,help='Optional output directory, e.g. for isolated package verification')
    args=parser.parse_args()
    package(args.targets.split(','),args.output)
