"""Replay first Numeric Stone pickup over real loopback TCP using an exact release.

Server and hidden/unlocked client use separate fresh directories. Normal fo263_2
provides read-only libraries/mods; its configuration/accounts/saves are never copied.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import time
from zipfile import ZipFile

from run_release_263_qa import library_classpath, normal_inventory, mod_metadata, sha256, java_executable

ROOT=Path(__file__).resolve().parents[1]
MC=Path.home()/'AppData/Roaming/.minecraft'
JAVA=MC/'runtime/java-runtime-epsilon'


def write(path,value):
    with path.open('x',encoding='utf-8') as output:json.dump(value,output,indent=2)


def copy_mod(source,directory):
    target=directory/'mods'/source.name
    assert not target.exists(),target
    shutil.copy2(source,target);assert sha256(source)==sha256(target)


def listeners(pid):
    return [row.split() for row in subprocess.check_output(['netstat','-ano','-p','tcp'],text=True).splitlines()
            if row.split() and row.split()[-1]==str(pid) and 'LISTENING' in row]


def run(a):
    assert re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_-]*',a.label),'Use one fresh directory label'
    instance=a.instance.resolve();assert instance.name=='fo263_2' and instance.parent.name=='versions'
    minecraft=instance.parent.parent;metadata=json.loads((instance/'fo263_2.json').read_text('utf-8-sig'))
    assert metadata['mainClass']=='net.fabricmc.loader.impl.launch.knot.KnotClient'
    release=a.release_jar.resolve();release_meta=mod_metadata(release);version=release_meta['version']
    assert release_meta['id']=='guogaology' and release_meta['depends']['minecraft']=='26.3'
    server_release=(a.server_release_jar or release).resolve();server_meta=mod_metadata(server_release)
    assert server_meta['id']=='guogaology' and server_meta['depends']['minecraft']=='26.3'
    server_version=re.fullmatch(r'([0-9]+)\.([0-9]+)\.([0-9]+)(?:[-+].*)?',server_meta['version'])
    assert server_version,'Server release must have a semantic three-part version'
    require_custom_recipes=tuple(map(int,server_version.groups()))>=(0,6,1)
    server_digest=sha256(server_release)
    digest=sha256(release);before=normal_inventory(instance)
    current=next(line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
    helper=(a.qa_jar or ROOT/f'ports/26.3/build/qa/guogaology-network-263-qa-{current}.jar').resolve()
    assert mod_metadata(helper)['id']=='guogaology-network-263-qa'
    base=(ROOT/'build/network-qa263').resolve();game=(base/a.label).resolve()
    assert game.parent==base and not game.exists(),'Never reuse a world or old result'
    server=game/'server';client=game/'client'
    for directory in (server,client):
        (directory/'mods').mkdir(parents=True);(directory/'temp').mkdir()
        write(directory/'network-qa-isolated.json',{'release':server_meta['version'] if directory==server else version,
            'sha256':server_digest if directory==server else digest,'role':directory.name})
    installed=[]
    for path in sorted((instance/'mods').glob('*.jar')):
        try:identity=mod_metadata(path,strict=False)['id']
        except (KeyError,json.JSONDecodeError):continue
        installed.append((identity,path))
    api=[path for identity,path in installed if identity=='fabric-api'];assert len(api)==1
    for directory in (server,client):
        for path in (server_release if directory==server else release,helper,api[0]):copy_mod(path,directory)
    if a.client_mods_full:
        for identity,path in installed:
            if identity in ('guogaology','fabric-api') or identity.startswith('guogaology-network-'):continue
            copy_mod(path,client)
    if a.client_jei_only or a.server_jei_only or a.jei_both_sides:
        for identity in ('jei','mezz_config'):
            matches=[path for mod_id,path in installed if mod_id==identity];assert len(matches)==1,identity
            if a.client_jei_only or a.jei_both_sides:copy_mod(matches[0],client)
            if a.server_jei_only or a.jei_both_sides:copy_mod(matches[0],server)
    cp=[]
    with ZipFile(ROOT.parent/'.gradle-user/caches/fabric-loom/26.3/minecraft-server.jar') as jar:
        import hashlib
        for kind in ('versions','libraries'):
            for line in jar.read(f'META-INF/{kind}.list').decode().splitlines():
                checksum,_,relative=line.split('\t');target=(server/kind/relative).resolve()
                assert target.is_relative_to(server)
                data=jar.read(f'META-INF/{kind}/{relative}');assert hashlib.sha256(data).hexdigest()==checksum
                target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);cp.append(target)
    for library in metadata['libraries']:
        group,artifact,v,*classifier=library['name'].split(':')
        if group not in ('net.fabricmc','org.ow2.asm'):continue
        relative=library.get('downloads',{}).get('artifact',{}).get('path')
        if not relative:
            suffix='-'+classifier[0] if classifier else ''
            relative=f'{group.replace(".","/")}/{artifact}/{v}/{artifact}-{v}{suffix}.jar'
        path=(minecraft/'libraries'/relative).resolve();assert path.is_file();cp.append(path)
    assert not any('client' in path.name or 'lwjgl' in path.name for path in cp)
    java=java_executable(a.java_home);client_cp=library_classpath(metadata,minecraft,instance/'fo263_2.jar')
    with socket.socket() as probe:probe.bind(('127.0.0.1',0));port=probe.getsockname()[1]
    (server/'eula.txt').write_text('eula=true\n',encoding='utf-8')
    (server/'server.properties').write_text(
        f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nenforce-secure-profile=false\n'
        'enable-status=false\nenable-query=false\nenable-rcon=false\nenable-jmx-monitoring=false\nmanagement-server-enabled=false\n'
        'white-list=false\nenforce-whitelist=false\nmax-players=1\ngamemode=survival\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\n'
        'pause-when-empty-seconds=0\nlevel-name=world\nlevel-seed=601263\n',encoding='utf-8')
    (client/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:3\nsimulationDistance:5\n'
        'textureFiltering:0\nmaxAnisotropyBit:2\nsoundCategory_master:0.0\nlang:zh_cn\n',encoding='utf-8')
    def jvm(directory):
        return [str(java),'-Xms512M','-Xmx3G','-XX:StackShadowPages=32','--enable-native-access=ALL-UNNAMED',
            '--add-exports','java.base/jdk.internal.misc=ALL-UNNAMED','-Dfile.encoding=UTF-8','-Dfabric.development=false',
            '-Dguogaology.qa.network263=true',f'-XX:ErrorFile={directory}/hs_err_pid%p.log',
            f'-Djava.io.tmpdir={directory}/temp',f'-Djna.tmpdir={directory}/temp']
    server_command=jvm(server)+['-Djava.awt.headless=true','-cp',';'.join(map(str,cp)),
        'net.fabricmc.loader.impl.launch.knot.KnotServer','nogui']
    client_command=jvm(client)+[f'-Dguogaology.qa.network263.address=127.0.0.1:{port}',
        f'-Dguogaology.qa.network263.serverJei={str(a.server_jei_only or a.jei_both_sides).lower()}',
        f'-Dguogaology.qa.network263.requireCustom={str(require_custom_recipes).lower()}',
        f'-Dguogaology.qa.network263.serverDir={server}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={client}/temp',
        '-cp',';'.join(map(str,client_cp)),metadata['mainClass'],'--username','GuogaologyQA','--version','26.3',
        '--gameDir',str(client),'--assetsDir',str(minecraft/'assets'),'--assetIndex',metadata['assetIndex']['id'],
        '--uuid','00000000000000000000000000000001','--accessToken','0','--userType','legacy','--versionType','release',
        '--width','1400','--height','900']
    report={'version':version,'productionSha256':digest,'bind':'127.0.0.1','port':port,'clientModsFull':a.client_mods_full,
        'serverProductionVersion':server_meta['version'],'serverProductionSha256':server_digest,
        'fullRecipeSyncExpected':require_custom_recipes,
        'jeiMode':'both' if a.jei_both_sides else 'client' if a.client_jei_only else 'server' if a.server_jei_only else 'none',
        'serverMods':{path.name:sha256(path) for path in (server/'mods').glob('*.jar')},
        'clientMods':{path.name:sha256(path) for path in (client/'mods').glob('*.jar')}}
    server_process=client_process=None
    environment=os.environ.copy()
    for key in ('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'):environment.pop(key,None)
    def start(command,directory,log):
        env=environment.copy();env.update(TEMP=str(directory/'temp'),TMP=str(directory/'temp'))
        return subprocess.Popen(command,cwd=directory,env=env,stdin=subprocess.PIPE,stdout=log,
            stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    print(f'Network263 {version} loopback-only {port}; full client mods={a.client_mods_full}',flush=True)
    try:
        with (server/'server-output.log').open('x',encoding='utf-8') as server_log,(client/'client-output.log').open('x',encoding='utf-8') as client_log:
            server_process=start(server_command,server,server_log);report['serverPid']=server_process.pid;write(game/'run-start.json',report)
            deadline=time.monotonic()+240
            while not listeners(server_process.pid):
                assert server_process.poll() is None,'Dedicated server exited before listening'
                assert time.monotonic()<deadline,'Dedicated startup timed out';time.sleep(.25)
            # Socket bind precedes complete world startup. Wait for the native ready marker.
            while ')! For help, type "help"' not in (server/'server-output.log').read_text('utf-8',errors='replace'):
                assert server_process.poll() is None,'Dedicated server exited during startup'
                assert time.monotonic()<deadline,'Dedicated world startup timed out';time.sleep(.25)
            listener_deadline=time.monotonic()+10
            rows=listeners(server_process.pid)
            while not rows and time.monotonic()<listener_deadline:
                time.sleep(.25);rows=listeners(server_process.pid)
            assert rows and all(row[1]==f'127.0.0.1:{port}' for row in rows),rows
            report['listeners']=rows
            client_process=start(client_command,client,client_log);report['clientPid']=client_process.pid
            write(game/'run-connected.json',report);deadline=time.monotonic()+660;requested=False
            while client_process.poll() is None:
                assert server_process.poll() is None,'Dedicated server exited during replay'
                if (server/'network-server-failed.json').exists():raise RuntimeError((server/'network-server-failed.json').read_text())
                if not requested and (client/'network-client-ready.json').exists() and (server/'network-server-before.json').exists():
                    # JEI's fallback runs asynchronously after login. Its client-only
                    # control must reach that precise path before the first pickup.
                    fallback=re.search(r'Loaded\s+[0-9]+\s+vanilla recipes',(client/'client-output.log').read_text('utf-8',errors='replace'))
                    if not a.client_jei_only or require_custom_recipes or fallback:
                        report['jeiVanillaRecipeFallbackObserved']=bool(fallback)
                        server_before=json.loads((server/'network-server-before.json').read_text())
                        client_before=json.loads((client/'network-client-ready.json').read_text())
                        report['staticTagsBeforeMatchServer']=server_before['staticTagFingerprints']==client_before['staticTagFingerprints']
                        (server/'network-pickup-request.txt').write_text('one actual first pickup\n',encoding='utf-8');requested=True
                assert time.monotonic()<deadline,'Hidden TCP client timed out';time.sleep(.25)
            report['clientExit']=client_process.returncode;report['pickupRequested']=requested
            server_process.stdin.write(b'stop\n');server_process.stdin.flush()
            report['serverExit']=server_process.wait(timeout=90)
        with socket.socket() as probe:
            probe.settimeout(1);report['portClosedAfterShutdown']=probe.connect_ex(('127.0.0.1',port))!=0
        assert report['serverExit']==0 and report['portClosedAfterShutdown'],'Server must shut down cleanly'
        result_path=client/'network-client-result.json'
        assert result_path.is_file(),f'No client result; see {client}/client-output.log'
        result=json.loads(result_path.read_text());report['clientResult']=result
        report['success']=report['clientExit']==0 and result.get('success') is True
        write(game/'network-result.json',report)
        print(json.dumps(report,ensure_ascii=False,indent=2),flush=True)
        assert report['success'],f'Native TCP recipe replay rejected; evidence in {game}'
        assert report['staticTagsBeforeMatchServer'],'Client/server whole block+item tag fingerprints differ'
        assert all(result.get(key) is True for key in ('hidden','notFocusable','mouseUnlocked','realTcp'))
        jei_expected=a.client_jei_only or a.jei_both_sides or a.client_mods_full
        assert result['jei']['installed'] is jei_expected,'Unexpected client JEI runtime presence'
        if jei_expected:
            assert result['jei']['checked'] and result['jei']['actualRecipeScreen'].startswith('mezz.jei.')
            expected_checks=6 if require_custom_recipes else 4 if a.jei_both_sides else 3
            assert len(result['jei']['recipes'])==expected_checks,'Missing real JEI recipe/input/output checks'
        assert (client/'screenshots'/result['screenshot']).read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
        print('NETWORK263_TCP_PICKUP_OK',flush=True)
    finally:
        if client_process is not None and client_process.poll() is None:client_process.terminate();client_process.wait(timeout=20)
        if server_process is not None and server_process.poll() is None:
            try:server_process.stdin.write(b'stop\n');server_process.stdin.flush();server_process.wait(timeout=60)
            except (OSError,subprocess.TimeoutExpired):server_process.terminate();server_process.wait(timeout=20)
        unchanged=normal_inventory(instance)==before;write(game/'normal-instance-unchanged.json',{'unchanged':unchanged})
        assert unchanged,'Normal instance metadata/mods changed';assert sha256(release)==digest,'Selected client release changed'
        assert sha256(server_release)==server_digest,'Selected server release changed'


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--release-jar',type=Path,required=True)
    parser.add_argument('--server-release-jar',type=Path,help='Optional distinct exact server release, proving a client-only update against an older server')
    parser.add_argument('--label',required=True)
    parser.add_argument('--qa-jar',type=Path)
    parser.add_argument('--java-home',type=Path,default=JAVA)
    parser.add_argument('--instance',type=Path,default=MC/'versions/fo263_2')
    mods=parser.add_mutually_exclusive_group()
    mods.add_argument('--client-mods-full',action='store_true',help='Copy installed client mods only, replacing main Mod with exact selected release; never copy normal config/accounts/saves')
    mods.add_argument('--client-jei-only',action='store_true',help='Single-variable client control: add installed JEI and its MezzConfig dependency only')
    mods.add_argument('--server-jei-only',action='store_true',help='Server-only JEI control; hidden client deliberately has no JEI')
    mods.add_argument('--jei-both-sides',action='store_true',help='JEI server-support control: install the same JEI and MezzConfig on both isolated sides')
    run(parser.parse_args())
