"""Validate acquisition costs and source portability, independently of render art."""
import importlib.util
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('core_resources',ROOT/'tools/generate_core_resources.py')
resources=importlib.util.module_from_spec(spec);spec.loader.exec_module(resources)


class CoreResourcesChecks(unittest.TestCase):
    def test_generation_current(self):
        resources.generate(check=True)

    def test_first_core_never_requires_a_core_or_enchantment(self):
        for root in resources.REGIONAL:
            recipe=resources.read(resources.D/f'recipe/{root}.json')
            self.assertEqual(len(recipe['ingredients']),9,root)
            self.assertEqual(recipe['result'],{'id':'guogaology:'+root,'count':1})
            crystal=[v for v in recipe['ingredients'] if v=={'item':'guogaology:ordinal_crystal'}]
            self.assertEqual(len(crystal),1)
            serialized=json.dumps(recipe['ingredients'])
            for core in resources.REGIONAL:
                self.assertNotIn('guogaology:'+core+'"',serialized,root)
            self.assertNotIn('enchant',serialized)
        for root in resources.REGIONAL:
            for stage in (2,3):
                recipe=resources.read(resources.D/f'recipe/{root}_lv{stage}.json')
                former=root+('' if stage==2 else '_lv2')
                self.assertEqual(recipe['ingredients'],[{'item':'guogaology:'+former}]*8+
                                 [{'item':f'guogaology:ordinal_crystal_lv{stage}'}])

    def test_shard_conversion_and_ninefold_grades_conserve_mass(self):
        forward=resources.read(resources.D/'recipe/ordinal_crystal.json')
        reverse=resources.read(resources.D/'recipe/ordinal_shards_from_crystal.json')
        self.assertEqual(forward['ingredients'],[{'item':'guogaology:ordinal_shard'}]*4)
        self.assertEqual(reverse['result'],{'id':'guogaology:ordinal_shard','count':4})
        for stage in (2,3,4):
            forward=resources.read(resources.D/f'recipe/ordinal_crystal_lv{stage}.json')
            reverse=resources.read(resources.D/f'recipe/ordinal_crystal_lv{stage}_unpack.json')
            self.assertEqual(len(forward['ingredients']),9)
            self.assertEqual(reverse['result']['count'],9)
            self.assertEqual(reverse['result']['id'],forward['ingredients'][0]['item'])

    def test_ore_silk_fortune_and_accessible_tools(self):
        balance=resources.read(resources.RES/'guogaology/block_balance.json')
        for name in resources.ORES:
            self.assertEqual(balance[name]['tier'],0)
            alternatives=resources.read(resources.D/f'loot_table/blocks/{name}.json')['pools'][0]['entries'][0]
            silk,ordinary=alternatives['children']
            self.assertEqual(silk['name'],'guogaology:'+name)
            self.assertEqual(silk['conditions'][0]['predicate']['predicates']['minecraft:enchantments'],
                             [{'enchantments':'minecraft:silk_touch','levels':{'min':1}}])
            self.assertEqual(ordinary['name'],'guogaology:ordinal_shard')
            self.assertNotIn('conditions',ordinary)
            funcs={v['function']:v for v in ordinary['functions']}
            self.assertEqual(funcs['minecraft:set_count']['count'],{'type':'minecraft:uniform','min':4,'max':6})
            self.assertEqual(funcs['minecraft:apply_bonus']['formula'],'minecraft:ore_drops')
        for name in resources.MATERIALS:
            self.assertEqual(balance[name]['tier'],0)
            loot=resources.read(resources.D/f'loot_table/blocks/{name}.json')
            self.assertEqual(loot['pools'][0]['entries'][0]['name'],'guogaology:'+name)
            self.assertNotIn('match_tool',json.dumps(loot))

    def test_ingredient_tags_reference_real_items(self):
        for name in resources.TAG_VALUES:
            values=resources.read(resources.D/f'tags/item/{name}.json')['values']
            for value in values:
                identifier=value.split(':')[1]
                self.assertTrue((resources.A/f'models/item/{identifier}.json').exists(),value)
        # Number/emotion/tape state must never enter the ingredient predicate.
        for root in resources.REGIONAL:
            recipe=resources.read(resources.D/f'recipe/{root}.json')
            for ingredient in recipe['ingredients']:
                self.assertIn(set(ingredient),({'tag'},{'item'}))

    def test_lamp_shaper_recipe_has_one_output(self):
        recipe=resources.read(resources.D/'recipe/guogao_lamp_shaper.json')
        self.assertEqual(recipe['result'],{'id':'guogaology:guogao_lamp_shaper','count':1})
        self.assertEqual(recipe['key']['L'],{'tag':'guogaology:guogao_lamps'})


if __name__=='__main__':unittest.main()
