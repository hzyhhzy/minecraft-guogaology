"""Release-runtime comparison in new, hidden 1.21.1 QA instances only.

Install the official NeoForge client into build/connector-investigation/client-base
first. Reads the ordinary launcher's libraries/assets, never settings or saves.
No test helper or loader dependency is added to the release artifact.
"""
import argparse, concurrent.futures, hashlib, json, shutil, subprocess, urllib.request, time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
INVESTIGATION=ROOT/'build/connector-investigation'
MINECRAFT=Path.home()/'AppData/Roaming/.minecraft'
RELEASE=next(line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))

def allowed(lib):
    rules=lib.get('rules',[]);value=not rules
    for rule in rules:
        os=rule.get('os',{})
        if os.get('name','windows')=='windows' and os.get('arch','x86_64') in ('x86_64','amd64'):
            value=rule['action']=='allow'
    return value and not any(x in lib['name'] for x in ('natives-windows-arm64','natives-windows-x86'))

def library_path(lib):
    path=lib.get('downloads',{}).get('artifact',{}).get('path')
    if path:return path
    group,artifact,version,*classifier=lib['name'].split(':')
    return f'{group.replace(".","/")}/{artifact}/{version}/{artifact}-{version}'+('-'+classifier[0] if classifier else '')+'.jar'

def ensure_library(base,lib):
    path=library_path(lib);target=base/'libraries'/path
    if not target.exists():
        target.parent.mkdir(parents=True,exist_ok=True);existing=MINECRAFT/'libraries'/path
        if existing.exists():shutil.copy2(existing,target)
        else:
            artifact=lib.get('downloads',{}).get('artifact',{})
            url=artifact.get('url') or lib.get('url','https://libraries.minecraft.net/')+path
            with urllib.request.urlopen(url,timeout=90) as response:data=response.read()
            if artifact.get('sha1'):assert hashlib.sha1(data).hexdigest()==artifact['sha1'],path
            target.write_bytes(data)
    return target

