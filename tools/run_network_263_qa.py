"""Replay first Numeric Stone or Branch Stone pickup over real loopback TCP.

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
from run_loot_263_qa import launcher_libraries, library_path

ROOT=Path(__file__).resolve().parents[1]
MC=Path.home()/'AppData/Roaming/.minecraft'
JAVA=MC/'runtime/java-runtime-epsilon'
BRANCH_ITEMS=('epsilon_symbol','omega_symbol','great_omega_symbol','zeta_symbol','psi_symbol','phi_symbol')
EXACT_061='1cf73cbd73b65bb04870f4b0c0dc5d772573a3d486196acaacc2c8b2e5b0ea30'
EXACT_060='a101bfd94cdf5cb27646bba2b10f881665bad89e180e6e59cf67121cdf83fe39'
TRUSTED_LEGACY={'0.6.0':EXACT_060,'0.6.1':EXACT_061,'0.6.7':'2d10fab0c7fae647064c4af9ab3530da252e290f685335759a79612bda371b98'}


def version_base(version):
    match=re.fullmatch(r'([0-9]+)\.([0-9]+)\.([0-9]+)(?:[a-z]+)?(?:[-+].*)?',version)
    assert match,'Release must have a three-part numeric base (optional letter suffix): '+version
    return tuple(map(int,match.groups()))


def hydra_expected(jar):
    """Read expected network slots from the exact authoritative server artifact."""
    with ZipFile(jar) as archive:
        recipe=json.loads(archive.read('data/guogaology/recipe/hydra_bud.json'))
        def token(value):
            if isinstance(value,str):return value
            return '#'+value['tag'] if 'tag' in value else value['item']
        slots=[token(recipe['key'][symbol]) for row in recipe['pattern'] for symbol in row]
        assert recipe['type']=='minecraft:crafting_shaped' and len(slots)==9
        assert recipe['result']['id']=='guogaology:hydra_bud' and recipe['result'].get('count',1)==1
        modern='#guogaology:ordinal_flowers' in slots
        expected_pattern=['ABA','CSC','ABA'] if modern else ['ABA','BSB','ABA']
        assert recipe['pattern']==expected_pattern,'Unreviewed Hydra shape in selected server artifact'
        def tag_members(key,seen=frozenset()):
            assert key not in seen,'Recursive item tag in server artifact'
            namespace,name=key.split(':',1)
            data=json.loads(archive.read(f'data/{namespace}/tags/item/{name}.json'))
            result=set()
            for value in data['values']:
                value=value if isinstance(value,str) else value['id']
                result.update(tag_members(value[1:],seen|{key}) if value.startswith('#') else [value])
            return result
        tags={value:sorted(tag_members(value[1:])) for value in set(slots) if value.startswith('#')}
        required=sorted({member for value in slots for member in (tags[value] if value.startswith('#') else [value])})
    return {'schema':'current065' if modern else 'legacy061','slots':slots,'tagMembers':tags,'requiredInputs':required,'serverSha256':sha256(jar)}


def write(path,value):
    with path.open('x',encoding='utf-8') as output:json.dump(value,output,indent=2)


def copy_mod(source,directory):
    target=directory/'mods'/source.name
    assert not target.exists(),target
    shutil.copy2(source,target);assert sha256(source)==sha256(target)


def listeners(pid):
    return [row.split() for row in subprocess.check_output(['netstat','-ano','-p','tcp'],text=True).splitlines()
            if row.split() and row.split()[-1]==str(pid) and 'LISTENING' in row]


def override_mod(path,identity):
    path=path.resolve();build=(ROOT/'build').resolve()
    assert path.is_relative_to(build) and path.is_file(),'Mod overrides must be existing build-only JARs'
    metadata=mod_metadata(path,strict=False)
    assert metadata.get('id')==identity,f'Wrong override Mod ID: {path}'
    dependency=metadata.get('depends',{}).get('minecraft')
    if dependency is not None:
        assert '26.3' in str(dependency) and not str(dependency).strip().startswith('<26.3'),'Override does not declare Minecraft26.3 support'
    else:
        # JEI72 itself omits a direct minecraft dependency. Its26.3 file and
        # explicit26.3 Fabric API dependency identify that supported artifact.
        assert '26.3' in path.name and '26.3' in str(metadata.get('depends',{}).get('fabric-api','')),'Override lacks both Minecraft26.3 and matching Fabric API target evidence'
    return path


def runtime_libraries(metadata,instance,args):
    """Share the build-only official Loader override with the isolated loot runner."""
    client_cp,loader_version,loader_override=launcher_libraries(metadata,instance,args)
    if loader_override:
        # The official client profile's seven common Loader/Mixin/ASM libraries
        # also replace every old Fabric/ASM entry in the dedicated-server path.
        server_loader=[Path(entry['path']) for entry in loader_override['libraries']]
    else:
        server_loader=[]
        for library in metadata['libraries']:
            if library['name'].split(':')[0] not in ('net.fabricmc','org.ow2.asm'):continue
            path=(instance.parent.parent/'libraries'/library_path(library)).resolve()
            assert path.is_file(),path
            server_loader.append(path)
    return client_cp,server_loader,loader_version,loader_override


def run(a):
    assert re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_-]*',a.label),'Use one fresh directory label'
    pickup=a.pickup_item or ('epsilon_symbol' if a.branch_pickup or a.candidate_regression else 'ordinal_stone')
    branch_control=a.branch_pickup or pickup in BRANCH_ITEMS
    if a.jei_placement:
        a.client_jei_only=a.jei_placement=='client';a.server_jei_only=a.jei_placement=='server';a.jei_both_sides=a.jei_placement=='both'
    if branch_control and not a.candidate_regression:
        assert not (a.client_mods_full or a.server_jei_only or a.jei_both_sides),'Branch control isolates client-only JEI'
        a.client_jei_only=True
    if a.jei_jar or a.mezz_config_jar:
        assert a.client_jei_only or a.server_jei_only or a.jei_both_sides,'Explicit JEI dependency overrides require a JEI mode'
    instance=a.instance.resolve();assert instance.name=='fo263_2' and instance.parent.name=='versions'
    minecraft=instance.parent.parent;metadata=json.loads((instance/'fo263_2.json').read_text('utf-8-sig'))
    assert metadata['mainClass']=='net.fabricmc.loader.impl.launch.knot.KnotClient'
    client_cp,server_loader,loader_version,loader_override=runtime_libraries(metadata,instance,a)
    release=a.release_jar.resolve();release_meta=mod_metadata(release);version=release_meta['version']
    assert release_meta['id']=='guogaology' and release_meta['depends']['minecraft']=='26.3'
    server_release=(a.server_release_jar or release).resolve();server_meta=mod_metadata(server_release)
    assert server_meta['id']=='guogaology' and server_meta['depends']['minecraft']=='26.3'
    require_custom_recipes=version_base(server_meta['version'])>=(0,6,1)
    server_digest=sha256(server_release)
    digest=sha256(release);before=normal_inventory(instance)
    if a.candidate_regression:
        assert not a.client_mods_full,'Candidate regression isolates the four explicit JEI placements'
        assert version in ('0.6.8','0.6.1a'),'Explicit candidate regression accepts068 or061a client artifacts'
        if server_meta['version']!=version:
            assert a.server_release_jar and server_digest==TRUSTED_LEGACY.get(server_meta['version']),'Mixed candidate controls require an explicit exact published060/061/067 server'
    elif branch_control:
        assert version=='0.6.1' and digest==EXACT_061,'Branch client must be the exact published0.6.1'
        assert (server_meta['version']=='0.6.1' and server_digest==EXACT_061) or (a.server_release_jar and server_meta['version']=='0.6.0' and server_digest==EXACT_060),'Use exact061 server by default; exact060 is only an explicit mixed-version control'
    if a.item_regression:
        assert a.candidate_regression and server_meta['version']==version and server_digest==digest,'Item regression requires the identical candidate artifact on both sides; legacy servers retain their original item behavior'
    current=next(line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
    helper=(a.qa_jar or ROOT/f'ports/26.3/build/qa/guogaology-network-263-qa-{current}.jar').resolve()
    assert mod_metadata(helper)['id']=='guogaology-network-263-qa'
    helper_digest=sha256(helper)
    base=(ROOT/'build/network-qa263').resolve();game=(base/a.label).resolve()
    assert game.parent==base and not game.exists(),'Never reuse a world or old result'
    server=game/'server';client=game/'client'
    hydra=hydra_expected(server_release)
    two_stages=a.candidate_regression and pickup in BRANCH_ITEMS
    for directory in (server,client):
        (directory/'mods').mkdir(parents=True);(directory/'temp').mkdir()
        write(directory/'network-qa-isolated.json',{'release':server_meta['version'] if directory==server else version,
            'sha256':server_digest if directory==server else digest,'role':directory.name,'pickupItem':'guogaology:'+pickup,'branchControl':branch_control,'candidateRegression':a.candidate_regression})
        write(directory/'network-hydra-expected.json',hydra)
    installed=[]
    for path in sorted((instance/'mods').glob('*.jar')):
        try:identity=mod_metadata(path,strict=False)['id']
        except (KeyError,json.JSONDecodeError):continue
        installed.append((identity,path))
    api=[override_mod(a.api_jar,'fabric-api')] if a.api_jar else [path for identity,path in installed if identity=='fabric-api']
    assert len(api)==1
    api_metadata=mod_metadata(api[0])
    for directory in (server,client):
        for path in (server_release if directory==server else release,helper,api[0]):copy_mod(path,directory)
    if a.client_mods_full:
        for identity,path in installed:
            if identity in ('guogaology','fabric-api') or identity.startswith('guogaology-network-'):continue
            copy_mod(path,client)
    if a.client_jei_only or a.server_jei_only or a.jei_both_sides:
        for identity in ('jei','mezz_config'):
            explicit=a.jei_jar if identity=='jei' else a.mezz_config_jar
            matches=[override_mod(explicit,identity)] if explicit else [path for mod_id,path in installed if mod_id==identity]
            assert len(matches)==1,identity
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
    cp.extend(server_loader)
    assert not any('client' in path.name or 'lwjgl' in path.name for path in cp)
    java=java_executable(a.java_home)
    with socket.socket() as probe:probe.bind(('127.0.0.1',0));port=probe.getsockname()[1]
    (server/'eula.txt').write_text('eula=true\n',encoding='utf-8')
    (server/'server.properties').write_text(
        f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nenforce-secure-profile=false\n'
        'enable-status=false\nenable-query=false\nenable-rcon=false\nenable-jmx-monitoring=false\nmanagement-server-enabled=false\n'
        'white-list=false\nenforce-whitelist=false\nmax-players=1\ngamemode=survival\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\n'
        'pause-when-empty-seconds=0\nlevel-name=world\nlevel-seed=601263\n',encoding='utf-8')
    filtering='textureFiltering:1\nmaxAnisotropyBit:1\n' if a.known_filter_workaround else 'textureFiltering:0\nmaxAnisotropyBit:2\n'
    (client/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:3\nsimulationDistance:5\n'
        +filtering+'soundCategory_master:0.0\nlang:zh_cn\n',encoding='utf-8')
    def jvm(directory):
        return [str(java),'-Xms512M','-Xmx3G','-XX:StackShadowPages=32','--enable-native-access=ALL-UNNAMED',
            '--add-exports','java.base/jdk.internal.misc=ALL-UNNAMED','-Dfile.encoding=UTF-8','-Dfabric.development=false',
            '-Dguogaology.qa.network263=true',f'-XX:ErrorFile={directory}/hs_err_pid%p.log',
            f'-Dguogaology.qa.network263.pickup=guogaology:{pickup}',
            f'-Dguogaology.qa.network263.branchControl={str(branch_control).lower()}',
            f'-Dguogaology.qa.network263.candidateRegression={str(a.candidate_regression).lower()}',
            f'-Dguogaology.qa.network263.itemRegression={a.item_regression or "off"}',
            f'-Dguogaology.qa.network263.expectedVersion={server_meta["version"] if directory==server else version}',
            f'-Djava.io.tmpdir={directory}/temp',f'-Djna.tmpdir={directory}/temp']
    server_command=jvm(server)+['-Djava.awt.headless=true','-cp',';'.join(map(str,cp)),
        'net.fabricmc.loader.impl.launch.knot.KnotServer','nogui']
    client_profile_args=loader_override['jvmArguments'] if loader_override else []
    client_command=jvm(client)+client_profile_args+[f'-Dguogaology.qa.network263.address=127.0.0.1:{port}',
        f'-Dguogaology.qa.network263.serverJei={str(a.server_jei_only or a.jei_both_sides).lower()}',
        f'-Dguogaology.qa.network263.requireCustom={str(require_custom_recipes).lower()}',
        f'-Dguogaology.qa.network263.serverDir={server}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={client}/temp',
        '-cp',';'.join(map(str,client_cp)),metadata['mainClass'],'--username','GuogaologyQA','--version','26.3',
        '--gameDir',str(client),'--assetsDir',str(minecraft/'assets'),'--assetIndex',metadata['assetIndex']['id'],
        '--uuid','00000000000000000000000000000001','--accessToken','0','--userType','legacy','--versionType','release',
        '--width','1400','--height','900']
    report={'version':version,'productionSha256':digest,'bind':'127.0.0.1','port':port,'clientModsFull':a.client_mods_full,
        'pickupItem':'guogaology:'+pickup,'branchControl':branch_control,'knownFilterWorkaround':a.known_filter_workaround,
        'candidateRegression':a.candidate_regression,'twoPickupStages':two_stages,'hydraRecipe':hydra,
        'itemRegressionMode':a.item_regression or 'off',
        'fallbackConfirmed':False,
        'mixedVersionControl':server_meta['version']!=version,
        'serverProductionVersion':server_meta['version'],'serverProductionSha256':server_digest,
        'fullRecipeSyncExpected':require_custom_recipes,
        'jeiMode':'both' if a.jei_both_sides else 'client' if a.client_jei_only else 'server' if a.server_jei_only else 'none',
        'serverMods':{path.name:sha256(path) for path in (server/'mods').glob('*.jar')},
        'clientMods':{path.name:sha256(path) for path in (client/'mods').glob('*.jar')}}
    def mod_inputs(directory):
        values=[]
        for path in sorted((directory/'mods').glob('*.jar')):
            data=mod_metadata(path,strict=False)
            values.append({'id':data['id'],'version':data['version'],'file':path.name,'sha256':sha256(path),'depends':data.get('depends',{})})
        return values
    report['clientModMetadata']=mod_inputs(client);report['serverModMetadata']=mod_inputs(server)
    report['loaderVersions']=['net.fabricmc:fabric-loader:'+loader_version]
    report['expectedLoaderVersion']=loader_version;report['expectedFabricApiVersion']=api_metadata['version']
    report['loaderOverride']=loader_override
    report['serverLoaderLibraries']=[{'path':str(path),'sha256':sha256(path)} for path in server_loader]
    report['instanceMetadataSha256']=sha256(instance/'fo263_2.json');report['minecraftClientSha256']=sha256(instance/'fo263_2.jar')
    write(game/'staging.json',{**report,'serverCommand':server_command,'clientCommand':client_command})
    server_process=client_process=None;native_verification_complete=False
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
            write(game/'run-connected.json',report);deadline=time.monotonic()+900;requested=set()
            while client_process.poll() is None:
                assert server_process.poll() is None,'Dedicated server exited during replay'
                if (server/'network-server-failed.json').exists():raise RuntimeError((server/'network-server-failed.json').read_text())
                for suffix in ('','-numeric') if two_stages else ('',):
                    if suffix in requested or not (client/f'network-client-ready{suffix}.json').exists() or not (server/f'network-server-before{suffix}.json').exists():continue
                    # JEI's fallback runs asynchronously after login. Its client-only
                    # control must reach that precise path before the first pickup.
                    fallback=re.search(r'Loaded\s+[0-9]+\s+vanilla recipes',(client/'client-output.log').read_text('utf-8',errors='replace'))
                    if not a.client_jei_only or require_custom_recipes or fallback:
                        report['jeiVanillaRecipeLogObserved']=bool(fallback)
                        server_before=json.loads((server/f'network-server-before{suffix}.json').read_text())
                        client_before=json.loads((client/f'network-client-ready{suffix}.json').read_text())
                        match=server_before['staticTagFingerprints']==client_before['staticTagFingerprints']
                        if not suffix:report['staticTagsBeforeMatchServer']=match
                        report.setdefault('stageTagFingerprintsMatchServer',{})[suffix or 'initial']=match
                        (server/f'network-pickup-request{suffix}.txt').write_text('one actual first pickup\n',encoding='utf-8');requested.add(suffix)
                assert time.monotonic()<deadline,'Hidden TCP client timed out';time.sleep(.25)
            report['clientExit']=client_process.returncode;report['pickupRequested']=bool(requested);report['pickupRequestedStages']=sorted(requested)
            server_process.stdin.write(b'stop\n');server_process.stdin.flush()
            report['serverExit']=server_process.wait(timeout=90)
        with socket.socket() as probe:
            probe.settimeout(1);report['portClosedAfterShutdown']=probe.connect_ex(('127.0.0.1',port))!=0
        assert report['serverExit']==0 and report['portClosedAfterShutdown'],'Server must shut down cleanly'
        result_path=client/'network-client-result.json'
        assert result_path.is_file(),f'No client result; see {client}/client-output.log'
        result=json.loads(result_path.read_text());report['clientResult']=result
        jei_expected=a.client_jei_only or a.jei_both_sides or a.client_mods_full
        def verify_phase(phase,suffix,phase_item):
            assert phase.get('success') is True,'Native TCP pickup failed: '+str(phase.get('error'))
            server_after=json.loads((server/f'network-server-after{suffix}.json').read_text())
            server_before=json.loads((server/f'network-server-before{suffix}.json').read_text())
            client_before=json.loads((client/f'network-client-ready{suffix}.json').read_text())
            fingerprints=server_before['staticTagFingerprints']
            assert all(fingerprints==observed['staticTagFingerprints'] for observed in (client_before,server_after,phase)),'Whole item/block tag fingerprints must remain equal across sides and before/after pickup'
            for observed in (phase,server_after):
                assert observed.get('loaderVersion')==loader_version,'Runtime Loader differs from the staged environment'
                assert observed.get('fabricApiVersion')==api_metadata['version'],'Runtime Fabric API differs from the staged environment'
            assert all(phase.get(key) is True for key in ('hidden','notFocusable','mouseUnlocked','realTcp'))
            assert phase['jei']['installed'] is jei_expected,'Unexpected client JEI runtime presence'
            server_jei_expected=a.server_jei_only or a.jei_both_sides
            assert server_after.get('jeiInstalled') is server_jei_expected,'Actual dedicated-server JEI placement differs from staging'
            if server_jei_expected:
                expected_server_jei=next(entry for entry in report['serverModMetadata'] if entry['id']=='jei')['version']
                assert server_after.get('jeiVersion')==expected_server_jei,'Actual server JEI version differs from staged artifact'
            if jei_expected:
                assert phase['jei']['checked'] and phase['jei']['actualRecipeScreen'].startswith('mezz.jei.')
                assert phase.get('jeiGuiSettleTicks',0)>=90 and phase.get('settledStaticTagFingerprints')==fingerprints,'Settled JEI GUI must preserve the complete native tag fingerprints'
                expected_checks=6 if require_custom_recipes else 4 if a.jei_both_sides else 3
                if phase_item in BRANCH_ITEMS and require_custom_recipes:expected_checks+=1
                assert len(phase['jei']['recipes'])==expected_checks,'Missing real JEI recipe/input/output checks'
                expected_jei=next(entry for entry in report['clientModMetadata'] if entry['id']=='jei')['version']
                assert phase.get('jeiVersion')==expected_jei,'Actual JEI runtime differs from staged artifact'
            assert (client/'screenshots'/phase['screenshot']).read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
            assert server_after.get('actualItemEntityPickedUp') is True and server_after.get('inventoryHasPickupItem') is True
            assert client_before.get('targetDisplaysAbsentBefore')==phase['expectedDisplayIds'],'Target displays were not absent before natural pickup'
            assert phase.get('pickupItem')=='guogaology:'+phase_item
            if phase_item in BRANCH_ITEMS:
                assert phase.get('hydraDisplay',{}).get('namedBranchSlots')==4,'Missing actual named-tag Hydra network display'
                assert phase['hydraDisplay']['schema']==hydra['schema'],'Actual display uses the wrong server recipe schema'
            if require_custom_recipes:
                assert phase['fabricRecipeSync']['recipeIds']==server_before['serverRecipeIds'],'Full Fabric recipe IDs differ from authoritative server'
        try:
            assert report['clientExit']==0 and result.get('success') is True,f'Native TCP recipe replay rejected; evidence in {game}'
            if two_stages:
                assert result.get('twoPickupsOneConnection') is True,'Candidate must complete both actual pickup stages on one TCP connection'
                verify_phase(result['branchPhase'],'',pickup);verify_phase(result,'-numeric','ordinal_stone')
                report['verifiedPickupStages']=2
                phases=[result['branchPhase'],result]
            else:
                verify_phase(result,'',pickup);report['verifiedPickupStages']=1
                phases=[result]
            if a.item_regression:
                items=result['itemRegression']
                assert items['mode']==a.item_regression
                native_server=json.loads((server/'item-regression-server.json').read_text('utf-8'))
                assert items['server']==native_server,'Final client evidence differs from the dedicated native item observations'
                assert native_server['actualServerPlayerPlacementAndDestruction'] is True and native_server['caseCount']==70
                assert len(native_server['cases'])==70 and len(native_server['unregisteredHangingVariantsNotTested'])==4
                native_client=items['client']
                assert native_client['actualNativeModelResolver'] is True and native_client['itemCount']==8 and native_client['contextsPerItem']==7 and len(native_client['models'])==56
                assert len(native_client['actualGroundEntities'])==8 and all(entry['actualRemoteItemEntityPresent'] for entry in native_client['actualGroundEntities'])
                assert len(native_client['actualSurvivalGrahamStackModels'])==7,'Actual dropped Graham stack must resolve every display context'
                for key in ('groundAndHeldScreenshot','inventoryScreenshot'):
                    assert (client/'screenshots'/items[key]).read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
                if a.item_regression=='verify':
                    assert native_server['expectedBehaviorMatches'] is True and native_client['expectedBehaviorMatches'] is True,'Collected native item regression defects'
                    graham=native_server['nativeGrahamBreak']
                    assert graham['actualSurvivalPlayerDestroy'] is True and graham['exactlyOneGrahamDrop'] is True and graham['actualItemEntityCount']==1
                    actual=next(entry for entry in native_client['actualGroundEntities'] if entry['item']=='guogaology:graham')
                    assert actual['nativeSurvivalDrop'] is True and actual['entityId']==graham['entityId'],'Graham scene must show the original survival-dropped entity'
                    assert native_client['automaticNativeSignEditorScreen'],'No real native sign editor was observed on the connected client'
                report['itemRegression']={'mode':a.item_regression,'serverExpectedMatches':native_server['expectedBehaviorMatches'],
                    'clientExpectedMatches':native_client['expectedBehaviorMatches'],'serverCases':70,'nativeModels':56}
            vanilla_ids={'minecraft:stick','minecraft:stone_pickaxe','minecraft:charcoal'}
            report['fallbackConfirmed']=bool(server_meta['version']=='0.6.0' and a.client_jei_only
                and report.get('jeiVanillaRecipeLogObserved') and all(phase['fabricRecipeSync']['count']==0
                and phase['jei'].get('checked') is True and vanilla_ids.issubset({entry['id'] for entry in phase['jei']['recipes']}) for phase in phases))
            report['success']=True
        except (AssertionError,KeyError,OSError) as error:
            report['success']=False;report['validationError']=str(error);write(game/'network-result.json',report);raise
        write(game/'network-result.json',report)
        if branch_control or a.candidate_regression:
            print(json.dumps({'success':True,'pickupItem':report['pickupItem'],'clientVersion':version,'serverVersion':server_meta['version'],
                'clientJei':next((entry for entry in report['clientModMetadata'] if entry['id']=='jei'),None),'placement':report['jeiMode'],
                'verifiedPickupStages':report['verifiedPickupStages'],'fullRecipeSyncExpected':require_custom_recipes,'outputDirectory':str(game)},ensure_ascii=False),flush=True)
        else:print(json.dumps(report,ensure_ascii=False,indent=2),flush=True)
        native_verification_complete=True
    finally:
        if client_process is not None and client_process.poll() is None:client_process.terminate();client_process.wait(timeout=20)
        if server_process is not None and server_process.poll() is None:
            try:server_process.stdin.write(b'stop\n');server_process.stdin.flush();server_process.wait(timeout=60)
            except (OSError,subprocess.TimeoutExpired):server_process.terminate();server_process.wait(timeout=20)
        unchanged=normal_inventory(instance)==before;write(game/'normal-instance-unchanged.json',{'unchanged':unchanged})
        assert unchanged,'Normal instance metadata/mods changed';assert sha256(release)==digest,'Selected client release changed'
        assert sha256(server_release)==server_digest,'Selected server release changed'
        assert sha256(helper)==helper_digest,'Selected QA helper changed'
        if native_verification_complete and report.get('success') is True:
            write(game/'network-verification-ok.json',{'runnerProtectionComplete':True,'nativeVerificationComplete':True,
                'normalInstanceUnchanged':True,'inputSha256':{'client':digest,'server':server_digest,'qa':helper_digest},
                'clientExit':report['clientExit'],'serverExit':report['serverExit'],'portClosedAfterShutdown':report['portClosedAfterShutdown'],
                'verifiedPickupStages':report['verifiedPickupStages'],'pickupItem':report['pickupItem'],
                'itemRegression':report.get('itemRegression',{'mode':'off'})})
            print('NETWORK263_TCP_PICKUP_OK',flush=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--release-jar',type=Path,required=True)
    parser.add_argument('--server-release-jar',type=Path,help='Optional distinct exact server release, proving a client-only update against an older server')
    parser.add_argument('--label',required=True)
    parser.add_argument('--qa-jar',type=Path)
    mode=parser.add_mutually_exclusive_group()
    mode.add_argument('--branch-pickup',action='store_true',help='Strict exact061 client-only JEI baseline; one first pickup, no candidate binaries')
    mode.add_argument('--candidate-regression',action='store_true',help='Explicit068/061a candidate: branch then Numeric Stone on one connection; preserves strict old baseline mode')
    parser.add_argument('--pickup-item',choices=(*BRANCH_ITEMS,'ordinal_stone'),help='One real pickup per new world/player; run each six branches in its own fresh --label')
    parser.add_argument('--item-regression',choices=('observe','verify'),help='Opt-in identical-candidate native door/slab/sign and item-display checks; observe records all old defects, verify requires the fixes; ordinary JEI matrix defaults off')
    parser.add_argument('--jei-jar',type=Path,help='Explicit build-only JEI override; otherwise use installed31.14.0.72')
    parser.add_argument('--mezz-config-jar',type=Path,help='Optional build-only MezzConfig override; otherwise use installed dependency')
    parser.add_argument('--api-jar',type=Path,help='Optional build-only Fabric API override copied unchanged into both isolated sides')
    parser.add_argument('--loader-profile',type=Path,help='Optional build-only official26.3 Fabric Loader profile for both isolated sides')
    parser.add_argument('--loader-libraries',type=Path,help='Build-only Maven tree for --loader-profile; defaults to profile sibling libraries/')
    parser.add_argument('--known-filter-workaround',action='store_true',help='Use textureFiltering1/maxAnisotropyBit1 only in the isolated client options')
    parser.add_argument('--java-home',type=Path,default=JAVA)
    parser.add_argument('--instance',type=Path,default=MC/'versions/fo263_2')
    mods=parser.add_mutually_exclusive_group()
    mods.add_argument('--jei-placement',choices=('none','client','server','both'),help='Explicit four-placement candidate matrix; default noJEI in candidate mode')
    mods.add_argument('--client-mods-full',action='store_true',help='Copy installed client mods only, replacing main Mod with exact selected release; never copy normal config/accounts/saves')
    mods.add_argument('--client-jei-only',action='store_true',help='Single-variable client control: add installed JEI and its MezzConfig dependency only')
    mods.add_argument('--server-jei-only',action='store_true',help='Server-only JEI control; hidden client deliberately has no JEI')
    mods.add_argument('--jei-both-sides',action='store_true',help='JEI server-support control: install the same JEI and MezzConfig on both isolated sides')
    run(parser.parse_args())
