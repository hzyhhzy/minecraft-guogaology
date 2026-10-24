"""Hidden disposable0614 tree/floor native QA, including genuine0613 disk upgrade.

Baseline mode loads the exact old production JAR with current QA only; upgrade
copies only that verified disposable fixture. Normal accounts/saves are unused.
"""
from pathlib import Path
from zipfile import ZipFile
import argparse,json,os,re,shutil,subprocess
import run_release_263_qa as base
from run_visual_0613_qa import snapshot

ROOT=base.ROOT
WORLD='tree-lights-0614-fixture'


def run(a):
    target=a.minecraft;instance=a.instance.resolve();name='fo263_2' if target=='26.3' else 'fo262'
    base.require(instance.name==name and instance.parent.name=='versions','Exact read-only normal installation required')
    base.require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]*',a.label) and not a.label.endswith(('.', ' ')),'One safe new directory label required')
    base.require(a.label.upper().split('.')[0] not in {'CON','PRN','AUX','NUL',*(f'COM{i}' for i in range(1,10)),*(f'LPT{i}' for i in range(1,10))},'Invalid Windows directory label')
    current=re.search(r'^mod_version\s*=\s*(\S+)\s*$',(ROOT/'gradle.properties').read_text('utf-8-sig'),re.M)[1]
    photo=bool(a.floor_photo_from);mode='floor-photo' if photo else 'baseline0613' if a.baseline_0613 else 'upgrade' if a.upgrade_from else 'fresh'
    base.require(sum(bool(v) for v in (a.baseline_0613,a.upgrade_from,a.floor_photo_from))<=1,'Separate baseline, upgrade and supplementary photo launches')
    release='0.6.13' if a.baseline_0613 else current
    production=(a.baseline_jar or ROOT/f'build/releases/0.6.13/guogaology-dimension-{target}-0.6.13.jar') if a.baseline_0613 else ROOT/f'ports/{target}/build/libs/guogaology-dimension-{target}-{release}.jar'
    qa_id='guogaology-release-263-qa' if target=='26.3' else 'guogaology-merge-qa';helper=ROOT/f'ports/{target}/build/qa/{qa_id}-{current}.jar'
    entry=('dev.guogaology.releaseqa263.' if target=='26.3' else 'dev.guogaology.mergeqa.')+('Floor0614PhotoRuntime' if photo else 'TreeLights0614Runtime')
    for path,identity in ((production,'guogaology'),(helper,qa_id)):
        info=base.mod_metadata(path);base.require(info['id']==identity and info['depends']['minecraft']==target,'Exact native artifact identity')
        if path==production:base.require(info['version']==release,'Exact requested production release')
        else:
            with ZipFile(path) as jar:base.require(entry.replace('.','/')+'.class' in jar.namelist(),'Compile new TreeLights0614 native helper first')
    metadata=base.read_json(instance/(name+'.json'));client=instance/(name+'.jar');minecraft=instance.parent.parent
    with ZipFile(client) as jar:base.require(json.loads(jar.read('version.json'))['id']==target,'Exact native Minecraft client required')
    normal_before=snapshot(instance);runtime=(ROOT/('build/runtime-qa263' if target=='26.3' else 'build/runtime-qa')).resolve();game=(runtime/a.label).resolve()
    base.require(game.parent==runtime and not game.exists(),'Fresh direct-child QA directory required');game.mkdir(parents=True);mods=game/'mods';mods.mkdir();temp=game/'temp';temp.mkdir();(game/'config').mkdir()
    inputs={'mode':mode,'minecraft':target,'productionVersion':release,'productionSha256':base.sha256(production),'helperSourceSha256':base.sha256(helper),'helperMetadataVersion':base.mod_metadata(helper)['version'],'mods':[]}
    try:
        if photo:
            old=a.floor_photo_from.resolve();base.require(old.parent==runtime and old!=game,'Only own completed same-target fixture accepted')
            proof=base.read_json(old/'tree0614-runner-ok.json');source_result=base.read_json(old/'tree0614-results.json');previous=base.read_json(old/'tree0614-inputs.json')
            base.require(proof['success'] and proof['mode'] in ('fresh','upgrade') and proof['minecraft']==target and proof['release']==release,'Successful current full native proof required')
            base.require(source_result['newFullChunkFloor']['actualFullChunks']==16 and source_result['newFullChunkFloor']['columns']==4096 and previous['productionSha256']==inputs['productionSha256'],'Exact current production and native floor provenance')
            saved=old/'saves'/WORLD;base.require(saved.is_relative_to(old) and (saved/'level.dat').is_file(),'Only own current disposable world copied')
            observed={str(q.relative_to(saved)):base.sha256(q) for q in sorted(saved.rglob('*.mca'))}
            shutil.copytree(saved,game/'saves'/WORLD);shutil.copy2(old/'tree0614-results.json',game/'floor0614-source-results.json')
            copied={str(q.relative_to(game/'saves'/WORLD)):base.sha256(q) for q in sorted((game/'saves'/WORLD).rglob('*.mca'))};base.require(copied==observed,'Exact current fixture region snapshot copied')
            base.require(observed=={str(q.relative_to(saved)):base.sha256(q) for q in sorted(saved.rglob('*.mca'))},'Source current regions stable during exact copy')
            inputs.update(photoSource=str(old),sourceResultSha256=base.sha256(old/'tree0614-results.json'),sourceProofSha256=base.sha256(old/'tree0614-runner-ok.json'),sourceSnapshotRegionSha256=observed)
        if a.upgrade_from:
            old=a.upgrade_from.resolve();base.require(old.parent==runtime and old!=game,'Only own disposable same-target runtime fixture accepted')
            proof=base.read_json(old/'tree0614-runner-ok.json');base.require(proof['success'] and proof['mode']=='baseline0613' and proof['minecraft']==target,'Completed genuine0613 native baseline required')
            previous=base.read_json(old/'tree0614-inputs.json');base.require(previous['productionVersion']=='0.6.13' and previous['mode']=='baseline0613','Old producer identity')
            manifest=base.read_json(old/'floor0614-baseline.json');base.require(manifest['fixtureOnly'] and manifest['producerVersion']=='0.6.13','Known own native chunk manifest')
            saved=(old/'saves'/WORLD).resolve();base.require(saved.is_relative_to(old) and (saved/'level.dat').is_file(),'Only own disposable world is copied')
            current_regions={str(p.relative_to(saved)):base.sha256(p) for p in sorted(saved.rglob('*.mca'))}
            base.require(current_regions==proof['regionFilesAfterNativeStop'],'Old fixture region files unchanged since native baseline')
            shutil.copytree(saved,game/'saves'/WORLD);shutil.copy2(old/'floor0614-baseline.json',game/'floor0614-baseline.json')
            copied={str(p.relative_to(game/'saves'/WORLD)):base.sha256(p) for p in sorted((game/'saves'/WORLD).rglob('*.mca'))};base.require(copied==current_regions,'Exact region-file copy before native reopening')
            inputs.update(upgradeSource=str(old),oldProductionSha256=previous['productionSha256'],oldRegionFileSha256=current_regions,oldManifestSha256=base.sha256(old/'floor0614-baseline.json'))
        for source,info in [(production,base.mod_metadata(production)),(helper,base.mod_metadata(helper)),*base.selected_mods(instance,a)]:
            dst=mods/source.name;base.require(not dst.exists(),'Unique staged native input names')
            if source==helper:
                info=json.loads(json.dumps(info));info['entrypoints']={'client':[entry]};info['depends']['guogaology']='='+release
                with ZipFile(source) as src,ZipFile(dst,'x') as out:
                    for member in src.infolist():out.writestr(member,json.dumps(info,ensure_ascii=False,indent=2).encode() if member.filename=='fabric.mod.json' else src.read(member.filename))
            else:shutil.copy2(source,dst);base.require(base.sha256(dst)==base.sha256(source),'Exact input copied')
            inputs['mods'].append({'id':info['id'],'metadataVersion':info['version'],'file':dst.name,'sha256':base.sha256(dst),'sourceSha256':base.sha256(source)})
        base.write_json_new(game/'tree0614-inputs.json',inputs)
        texture='textureFiltering:1\nmaxAnisotropyBit:1\n' if target=='26.3' else ''
        (game/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:5\nsimulationDistance:5\nsoundCategory_master:0.0\nlang:'+a.language+'\nguiScale:3\n'+texture,encoding='utf-8')
        java=base.java_executable(a.java_home);command=[str(java),'-Xmx5G','-XX:StackShadowPages=32','--enable-native-access=ALL-UNNAMED','--add-exports','java.base/jdk.internal.misc=ALL-UNNAMED','-Dfabric.development=false','-Dfile.encoding=UTF-8','-Dguogaology.tree0614=true',f'-Dguogaology.tree0614.mode={mode}',f'-Dguogaology.tree0614.minecraft={target}',f'-Dguogaology.tree0614.release={release}',f'-Djava.io.tmpdir={temp}',f'-Djna.tmpdir={temp}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}','-cp',';'.join(map(str,base.library_classpath(metadata,minecraft,client))),metadata['mainClass'],'--username','GuogaologyQA','--version',target,'--gameDir',str(game),'--assetsDir',str(minecraft/'assets'),'--assetIndex',metadata['assetIndex']['id'],'--uuid','00000000000000000000000000000001','--accessToken','0','--userType','legacy','--versionType','release','--width','1600','--height','1000']
        base.write_json_new(game/'staging.json',{'inputs':inputs,'command':command});env=os.environ.copy()
        for key in ('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'):env.pop(key,None)
        env.update(TEMP=str(temp),TMP=str(temp))
        with (game/'launch-output.log').open('x',encoding='utf-8') as output:
            proc=subprocess.Popen(command,cwd=game,env=env,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW);base.write_json_new(game/'qa-process.json',{'pid':proc.pid,'gameDirectory':str(game),'minecraft':target,'mode':mode})
            try:code=proc.wait(timeout=a.timeout)
            except subprocess.TimeoutExpired:
                proc.terminate()
                try:proc.wait(timeout=15)
                except subprocess.TimeoutExpired:proc.kill();proc.wait(timeout=15)
                raise RuntimeError('Own isolated native tree/floor JVM timed out')
        base.require(code==0 and (game/'tree0614-ok.txt').is_file(),'Native tree/floor QA failed: '+str(game/'launch-output.log'))
        result=base.read_json(game/'tree0614-results.json');base.require(result['success'] and result['minecraft']==target and result['mode']==mode and result['productionVersion']==release,'Native result identity');base.require(result['inputs']==inputs,'Native provenance is exact');base.require(photo or (result['nativeSilk']['passed'] and result['nativeSilk']['ordinaryNativeSilkPreserved'] and result['nativeSilk']['baseline0613']==(mode=='baseline0613')),'Native Silk controls and phase identity required')
        proof={'success':True,'minecraft':target,'release':release,'mode':mode,'productionSha256':inputs['productionSha256'],'checks':result['checks'],'screenshots':result['screenshots']}
        if photo:
            base.require(result['sourceFullChunkFloorVerified'] and result['clientDimensionVerified'] and result['screenEmptyAtCapture'] and result['overlayEmptyAtCapture'] and result['clientFloorSynchronizedColumns']==1024 and len(result['screenshots'])==1,'Actual supplementary floor photo readiness proof required')
            base.verify_png(game,result['screenshots'][0])
        elif mode=='baseline0613':
            base.require(len(result['baseline']['chunks'])==8,'All eight genuinely old FULL chunks required');base.require(result['baselineWood']['passed'] and result['baselineWood']['baselineObservational'],'Native and old-wood baseline observations required')
            proof['regionFilesAfterNativeStop']={str(p.relative_to(game/'saves'/WORLD)):base.sha256(p) for p in sorted((game/'saves'/WORLD).rglob('*.mca'))}
        else:
            base.require(len(result['fixtures'])==6 and len(result['screenshots'])==13,'Complete bounded fixture/photo coverage');base.require(result['nativeWood']['passed'],'Native wood mechanism controls required')
            base.require(result['newFullChunkFloor']['actualFullChunks']==16 and result['newFullChunkFloor']['columns']==4096,'Native16FULLchunks/4096columns required')
            if mode=='upgrade':base.require(len(result['oldFullChunkReopen'])==8,'All old FULL chunks reopened and compared')
            for name in result['screenshots']:base.verify_png(game,name)
        for row in inputs['mods']:base.require(base.sha256(mods/row['file'])==row['sha256'],'Staged native artifact changed')
        base.write_json_new(game/'tree0614-runner-ok.json',proof);print('TREE0614_NATIVE_OK '+str(game),flush=True)
    finally:
        after=snapshot(instance);changed=sorted(k for k in normal_before.keys()|after.keys() if normal_before.get(k)!=after.get(k));base.write_json_new(game/'normal-instance-after.json',{'unchanged':not changed,'changedEntries':changed,'protectedFiles':len(normal_before),'scope':'Normal mods and launcher metadata/JAR SHA256; normal settings/config/saves neither opened nor copied'});base.require(not changed,'Normal instance changed during isolated native QA')


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--minecraft',choices=('26.2','26.3'),required=True);p.add_argument('--instance',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--label',required=True);p.add_argument('--baseline-0613',action='store_true');p.add_argument('--baseline-jar',type=Path);p.add_argument('--upgrade-from',type=Path);p.add_argument('--floor-photo-from',type=Path);p.add_argument('--without-sodium',action='store_true');p.add_argument('--language',choices=('zh_cn','en_us'),default='zh_cn');p.add_argument('--timeout',type=int,default=1300);a=p.parse_args();a.voxy=False;run(a)


if __name__=='__main__':main()