def run(a):
    base=(INVESTIGATION/'client-base').resolve();assert base.is_dir(),'Install official NeoForge into isolated client-base first'
    game=(ROOT/'build/runtime-qa'/a.label).resolve()
    assert game.parent==(ROOT/'build/runtime-qa').resolve() and not game.exists(),'Use a new single-directory label'
    mods=game/'mods';mods.mkdir(parents=True);(game/'config').mkdir()
    (game/'config/fml.toml').write_text('earlyWindowControl=false\n',encoding='utf8')
    (game/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:4\nsimulationDistance:5\nsoundCategory_master:0.0\nlang:en_us\nmaxFps:60\n',encoding='utf8')
    temp=game/'temp';temp.mkdir()
    version=a.mod_version
    production=ROOT/f'build/libs/guogaology-dimension-1.21.1-{version}.jar'
    helper=ROOT/f'build/qa/guogaology-connector-qa-{RELEASE}.jar'
    for jar in (production,helper):assert jar.is_file(),jar;shutil.copy2(jar,mods/jar.name)
    if a.loader=='connector':
        for name in (f'connector-2.0.0-beta.{a.beta}+1.21.1-full.jar','forgified-fabric-api-0.116.15+2.3.1+1.21.1.jar'):
            shutil.copy2(INVESTIGATION/name,mods/name)
        vanilla=json.loads((base/'versions/1.21.1/1.21.1.json').read_text('utf8'))
        metadata=json.loads((base/'versions/neoforge-21.1.248/neoforge-21.1.248.json').read_text('utf8'))
        libs={lib['name']:lib for lib in vanilla['libraries'] if allowed(lib)}
        libs.update({lib['name']:lib for lib in metadata['libraries'] if allowed(lib)})
        with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:classpath=list(pool.map(lambda lib:ensure_library(base,lib),libs.values()))
        mcjar=game/'neoforge-21.1.248.jar';shutil.copy2(base/'versions/1.21.1/1.21.1.jar',mcjar);classpath.append(mcjar)
        variables={'library_directory':str(base/'libraries'),'classpath_separator':';','version_name':'neoforge-21.1.248'}
        def expand(s):
            for k,v in variables.items():s=s.replace('${'+k+'}',v)
            return s
        loader_jvm=[expand(x) for x in metadata['arguments']['jvm']]
        loader_game=metadata['arguments']['game'];asset_index=vanilla['assetIndex']['id']
    else:
        instance=MINECRAFT/'versions/1.21.1-Fabric 0.19.5'
        metadata=json.loads((instance/(instance.name+'.json')).read_text('utf-8-sig'))
        classpath=[MINECRAFT/'libraries'/library_path(lib) for lib in metadata['libraries'] if allowed(lib)]
        classpath.append(instance/(instance.name+'.jar'));loader_jvm=[];loader_game=[];asset_index=metadata['assetIndex']['id']
        api=next((ROOT.parent/'.gradle-user/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.116.17+1.21.1').glob('*/fabric-api-0.116.17+1.21.1.jar'))
        shutil.copy2(api,mods/api.name)
    assert all(p.is_file() for p in classpath),'Missing classpath library'
    args=['-Xmx4G','-Dfile.encoding=UTF-8','-Djava.awt.headless=true','-Dfabric.development=false',
          f'-Djava.io.tmpdir={temp}',f'-Djna.tmpdir={temp}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}',
          f'-Dguogaology.qa.startupOnly={str(a.startup_only).lower()}',*loader_jvm,
          '-cp',';'.join(map(str,classpath)),metadata['mainClass'],
          '--username','GuogaologyQA','--version','neoforge-21.1.248' if a.loader=='connector' else '1.21.1',
          '--gameDir',str(game),'--assetsDir',str(MINECRAFT/'assets'),'--assetIndex',asset_index,
          '--uuid','00000000000000000000000000000001','--accessToken','0','--userType','legacy','--versionType','release',
          '--width','1280','--height','900',*loader_game]
    argfile=game/'launch.args';argfile.write_text('\n'.join('"'+str(s).replace('\\','/').replace('"','\\"')+'"' for s in args),encoding='utf8')
    (game/'environment.json').write_text(json.dumps({'loader':a.loader,'beta':a.beta,'mod_version':version,'jars':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in mods.glob('*.jar')}},indent=2),encoding='utf8')
    print('Hidden release QA:',game,flush=True)
    with (game/'launch-output.log').open('w',encoding='utf8') as output:
        process=subprocess.Popen([str(a.java_home/'bin/java.exe'),'@'+str(argfile)],cwd=game,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        deadline=time.monotonic()+900
        while process.poll() is None:
            time.sleep(.5)
            if a.expect_flight_crash:
                text=(game/'launch-output.log').read_text('utf8',errors='replace')
                if 'ArbitraryInjectionPointSubResolver' in text and 'StringIndexOutOfBoundsException' in text:
                    process.terminate();break
            if time.monotonic()>deadline:
                process.terminate();process.wait(timeout=30);raise RuntimeError('Own isolated QA process timed out')
        code=process.wait(timeout=30)
    log=(game/'launch-output.log').read_text('utf8',errors='replace')
    if a.expect_flight_crash:
        assert code and 'ArbitraryInjectionPointSubResolver' in log and 'StringIndexOutOfBoundsException' in log,'Reported baseline failure was not reproduced'
        print('BASELINE_FLIGHT_CRASH_REPRODUCED',flush=True);return
    if code or not (game/'connector-qa-ok.txt').exists():print(log[-18000:]);raise RuntimeError(f'QA failed exit={code}')
    print((game/'connector-qa-ok.txt').read_text(),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--label',required=True);p.add_argument('--loader',choices=('fabric','connector'),required=True)
    p.add_argument('--beta',choices=('16','17'),default='16');p.add_argument('--mod-version',default=RELEASE)
    p.add_argument('--java-home',type=Path,required=True,help='Java 21 JDK directory')
    p.add_argument('--startup-only',action='store_true');p.add_argument('--expect-flight-crash',action='store_true');run(p.parse_args())
