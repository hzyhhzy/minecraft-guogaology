"""Version schemas must retain the imported recipe/leaf behavior after regeneration."""
import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_outer_resources import recipe_unlocks, prepare
from prepare_port_resources import convert_263_misc
from package_multiversion import verify_gel_loot_policy

ROOT = Path(__file__).resolve().parents[2]


class ImportedResources(unittest.TestCase):
    def setUp(self):
        (ROOT / 'build').mkdir(exist_ok=True)

    def test_recipe_unlock_uses_target_schema(self):
        with tempfile.TemporaryDirectory(dir=ROOT / 'build') as folder:
            root = Path(folder)
            path = root / 'sample.json'
            path.write_text(json.dumps({'ingredients': ['minecraft:apple', 'guogaology:fruit_cake_gel']}))
            for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
                recipe_unlocks(root, [path], target)
                result = json.loads((root / 'advancement/recipes/sample.json').read_text())
                actual = result['criteria']['has_recipe']['conditions']
                expected = {'recipes': ['guogaology:sample']} if target == '26.3' else {'recipe': 'guogaology:sample'}
                self.assertEqual(actual, expected)
                self.assertIn('guogaology:fruit_cake_gel', json.dumps(result))

    def test_block_state_and_nested_loot_conversion(self):
        with tempfile.TemporaryDirectory(dir=ROOT / 'build') as folder:
            root = Path(folder)
            path = root / 'data/guogaology/loot_table/blocks/example.json'
            path.parent.mkdir(parents=True)
            predicate = {'condition': 'minecraft:block_state_property', 'block': 'guogaology:dread_door', 'properties': {'half': 'lower'}}
            path.write_text(json.dumps({'pools': [{'conditions': [predicate]}]}))
            convert_263_misc(root)
            self.assertEqual(json.loads(path.read_text())['pools'][0]['condition'],
                             {'type': 'minecraft:match_block', 'blocks': 'guogaology:dread_door', 'state': {'half': 'lower'}})

    def test_leaf_predicates_keep_alternatives_and_fortune(self):
        source = json.loads((ROOT / 'src/main/resources/data/guogaology/loot_table/blocks/giant_laver.json').read_text())
        children = source['pools'][0]['entries'][0]['children']
        self.assertEqual([x['name'] for x in children], ['guogaology:giant_laver', 'guogaology:laver_ribbon'])
        collection = children[0]['conditions'][0]
        self.assertEqual(collection['condition'], 'minecraft:any_of')
        silk, shears = collection['terms']
        self.assertEqual(silk['predicate']['predicates']['minecraft:enchantments'][0]['enchantments'], 'minecraft:silk_touch')
        self.assertEqual(shears, {'condition':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}})
        terms = children[1]['conditions'][0]['terms']
        self.assertEqual(terms[0]['condition'], 'minecraft:survives_explosion')
        self.assertEqual(terms[1]['chances'], [.05, .0625, .083333336, .1])

    def test_shared_recipe_ingredients(self):
        for color in ('amber', 'berry', 'lime', 'azure'):
            source = json.loads((ROOT / f'src/main/resources/data/guogaology/recipe/{color}_guogao.json').read_text())
            ingredients = [x['item'] for x in source['ingredients']]
            self.assertIn('guogaology:fruit_cake_gel', ingredients)
            self.assertNotIn('minecraft:slime_ball', ingredients)
            self.assertEqual(source['result']['count'], 2)

    def test_final_outer_import_preserves_both_current_gel_tables_on_all_targets(self):
        for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
            with self.subTest(target=target), tempfile.TemporaryDirectory(dir=ROOT / 'build') as folder:
                root=Path(folder)
                # The complete donor import includes clear_generated and stale donor
                # entity tables. Its final overlay must restore current host balance.
                prepare(target,root)
                class Archive:
                    def read(self,name):return (root/name).read_bytes()
                verify_gel_loot_policy(Archive(),target)
                for entity,base in (('fruit_cake_slime',2),('fruit_slime',1)):
                    actual=json.loads((root/f'data/guogaology/loot_table/entities/{entity}.json').read_text('utf8'))
                    functions='modifier' if target=='26.3' else 'functions'
                    entry=actual['pools'][0]['entries'][0]
                    self.assertEqual(entry[functions][0]['count'],base)
                    self.assertEqual(entry[functions][1]['enchantment'],'minecraft:looting')

    def test_packaging_rejects_empty_old_random_and_missing_looting_tables(self):
        sources={entity:json.loads((ROOT/f'src/main/resources/data/guogaology/loot_table/entities/{entity}.json').read_text('utf8'))
                 for entity in ('fruit_cake_slime','fruit_slime')}
        for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
            for failure in ('empty_small','old_random_large','missing_looting'):
                with self.subTest(target=target,failure=failure):
                    tables=copy.deepcopy(sources)
                    if failure=='empty_small':tables['fruit_slime'].pop('pools')
                    elif failure=='old_random_large':tables['fruit_cake_slime']['pools'][0]['entries'][0]['functions'][0]['count']={'type':'minecraft:uniform','min':1,'max':2}
                    else:tables['fruit_slime']['pools'][0]['entries'][0]['functions'].pop()
                    if target=='26.3':
                        from prepare_port_resources import convert_263_loot
                        tables={name:convert_263_loot(value) for name,value in tables.items()}
                    class Archive:
                        def read(self,name):return json.dumps(tables[Path(name).stem]).encode()
                    with self.assertRaisesRegex(AssertionError,'Stale/overwritten gel mob loot'):
                        verify_gel_loot_policy(Archive(),target)


if __name__ == '__main__':
    unittest.main()
