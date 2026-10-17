"""Validate acquisition costs and source portability, independently of render art."""
import importlib.util
import json
from pathlib import Path
import unittest
from collections import Counter
from PIL import Image

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

    def test_symmetric_rings_keep_exact_current_materials(self):
        rings={
            'sequence_core':['matrix_sheet','ordinal_bricks','matrix_sheet','ridge_lamina','ridge_lamina','matrix_sheet','ordinal_bricks','matrix_sheet'],
            'power_tower_core':['power_bricks','scg_node','power_bricks','tree_node_red','tree_node_green','power_bricks','tree_node_blue','power_bricks'],
            'hydra_bud':['hydra_sprig','#ordinal_branch_stones','hydra_sprig','#ordinal_branch_stones','#ordinal_branch_stones','hydra_sprig','#ordinal_branch_stones','hydra_sprig'],
            'lho_trace':['#absence_fragments','stellar_stone','#absence_fragments','stellar_stone','stellar_stone','#absence_fragments','stellar_stone','#absence_fragments'],
            'laver_core':['iblp_node','gummy','laver_ribbon','iblp_marked','iblp_marked','laver_ribbon','gummy','iblp_node'],
            'astra_critical_core':['compute_chip','cloud_pearl','compute_chip','cloud_pearl','cloud_pearl','compute_chip','cloud_pearl','compute_chip'],
            'boundary_core':['set_membrane','turing_endcap','set_membrane','turing_endcap','turing_endcap','set_membrane','turing_endcap','set_membrane'],
            'guogao_heart':['guogao_drop','fruit_cake_gel','guogao_drop','fruit_cake_gel','fruit_cake_gel','guogao_drop','fruit_cake_gel','guogao_drop'],
        }
        for root,ring in rings.items():
            actual=cells(resources.read(resources.D/f'recipe/{root}.json'))
            expected=[{'tag':'guogaology:'+name[1:]} if name.startswith('#') else {'item':'guogaology:'+name} for name in ring]
            expected.insert(4,{'item':'guogaology:ordinal_crystal'})
            self.assertEqual(actual,expected,root)
        fragments=resources.read(resources.D/'tags/item/absence_fragments.json')['values']
        self.assertEqual(set(fragments),{'guogaology:fffz_trace','guogaology:fos_trace'})
        branches=resources.read(resources.D/'tags/item/ordinal_branch_stones.json')['values']
        self.assertEqual(set(branches),{'guogaology:'+name for name in ('epsilon_symbol','omega_symbol','great_omega_symbol','zeta_symbol','psi_symbol','phi_symbol')})

    def test_removed_materials_have_no_live_inventory_or_recipe_entrypoints(self):
        balance=resources.read(resources.RES/'guogaology/block_balance.json')
        for name in ('arrow_knot','laver_gel','dread_resin'):
            self.assertNotIn(name,balance)
            self.assertFalse((resources.A/f'models/item/{name}.json').exists())
            self.assertFalse((resources.A/f'blockstates/{name}.json').exists())
            for recipe in (resources.D/'recipe').glob('*.json'):
                self.assertNotIn('guogaology:'+name+'"',recipe.read_text('utf8'),recipe.name)

    def test_secondary_material_bundles_count_repeated_shaped_cells(self):
        self.assertEqual(loot.raw_recipe('astra')['compute_chip'],4)
        self.assertEqual(loot.raw_recipe('astra')['cloud_pearl'],4)
        self.assertEqual(loot.raw_recipe('weaver')['iblp_node'],2)
        self.assertEqual(loot.raw_recipe('matrix')['ordinal_bricks'],2)
        self.assertEqual(loot.raw_recipe('matrix')['matrix_sheet'],4)
        for theme in loot.THEMES:
            raw=loot.raw_recipe(theme)
            self.assertEqual(sum(raw.values()),8,theme)
            self.assertNotIn('ordinal_crystal',raw,theme)

    def test_matrix_recipe_and_union_materials(self):
        recipe=cells(resources.read(resources.D/'recipe/sequence_core.json'))
        self.assertEqual(recipe.count({'item':'guogaology:ordinal_bricks'}),2)
        self.assertEqual(recipe.count({'item':'guogaology:matrix_sheet'}),4)
        self.assertEqual(recipe.count({'item':'guogaology:ridge_lamina'}),2)
        pearl=resources.read(resources.A/'models/block/cloud_pearl.json')
        self.assertEqual(pearl,{'parent':'minecraft:block/cube_all','textures':{'all':'guogaology:block/cloud_pearl'}})
        membrane=resources.read(resources.A/'models/block/set_membrane.json')
        self.assertEqual({f['texture'] for e in membrane['elements'] for f in e['faces'].values()}, {'#body'})

    def test_guogao_has_native32_expression_on_six_lightly_rounded_faces(self):
        from guogao_block_art import guogao_face_texture,guogao_cube_model
        model=resources.read(resources.A/'models/block/guogao_drop.json')
        self.assertEqual(model,guogao_cube_model())
        self.assertEqual(model['parent'],'minecraft:block/block')
        self.assertEqual(model['textures']['body'],'guogaology:block/guogao_drop')
        self.assertEqual(len(model['elements']),30)
        broad={}
        for element in model['elements']:
            self.assertEqual(len(element['faces']),1)
            direction,face=next(iter(element['faces'].items()))
            self.assertEqual(face['texture'],'#body')
            for coordinate in element['from']+element['to']:
                self.assertGreaterEqual(coordinate,0)
                self.assertLessEqual(coordinate,16)
                self.assertEqual(coordinate,int(coordinate),'one model-unit step, without half-unit tiers')
            if 'cullface' in face:
                broad[direction]=face
                self.assertEqual(face['uv'],[1,1,15,15])
        self.assertEqual(set(broad),{'up','down','north','south','west','east'})
        actual=Image.open(resources.A/'textures/block/guogao_drop.png').convert('RGBA')
        self.assertEqual(actual.size,(32,32))
        self.assertEqual(actual.tobytes(),guogao_face_texture().tobytes())
        self.assertEqual(actual.getchannel('A').getextrema(),(255,255))
        features={(48,57,50),(99,66,13),(96,57,12),(15,132,192),(86,210,244),(229,253,255)}
        for y in range(32):
            for x in range(32):
                if actual.getpixel((x,y))[:3] in features:
                    self.assertTrue(2<=x<=29 and 2<=y<=29,'complete features stay outside the corner steps')
        for cap in ('top','bottom'):
            self.assertFalse((resources.A/f'textures/block/guogao_drop_{cap}.png').exists())

    def test_membrane_emits_exact_union_exterior_without_duplicate_faces(self):
        from set_membrane_art import BOXES,membrane_model,membrane_texture
        model=resources.read(resources.A/'models/block/set_membrane.json')
        self.assertEqual(model,membrane_model())
        texture=membrane_texture()
        self.assertEqual(texture.tobytes(),texture.transpose(Image.Transpose.FLIP_LEFT_RIGHT).tobytes())
        self.assertEqual(texture.tobytes(),texture.transpose(Image.Transpose.FLIP_TOP_BOTTOM).tobytes())
        occupied={(x,y,z) for a,b in BOXES for x in range(a[0],b[0]) for y in range(a[1],b[1]) for z in range(a[2],b[2])}
        expected=set()
        directions={'west':(0,-1),'east':(0,1),'down':(1,-1),'up':(1,1),'north':(2,-1),'south':(2,1)}
        for p in occupied:
            for direction,(axis,sign) in directions.items():
                neighbor=list(p);neighbor[axis]+=sign
                if tuple(neighbor) not in occupied:
                    other=[k for k in range(3) if k!=axis]
                    expected.add((direction,p[axis]+(sign>0),p[other[0]],p[other[1]]))
        actual=Counter()
        for element in model['elements']:
            for direction,face in element['faces'].items():
                axis,_=directions[direction];other=[k for k in range(3) if k!=axis]
                self.assertTrue(all(0<=value<=16 for value in face['uv']))
                for u in range(element['from'][other[0]],element['to'][other[0]]):
                    for v in range(element['from'][other[1]],element['to'][other[1]]):
                        actual[(direction,element['from'][axis],u,v)]+=1
        self.assertEqual(set(actual),expected)
        self.assertEqual(set(actual.values()),{1})

    def test_lamp_shaper_recipe_has_one_output(self):
        recipe=resources.read(resources.D/'recipe/guogao_lamp_shaper.json')
        self.assertEqual(recipe['result'],{'id':'guogaology:guogao_lamp_shaper','count':1})
        self.assertEqual(recipe['key']['L'],{'tag':'guogaology:guogao_lamps'})


if __name__=='__main__':unittest.main()
