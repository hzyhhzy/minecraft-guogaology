"""Narrow hidden native iBLP catalog/table QA and genuine0615 disposable save upgrade.

Normal instance is read only for launcher metadata, libraries and selected Mods.
Only a successful own baseline fixture can be copied; normal saves are not opened.
"""
from pathlib import Path
from zipfile import ZipFile
import argparse,json,os,re,shutil,subprocess
import run_release_263_qa as base
from run_tooltip_0615_qa import snapshot

ROOT=base.ROOT
WORLD='iblp-0616-fixture'
CATALOG='data/guogaology/notation/catalog.json'


def run(a):
    target=a.minecraft;instance=a.instance.resolve();name='fo263_2' if target=='26.3' else 'fo262'
    base.require(instance.name==name and instance.parent.name=='versions','Exact read-only normal instance required')
    base.require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]*',a.label) and not a.label.endswith(('.', ' ')),'One fresh safe child directory label required')
    base.require(a.label.upper().split('.')[0] not in {'CON','PRN','AUX','NUL',*(f'COM{i}' for i in range(1,10)),*(f'LPT{i}' for i in range(1,10))},'Invalid Windows directory label')
    base.require(not(a.baseline_0615 and a.upgrade_from),'Separate genuine-old baseline and new-upgrade launches')
    base.require(not(a.baseline_0615 or a.upgrade_from) or target=='26.3','Focused262 is fresh; genuine old disk upgrade primary263 only')
    current=re.search(r'^mod_version\s*=\s*(\S+)\s*$',(ROOT/'gradle.properties').read_text('utf-8-sig'),re.M)[1]
    mode='baseline0615' if a.baseline_0615 else 'upgrade' if a.upgrade_from else 'fresh'
    release='0.6.15' if a.baseline_0615 else current
    production=(a.baseline_jar or ROOT/f'build/releases/0.6.15/guogaology-dimension-{target}-0.6.15.jar') if a.baseline_0615 else ROOT/f'ports/{target}/build/libs/guogaology-dimension-{target}-{current}.jar'
    helper_id='guogaology-release-263-qa' if target=='26.3' else 'guogaology-merge-qa'
    helper=ROOT/f'ports/{target}/build/qa/{helper_id}-{current}.jar';entry=('dev.guogaology.releaseqa263.' if target=='26.3' else 'dev.guogaology.mergeqa.')+'Iblp0616Runtime'
    for path,identity in ((production,'guogaology'),(helper,helper_id)):
        meta=base.mod_metadata(path);base.require(meta['id']==identity and meta['depends']['minecraft']==target,'Exact artifact target/identity required')
        if path==production:base.require(meta['version']==release,'Exact requested producer release required')
        else:
            base.require(path.name==f'{helper_id}-{current}.jar','Current release helper filename required')
            with ZipFile(path) as z:base.require(entry.replace('.','/')+'.class' in z.namelist(),'Compile tiny new native helper first')
    with ZipFile(production) as z:archive_catalog=json.loads(z.read(CATALOG))
    selection=json.loads((ROOT/'src/main/resources'/CATALOG).read_text('utf-8'))
    base.require(len(selection['iblp'])==1000 and len(selection['bms'])==1070,'Final1000-pattern production selection required before QA')
    if not a.baseline_0615:base.require(archive_catalog==selection,'Actual staged production catalog matches exact selected source')
    else:base.require(len(archive_catalog['iblp'])==14661,'Genuine old14661-pattern producer required')
    retained=[str(p['sourceRow'])+'|'+p['expression'] for p in selection['iblp']]
    base.require(len(set(retained))==1000,'Retained whole expressions have unique provenance')
    metadata=base.read_json(instance/(name+'.json'));client=instance/(name+'.jar');minecraft=instance.parent.parent
    with ZipFile(client) as z:base.require(json.loads(z.read('version.json'))['id']==target,'Exact native Minecraft target required')
    before=snapshot(instance);runtime=(ROOT/('build/runtime-qa263' if target=='26.3' else 'build/runtime-qa')).resolve();game=(runtime/a.label).resolve()
    base.require(game.parent==runtime and not game.exists(),'Fresh direct-child disposable directory required');game.mkdir(parents=True);mods=game/'mods';mods.mkdir();temp=game/'temp';temp.mkdir();(game/'config').mkdir()
    inputs={'mode':mode,'minecraft':target,'productionVersion':release,'productionSha256':base.sha256(production),'helperSourceSha256':base.sha256(helper),'helperMetadataVersion':base.mod_metadata(helper)['version'],'expectedCatalogCount':len(archive_catalog['iblp']),'retainedExpressionKeys':retained,'mods':[]}
    try:
        if a.upgrade_from:
            old=a.upgrade_from.resolve();base.require(old.parent==runtime and old!=game,'Only own successful same-target baseline accepted')
            proof=base.read_json(old/'iblp0616-runner-ok.json');previous=base.read_json(old/'iblp0616-inputs.json')
            base.require(proof['success'] and proof['mode']=='baseline0615' and proof['minecraft']==target and proof['release']=='0.6.15','Exact old completed producer proof required')
            base.require(previous['retainedExpressionKeys']==retained,'Same selected expression identities for baseline and upgrade')
            manifest=base.read_json(old/'iblp0616-baseline.json');base.require(manifest['fixtureOnly'] and manifest['producerVersion']=='0.6.15' and len(manifest['chunks'])==2,'Own old native FULL/table manifest required')
            saved=(old/'saves'/WORLD).resolve();base.require(saved.is_relative_to(old) and (saved/'level.dat').is_file(),'Only own known fixture world is copied')
            regions={str(p.relative_to(saved)):base.sha256(p) for p in sorted(saved.rglob('*.mca'))};base.require(regions==proof['regionFilesAfterNativeStop'],'Old saved regions unchanged since native stop')
            shutil.copytree(saved,game/'saves'/WORLD);shutil.copy2(old/'iblp0616-baseline.json',game/'iblp0616-baseline.json')
            copied={str(p.relative_to(game/'saves'/WORLD)):base.sha256(p) for p in sorted((game/'saves'/WORLD).rglob('*.mca'))};base.require(copied==regions,'Exact baseline region copy before reopening')
            inputs.update(upgradeSource=str(old),oldProductionSha256=previous['productionSha256'],oldRegionFileSha256=regions,oldManifestSha256=base.sha256(old/'iblp0616-baseline.json'))
        for source,meta in [(production,base.mod_metadata(production)),(helper,base.mod_metadata(helper)),*base.selected_mods(instance,a)]:
            dst=mods/source.name;base.require(not dst.exists(),'Unique staged artifacts required')
            if source==helper:
                meta=json.loads(json.dumps(meta));meta['entrypoints']={'client':[entry]};meta['depends']['guogaology']='='+release
                with ZipFile(source) as src,ZipFile(dst,'x') as out:
                    for item in src.infolist():out.writestr(item,json.dumps(meta,ensure_ascii=False,indent=2).encode() if item.filename=='fabric.mod.json' else src.read(item.filename))
            else:shutil.copy2(source,dst);base.require(base.sha256(source)==base.sha256(dst),'Exact staged artifact copy')
            inputs['mods'].append({'id':meta['id'],'file':dst.name,'sha256':base.sha256(dst),'sourceSha256':base.sha256(source)})
        base.write_json_new(game/'iblp0616-inputs.json',inputs)
        texture='textureFiltering:1\nmaxAnisotropyBit:1\n' if target=='26.3' else ''
        (game/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:5\nsimulationDistance:4\nsoundCategory_master:0.0\nlang:zh_cn\nguiScale:3\n'+texture,encoding='utf-8')
        java=base.java_executable(a.java_home);command=[str(java),'-Xmx5G','-XX:StackShadowPages=32','--enable-native-access=ALL-UNNAMED','--add-exports','java.base/jdk.internal.misc=ALL-UNNAMED','-Dfabric.development=false','-Dfile.encoding=UTF-8','-Dguogaology.iblp0616=true',f'-Dguogaology.iblp0616.focused={str(a.focused).lower()}',f'-Djava.io.tmpdir={temp}',f'-Djna.tmpdir={temp}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}','-cp',';'.join(map(str,base.library_classpath(metadata,minecraft,client))),metadata['mainClass'],'--username','GuogaologyQA','--version',target,'--gameDir',str(game),'--assetsDir',str(minecraft/'assets'),'--assetIndex',metadata['assetIndex']['id'],'--uuid','00000000000000000000000000000001','--accessToken','0','--userType','legacy','--versionType','release','--width','1600','--height','1000']
        base.write_json_new(game/'staging.json',{'inputs':inputs,'command':command});env=os.environ.copy()
        for key in ('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'):env.pop(key,None)
        env.update(TEMP=str(temp),TMP=str(temp))
        if a.prepare_only:print('IBLP0616_STAGED '+str(game));return
        with (game/'launch-output.log').open('x',encoding='utf-8') as output:
            proc=subprocess.Popen(command,cwd=game,env=env,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW);base.write_json_new(game/'qa-process.json',{'pid':proc.pid,'gameDirectory':str(game),'minecraft':target,'mode':mode})
            try:code=proc.wait(timeout=a.timeout)
            except subprocess.TimeoutExpired:
                proc.terminate()
                try:proc.wait(timeout=15)
                except subprocess.TimeoutExpired:proc.kill();proc.wait(timeout=15)
                raise RuntimeError('Own isolated native iBLP JVM timed out')
        base.require(code==0 and (game/'iblp0616-ok.txt').is_file(),'Native iBLP QA failed: '+str(game/'launch-output.log'))
        result=base.read_json(game/'iblp0616-results.json');base.require(result['success'] and result['mode']==mode and result['minecraft']==target and result['productionVersion']==release and result['inputs']==inputs,'Exact native result/provenance required')
        base.require(result['catalog']['loadedPatterns']==inputs['expectedCatalogCount'] and len(result['catalog']['patternsBySize'])==69 and result['fixedPalacePatterns']==4,'All intended native patterns/sizes/palace controls required')
        proof={'success':True,'mode':mode,'minecraft':target,'release':release,'checks':result['checks'],'productionSha256':inputs['productionSha256'],'screenshots':result['screenshots']}
        if mode=='baseline0615':
            base.require(len(result['baseline']['chunks'])==2,'Two genuine old FULL chunks required')
            proof['regionFilesAfterNativeStop']={str(p.relative_to(game/'saves'/WORLD)):base.sha256(p) for p in sorted((game/'saves'/WORLD).rglob('*.mca'))}
        else:
            base.require(result['nativeTableOrientations']==16 and result['newNaturalFullTable']['actualFreshFullNativeGeneration'],'Native complete table painter/readers and actual fresh FULL generation required')
            if mode=='upgrade':base.require(len(result['oldFullChunksPreserved'])==2 and result['removedOldTablePhysicalAndMusicPreserved'],'All old native physical/music data preserved after real reopening')
        expected_photos=0 if a.focused or mode=='baseline0615' else 2 if mode=='upgrade' else 1
        base.require(len(result['screenshots'])==expected_photos,'Only bounded requested native photos required')
        for name in result['screenshots']:base.verify_png(game,name)
        for row in inputs['mods']:base.require(base.sha256(mods/row['file'])==row['sha256'],'Staged artifact unchanged')
        base.write_json_new(game/'iblp0616-runner-ok.json',proof);print('IBLP0616_NATIVE_OK '+str(game),flush=True)
    finally:
        after=snapshot(instance);changed=sorted(k for k in before.keys()|after.keys() if before.get(k)!=after.get(k));base.write_json_new(game/'normal-instance-after.json',{'unchanged':not changed,'changedEntries':changed,'protectedFiles':len(before),'scope':'Normal mods and launcher metadata/JAR SHA256; normal settings/config/saves neither opened nor copied'});base.require(not changed,'Normal instance changed during isolated QA')


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--minecraft',choices=('26.2','26.3'),required=True);p.add_argument('--instance',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--label',required=True);p.add_argument('--baseline-0615',action='store_true');p.add_argument('--baseline-jar',type=Path);p.add_argument('--upgrade-from',type=Path);p.add_argument('--focused',action='store_true');p.add_argument('--without-sodium',action='store_true');p.add_argument('--prepare-only',action='store_true');p.add_argument('--timeout',type=int,default=1200);a=p.parse_args();a.voxy=False;run(a)


if __name__=='__main__':main()
