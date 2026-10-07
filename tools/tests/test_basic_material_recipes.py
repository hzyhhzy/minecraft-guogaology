"""Check the narrow generator without rewriting the resources used by a build."""
import json
from pathlib import Path
import sys
import shutil
import unittest
import uuid

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from generate_merge_progression import generate_basic_material_recipes
from prepare_port_resources import ingredient


class BasicMaterialRecipes(unittest.TestCase):
    def setUp(self):
        # Normal inherited Windows ACLs; tempfile's owner-only directory mode
        # denies access to the sandbox runner's restricted secondary token.
        self.data = (ROOT / 'build' / ('recipe043-check-' + uuid.uuid4().hex)).resolve()
        self.assertTrue(self.data.is_relative_to(ROOT.resolve()))
        self.data.mkdir(parents=True)
        self.addCleanup(shutil.rmtree, self.data)
        self.advanced = self.data / 'recipe/enhancement_table_2.json'
        self.ultimate = self.data / 'recipe/enhancement_table_3.json'
        self.advanced.parent.mkdir(parents=True)
        for path in (self.advanced, self.ultimate):
            path.write_bytes((ROOT / 'src/main/resources/data/googology/recipe' / path.name).read_bytes())
        self.original = {path: path.read_bytes() for path in (self.advanced, self.ultimate)}
        generate_basic_material_recipes(self.data)

    def read(self, relative):
        return json.loads((self.data / relative).read_text(encoding='utf-8'))

    def test_digit_conversion_is_one_way_and_all_ten(self):
        recipe = self.read('recipe/cobblestone_from_number_stones.json')
        self.assertEqual(recipe['type'], 'minecraft:crafting_shapeless')
        self.assertEqual(recipe['ingredients'], [{'tag': 'googology:number_stones'}])
        self.assertEqual(recipe['result'], {'id': 'minecraft:cobblestone', 'count': 1})
        tag = json.loads((ROOT / 'src/main/resources/data/googology/tags/item/number_stones.json').read_text())
        self.assertEqual(tag['values'], ['googology:ordinal_stone' + (f'_{digit}' if digit else '') for digit in range(10)])
        self.assertEqual(ingredient(recipe['ingredients']), ['#googology:number_stones'])
        self.assertFalse(any('ordinal_stone' in p.name for p in (self.data / 'recipe').glob('*.json')))

    def test_basic_station_uses_ungraded_crystal_and_retains_layout(self):
        recipe = self.read('recipe/enhancement_table.json')
        self.assertEqual(recipe['pattern'], [' M ', 'BWB', 'SSS'])
        self.assertEqual(recipe['key']['M'], {'item': 'googology:ordinal_crystal'})
        self.assertEqual(recipe['key']['S'], {'tag': 'minecraft:stone_crafting_materials'})
        self.assertEqual(recipe['result'], {'id': 'googology:enhancement_table', 'count': 1})
        for path, value in self.original.items():
            self.assertEqual(path.read_bytes(), value, 'Higher stations must remain byte-identical')

    def test_unlocks_cover_new_ingredients_and_historical_path(self):
        table = self.read('advancement/recipes/enhancement_table.json')
        self.assertEqual(table, self.read('advancement/recipes/mining/enhancement_table.json'))
        predicates = [criterion['conditions']['items'][0]['items'] for criterion in table['criteria'].values()
                      if criterion['trigger'] == 'minecraft:inventory_changed']
        self.assertIn('googology:ordinal_crystal', predicates)
        self.assertIn('#minecraft:stone_crafting_materials', predicates)
        self.assertNotIn('googology:omega_material', predicates)
        stone = self.read('advancement/recipes/cobblestone_from_number_stones.json')
        self.assertIn('#googology:number_stones', json.dumps(stone))
        self.assertEqual(stone['rewards']['recipes'], ['googology:cobblestone_from_number_stones'])
        before = {p.relative_to(self.data): p.read_bytes() for p in self.data.rglob('*.json')}
        generate_basic_material_recipes(self.data)
        self.assertEqual(before, {p.relative_to(self.data): p.read_bytes() for p in self.data.rglob('*.json')})


if __name__ == '__main__':
    unittest.main()
