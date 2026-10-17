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
        self.assertEqual(harvest.HARDNESS['ordinal_crystal'], 1.5)
        self.assertEqual(len(harvest.BREAK_TICKS),27)
        for grade, ticks in enumerate((100,200,400),1):
            for root in harvest.REGIONAL:
                self.assertEqual(harvest.BREAK_TICKS[root+('' if grade==1 else f'_lv{grade}')],ticks)
            self.assertEqual(harvest.BREAK_TICKS[f'ordinal_crystal_lv{grade+1}'],ticks)
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
            if name in harvest.BREAK_TICKS:
                self.assertEqual(generated[name]['fixed_break_ticks'],harvest.BREAK_TICKS[name])
                self.assertEqual(generated[name]['tool'],'hand')

    def test_ordinal_crystal_complete_drop_contract(self):
        loot = harvest.read(harvest.RES / 'data/guogaology/loot_table/blocks/ordinal_crystal.json')
        self.assertEqual(loot['pools'][0]['entries'], [{'type':'minecraft:item','name':'guogaology:ordinal_crystal'}])
        self.assertNotIn('minecraft:match_tool',json.dumps(loot))
        self.assertNotIn('guogaology:ordinal_shard',json.dumps(loot))
        for name in ('tree_node_red','tree_node_green','tree_node_blue'):
            self.assertEqual(harvest.HARDNESS[name],1.5,'TREE nodes are ordinary crafting materials')

    def test_non_lho_core_loot_has_no_tool_condition(self):
        for name in harvest.HARDNESS.keys() - harvest.ANCHORED - {'ordinal_crystal'}:
            loot = harvest.read(harvest.RES / f'data/guogaology/loot_table/blocks/{name}.json')
            self.assertNotIn('minecraft:match_tool', json.dumps(loot), name)
            self.assertEqual(loot['pools'][0]['entries'][0]['name'], 'guogaology:' + name)


if __name__ == '__main__':
    unittest.main()
