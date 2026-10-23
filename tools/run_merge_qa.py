"""Run the release + QA helper only in a disposable hidden fo262 copy.

Reads libraries and Fabric/Sodium from the chosen installation. Never copies
accounts, normal settings or normal saves. Use a QA fixture or a fresh normal world.
"""
import argparse,hashlib,json,shutil,subprocess
from pathlib import Path
from zipfile import ZipFile
ROOT=Path(__file__).resolve().parents[1]

def item068_normal_snapshot(instance):
    """Read-only proof that the normal instance was never a QA game directory."""
    result={}
    for folder in ('mods','config'):
        for path in sorted((instance/folder).rglob('*')):
            if path.is_file():result[str(path.relative_to(instance))]=hashlib.sha256(path.read_bytes()).hexdigest()
    for path in sorted((instance/'saves').rglob('*')):
        if path.is_file():
            stat=path.stat();result[str(path.relative_to(instance))]=[stat.st_size,stat.st_mtime_ns]
    return result

def verify_item068_outputs(game):
    """Post-check original native evidence; Gson may omit JSON-null input fields."""
    proof=json.loads((game/'item068-normal-protection.json').read_text('utf8'))
    assert proof['unchanged'] and not proof['changedEntries'],'Normal instance changed during isolated item QA'
    result=json.loads((game/'item068-results.json').read_text('utf8'))
    expected=json.loads((game/'item068-run-input.json').read_text('utf8'))
    actual=result['inputs']
    # Only absent expected-null keys are equivalent. Every other field remains
    # mandatory, and unexpected fields (even null ones) are rejected.
    assert set(actual)<=set(expected),'Unexpected native QA provenance fields'
    assert all(key in actual or value is None for key,value in expected.items()),'Missing non-null native QA provenance field'
    assert all(actual[key]==expected[key] for key in actual),'Native QA input provenance changed'
    for role in ('production','qa'):
        path=game/'mods'/expected[role+'Jar']
        assert hashlib.sha256(path.read_bytes()).hexdigest()==expected[role+'Sha256'],f'Changed {role} artifact'
    assert result['status']=='PASS' and result['minecraft']=='26.2'
    assert result['nativeItemContextCount']==56 and len(result['signPlacementAndPersistence'])==4 and result['actualPlayerHarvest']['caseCount']==50
    assert len(result['screenshots'])==7 and all((game/'screenshots'/name).is_file() for name in result['screenshots']),'Missing completed item QA photos'
    return {'status':'PASS','checks':result['checks'],'nativeItemContexts':56,'harvestCases':50,'signCases':4,'screenshots':result['screenshots'],
            'productionSha256':expected['productionSha256'],'qaSha256':expected['qaSha256'],'protectedEntries':proof['protectedEntries'],
            'omittedNullInputFields':sorted(set(expected)-set(actual))}

