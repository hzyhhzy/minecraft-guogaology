"""Run the release + QA helper only in a disposable hidden fo262 copy.

Reads libraries and Fabric/Sodium from the chosen installation. Never copies
accounts, normal settings or normal saves. Use a QA fixture or a fresh normal world.
"""
import argparse,json,shutil,subprocess
from pathlib import Path
from zipfile import ZipFile
ROOT=Path(__file__).resolve().parents[1]

def allowed(lib):
    rules=lib.get('rules',[]);result=not rules
    for rule in rules:
        os=rule.get('os',{})
        if os.get('name','windows')!='windows' or os.get('arch','x86_64') not in ('x86_64','amd64'):continue
        result=rule['action']=='allow'
    return result and not any(x in lib['name'] for x in ('natives-windows-arm64','natives-windows-x86'))

def run(a):
    instance=a.instance.resolve();assert instance.name=='fo262','Runtime scope is fo262 only'
    metadata=json.loads((instance/'fo262.json').read_text('utf-8-sig'));minecraft=instance.parent.parent
    assert bool(a.fixture) != a.fresh_world,'Choose exactly one of --fixture or --fresh-world'
    assert not a.namespace_only or a.fresh_world,'Namespace QA must use --fresh-world'
    assert a.seed is None or a.fresh_world,'--seed applies only to --fresh-world'
    assert a.seed is None or -(1<<63)<=a.seed<(1<<63),'Seed must fit a signed Java long'
    if a.fixture:assert a.fixture.name in ('port-0217','port-qa'),'Use only a disposable QA fixture'
    release=next(l.split('=')[1].strip() for l in (ROOT/'gradle.properties').read_text().splitlines() if l.startswith('mod_version='))
    game=(ROOT/'build/runtime-qa'/a.label).resolve()
    assert game.parent==(ROOT/'build/runtime-qa').resolve(),'Label must be one new directory name'
    assert not game.exists(),'New label required; never reuse success markers'
    mods=game/'mods';mods.mkdir(parents=True);classpath=[]
    for library in metadata['libraries']:
        if not allowed(library):continue
        path=library.get('downloads',{}).get('artifact',{}).get('path')
        if not path:
            group,artifact,version,*classifier=library['name'].split(':');suffix='-'+classifier[0] if classifier else ''
            path=f'{group.replace(".","/")}/{artifact}/{version}/{artifact}-{version}{suffix}.jar'
        full=minecraft/'libraries'/path;assert full.is_file(),full;classpath.append(full)
    classpath.append(instance/'fo262.jar')
    for folder,name in [('libs',f'guogaology-dimension-26.2-{release}.jar'),('qa',f'guogaology-merge-qa-{release}.jar')]:
        shutil.copy2(ROOT/'ports/26.2/build'/folder/name,mods/name)
    copied=set();required={'fabric-api','sodium'} | ({'voxy'} if a.voxy else set()) | ({'creative_flight_speed'} if a.flight_addon else set())
    for jar in (instance/'mods').glob('*.jar'):
        try:
            with ZipFile(jar) as z:
                if 'fabric.mod.json' not in z.namelist():continue
                identity=json.loads(z.read('fabric.mod.json'))['id']
        except (KeyError,json.JSONDecodeError):continue
        if identity in required:shutil.copy2(jar,mods/jar.name);copied.add(identity)
    assert copied==required
    if a.fixture:shutil.copytree(a.fixture,game/'saves/port-qa',ignore=shutil.ignore_patterns('session.lock'))
    (game/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:5\nsimulationDistance:5\nsoundCategory_master:0.0\nlang:zh_cn\n','utf8')
    temp=game/'temp';temp.mkdir()
    command=[str(a.java_home/'bin/java.exe'),'-Xmx4G','--enable-native-access=ALL-UNNAMED','-Dfile.encoding=UTF-8',
             f'-Dguogaology.qa.namespace050={str(a.namespace_only).lower()}',f'-Dguogaology.qa.freshworld={str(a.fresh_world).lower()}',
             '-Dfabric.development=false',f'-Dguogaology.qa.cliffchain049={str(a.cliff_chain049_only).lower()}',f'-Dguogaology.qa.cliffchain048={str(a.cliff_chain_only).lower()}',f'-Dguogaology.qa.coreitems046={str(a.core_items_only).lower()}',f'-Dguogaology.qa.underworld046={str(a.underworld_only).lower()}',f'-Dguogaology.qa.voxy={str(a.voxy).lower()}',f'-Dguogaology.qa.boundaries={str(a.boundaries_only).lower()}',f'-Dguogaology.qa.manuscripts={str(a.manuscripts_only).lower()}',f'-Dguogaology.qa.inventory={str(a.inventory_only).lower()}',f'-Dguogaology.qa.portals={str(a.portals_only).lower()}',f'-Dguogaology.qa.patch051={str(a.patch051_only or a.creative_icons_only).lower()}',f'-Dguogaology.qa.creativeicons047={str(a.creative_icons_only).lower()}',f'-Dguogaology.qa.landmarks043={str(a.landmarks_only).lower()}',f'-Djava.io.tmpdir={temp}',f'-Djna.tmpdir={temp}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}',
             '-cp',';'.join(map(str,classpath)),metadata['mainClass'],'--username','GuogaologyQA','--version','26.2','--gameDir',str(game),
             '--assetsDir',str(minecraft/'assets'),'--assetIndex',metadata['assetIndex']['id'],'--uuid','00000000000000000000000000000001',
             '--accessToken','0','--userType','legacy','--versionType','release','--width','1600','--height','1200' if a.inventory_only else '1000']
    if a.seed is not None:command.insert(1,f'-Dguogaology.qa.seed={a.seed}')
    print('Hidden QA:',game,flush=True)
    with (game/'launch-output.log').open('w',encoding='utf8') as log:
        process=subprocess.Popen(command,cwd=game,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        try:code=process.wait(timeout=300 if a.landmarks_only else 1100)
        except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=20);raise RuntimeError('Own QA process timed out')
    if code or not (game/'port-client-ok.txt').is_file():
        print((game/'launch-output.log').read_text('utf8',errors='replace')[-14000:]);raise RuntimeError(f'QA failed: {code}')
    print((game/'port-client-ok.txt').read_text(),flush=True)
    print('PRODUCTION_PORT_OK 26.2',flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--instance',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True)
    p.add_argument('--fresh-world',action='store_true',help='Create a new default normal world; copy no fixture')
    p.add_argument('--seed',type=int,help='Signed 64-bit seed for a fresh world; omitted uses a new random seed')
    p.add_argument('--namespace-only',action='store_true',help='Run the 0.5.0 namespace and command smoke test (requires --fresh-world)')
    p.add_argument('--fixture',type=Path);p.add_argument('--label',required=True);p.add_argument('--voxy',action='store_true');p.add_argument('--flight-addon',action='store_true');p.add_argument('--boundaries-only',action='store_true');p.add_argument('--manuscripts-only',action='store_true');p.add_argument('--inventory-only',action='store_true');p.add_argument('--portals-only',action='store_true');p.add_argument('--patch051-only',action='store_true');p.add_argument('--landmarks-only',action='store_true');p.add_argument('--underworld-only',action='store_true');p.add_argument('--core-items-only',action='store_true');p.add_argument('--creative-icons-only',action='store_true');p.add_argument('--cliff-chain-only',action='store_true');p.add_argument('--cliff-chain049-only',action='store_true');run(p.parse_args())
