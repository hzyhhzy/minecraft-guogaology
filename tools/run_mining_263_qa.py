"""Run a selected26.3 release on an isolated, loopback-only dedicated mining probe."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import time
from zipfile import ZipFile

from run_release_263_qa import normal_inventory

ROOT=Path(__file__).resolve().parents[1]
MC=Path.home()/'AppData/Roaming/.minecraft'
JAVA=MC/'runtime/java-runtime-epsilon'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def read_mod(path):
    with ZipFile(path) as jar:
        return json.loads(jar.read('fabric.mod.json'),strict=False)


def verify_override_control(result):
    """Accept only the specific tag-override symptom, never a startup/fatal failure."""
    errors=[]
    if result.get('success') is not False or not result.get('failures'):
        errors.append('Native checks must reject the controlled broken tag')
    allowed=('native minecraft:mineable/axe contains ', 'actual survival destroy progress accelerates ')
    speed_prefixes=tuple('native '+material+' axe accelerates ' for material in
                         ('wooden','stone','copper','iron','diamond','netherite','golden'))
    for failure in result.get('failures',[]):
        if not failure.startswith(allowed+speed_prefixes):errors.append('Unrelated failure: '+failure)
    for kind,group in result.get('groups',{}).items():
        for target in group['blocks']:
            broken=kind=='axe' and target['id']!='minecraft:oak_log'
            if target['member'] is broken:errors.append('Unexpected tag membership: '+target['id']+' / '+kind)
            for material,values in target['tools'].items():
                expected=1 if broken else group['toolDefinitions'][material]['controlSpeed']
                if abs(values['stackSpeed']-expected)>.0001 or abs(values['playerSpeed']-expected)>.0001:
                    errors.append('Unexpected tool speed: '+target['id']+' / '+kind+' / '+material)
    axe=result.get('groups',{}).get('axe',{})
    rows={entry['id']:entry for entry in axe.get('blocks',[])}
    for identity in ('guogaology:laver_vein','guogaology:dread_log'):
        row=rows.get(identity)
        if row is None:errors.append('Missing symptom target: '+identity);continue
        for values in row['tools'].values():
            if abs(values['destroyProgress']-row['handProgress'])>.000001:
                errors.append('Broken-tag axe still accelerates player progress: '+identity)
    oak=rows.get('minecraft:oak_log')
    if oak is None or abs(oak['tools']['diamond']['stackSpeed']-8)>.0001:
        errors.append('Native oak/diamond control must remain speed8')
    if set(result.get('groups',{}))!={'axe','pickaxe','hoe','shovel'}:
        errors.append('All four tool-group audits are required')
    return {'verified':not errors,'errors':errors,'nativeFailureCount':len(result.get('failures',[])),
            'expectedRunnerExit':1,'symptom':'Only overridden axe targets lose tag membership and acceleration; oak remains normal'}


def main(a):
    assert re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_-]*',a.label),'Use one new directory label'
    instance=a.instance.resolve()
    assert instance.name=='fo263_2' and instance.parent.name=='versions'
    minecraft=instance.parent.parent
    release=a.release_jar.resolve();metadata=read_mod(release)
    assert metadata['id']=='guogaology' and metadata['depends']['minecraft']=='26.3'
    version=metadata['version'];release_hash=sha(release.read_bytes())
    current=next(line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
    helper=(a.qa_jar or ROOT/f'ports/26.3/build/qa/guogaology-mining-263-qa-{current}.jar').resolve()
    helper_meta=read_mod(helper)
    assert helper_meta['id']=='guogaology-mining-263-qa' and helper_meta['environment']=='server'
    assert not helper_meta.get('mixins')
    base=(ROOT/'build/mining-qa263').resolve();game=(base/a.label).resolve()
    assert game.parent==base and not game.exists(),'New label required; never reuse a world or result'
    before=normal_inventory(instance)
    (game/'mods').mkdir(parents=True);temp=game/'temp';temp.mkdir()
    with socket.socket() as probe:
        probe.bind(('127.0.0.1',0));port=probe.getsockname()[1]
    (game/'mining-qa-isolated.json').write_text(json.dumps({'release':version,'sha256':release_hash,'bind':'127.0.0.1','port':port}),encoding='utf-8')
    expected={'release':version,'groups':{}}
    with ZipFile(release) as jar:
        for kind in ('axe','pickaxe','hoe','shovel'):
            values=json.loads(jar.read(f'data/minecraft/tags/block/mineable/{kind}.json'))['values']
            assert values and all(isinstance(value,str) for value in values)
            expected['groups'][kind]=values
    (game/'mining-expectations.json').write_text(json.dumps(expected,indent=2),encoding='utf-8')
    cp=[]
    bundle=ROOT.parent/'.gradle-user/caches/fabric-loom/26.3/minecraft-server.jar'
    with ZipFile(bundle) as jar:
        for kind in ('versions','libraries'):
            for line in jar.read(f'META-INF/{kind}.list').decode().splitlines():
                checksum,_,relative=line.split('\t')
                path=(game/kind/relative).resolve();assert path.is_relative_to(game)
                data=jar.read(f'META-INF/{kind}/{relative}');assert sha(data)==checksum
                path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data);cp.append(path)
    installed=json.loads((instance/'fo263_2.json').read_text(encoding='utf-8-sig'))
    for lib in installed['libraries']:
        group,artifact,v,*classifier=lib['name'].split(':')
        if group not in ('net.fabricmc','org.ow2.asm'):continue
        relative=lib.get('downloads',{}).get('artifact',{}).get('path')
        if not relative:
            suffix='-'+classifier[0] if classifier else ''
            relative=f'{group.replace(".","/")}/{artifact}/{v}/{artifact}-{v}{suffix}.jar'
        path=(minecraft/'libraries'/relative).resolve();assert path.is_file();cp.append(path)
    assert not any('client' in path.name or 'lwjgl' in path.name for path in cp)
    api=[]
    for path in (instance/'mods').glob('*.jar'):
        try:
            if read_mod(path).get('id')=='fabric-api':api.append(path)
        except (KeyError,json.JSONDecodeError):pass
    assert len(api)==1,'Matching installed Fabric API required'
    for path in (release,helper,*api):
        destination=game/'mods'/path.name;shutil.copy2(path,destination)
        assert sha(path.read_bytes())==sha(destination.read_bytes())
    (game/'eula.txt').write_text('eula=true\n',encoding='utf-8')
    (game/'server.properties').write_text(
        f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nenforce-secure-profile=false\n'
        'enable-status=false\nenable-query=false\nenable-rcon=false\nenable-jmx-monitoring=false\n'
        'management-server-enabled=false\nwhite-list=false\nmax-players=1\ngamemode=survival\n'
        'view-distance=2\nsimulation-distance=2\nspawn-protection=0\npause-when-empty-seconds=0\n'
        'level-name=world\nlevel-seed=601263\nmotd=Isolated loopback mining QA\n',encoding='utf-8')
    if a.axe_tag_override_control:
        # The installed26.3 version.json declares data format121.0. This pack
        # exists only inside this new disposable world; production JARs stay intact.
        pack=game/'world/datapacks/axe-tag-override-control'
        tag=pack/'data/minecraft/tags/block/mineable/axe.json'
        tag.parent.mkdir(parents=True)
        (pack/'pack.mcmeta').write_text(json.dumps({'pack':{'description':'Isolated axe-tag override negative control',
                'min_format':[121,0],'max_format':[121,0]}}),encoding='utf-8')
        tag.write_text(json.dumps({'replace':True,'values':['minecraft:oak_log']}),encoding='utf-8')
    command=[str(a.java_home/'bin/java.exe'),'-Xms512M','-Xmx3G','-XX:StackShadowPages=32',
        '--enable-native-access=ALL-UNNAMED','--add-exports','java.base/jdk.internal.misc=ALL-UNNAMED',
        '-Djava.awt.headless=true','-Dfile.encoding=UTF-8','-Dfabric.development=false','-Dguogaology.qa.mining263=true',
        f'-XX:ErrorFile={game}/hs_err_pid%p.log',f'-Djava.io.tmpdir={temp}',f'-Djna.tmpdir={temp}',
        '-cp',';'.join(map(str,cp)),'net.fabricmc.loader.impl.launch.knot.KnotServer','nogui']
    result_path=game/'mining-server-result.json';log_path=game/'server-output.log'
    run={'productionVersion':version,'productionSha256':release_hash,'bind':'127.0.0.1','port':port,
         'serverClasspathEntries':len(cp),'clientClassesPresent':False,
         'axeTagOverrideControl':a.axe_tag_override_control,
         'mods':{path.name:sha(path.read_bytes()) for path in (game/'mods').glob('*.jar')}}
    process=None
    print(f'Mining dedicated QA: {version}, {game}, 127.0.0.1:{port}',flush=True)
    try:
        environment=os.environ.copy()
        for key in ('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'):environment.pop(key,None)
        environment.update(TEMP=str(temp),TMP=str(temp))
        with log_path.open('x',encoding='utf-8') as log:
            process=subprocess.Popen(command,cwd=game,env=environment,stdin=subprocess.PIPE,stdout=log,
                                     stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
            run['pid']=process.pid;(game/'run.json').write_text(json.dumps(run,indent=2),encoding='utf-8')
            deadline=time.monotonic()+240
            while not result_path.exists():
                if process.poll() is not None:raise RuntimeError(f'Dedicated server exited {process.returncode}; see {log_path}')
                if (game/'mining-qa-fatal.txt').exists():raise RuntimeError((game/'mining-qa-fatal.txt').read_text())
                if time.monotonic()>deadline:raise TimeoutError('Dedicated mining audit timed out')
                time.sleep(.25)
            listeners=[row.split() for row in subprocess.check_output(['netstat','-ano','-p','tcp'],text=True).splitlines()
                       if row.split() and row.split()[-1]==str(process.pid) and 'LISTENING' in row]
            assert listeners and all(row[1]==f'127.0.0.1:{port}' for row in listeners),listeners
            run['listeners']=listeners
            process.stdin.write(b'stop\n');process.stdin.flush()
            assert process.wait(timeout=90)==0,'Dedicated shutdown failed'
        with socket.socket() as probe:
            probe.settimeout(1);assert probe.connect_ex(('127.0.0.1',port))!=0,'Own test listener must close'
        run['portClosedAfterShutdown']=True
        result=json.loads(result_path.read_text(encoding='utf-8'))
        run['checks']=result['checks'];run['success']=result['success'];run['failureCount']=len(result['failures'])
        assert (game/'world/level.dat').is_file(),'Dedicated world was not saved'
        run['savedWorld']=True
        (game/'run.json').write_text(json.dumps(run,indent=2),encoding='utf-8')
        print(json.dumps(run,indent=2),flush=True)
        if a.axe_tag_override_control:
            control=verify_override_control(result)
            (game/'axe-tag-override-control-result.json').write_text(json.dumps(control,indent=2),encoding='utf-8')
            assert control['verified'], 'Negative control failed for an unrelated reason: '+str(control['errors'])
            print('MINING263_NEGATIVE_CONTROL_OK '+json.dumps(control),flush=True)
            # Keep native test failure semantics: this intentionally broken world
            # must never appear as a successful production regression run.
            raise RuntimeError(f'Expected controlled axe-tag regression confirmed (native failures={len(result["failures"])}); {result_path}')
        if not result['success']:
            print(json.dumps(result['failures'],ensure_ascii=False,indent=2),flush=True)
            raise RuntimeError(f'Native mining regression failed; complete tag/tool/player report: {result_path}')
        print('MINING263_DEDICATED_OK',flush=True)
    finally:
        if process is not None and process.poll() is None:
            try:process.stdin.write(b'stop\n');process.stdin.flush();process.wait(timeout=45)
            except (OSError,subprocess.TimeoutExpired):process.terminate();process.wait(timeout=20)
        unchanged=normal_inventory(instance)==before
        (game/'normal-instance-unchanged.json').write_text(json.dumps({'unchanged':unchanged}),encoding='utf-8')
        assert unchanged,'Normal fo263_2 metadata/mods changed'
        assert sha(release.read_bytes())==release_hash,'Selected production artifact changed during QA'


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--release-jar',type=Path,required=True)
    parser.add_argument('--label',required=True)
    parser.add_argument('--qa-jar',type=Path)
    parser.add_argument('--java-home',type=Path,default=JAVA)
    parser.add_argument('--instance',type=Path,default=MC/'versions/fo263_2')
    parser.add_argument('--axe-tag-override-control',action='store_true',
                        help='Disposable negative control: replace axe tag with oak only; expected runner exit1')
    main(parser.parse_args())
