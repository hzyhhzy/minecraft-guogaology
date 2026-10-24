"""Fresh hidden26.3/26.2 compact equipment tooltip checks.

Reads only normal instance metadata/libraries/selected Mod files. Never copies
accounts, settings, resourcepacks or saves. All staged changes stay under build/.
"""
from pathlib import Path
import argparse, json, os, re, shutil, subprocess
from zipfile import ZipFile
import run_release_263_qa as base

ROOT=base.ROOT


def snapshot(instance):
    paths=[instance/(instance.name+'.json'),instance/(instance.name+'.jar')]
    # Do not open normal configuration files: a running normal game may hold
    # exclusive cache handles. QA generates its own settings under build/.
    paths+=sorted(p for p in (instance/'mods').rglob('*') if p.is_file())
    return {str(p.relative_to(instance)):base.sha256(p) for p in paths if p.is_file()}


def run(args):
    target=args.minecraft
    instance=args.instance.resolve()
    expected='fo263_2' if target=='26.3' else 'fo262'
    base.require(instance.name==expected and instance.parent.name=='versions','Read-only exact normal installation required')
    base.require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]*',args.label) is not None,'One safe disposable directory label required')
    base.require(not args.label.endswith(('.', ' ')) and args.label.upper().split('.')[0] not in {'CON','PRN','AUX','NUL',*(f'COM{i}' for i in range(1,10)),*(f'LPT{i}' for i in range(1,10))},'Invalid Windows directory label')
    metadata=base.read_json(instance/(expected+'.json'))
    minecraft=instance.parent.parent
    client=instance/(expected+'.jar')
    with ZipFile(client) as jar:base.require(json.loads(jar.read('version.json'))['id']==target,'Actual installed native client target mismatch')
    release=re.search(r'^mod_version\s*=\s*(\S+)\s*$',(ROOT/'gradle.properties').read_text('utf-8-sig'),re.M)[1]
    main=ROOT/f'ports/{target}/build/libs/guogaology-dimension-{target}-{release}.jar'
    qa_id='guogaology-release-263-qa' if target=='26.3' else 'guogaology-merge-qa'
    helper=ROOT/f'ports/{target}/build/qa/{qa_id}-{release}.jar'
    entry=('dev.guogaology.releaseqa263.' if target=='26.3' else 'dev.guogaology.mergeqa.')+'Tooltip0615Runtime'
    for path,identity in ((main,'guogaology'),(helper,qa_id)):
        info=base.mod_metadata(path)
        base.require(info['id']==identity and info['depends']['minecraft']==target,'Exact current target artifact identity required: '+str(path))
        if path==main:
            base.require(info['version']==release,'Exact production release version required: '+str(path))
        else:
            # The26.2 QA Mod deliberately keeps its independent1.0.0 metadata
            # version. The release-specific filename and class are authoritative
            # helper provenance; never rewrite that unrelated metadata version.
            base.require(path.name==f'{qa_id}-{release}.jar','Exact current QA artifact filename required')
            with ZipFile(path) as archive:
                base.require(entry.replace('.','/')+'.class' in archive.namelist(),'Current Tooltip0615 helper class required')
    before=snapshot(instance)
    runtime=(ROOT/('build/runtime-qa263' if target=='26.3' else 'build/runtime-qa')).resolve()
    game=(runtime/args.label).resolve()
    base.require(game.parent==runtime and not game.exists(),'Fresh direct-child disposable directory required')
    game.mkdir(parents=True)
    mods=game/'mods';mods.mkdir();temp=game/'temp';temp.mkdir();(game/'config').mkdir()
    inputs={'minecraft':target,'release':release,'normalReadOnlyInstance':str(instance),'productionSha256':base.sha256(main),'helperSourceSha256':base.sha256(helper),'helperMetadataVersion':base.mod_metadata(helper)['version'],'language':args.language,'focused':args.focused,'mods':[]}
    try:
        selected=base.selected_mods(instance,args)
        for source,info in [(main,base.mod_metadata(main)),(helper,base.mod_metadata(helper)),*selected]:
            dst=mods/source.name
            base.require(not dst.exists(),'Unique staged artifact names')
            if source==helper:
                info=json.loads(json.dumps(info));info['entrypoints']={'client':[entry]}
                with ZipFile(source) as src,ZipFile(dst,'x') as out:
                    base.require(entry.replace('.','/')+'.class' in src.namelist(),'Build the new native helper before running')
                    for item in src.infolist():out.writestr(item,json.dumps(info,ensure_ascii=False,indent=2).encode() if item.filename=='fabric.mod.json' else src.read(item.filename))
            else:shutil.copy2(source,dst);base.require(base.sha256(dst)==base.sha256(source),'Exact staged input hash')
            inputs['mods'].append({'id':info['id'],'metadataVersion':info['version'],'file':dst.name,'sha256':base.sha256(dst),'sourceSha256':base.sha256(source)})
        base.write_json_new(game/'tooltip0615-inputs.json',inputs)
        if target=='26.3':base.write_json_new(game/'qa263-isolated.json',{'mode':'tooltip0615','minecraft':target,'release':release})
        texture='textureFiltering:1\nmaxAnisotropyBit:1\n' if target=='26.3' else ''
        (game/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:4\nsimulationDistance:4\nsoundCategory_master:0.0\nlang:'+args.language+'\nguiScale:3\n'+texture,encoding='utf-8')
        java=base.java_executable(args.java_home)
        command=[str(java),'-Xmx4G','-XX:StackShadowPages=32','--enable-native-access=ALL-UNNAMED','--add-exports','java.base/jdk.internal.misc=ALL-UNNAMED','-Dfabric.development=false','-Dfile.encoding=UTF-8','-Dguogaology.tooltip0615=true',f'-Dguogaology.tooltip0615.minecraft={target}',f'-Dguogaology.tooltip0615.release={release}',f'-Dguogaology.tooltip0615.focused={str(args.focused).lower()}',f'-Djava.io.tmpdir={temp}',f'-Djna.tmpdir={temp}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}','-cp',';'.join(map(str,base.library_classpath(metadata,minecraft,client))),metadata['mainClass'],'--username','GuogaologyQA','--version',target,'--gameDir',str(game),'--assetsDir',str(minecraft/'assets'),'--assetIndex',metadata['assetIndex']['id'],'--uuid','00000000000000000000000000000001','--accessToken','0','--userType','legacy','--versionType','release','--width','1600','--height','1000']
        base.write_json_new(game/'staging.json',{'command':command,'inputs':inputs})
        if args.prepare_only:print('TOOLTIP0615_STAGED '+str(game));return
        env=os.environ.copy()
        for key in ('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'):env.pop(key,None)
        env.update(TEMP=str(temp),TMP=str(temp))
        with (game/'launch-output.log').open('x',encoding='utf-8') as output:
            proc=subprocess.Popen(command,cwd=game,env=env,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
            base.write_json_new(game/'qa-process.json',{'pid':proc.pid,'gameDirectory':str(game),'minecraft':target,'mode':'tooltip0615'})
            try:code=proc.wait(timeout=args.timeout)
            except subprocess.TimeoutExpired:
                proc.terminate()
                try:proc.wait(timeout=15)
                except subprocess.TimeoutExpired:proc.kill();proc.wait(timeout=15)
                raise RuntimeError('Own isolated native tooltip JVM timed out')
        base.require(code==0 and (game/'tooltip0615-ok.txt').is_file(),'Native tooltip QA failed: '+str(game/'launch-output.log'))
        result=base.read_json(game/'tooltip0615-results.json')
        base.require(result['success'] is True and result['minecraft']==target and result['productionVersion']==release,'Native runtime identity/result mismatch')
        base.require(result['inputs']==inputs,'Native recorded provenance mismatch')
        base.require(result['language']==args.language and result['equipmentTypes']==34 and result['equipmentCases']==109,'Complete native equipment and edge-case coverage required')
        base.require(len(result['nativeVanillaControls'])==6 and result['deepComponentCodecsUnchanged'],'Native vanilla attributes and deep component conservation required')
        base.require(len(result['screenshots'])==(1 if args.focused else 2),'Bounded native tooltip captures required')
        for name in result['screenshots']:base.verify_png(game,name)
        for row in inputs['mods']:base.require(base.sha256(mods/row['file'])==row['sha256'],'Staged artifact changed during runtime')
        base.write_json_new(game/'tooltip0615-runner-ok.json',{'success':True,'minecraft':target,'release':release,'checks':result['checks'],'productionSha256':inputs['productionSha256'],'language':args.language,'equipmentTypes':34,'equipmentCases':result['equipmentCases'],'vanillaControls':6,'screenshots':len(result['screenshots'])})
        print('TOOLTIP0615_NATIVE_OK '+str(game),flush=True)
    finally:
        after=snapshot(instance);changed=sorted(k for k in before.keys()|after.keys() if before.get(k)!=after.get(k))
        base.write_json_new(game/'normal-instance-after.json',{'unchanged':not changed,'changedEntries':changed,'protectedFiles':len(before),'scope':'Normal mods/metadata SHA256; normal configuration, accounts and saves are never copied or opened'})
        base.require(not changed,'Normal instance changed during isolated QA: '+str(changed))


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--minecraft',choices=('26.2','26.3'),required=True)
    p.add_argument('--instance',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--label',required=True)
    p.add_argument('--focused',action='store_true');p.add_argument('--language',choices=('zh_cn','en_us'),default='zh_cn');p.add_argument('--without-sodium',action='store_true');p.add_argument('--prepare-only',action='store_true');p.add_argument('--timeout',type=int,default=700)
    args=p.parse_args();args.voxy=False;run(args)


if __name__=='__main__':main()
