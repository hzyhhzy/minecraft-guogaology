"""Validate version-specific release jars and collect them with hashes; never installs them."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
CONFIG=json.loads((ROOT/'ports/targets.json').read_text())

def package(targets):
    from audit_localization import audit
    audit(ROOT)
    release=next(line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
    output=ROOT/'build/releases'/release
    output.mkdir(parents=True,exist_ok=True)
    manifest=[]
    for target in targets:
        assert target in ('1.21.1',*CONFIG),target
        directory=ROOT if target=='1.21.1' else ROOT/'ports'/target
        jar=directory/'build/libs'/f'googology-dimension-{target}-{release}.jar'
        with ZipFile(jar) as archive:
            metadata=json.loads(archive.read('fabric.mod.json'))
            assert metadata['id']=='googology' and metadata['version']==release
            assert metadata['name']=='果糕逻辑 · Guogaology'
            assert metadata['description']==json.loads((ROOT/'src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))['description']
            assert metadata['license']=='GPL-3.0-only WITH GPL-3.0-linking-exception'
            license_name = 'LICENSE_'+jar.stem.rsplit('-'+release,1)[0] if target=='1.21.1' else 'LICENSE'
            assert archive.read(license_name)==(ROOT/'LICENSE').read_bytes()
            for notice in ('LICENSE-MINECRAFT-EXCEPTION','COPYRIGHT','THIRD_PARTY.md'):
                assert archive.read(notice)==(ROOT/notice).read_bytes(),f'Missing or stale license notice: {target}/{notice}'
            from copy_catalog import normalize
            for locale in ('zh_cn','en_us'):
                key=f'assets/googology/lang/{locale}.json'
                translated=json.loads(archive.read(key))
                assert translated==normalize(Path(key),translated),f'Stale copy in {target}: {locale}'
                assert archive.read(key)==(ROOT/'src/main/resources'/key).read_bytes()
            assert archive.read(metadata['icon'])==(ROOT/'src/main/resources'/metadata['icon']).read_bytes()
            assert metadata['depends']['minecraft']==target
            entries=archive.namelist()
            assert not any('/qa/' in n or 'port-qa' in n or 'visualqa' in n for n in entries),'QA code leaked into release'
            assert not any('/Fusion' in n or '/SurvivalEntities' in n or '/OrdinalWand' in n for n in entries),'Retired systems returned'
            meshes=[n for n in entries if n.startswith('assets/googology/core_meshes/') and n.endswith('.json')]
            assert len(meshes)==28,(target,len(meshes))
            for name in meshes:
                source=ROOT/'src/main/resources'/name
                assert archive.read(name)==source.read_bytes(),f'{target} diverged from final D mesh: {name}'
            java=21 if target in ('1.21.1','1.21.11') else 25
            assert max(int.from_bytes(archive.read(n)[6:8],'big') for n in entries if n.startswith('dev/googology/') and n.endswith('.class'))==java+44
            if target=='26.3':
                assert 'data/googology/worldgen/feature/monumental_landmarks.json' in entries
                assert 'data/googology/worldgen/material_rule/googology.json' in entries
                for n in entries:
                    if n.startswith('data/googology/loot_table/') and n.endswith('.json'):
                        data=archive.read(n).decode()
                        assert '"functions"' not in data and '"conditions"' not in data,(target,n)
        destination=output/jar.name
        shutil.copy2(jar,destination)
        manifest.append({'minecraft':target,'mod':release,'java':java,'license':metadata['license'],'file':jar.name,'bytes':jar.stat().st_size,
                         'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'requires':metadata['depends']})
    (output/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (output/'SHA256SUMS.txt').write_text(''.join(f"{m['sha256']}  {m['file']}\n" for m in manifest),encoding='utf-8')
    lines=['Guogaology / 果糕逻辑 '+release,'','Choose exactly ONE JAR matching the Minecraft version. Install Fabric Loader and matching Fabric API.','Both server and client need the same matching Guogaology JAR. Do not mix Minecraft versions in one mods folder.','No gallery or flight-speed addon is required. Test old worlds on a backup; cross-version world migration is not guaranteed.','']
    for m in manifest:lines.append(f"{m['minecraft']}: Java {m['java']}+, Fabric Loader {m['requires']['fabricloader']}, Fabric API {m['requires']['fabric-api']}")
    (output/'INSTALL.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    for notice in ('LICENSE','LICENSE-MINECRAFT-EXCEPTION','COPYRIGHT'):
        shutil.copy2(ROOT/notice,output/notice)
    (output/'SOURCE.txt').write_text(f'Guogaology {release} is GPL-3.0-only WITH GPL-3.0-linking-exception.\n'
        f'The matching complete source is Guogaology-v{release}-source.zip. Share that source archive with these JARs, or make the corresponding source available as required by GPLv3.\n',encoding='utf8')
    print(json.dumps({'output':str(output),'releases':manifest},ensure_ascii=False,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--targets',default='1.21.1,1.21.11,26.2,26.3')
    package(parser.parse_args().targets.split(','))
