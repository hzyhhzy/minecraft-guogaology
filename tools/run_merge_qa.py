"""Run the release + QA helper only in a disposable hidden fo262 copy.

Reads libraries and Fabric/Sodium from the chosen installation. Never copies
accounts, normal settings or normal saves. The fixture must be a QA world.
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
    assert a.fixture.name in ('port-0217','port-qa'),'Use only a disposable QA fixture'
    release=next(l.split('=')[1].strip() for l in (ROOT/'gradle.properties').read_text().splitlines() if l.startswith('mod_version='))
    game=ROOT/'build/runtime-qa'/a.label;assert not game.exists(),'New label required; never reuse success markers'
    mods=game/'mods';mods.mkdir(parents=True);classpath=[]
    for library in metadata['libraries']:
        if not allowed(library):continue
        path=library.get('downloads',{}).get('artifact',{}).get('path')
        if not path:
            group,artifact,version,*classifier=library['name'].split(':');suffix='-'+classifier[0] if classifier else ''
            path=f'{group.replace(".","/")}/{artifact}/{version}/{artifact}-{version}{suffix}.jar'
        full=minecraft/'libraries'/path;assert full.is_file(),full;classpath.append(full)
    classpath.append(instance/'fo262.jar')
    for folder,name in [('libs',f'googology-dimension-26.2-{release}.jar'),('qa',f'googology-merge-qa-{release}.jar')]:
        shutil.copy2(ROOT/'ports/26.2/build'/folder/name,mods/name)
    copied=set();required={'fabric-api','sodium'} | ({'voxy'} if a.voxy else set())
    for jar in (instance/'mods').glob('*.jar'):
        try:
            with ZipFile(jar) as z:
                if 'fabric.mod.json' not in z.namelist():continue
                identity=json.loads(z.read('fabric.mod.json'))['id']
        except (KeyError,json.JSONDecodeError):continue
        if identity in required:shutil.copy2(jar,mods/jar.name);copied.add(identity)
    assert copied==required
    shutil.copytree(a.fixture,game/'saves/port-qa',ignore=shutil.ignore_patterns('session.lock'))
    (game/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:5\nsimulationDistance:5\nsoundCategory_master:0.0\nlang:zh_cn\n','utf8')
    temp=game/'temp';temp.mkdir()
    command=[str(a.java_home/'bin/java.exe'),'-Xmx4G','--enable-native-access=ALL-UNNAMED','-Dfile.encoding=UTF-8',
             '-Dfabric.development=false',f'-Dgoogology.qa.voxy={str(a.voxy).lower()}',f'-Djava.io.tmpdir={temp}',f'-Djna.tmpdir={temp}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}',
             '-cp',';'.join(map(str,classpath)),metadata['mainClass'],'--username','GuogaologyQA','--version','26.2','--gameDir',str(game),
             '--assetsDir',str(minecraft/'assets'),'--assetIndex',metadata['assetIndex']['id'],'--uuid','00000000000000000000000000000001',
             '--accessToken','0','--userType','legacy','--versionType','release','--width','1600','--height','1000']
    print('Hidden QA:',game,flush=True)
    with (game/'launch-output.log').open('w',encoding='utf8') as log:
        process=subprocess.Popen(command,cwd=game,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        try:code=process.wait(timeout=1100)
        except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=20);raise RuntimeError('Own QA process timed out')
    if code or not (game/'port-client-ok.txt').is_file():
        print((game/'launch-output.log').read_text('utf8',errors='replace')[-14000:]);raise RuntimeError(f'QA failed: {code}')
    print((game/'port-client-ok.txt').read_text(),flush=True)
    print('PRODUCTION_PORT_OK 26.2',flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--instance',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True)
    p.add_argument('--fixture',type=Path,required=True);p.add_argument('--label',required=True);p.add_argument('--voxy',action='store_true');run(p.parse_args())
