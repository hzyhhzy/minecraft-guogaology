"""Validate acquisition costs and source portability, independently of render art."""
import importlib.util
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('core_resources',ROOT/'tools/generate_core_resources.py')
resources=importlib.util.module_from_spec(spec);spec.loader.exec_module(resources)
loot_spec=importlib.util.spec_from_file_location('sanctuary_loot',ROOT/'tools/generate_sanctuary_loot.py')
loot=importlib.util.module_from_spec(loot_spec);loot_spec.loader.exec_module(loot)


def cells(recipe):
    if recipe['type']=='minecraft:crafting_shaped':
        return [recipe['key'][symbol] for row in recipe['pattern'] for symbol in row if symbol!=' ']
    return recipe['ingredients']


class CoreResourcesChecks(unittest.TestCase):
    def test_generation_current(self):
        resources.generate(check=True)

    def test_first_core_never_requires_a_core_or_enchantment(self):
        for root in resources.REGIONAL:
            recipe=resources.read(resources.D/f'recipe/{root}.json')
            self.assertEqual(recipe['type'],'minecraft:crafting_shaped',root)
            self.assertEqual([len(row) for row in recipe['pattern']],[3,3,3],root)
            ingredients=cells(recipe)
            self.assertEqual(len(ingredients),9,root)
            self.assertEqual(recipe['result'],{'id':'guogaology:'+root,'count':1})
            self.assertEqual(ingredients[4],{'item':'guogaology:ordinal_crystal'},root)
            crystal=[v for v in ingredients if v=={'item':'guogaology:ordinal_crystal'}]
            self.assertEqual(len(crystal),1)
            serialized=json.dumps(ingredients)
            for core in resources.REGIONAL:
                self.assertNotIn('guogaology:'+core+'"',serialized,root)
            self.assertNotIn('enchant',serialized)
        for root in resources.REGIONAL:
            for stage in (2,3):
                recipe=resources.read(resources.D/f'recipe/{root}_lv{stage}.json')
                former=root+('' if stage==2 else '_lv2')
                self.assertEqual(recipe['type'],'minecraft:crafting_shaped')
                self.assertEqual([len(row) for row in recipe['pattern']],[3,3,3])
                expected=[{'item':'guogaology:'+former}]*9
                expected[4]={'item':'guogaology:'+('epsilon_block' if stage==2 else 'gamma_block')}
                self.assertEqual(cells(recipe),expected)

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
            for ingredient in cells(recipe):
                self.assertIn(set(ingredient),({'tag'},{'item'}))

    def test_astra_and_boundary_sources(self):
        recipe=cells(resources.read(resources.D/'recipe/astra_critical_core.json'))
        self.assertEqual(recipe.count({'item':'guogaology:compute_chip'}),5)
        self.assertEqual(recipe.count({'item':'guogaology:cloud_pearl'}),1)
        self.assertEqual(recipe.count({'item':'guogaology:nuke_mushroom'}),1)
        self.assertIn({'tag':'guogaology:guogao_lamps'},recipe)
        heart=cells(resources.read(resources.D/'recipe/guogao_heart.json'))
        for name in ('dread_resin','guogao_drop','dread_leaves','emoji_flower','loquat','star_gold','fruit_cake_gel'):
            self.assertEqual(heart.count({'item':'guogaology:'+name}),1)
        boundary=cells(resources.read(resources.D/'recipe/boundary_core.json'))
        self.assertEqual(boundary.count({'item':'guogaology:proof_stone'}),1)
        self.assertEqual(boundary.count({'item':'guogaology:turing_endcap'}),1)

    def test_outer_materials_keep_inner_recipe_roles(self):
        laver=cells(resources.read(resources.D/'recipe/laver_core.json'))
        self.assertEqual(laver.count({'item':'guogaology:iblp_node'}),2)
        for name in ('laver_gel','laver_ribbon','iblp_marked','iblp_blank','gummy','tianyi_fiber'):
            self.assertEqual(laver.count({'item':'guogaology:'+name}),1)
        absence=cells(resources.read(resources.D/'recipe/lho_trace.json'))
        self.assertIn({'item':'guogaology:stellar_stone'},absence)
        self.assertNotIn({'item':'guogaology:absence_glass'},absence)
        power=cells(resources.read(resources.D/'recipe/power_tower_core.json'))
        self.assertEqual(power.count({'item':'guogaology:arrow_knot'}),2)
        self.assertEqual(power.count({'item':'guogaology:scg_node'}),2)
        alternatives=[entry['tag'] for entry in power if 'tag' in entry]
        self.assertEqual(len(alternatives),1)
        values=resources.read(resources.D/f"tags/item/{alternatives[0].split(':')[1]}.json")['values']
        self.assertEqual(set(values),{'guogaology:graham','guogaology:graham_flower'})

    def test_secondary_material_bundles_count_repeated_shaped_cells(self):
        self.assertEqual(loot.raw_recipe('astra')['compute_chip'],5)
        self.assertEqual(loot.raw_recipe('weaver')['iblp_node'],2)
        self.assertEqual(loot.raw_recipe('matrix')['ordinal_bricks'],4)
        for theme in loot.THEMES:
            raw=loot.raw_recipe(theme)
            self.assertEqual(sum(raw.values()),8,theme)
            self.assertNotIn('ordinal_crystal',raw,theme)

    def test_matrix_recipe_and_union_materials(self):
        recipe=cells(resources.read(resources.D/'recipe/sequence_core.json'))
        self.assertEqual(recipe.count({'item':'guogaology:ordinal_bricks'}),4)
        for name in ('matrix_sheet','ridge_lamina','projection_glass','y_leaves','ordinal_crystal'):
            self.assertEqual(recipe.count({'item':'guogaology:'+name}),1)
        pearl=resources.read(resources.A/'models/block/cloud_pearl.json')
        self.assertEqual({f['texture'] for e in pearl['elements'] for f in e['faces'].values()}, {'#body','#cloud','#root'})
        membrane=resources.read(resources.A/'models/block/set_membrane.json')
        self.assertEqual({f['texture'] for e in membrane['elements'] for f in e['faces'].values()}, {'#body'})

    def test_lamp_shaper_recipe_has_one_output(self):
        recipe=resources.read(resources.D/'recipe/guogao_lamp_shaper.json')
        self.assertEqual(recipe['result'],{'id':'guogaology:guogao_lamp_shaper','count':1})
        self.assertEqual(recipe['key']['L'],{'tag':'guogaology:guogao_lamps'})


if __name__=='__main__':unittest.main()