def material065_inputs(production,helper,release,*,language,voxy,seed):
    records={}
    for role,path,expected_id in (('production',production,'guogaology'),('qa',helper,'guogaology-merge-qa')):
        with ZipFile(path) as archive:identity=json.loads(archive.read('fabric.mod.json'))
        assert identity['id']==expected_id,f'Wrong {role} QA input: {path}'
        if role=='production':assert identity['version']==release,f'Wrong production version: {path}'
        else:assert path.name==f'guogaology-merge-qa-{release}.jar',f'Wrong QA artifact: {path}'
        records[role+'Jar']=path.name
        records[role+'MetadataVersion']=identity['version']
        records[role+'Sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
    return {'mode':'material065','minecraft':'26.2','modVersion':release,**records,
            'freshWorld':True,'fixture':None,'language':language,'voxy':voxy,'seed':seed}

def allowed(lib):
    rules=lib.get('rules',[]);result=not rules
    for rule in rules:
        os=rule.get('os',{})
        if os.get('name','windows')!='windows' or os.get('arch','x86_64') not in ('x86_64','amd64'):continue
        result=rule['action']=='allow'
    return result and not any(x in lib['name'] for x in ('natives-windows-arm64','natives-windows-x86'))

def run(a):
    assert not (a.repair069 or a.repair0610) or (a.item068_only and a.audit068_focus and a.fresh_world), 'Repair069 requires fresh focused item/native observations'
    instance=a.instance.resolve();assert instance.name=='fo262','Runtime scope is fo262 only'
    metadata=json.loads((instance/'fo262.json').read_text('utf-8-sig'));minecraft=instance.parent.parent
    assert bool(a.fixture) != a.fresh_world,'Choose exactly one of --fixture or --fresh-world'
    assert not a.item068_only or not any(value for key,value in vars(a).items() if key.endswith('_only') and key!='item068_only'),'Item QA is a separate focused mode'
    assert not a.namespace_only or a.fresh_world,'Namespace QA must use --fresh-world'
    assert not a.chain_loot0501_only or a.fresh_world,'0.5.1 chain/loot QA must use --fresh-world'
    assert not a.jar_import_only or a.fresh_world,'JAR import QA must use --fresh-world'
    assert not a.core_resources_only or a.fresh_world,'Core resources QA must use --fresh-world'
    assert not a.crystal_ore_only or a.fresh_world,'Crystal ore QA must use --fresh-world'
    assert not a.balance057_only or a.fresh_world,'Balance0.5.7 QA must use --fresh-world'
    assert not a.lamps058_only or a.fresh_world,'Lamp0.5.8 QA must use --fresh-world'
    assert not a.laver_scenes_only or a.fresh_world,'Laver/material scene QA must use a disposable new world'
    assert not (a.advancements057_only or a.advancements510_only or a.advancements_baseline) or a.fresh_world,'Advancement QA must use a disposable new world'
    assert not a.astra512_only or a.fresh_world,'Astra QA must use a disposable new world'
    assert not a.core514_only or a.fresh_world,'Core0.5.14 QA must use a disposable new world'
    assert not a.core515_only or a.fresh_world,'Core0.5.15 QA must use a disposable new world'
    assert not a.astra516_only or a.fresh_world,'Astra0.5.16 QA must use a disposable new world'
    assert not a.building063_only or a.fresh_world,'Building0.6.3 QA must use a disposable new world'
    assert not a.item068_only or a.fresh_world,'Item0.6.8 QA must use a disposable new world'
    assert not a.recipebook066_only or a.fresh_world,'Recipebook0.6.6 QA must use a disposable new world'
    assert not a.recipebook066_baseline or a.recipebook066_only,'Baseline requires recipebook066 mode'
    assert not a.material065_only or a.fresh_world,'Material0.6.5 QA must use a disposable new world'
    assert not a.absence064_baseline or (a.absence064_only and a.fresh_world),'Absence baseline requires --absence064-only --fresh-world'
    assert not a.absence064_only or a.absence064_baseline or a.fixture,'Absence upgrade verification requires the actual old QA fixture'
    assert not a.absence064_baseline_jar or a.absence064_baseline,'An old JAR is used only for the explicit absence baseline writer'
    if a.absence064_only and a.fixture:
        assert a.fixture.resolve().is_relative_to((ROOT/'build/runtime-qa').resolve()),'Absence fixtures must be earlier disposable runtime-qa worlds, never normal saves'
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
        if folder=='libs' and a.recipebook066_baseline:
            old=ROOT/'build/releases/0.6.5/guogaology-dimension-26.2-0.6.5.jar'
            with ZipFile(old) as archive:assert json.loads(archive.read('fabric.mod.json'))['version']=='0.6.5'
            shutil.copy2(old,mods/old.name);continue
        if folder=='libs' and a.absence064_baseline:
            old=(a.absence064_baseline_jar or ROOT/'build/absence064-baseline/guogaology-dimension-26.2-0.6.3.jar').resolve()
            assert old.is_file(),f'Missing preserved0.6.3 release JAR: {old}'
            with ZipFile(old) as archive:
                identity=json.loads(archive.read('fabric.mod.json'))
                assert identity['id']=='guogaology' and identity['version']=='0.6.3','Absence fixture writer requires actual0.6.3 production'
            shutil.copy2(old,mods/old.name);continue
        if folder=='libs' and a.advancements_baseline:
            baseline=list((instance/'mods').glob('guogaology-dimension-26.2-*.jar'))
            assert len(baseline)==1,'Baseline reads the unique installed main Mod, without modifying it'
            with ZipFile(baseline[0]) as archive:
                assert json.loads(archive.read('fabric.mod.json'))['version']=='0.5.6','Historical baseline requires the installed0.5.6; use --advancements057-only for the current version'
            shutil.copy2(baseline[0],mods/baseline[0].name);continue
        shutil.copy2(ROOT/'ports/26.2/build'/folder/name,mods/name)
    copied=set();required={'fabric-api','sodium'} | ({'voxy'} if a.voxy else set()) | ({'creative_flight_speed'} if a.flight_addon else set()) | ({'paginatedadvancements','cloth-config'} if a.paginated_advancements else set())
    for jar in (instance/'mods').glob('*.jar'):
        try:
            with ZipFile(jar) as z:
                if 'fabric.mod.json' not in z.namelist():continue
                identity=json.loads(z.read('fabric.mod.json'),strict=False)['id']
        except (KeyError,json.JSONDecodeError):continue
        if identity in required:shutil.copy2(jar,mods/jar.name);copied.add(identity)
    assert copied==required
    if a.fixture:shutil.copytree(a.fixture,game/'saves/port-qa',ignore=shutil.ignore_patterns('session.lock'))
    if a.absence064_only:
        main=[jar for jar in mods.glob('*.jar') if jar.name.startswith('guogaology-dimension-26.2-')]
        assert len(main)==1,'One exact production JAR per absence QA run'
        (game/'absence064-run-input.json').write_text(json.dumps({'mode':'baseline063' if a.absence064_baseline else 'upgrade064','productionJar':main[0].name,'productionSha256':hashlib.sha256(main[0].read_bytes()).hexdigest(),'fixture':str(a.fixture.resolve()) if a.fixture else None,'freshWorld':a.fresh_world},indent=2)+'\n','utf8')
    if a.material065_only:
        main=list(mods.glob('guogaology-dimension-26.2-*.jar'));helper=list(mods.glob('guogaology-merge-qa-*.jar'))
        assert len(main)==len(helper)==1,'One exact production and QA JAR per material QA run'
        inputs=material065_inputs(main[0],helper[0],release,language=a.language,voxy=a.voxy,seed=a.seed)
        (game/'material065-run-input.json').write_text(json.dumps(inputs,indent=2)+'\n','utf8')
    if a.recipebook066_only:
        main=list(mods.glob('guogaology-dimension-26.2-*.jar'));helper=list(mods.glob('guogaology-merge-qa-*.jar'));assert len(main)==len(helper)==1
        record={'mode':'baseline065' if a.recipebook066_baseline else 'fixed066','language':a.language,'freshWorld':a.fresh_world}
        for role,path in [('production',main[0]),('qa',helper[0])]:
            with ZipFile(path) as archive:identity=json.loads(archive.read('fabric.mod.json'))
            assert identity['id']==('guogaology' if role=='production' else 'guogaology-merge-qa'),'Wrong native QA input identity'
            if role=='production':assert identity['version']==('0.6.5' if a.recipebook066_baseline else release),'Wrong native production version'
            else:assert path.name==f'guogaology-merge-qa-{release}.jar','Wrong native QA helper artifact'
            record[role+'Jar']=path.name;record[role+'Sha256']=hashlib.sha256(path.read_bytes()).hexdigest();record[role+'MetadataVersion']=identity['version']
        (game/'recipebook066-run-input.json').write_text(json.dumps(record,indent=2)+'\n','utf8')
    if a.item068_only:
        main=list(mods.glob('guogaology-dimension-26.2-*.jar'));helper=list(mods.glob('guogaology-merge-qa-*.jar'));assert len(main)==len(helper)==1
        record=material065_inputs(main[0],helper[0],release,language=a.language,voxy=a.voxy,seed=a.seed);record['mode']='item068'
        (game/'item068-run-input.json').write_text(json.dumps(record,indent=2)+'\n','utf8')
        protected_before=item068_normal_snapshot(instance)
    (game/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:5\nsimulationDistance:5\nsoundCategory_master:0.0\nlang:'+a.language+'\n','utf8')
    temp=game/'temp';temp.mkdir()
    command=[str(a.java_home/'bin/java.exe'),'-Xmx4G','--enable-native-access=ALL-UNNAMED','-Dfile.encoding=UTF-8',
             f'-Dguogaology.qa.general068={str(a.audit068_focus).lower()}', f'-Dguogaology.repair0610={str(a.repair0610).lower()}',f'-Dguogaology.qa.item068={str(a.item068_only).lower()}',f'-Dguogaology.qa.recipebook066={str(a.recipebook066_only).lower()}',f'-Dguogaology.qa.recipebook066Baseline={str(a.recipebook066_baseline).lower()}',
             f'-Dguogaology.qa.material065={str(a.material065_only).lower()}',
             f'-Dguogaology.qa.absence064={str(a.absence064_only).lower()}',f'-Dguogaology.qa.absence064Baseline={str(a.absence064_baseline).lower()}',
             f'-Dguogaology.qa.building063={str(a.building063_only).lower()}',f'-Dguogaology.qa.core515={str(a.core515_only or a.astra516_only).lower()}',f'-Dguogaology.qa.astra516={str(a.astra516_only).lower()}',f'-Dguogaology.qa.core514={str(a.core514_only).lower()}',f'-Dguogaology.qa.astra512={str(a.astra512_only).lower()}',f'-Dguogaology.qa.lamps058={str(a.lamps058_only).lower()}',
             f'-Dguogaology.qa.advancements057={str(a.advancements057_only or a.advancements510_only or a.advancements_baseline or a.laver_scenes_only).lower()}',f'-Dguogaology.qa.advancementsBaseline={str(a.advancements_baseline).lower()}',f'-Dguogaology.qa.advancements510={str(a.advancements510_only).lower()}',f'-Dguogaology.qa.laverScenes={str(a.laver_scenes_only).lower()}',
             f'-Dguogaology.qa.chainloot0501={str(a.chain_loot0501_only).lower()}',f'-Dguogaology.qa.namespace050={str(a.namespace_only).lower()}',f'-Dguogaology.qa.freshworld={str(a.fresh_world).lower()}',
             f'-Dguogaology.qa.balance057={str(a.balance057_only).lower()}',f'-Dguogaology.qa.ore056={str(a.crystal_ore_only).lower()}',f'-Dguogaology.qa.core055={str(a.core_resources_only or a.crystal_ore_only or a.balance057_only).lower()}',f'-Dguogaology.qa.import0541={str(a.jar_import_only).lower()}','-Dfabric.development=false',f'-Dguogaology.qa.cliffchain049={str(a.cliff_chain049_only).lower()}',f'-Dguogaology.qa.cliffchain048={str(a.cliff_chain_only).lower()}',f'-Dguogaology.qa.coreitems046={str(a.core_items_only).lower()}',f'-Dguogaology.qa.underworld046={str(a.underworld_only).lower()}',f'-Dguogaology.qa.voxy={str(a.voxy).lower()}',f'-Dguogaology.qa.boundaries={str(a.boundaries_only).lower()}',f'-Dguogaology.qa.manuscripts={str(a.manuscripts_only).lower()}',f'-Dguogaology.qa.inventory={str(a.inventory_only).lower()}',f'-Dguogaology.qa.portals={str(a.portals_only).lower()}',f'-Dguogaology.qa.patch051={str(a.patch051_only or a.creative_icons_only).lower()}',f'-Dguogaology.qa.creativeicons047={str(a.creative_icons_only).lower()}',f'-Dguogaology.qa.landmarks043={str(a.landmarks_only).lower()}',f'-Djava.io.tmpdir={temp}',f'-Djna.tmpdir={temp}',f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}',
             '-cp',';'.join(map(str,classpath)),metadata['mainClass'],'--username','GuogaologyQA','--version','26.2','--gameDir',str(game),
             '--assetsDir',str(minecraft/'assets'),'--assetIndex',metadata['assetIndex']['id'],'--uuid','00000000000000000000000000000001',
             '--accessToken','0','--userType','legacy','--versionType','release','--width','1600','--height','1200' if a.inventory_only or a.core_resources_only or a.lamps058_only else '1000']
    if a.seed is not None:command.insert(1,f'-Dguogaology.qa.seed={a.seed}')
    print('Hidden QA:',game,flush=True)
    with (game/'launch-output.log').open('w',encoding='utf8') as log:
        process=subprocess.Popen(command,cwd=game,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        try:code=process.wait(timeout=300 if a.landmarks_only else 1100)
        except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=20);raise RuntimeError('Own QA process timed out')
        finally:
            if a.item068_only:
                protected_after=item068_normal_snapshot(instance)
                changed=sorted(key for key in protected_before.keys()|protected_after.keys() if protected_before.get(key)!=protected_after.get(key))
                proof={'normalInstance':str(instance),'mode':'item068','protectedEntries':len(protected_before),'changedEntries':changed,'unchanged':not changed,'scope':'SHA256 of normal mods/config; size+mtime of normal save files; normal files are never copied as QA fixtures.'}
                (game/'item068-normal-protection.json').write_text(json.dumps(proof,indent=2)+'\n','utf8')
    if code or not (game/'port-client-ok.txt').is_file():
        print((game/'launch-output.log').read_text('utf8',errors='replace')[-14000:]);raise RuntimeError(f'QA failed: {code}')
    if a.item068_only:
        verification=verify_item068_outputs(game)
        (game/'item068-runner-verification.json').write_text(json.dumps(verification,indent=2)+'\n','utf8')
        if a.repair0610:
            from verify_repair_0610_runtime import verify
            verify(game,"26.2")
        if a.repair069:
            from verify_repair_069_runtime import verify
            verify(game,'26.2')
    print((game/'port-client-ok.txt').read_text(),flush=True)
    print('PRODUCTION_PORT_OK 26.2',flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--instance',type=Path,required=True);p.add_argument('--java-home',type=Path,required=True)
    p.add_argument('--repair0610',action='store_true',help='Require current0610 strict focused native checks')
    p.add_argument('--repair069',action='store_true',help='Require fixed facing, stripping, boat naming and real extra-hit damage in focused native QA')
    p.add_argument('--fresh-world',action='store_true',help='Create a new default normal world; copy no fixture')
    p.add_argument('--seed',type=int,help='Signed 64-bit seed for a fresh world; omitted uses a new random seed')
    p.add_argument('--jar-import-only',action='store_true',help='Check reconstructed 0.5.4.1 jars, loot and surface shafts')
    p.add_argument('--core-resources-only',action='store_true',help='Run0.5.5 resource/ore/LHO/lamp/enchantment tests and screenshots (requires --fresh-world)')
    p.add_argument('--crystal-ore-only',action='store_true',help='Calibrate actual iron density, full-height Ordinal veins and LHO art in a new world')
    p.add_argument('--balance057-only',action='store_true',help='Native0.5.7 armor, fixed core progress and ore-count checks; wood is static-only')
    p.add_argument('--lamps058-only',action='store_true',help='Native expression-preserving lamp drops/clicks, mineral storage mining and actual tooltips')
    p.add_argument('--astra512-only',action='store_true',help='Native Astra cloud/chair/light/recipes and redesigned art, disposable fo262 only')
    p.add_argument('--core514-only',action='store_true',help='Native0.5.14 recipes, safe Graham growth and full luminous Guogao cube; four settled native scenes, disposable fo262 only')
    p.add_argument('--core515-only',action='store_true',help='Native0.5.15 recipes/scenery and nine settled art scenes, including all six Guogao faces and exact production firs; requires --fresh-world')
    p.add_argument('--building063-only',action='store_true',help='Native fixed-digit palettes and legitimate Guogao building material regression')
    p.add_argument('--item068-only',action='store_true',help='Focused actual door/slab/sign placement, NBT and eight item-context regression in a fresh disposable flat world')
    p.add_argument('--audit068-focus',action='store_true',help='Add boat-name, four-way placement and native axe-stripping observations to isolated item068 QA')
    p.add_argument('--recipebook066-only',action='store_true',help='Focused native0.6.6 survival XP and exclusive branded recipe-book UI; fresh world only')
    p.add_argument('--recipebook066-baseline',action='store_true',help='Reproduce BranchSilk XP with the exact preserved0.6.5JAR, no UI or normal save')
    p.add_argument('--material065-only',action='store_true',help='Native0.6.5 recipes/chests/mobLooting/Stellar16x and actual core/GGG recipe-book groups; requires --fresh-world')
    p.add_argument('--absence064-only',action='store_true',help='Native Lv1 Empty legacy-state/item compatibility; baseline writes a fresh0.6.3world, verifier requires that disposable --fixture')
    p.add_argument('--absence064-baseline',action='store_true',help='With --absence064-only --fresh-world, write old states/items using preserved release0.6.3')
    p.add_argument('--absence064-baseline-jar','--baseline-jar',dest='absence064_baseline_jar',type=Path,help='Explicit0.6.3production JAR; default build/absence064-baseline/guogaology-dimension-26.2-0.6.3.jar')
    p.add_argument('--astra516-only',action='store_true',help='Native0.5.16 straight Astra firs and vivid SCG: current checks plus three focused settled scenes; requires --fresh-world')
    p.add_argument('--laver-scenes-only',action='store_true',help='Focused native Laver-table/audio and redesigned material screenshots, without repeating the advancement sweep')
    p.add_argument('--advancements057-only',action='store_true',help='Real vanilla/guide criteria, client sync and advancement-page screenshots')
    p.add_argument('--advancements510-only',action='store_true',help='Eight biome/material/core/visit paths, native food/table and fully socketed Omega loadout')
    p.add_argument('--advancements-baseline',action='store_true',help='Read-only reproduction with the installed main Mod in a disposable new world')
    p.add_argument('--paginated-advancements',action='store_true',help='Include installed Paginated Advancements and Cloth in this isolated copy')
    p.add_argument('--language',choices=('zh_cn','en_us'),default='zh_cn',help='Language in this disposable QA copy only')
    p.add_argument('--chain-loot0501-only',action='store_true',help='0.5.1 chain signs, FULL generation photographs and restored native loot (requires --fresh-world)')
    p.add_argument('--namespace-only',action='store_true',help='Run the 0.5.0 namespace and command smoke test (requires --fresh-world)')
    p.add_argument('--fixture',type=Path);p.add_argument('--label',required=True);p.add_argument('--voxy',action='store_true');p.add_argument('--flight-addon',action='store_true');p.add_argument('--boundaries-only',action='store_true');p.add_argument('--manuscripts-only',action='store_true');p.add_argument('--inventory-only',action='store_true');p.add_argument('--portals-only',action='store_true');p.add_argument('--patch051-only',action='store_true');p.add_argument('--landmarks-only',action='store_true');p.add_argument('--underworld-only',action='store_true');p.add_argument('--core-items-only',action='store_true');p.add_argument('--creative-icons-only',action='store_true');p.add_argument('--cliff-chain-only',action='store_true');p.add_argument('--cliff-chain049-only',action='store_true');run(p.parse_args())
