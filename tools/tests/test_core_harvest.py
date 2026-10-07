"""Focused data audit; native mining/loot is exercised by CoreHarvest043Checks."""
import copy
import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('core_harvest', ROOT / 'tools/generate_core_harvest.py')
harvest = importlib.util.module_from_spec(spec)
spec.loader.exec_module(harvest)


class CoreHarvestChecks(unittest.TestCase):
    def test_generated_data_current(self):
        harvest.generate(check=True)

    def test_display_grade_mapping_and_scope(self):
        self.assertEqual(len(harvest.HARDNESS), 35)
        for grade, hardness in enumerate((25, 50, 100), 1):
            for root in harvest.REGIONAL:
                self.assertEqual(harvest.HARDNESS[root + ('' if grade == 1 else f'_lv{grade}')], hardness)
            self.assertEqual(harvest.HARDNESS[f'ordinal_crystal_lv{grade + 1}'], hardness)
        self.assertEqual(harvest.HARDNESS['ordinal_crystal'], 2)
        original = harvest.read(harvest.RES / 'guogaology/block_balance.json')
        original['unrelated'] = {'hardness': 734, 'tier': 2, 'custom': ['preserve']}
        snapshot = copy.deepcopy(original)
        generated = harvest.profile_updates(original)
        self.assertEqual(original, snapshot, 'generator must not mutate its inputs')
        for name in original.keys() - harvest.HARDNESS.keys():
            self.assertEqual(generated[name], original[name], name)
        for name in harvest.HARDNESS:
            for key in ('resistance', 'light', 'drop', 'friction', 'collision', 'renewable'):
                self.assertEqual(generated[name][key], original[name][key], (name, key))

    def test_ordinal_glowstone_drop_contract(self):
        loot = harvest.read(harvest.RES / 'data/guogaology/loot_table/blocks/ordinal_crystal.json')
        alternatives = loot['pools'][0]['entries'][0]
        self.assertEqual(alternatives['type'], 'minecraft:alternatives')
        silk, ordinary = alternatives['children']
        self.assertEqual(silk['name'], 'guogaology:ordinal_crystal')
        self.assertEqual(silk['conditions'][0]['predicate']['predicates']['minecraft:enchantments'][0],
                         {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}})
        self.assertEqual(ordinary['name'], 'guogaology:ordinal_shard')
        self.assertNotIn('conditions', ordinary)
        functions = {entry['function']: entry for entry in ordinary['functions']}
        self.assertEqual(functions['minecraft:set_count']['count'], {'type': 'minecraft:uniform', 'min': 2, 'max': 4})
        self.assertEqual(functions['minecraft:apply_bonus']['enchantment'], 'minecraft:fortune')
        self.assertEqual(functions['minecraft:apply_bonus']['formula'], 'minecraft:uniform_bonus_count')
        self.assertEqual(functions['minecraft:limit_count']['limit']['max'], 4)
        self.assertIn('minecraft:explosion_decay', functions)

    def test_non_lho_core_loot_has_no_tool_condition(self):
        for name in harvest.HARDNESS.keys() - harvest.ANCHORED - {'ordinal_crystal'}:
            loot = harvest.read(harvest.RES / f'data/guogaology/loot_table/blocks/{name}.json')
            self.assertNotIn('minecraft:match_tool', json.dumps(loot), name)
            self.assertEqual(loot['pools'][0]['entries'][0]['name'], 'guogaology:' + name)


if __name__ == '__main__':
    unittest.main()
