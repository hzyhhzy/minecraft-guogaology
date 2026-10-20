"""Regress the six real 0.6.6 tag entries rejected by Minecraft 26.3."""
import copy
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import prepare_port_resources as ports
from package_multiversion import verify_loot_tag_entries

ROOT=Path(__file__).resolve().parents[2]
FIXTURE=json.loads((Path(__file__).parent/'fixtures/loot-tag-0.6.6-26.3.json').read_text('utf8'))


class Archive:
    def __init__(self,files):self.files=files
    def namelist(self):return list(self.files)
    def read(self,name):return json.dumps(self.files[name]).encode('utf8')


class LootTagResources(unittest.TestCase):
    def archive(self,target,changes=None):
        files={}
        for record in FIXTURE['entries']:
            name=record['table']
            files[name]=json.loads((ROOT/'src/main/resources'/name).read_text('utf8'))
            tag=record['entry']['name'];namespace,path=tag.split(':',1)
            tag_path=f'data/{namespace}/tags/item/{path}.json'
            files[tag_path]=json.loads((ROOT/'src/main/resources'/tag_path).read_text('utf8'))
            for member in files[tag_path]['values']:
                ns,item=member.split(':',1);model=f'assets/{ns}/models/item/{item}.json'
                files[model]=json.loads((ROOT/'src/main/resources'/model).read_text('utf8'))
        if target=='26.3':
            files={name:ports.convert_263_loot(value) if '/loot_table/' in name else value for name,value in files.items()}
        for name,value in (changes or {}).items():
            if value is None:files.pop(name,None)
            else:files[name]=value
        return Archive(files)

    def test_all_six_exact_released_entries_convert_without_mutating_inputs(self):
        self.assertEqual(FIXTURE['sha256'],'c72f91f4588b2271f812c4e0e0479a30ac06c66b7f0927a4cbe4cef38a9226f0')
        self.assertEqual(len(FIXTURE['entries']),6)
        self.assertEqual(len({v['table'] for v in FIXTURE['entries']}),4)
        for record in FIXTURE['entries']:
            with self.subTest(table=record['table'],pointer=record['pointer']):
                source=record['entry'];snapshot=copy.deepcopy(source)
                self.assertEqual(ports.convert_263_loot(source),{
                    'type':'minecraft:tag','items':'#'+source['name'],'expand':True})
                self.assertEqual(source,snapshot)

    def test_nested_conversion_preserves_selection_weights_counts_and_item_names(self):
        source={'pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
            {'type':'minecraft:tag','name':'guogaology:ordinal_flowers','expand':False,'weight':7,'quality':2},
            {'type':'minecraft:item','name':'guogaology:fruit_cake_gel'}]}],
            'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':4,'max':8}}]}]}
        snapshot=copy.deepcopy(source);result=ports.convert_263_loot(source);pool=result['pools'][0]
        self.assertEqual(pool['entries'][0]['children'][0],{'type':'minecraft:tag',
            'items':'#guogaology:ordinal_flowers','expand':False,'weight':7,'quality':2})
        self.assertEqual(pool['entries'][0]['children'][1],{'type':'minecraft:item','name':'guogaology:fruit_cake_gel'})
        self.assertEqual(pool['modifier'],[{'type':'minecraft:set_count','count':{'type':'minecraft:uniform','min':4,'max':8}}])
        self.assertEqual(source,snapshot)

    def test_already_modern_holder_sets_are_idempotent(self):
        for items in ('#guogaology:fruit_guogao',['guogaology:omega_bloom','guogaology:epsilon_bloom']):
            with self.subTest(items=items):
                source={'type':'minecraft:tag','items':items,'expand':True,'weight':3}
                self.assertEqual(ports.convert_263_loot(source),source)
                self.assertEqual(ports.convert_263_loot(ports.convert_263_loot(source)),source)

    def test_final_package_schema_and_members_resolve_on_all_four_targets(self):
        for target in ('1.21.1','1.21.11','26.2','26.3'):
            with self.subTest(target=target):
                archive=self.archive(target);result=verify_loot_tag_entries(archive,target)
                self.assertEqual(result,{'lootTables':4,'tagEntries':6,'resolvedTags':3,'resolvedItems':20})
                # Older targets keep the exact authored TagKey/name representation.
                if target!='26.3':
                    for record in FIXTURE['entries']:
                        value=archive.files[record['table']]
                        for key in record['pointer'].strip('/').split('/'):
                            value=value[int(key)] if isinstance(value,list) else value[key]
                        self.assertEqual(value,record['entry'])

    def test_package_guard_rejects_bad_schema_independently_of_converter(self):
        # A future broken converter must not make an equally broken expected
        # payload pass; the package guard checks the native schema separately.
        with patch.object(ports,'convert_263_loot',lambda value:copy.deepcopy(value)):
            with self.assertRaisesRegex(AssertionError,'Legacy loot tag codec'):
                verify_loot_tag_entries(self.archive('26.3'),'26.3')
        modern=self.archive('26.3')
        with self.assertRaisesRegex(AssertionError,'Wrong legacy loot tag codec'):
            verify_loot_tag_entries(modern,'26.2')

    def test_package_guard_rejects_unresolved_empty_or_cyclic_tags(self):
        branch='data/guogaology/tags/item/ordinal_branch_stones.json'
        flowers='data/guogaology/tags/item/ordinal_flowers.json'
        cases=(
            ({flowers:None},'Unresolved loot item tag'),
            ({flowers:{'values':['guogaology:missing_item']}},'Unresolved loot tag item'),
            ({flowers:{'values':[]}},'Empty loot tag selection'),
            ({flowers:{'values':['#guogaology:ordinal_branch_stones']},branch:{'values':['#guogaology:ordinal_flowers']}},'Cyclic loot tag'),
        )
        for changes,error in cases:
            with self.subTest(error=error),self.assertRaisesRegex(AssertionError,error):
                verify_loot_tag_entries(self.archive('26.3',changes),'26.3')
        optional={'values':['guogaology:omega_bloom',{'id':'guogaology:missing_item','required':False},
                            {'id':'#guogaology:missing_tag','required':False}]}
        result=verify_loot_tag_entries(self.archive('26.3',{flowers:optional}),'26.3')
        self.assertEqual(result['tagEntries'],6)


if __name__=='__main__':unittest.main()
