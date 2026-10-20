"""Validate version-specific release jars and collect them with hashes; never installs them."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import shutil
from zipfile import ZipFile
from io import BytesIO

ROOT=Path(__file__).resolve().parents[1]
CONFIG=json.loads((ROOT/'ports/targets.json').read_text())

def verify_namespace(archive):
    """Include nested legacy bridge classes and compressed NBT, not only JSON."""
    from audit_namespace import audit_archive
    return audit_archive(archive)

def verify_structure_template(actual, expected, name):
    # gzip headers (notably the OS byte) and compression output can differ
    # between Python/zlib versions. Minecraft loads the decompressed NBT.
    # Compare every NBT byte, and let gzip reject damaged streams/CRC values.
    assert gzip.decompress(actual) == gzip.decompress(expected), f'Structure content mismatch: {name}'


def verify_client_hooks(archive, metadata):
    active=set()
    for entry in metadata['mixins']:
        config=json.loads(archive.read(entry if isinstance(entry,str) else entry['config']))
        for kind in ('mixins','client','server'):
            for name in config.get(kind,[]):
                full=config['package']+'.'+name
                assert full.replace('.','/')+'.class' in archive.namelist(),f'Missing registered mixin: {full}'
                active.add(full)
    for required in ('ManuscriptVerticalFlightMixin','InventoryOriginAccessor','RecipeBookSectionsMixin','RecipeBookCollectionsMixin'):
        assert 'dev.guogaology.client.mixin.'+required in active,f'Unregistered client hook: {required}'
    modern=metadata['depends']['minecraft']!='1.21.1'
    if modern:
        for required in ('RecipeBookTabTooltipMixin','RecipeBookBrandIconMixin'):
            assert 'dev.guogaology.client.mixin.'+required in active,f'Unregistered modern recipe-book hook: {required}'
        assert 'dev/guogaology/client/recipebook/SectionRecipeBookCategory.class' in archive.namelist(),'Missing modern recipe-book category'
    # Read the packaged UTF-8 constant, including javac's inlined render-site copy.
    # Resource IDs resolve directly to assets/<namespace>/<path> for GUI blits.
    icon=metadata['icon'];assert icon.startswith('assets/'),'Main logo must be an asset path'
    namespace,path=icon.removeprefix('assets/').split('/',1)
    texture=(namespace+':'+path).encode('utf8')
    constant=b'\x01'+len(texture).to_bytes(2,'big')+texture
    renderer='dev/guogaology/client/mixin/RecipeBookBrandIconMixin.class' if modern else 'dev/guogaology/client/recipebook/SectionRecipeBookButton.class'
    for name in ('dev/guogaology/client/recipebook/RecipeBookSections.class',renderer):
        assert constant in archive.read(name),f'Recipe-book brand icon differs from the main Mod logo: {name}'
    assert icon in archive.namelist(),'Missing recipe-book/main brand texture'


def verify_jei_compat(archive, metadata, target):
    """Optional26.3 workaround must never create a server/JEI hard dependency."""
    config_name='guogaology.jei-compat.mixins.json'
    entry={'config':config_name,'environment':'client'}
    assert 'jei' not in metadata['depends'], 'JEI must remain optional'
    prefix='dev/guogaology/client/compat/jei/'
    sync='dev/guogaology/compat/RecipeSyncCompat.class'
    if target!='26.3':
        assert config_name not in archive.namelist(), '26.3 JEI hook leaked to another target'
        assert not any(name.startswith(prefix) for name in archive.namelist()), '26.3 JEI classes leaked to another target'
        assert sync not in archive.namelist(), '26.3 recipe sync leaked to another target'
        return
    assert entry in metadata['mixins'], 'JEI hook must be client-only'
    config=json.loads(archive.read(config_name))
    assert config['plugin']=='dev.guogaology.client.compat.jei.JeiTagCompatPlugin', 'Missing JEI structural guard'
    assert config['client']==['JeiVanillaTagsMixin'] and not config.get('mixins'), 'JEI hook must be client-only'
    for name in ('JeiTagCompatPlugin','JeiTagCompatShape','JeiTagCompatShape$Flow'):
        assert prefix+name+'.class' in archive.namelist(), 'Missing JEI structural guard class: '+name
    assert sync in archive.namelist(), 'Missing recipe synchronization'
    assert metadata['entrypoints']['main'].index('dev.guogaology.compat.RecipeSyncCompat') > metadata['entrypoints']['main'].index('dev.guogaology.GuogaologyMod'), 'Recipe synchronization must follow serializer registration'


def verify_wood_loot_policy(archive, target):
    """A final donor overlay must not duplicate doors or lose double slabs."""
    from generate_wood_loot import FAMILIES, STRUCTURAL_SHAPES, wood_loot
    for family in FAMILIES:
        for shape in STRUCTURAL_SHAPES:
            expected=wood_loot(family,shape)
            if target=='26.3':
                from prepare_port_resources import convert_263_loot
                expected=convert_263_loot(expected)
            name=family+'_'+shape
            actual=json.loads(archive.read('data/guogaology/loot_table/blocks/'+name+'.json'))
            assert actual==expected,f'Stale/overwritten wooden door/slab loot: {target}/{name}'


def verify_decor_orientation(archive, target):
    """All final furniture states rotate while retaining the original north/item art."""
    from generate_decor_orientation import verify_archive
    verify_archive(archive, target)


def verify_ore_policy(archive, target):
    """Resource imports must not restore donor counts after the host overlay."""
    from generate_ore_distribution import COUNTS, TOPS
    folder='feature' if target=='26.3' else 'configured_feature'
    prefix='data/guogaology/worldgen/'
    for mineral,count in COUNTS.items():
        for suffix in ('','_hell'):
            name='ore_'+mineral+suffix+'.json'
            configured=json.loads(archive.read(prefix+folder+'/'+name))
            config=configured.get('config',configured)
            assert config['discard_chance_on_air_exposure']==0.5,f'Stale ore air policy: {target}/{name}'
            placed=json.loads(archive.read(prefix+'placed_feature/'+name))['placement']
            actual_counts=[p['count'] for p in placed if p['type']=='minecraft:count']
            assert actual_counts==[count],f'Stale ore count: {target}/{name}'
            if mineral in TOPS:
                heights=[p['height'] for p in placed if p['type']=='minecraft:height_range']
                assert heights==[{'type':'minecraft:biased_to_bottom','min_inclusive':{'absolute':-64},
                    'max_inclusive':{'absolute':TOPS[mineral]},'inner':1}],f'Stale ore height: {target}/{name}'


def verify_crystal_ore_policy(archive, target):
    """Check the final overlay, including the flattened26.3 feature codec."""
    from generate_ordinal_crystal_ore import BIOMES, HOSTS, FEATURES, LEGACY_FEATURES, BATCHES, ore_config, placed_config
    prefix='data/guogaology/worldgen/'
    folder='feature' if target=='26.3' else 'configured_feature'
    actual=json.loads(archive.read(prefix+folder+'/ore_ordinal_crystal.json'))
    expected=ore_config(9)['config']
    if target=='26.3':
        from prepare_port_resources import convert_263_value
        expected=convert_263_value(expected)
        def verify_state_keys(value):
            if isinstance(value,dict):
                assert not ({'Name','Properties'} & value.keys()), 'Legacy block-state codec in26.3 crystal feature'
                for child in value.values():verify_state_keys(child)
            elif isinstance(value,list):
                for child in value:verify_state_keys(child)
        verify_state_keys(actual)
    assert actual['type']=='minecraft:ore',f'Wrong crystal vein feature type: {target}'
    assert actual.get('config', {k:v for k,v in actual.items() if k!='type'})==expected,f'Stale crystal vein targets: {target}'
    for batch in BATCHES:
        actual=json.loads(archive.read(prefix+f'placed_feature/ore_ordinal_crystal_{batch}.json'))
        assert actual==placed_config(batch),f'Stale uniform/LHO crystal distribution: {target}/{batch}'
    for name,values in HOSTS.items():
        actual=json.loads(archive.read(f'data/guogaology/tags/block/{name}.json'))
        assert actual['values']==values,f'Stale crystal host tag: {target}/{name}'
    for name in BIOMES:
        data=json.loads(archive.read(prefix+f'biome/{name}.json'))
        def refs(value):
            if isinstance(value,str):return [value]
            if isinstance(value,list):return [s for child in value for s in refs(child)]
            if isinstance(value,dict):return [s for child in value.values() for s in refs(child)]
            return []
        strings=refs(data);expected=FEATURES[1 if name=='lho_absence' else 0]
        for feature in FEATURES+LEGACY_FEATURES:
            assert strings.count(feature)==int(feature==expected),f'Duplicate/stale crystal biome feature: {target}/{name}/{feature}'
    for batch in ('upper','middle','small'):
        assert prefix+f'placed_feature/ore_ordinal_crystal_{batch}.json' not in archive.namelist(),f'Retired height band retained: {target}/{batch}'


def verify_motion_tags263(archive, target):
    """Inspect the final archive, after imported-content overlays have run."""
    if target!='26.3':
        return
    from prepare_263_motion_tags import classification
    path='data/minecraft/tags/block/blocks_motion_no_leaves.json'
    actual=json.loads(archive.read(path))
    source=json.loads((ROOT/'src/main/resources'/path).read_text(encoding='utf-8'))
    catalog=classification()
    expected=list(dict.fromkeys(source['values']+['guogaology:'+name for name in catalog['motion_no_leaves']]))
    assert actual.get('replace',False) is False,'26.3 motion tags must extend vanilla'
    assert actual['values']==expected,'Stale/overwritten26.3 custom-solid motion tags'


def verify_guide_policy(archive, target):
    """Catch advancement overlays and version-sensitive background paths before shipping."""
    from generate_guide_advancements import build, positions
    expected,_,_=build()
    visible={}
    for name in archive.namelist():
        assert not (name.startswith('data/minecraft/advancement/') and name.endswith('.json')),f'Vanilla advancement replaced: {target}/{name}'
        if name.startswith('data/guogaology/advancement/') and name.endswith('.json'):
            value=json.loads(archive.read(name))
            if 'display' in value:visible[name.removeprefix('data/guogaology/advancement/').removesuffix('.json')]=value
    assert visible.keys()==expected.keys(),f'Guide nodes missing or unexpectedly replaced: {target}'
    for name,value in expected.items():
        value=json.loads(json.dumps(value))
        if name=='arrival' and target!='1.21.1':value['display']['background']='minecraft:gui/advancements/backgrounds/stone'
        assert visible[name]==value,f'Incorrect guide payload: {target}/{name}'
    assert json.loads(archive.read('guogaology/guide_layout.json'))==positions(expected),f'Stale guide order: {target}'
    assert archive.read('assets/guogaology/textures/item/brand_emblem.png')==archive.read('assets/guogaology/icon.png'),f'Wrong directory logo: {target}'


def verify_mineral_block_tools(archive, target):
    for tag in ('mineable/pickaxe','needs_iron_tool'):
        values=json.loads(archive.read(f'data/minecraft/tags/block/{tag}.json'))['values']
        for mineral in ('omega','epsilon','gamma','true_omega'):
            assert 'guogaology:'+mineral+'_block' in values,f'Mineral storage tool policy missing: {target}/{tag}/{mineral}'


def verify_gel_loot_policy(archive, target):
    """Check final imported payloads independently of the generator and adapter."""
    functions='modifier' if target=='26.3' else 'functions'
    function='type' if target=='26.3' else 'function'
    for entity,count in (('fruit_cake_slime',2),('fruit_slime',1)):
        path=f'data/guogaology/loot_table/entities/{entity}.json'
        expected={'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{
            'type':'minecraft:item','name':'guogaology:fruit_cake_gel',functions:[
                {function:'minecraft:set_count','count':count},
                {function:'minecraft:enchanted_count_increase','enchantment':'minecraft:looting',
                 'count':{'type':'minecraft:uniform','min':0.0,'max':1.0}}]}]}],
            'random_sequence':'guogaology:'+entity}
        assert json.loads(archive.read(path))==expected,f'Stale/overwritten gel mob loot: {target}/{entity}'


def verify_loot_tag_entries(archive, target):
    """Resolve every packaged loot tag and member, independently of port conversion.

    Current loot sets refer to shipped item roots. An external tag/member must
    acquire an explicit resolution source before it can pass this release guard.
    Actual registry/codec loading remains a separate native check.
    """
    entries=set(archive.namelist());resolved={};used_members=set();count=0;tables=0
    def item(identifier,where,required=True):
        assert isinstance(identifier,str) and ':' in identifier and not identifier.startswith('#'),f'Invalid loot item: {where}/{identifier}'
        namespace,path=identifier.split(':',1)
        roots=(f'assets/{namespace}/models/item/{path}.json',f'assets/{namespace}/items/{path}.json')
        if not any(root in entries for root in roots):
            assert not required,f'Unresolved loot tag item: {where}/{identifier}'
            return set()
        return {identifier}
    def tag(identifier,where,chain=(),required=True):
        assert isinstance(identifier,str) and ':' in identifier and not identifier.startswith('#'),f'Invalid loot tag: {where}/{identifier}'
        assert identifier not in chain,f'Cyclic loot tag: {where}/{identifier}'
        if identifier in resolved:return resolved[identifier]
        namespace,path=identifier.split(':',1);name=f'data/{namespace}/tags/item/{path}.json'
        if name not in entries:
            assert not required,f'Unresolved loot item tag: {where}/{identifier}'
            return set()
        members=set()
        for value in json.loads(archive.read(name))['values']:
            member=value['id'] if isinstance(value,dict) else value
            needed=value.get('required',True) if isinstance(value,dict) else True
            assert isinstance(member,str),f'Invalid loot tag member: {name}/{member}'
            members.update(tag(member[1:],name,(*chain,identifier),needed) if member.startswith('#') else item(member,name,needed))
        resolved[identifier]=members
        return members
    def walk(value,where):
        nonlocal count
        if isinstance(value,list):
            for index,child in enumerate(value):walk(child,f'{where}[{index}]')
        elif isinstance(value,dict):
            if value.get('type')=='minecraft:tag':
                count+=1
                if target=='26.3':
                    assert 'items' in value and 'name' not in value,f'Legacy loot tag codec: {target}/{where}'
                    holders=value['items']
                    if isinstance(holders,str) and holders.startswith('#'):members=tag(holders[1:],where)
                    elif isinstance(holders,list):members=set().union(*(item(member,where) for member in holders))
                    else:members=item(holders,where)
                else:
                    assert 'name' in value and 'items' not in value,f'Wrong legacy loot tag codec: {target}/{where}'
                    members=tag(value['name'],where)
                assert members,f'Empty loot tag selection: {target}/{where}'
                used_members.update(members)
            for key,child in value.items():walk(child,where+'.'+key)
    for name in sorted(entries):
        if name.startswith('data/guogaology/loot_table/') and name.endswith('.json'):
            tables+=1;walk(json.loads(archive.read(name)),name)
    return {'lootTables':tables,'tagEntries':count,'resolvedTags':len(resolved),'resolvedItems':len(used_members)}


def package(targets, output_dir=None):
    from audit_localization import audit
    audit(ROOT)
    from audit_outer_content import audit as audit_outer
    assert not audit_outer()['unused'],'Unused outer-world assets remain'
    release=next(line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
    output=Path(output_dir) if output_dir is not None else ROOT/'build/releases'/release
    output.mkdir(parents=True,exist_ok=True)
    manifest=[]
    for target in targets:
        assert target in ('1.21.1',*CONFIG),target
        directory=ROOT if target=='1.21.1' else ROOT/'ports'/target
        jar=directory/'build/libs'/f'guogaology-dimension-{target}-{release}.jar'
        with ZipFile(jar) as archive:
            metadata=json.loads(archive.read('fabric.mod.json'))
            assert metadata['id']=='guogaology' and metadata['version']==release
            assert metadata['name']=='果糕逻辑 · Guogaology'
            assert metadata['description']==json.loads((ROOT/'src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))['description']
            assert metadata['license']=='GPL-3.0-only WITH GPL-3.0-linking-exception'
            license_name = 'LICENSE_'+jar.stem.rsplit('-'+release,1)[0] if target=='1.21.1' else 'LICENSE'
            assert archive.read(license_name)==(ROOT/'LICENSE').read_bytes()
            for notice in ('LICENSE-MINECRAFT-EXCEPTION','COPYRIGHT','THIRD_PARTY.md'):
                assert archive.read(notice)==(ROOT/notice).read_bytes(),f'Missing or stale license notice: {target}/{notice}'
            from copy_catalog import normalize
            for locale in ('zh_cn','en_us'):
                key=f'assets/guogaology/lang/{locale}.json'
                translated=json.loads(archive.read(key))
                assert translated==normalize(Path(key),translated),f'Stale copy in {target}: {locale}'
                assert archive.read(key)==(ROOT/'src/main/resources'/key).read_bytes()
            assert archive.read(metadata['icon'])==(ROOT/'src/main/resources'/metadata['icon']).read_bytes()
            assert metadata['depends']['minecraft']==target
            entries=archive.namelist()
            verify_client_hooks(archive, metadata)
            verify_jei_compat(archive, metadata, target)
            verify_wood_loot_policy(archive, target)
            verify_decor_orientation(archive, target)
            from prepare_sign_resources import verify_archive as verify_sign_resources
            verify_sign_resources(archive, target)
            verify_namespace(archive)
            verify_ore_policy(archive, target)
            verify_crystal_ore_policy(archive, target)
            verify_motion_tags263(archive, target)
            verify_guide_policy(archive, target)
            verify_mineral_block_tools(archive, target)
            verify_gel_loot_policy(archive, target)
            verify_loot_tag_entries(archive, target)
            frame=json.loads(archive.read('guogaology/block_balance.json'))['guogao_portal_frame']
            assert frame['hardness']==200 and frame['drop']=='none',f'Stale activated portal frame: {target}'
            loot=json.loads(archive.read('data/guogaology/loot_table/blocks/guogao_portal_frame.json'))
            assert not loot.get('pools'),f'Activated frame must never drop: {target}'
            assert not any('PortalGroundChecks' in name for name in entries),'Ground-placement QA leaked into release'
            assert not any('/qa/' in n or '/qa263/' in n or '/releaseqa263/' in n or '/mergeqa/' in n or '/connectorqa/' in n or 'connector-qa' in n or 'port-qa' in n or 'visualqa' in n for n in entries),'QA code leaked into release'
            assert not any('/Fusion' in n or '/SurvivalEntities' in n or '/OrdinalWand' in n for n in entries),'Retired systems returned'
            templates=[n for n in entries if n.startswith('data/guogaology/structure/') and n.endswith('.nbt')]
            assert len(templates)==61,(target,len(templates))
            from import_outer_content import nbt_transform
            for name in templates:
                original=ROOT/'content/outer-1.0.0'/name.removeprefix('data/guogaology/')
                verify_structure_template(archive.read(name),nbt_transform(original.read_bytes(),legacy=target!='26.3'),name)
            for name in entries:
                if name.startswith('data/guogaology/advancement/') and name.endswith('.json'):
                    for recipe in json.loads(archive.read(name)).get('rewards',{}).get('recipes',[]):
                        namespace,path=recipe.split(':',1)
                        assert f'data/{namespace}/recipe/{path}.json' in entries,(target,name,recipe)
            if target=='1.21.1':
                from prepare_legacy_resources import adapt
                for name in entries:
                    if name.endswith('.json') and name.startswith(('data/guogaology/worldgen/', 'assets/guogaology/models/')):
                        source=ROOT/'src/main/resources'/name
                        if source.exists():
                            assert json.loads(archive.read(name))==adapt(json.loads(source.read_text('utf8'))),f'Stale 1.21.1 resource adaptation: {name}'
                nested=metadata['jars'][0]['file']
                with ZipFile(BytesIO(archive.read(nested))) as bridge:
                    info=json.loads(bridge.read('fabric.mod.json'))
                    assert info['version']==release and info['depends']['minecraft']==target
                    assert info['entrypoints'].keys()=={'guogaology:outer','guogaology:outer_client'}
                    # Loom 1.17 remaps Mixin annotation selectors in-place, without a refmap.
                    noise=bridge.read('dev/guogaology/mixin/OuterNoiseSettingsMixin.class')
                    spawn=bridge.read('dev/guogaology/mixin/OuterSpawnPlacementInvoker.class')
                    assert b'Lnet/minecraft/class_7138;method_41556(' in noise
                    assert b'method_20637' in spawn
                    assert b'net/minecraft/world/level' not in noise+spawn
            else:assert not metadata.get('jars'),'Legacy bridge leaked into modern target'
            meshes=[n for n in entries if n.startswith('assets/guogaology/core_meshes/') and n.endswith('.json')]
            assert len(meshes)==28,(target,len(meshes))
            for name in meshes:
                source=ROOT/'src/main/resources'/name
                assert archive.read(name)==source.read_bytes(),f'{target} diverged from final D mesh: {name}'
            java=21 if target in ('1.21.1','1.21.11') else 25
            assert max(int.from_bytes(archive.read(n)[6:8],'big') for n in entries if n.startswith('dev/guogaology/') and n.endswith('.class'))==java+44
            if target=='26.3':
                assert 'data/guogaology/worldgen/feature/monumental_landmarks.json' in entries
                assert 'data/guogaology/worldgen/material_rule/guogaology.json' in entries
                for n in entries:
                    if n.startswith('data/guogaology/loot_table/') and n.endswith('.json'):
                        data=archive.read(n).decode()
                        assert '"functions"' not in data and '"conditions"' not in data,(target,n)
        destination=output/jar.name
        shutil.copy2(jar,destination)
        manifest.append({'minecraft':target,'mod':release,'java':java,'license':metadata['license'],'file':jar.name,'bytes':jar.stat().st_size,
                         'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'requires':metadata['depends']})
    (output/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (output/'SHA256SUMS.txt').write_text(''.join(f"{m['sha256']}  {m['file']}\n" for m in manifest),encoding='utf-8')
    lines=['Guogaology / 果糕逻辑 '+release,'','Choose exactly ONE JAR matching the Minecraft version. Install Fabric Loader and matching Fabric API.','Both server and client need the same matching Guogaology JAR. Do not mix Minecraft versions in one mods folder.','No gallery or flight-speed addon is required. The 0.5.0 namespace change requires a new world; keep earlier saves with their previous Mod build.','']
    for m in manifest:lines.append(f"{m['minecraft']}: Java {m['java']}+, Fabric Loader {m['requires']['fabricloader']}, Fabric API {m['requires']['fabric-api']}")
    lines.extend(['', 'Minecraft 1.21.1 only: the SAME 1.21.1 JAR also runs on NeoForge 21.1.248 with Sinytra Connector 2.0.0-beta.16 and Forgified Fabric API 0.116.15+2.3.1+1.21.1.',
                  'Connector beta.16 was runtime-tested with 0.5.3; beta.17 was previously tested with 0.5.2.',
                  'Use Forgified Fabric API instead of the ordinary Fabric API in that NeoForge setup. Do not install both API distributions.',
                  'Connector is optional. There is no official Connector 26.2 build in this validation; 26.2 is tested on native Fabric.',
                  'Validation scope and reproducible checks: docs/CONNECTOR-COMPATIBILITY.md in the source repository.'])
    (output/'INSTALL.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    for notice in ('LICENSE','LICENSE-MINECRAFT-EXCEPTION','COPYRIGHT','THIRD_PARTY.md'):
        shutil.copy2(ROOT/notice,output/notice)
    (output/'SOURCE.txt').write_text(f'Guogaology {release} is GPL-3.0-only WITH GPL-3.0-linking-exception.\n'
        f'The matching complete source is Guogaology-v{release}-source.zip. Share that source archive with these JARs, or make the corresponding source available as required by GPLv3.\n',encoding='utf8')
    print(json.dumps({'output':str(output),'releases':manifest},ensure_ascii=False,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--targets',default='1.21.1,1.21.11,26.2,26.3')
    parser.add_argument('--output',type=Path,help='Optional output directory, e.g. for isolated package verification')
    args=parser.parse_args()
    package(args.targets.split(','),args.output)
