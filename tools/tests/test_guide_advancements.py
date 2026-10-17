"""Guide graph/resource contracts; actual native predicates are covered in hidden fo262."""
import importlib.util
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('guide057',ROOT/'tools/generate_guide_advancements.py')
guide=importlib.util.module_from_spec(spec);spec.loader.exec_module(guide)

class GuideChecks(unittest.TestCase):
    def test_generated_tree_and_translations_are_current(self):
        guide.generate(check=True)

    def test_reachable_graph_and_real_item_loot_references(self):
        nodes,_,_=guide.build();self.assertEqual(len(nodes),39)
        for name,node in nodes.items():
            seen={name};parent=node.get('parent')
            while parent:
                key=parent.removeprefix('guogaology:');self.assertIn(key,nodes);self.assertNotIn(key,seen)
                seen.add(key);parent=nodes[key].get('parent')
            for criterion in node['criteria'].values():
                conditions=criterion.get('conditions',{})
                if 'loot_table' in conditions:
                    ns,path=conditions['loot_table'].split(':');self.assertTrue((guide.RES/f'data/{ns}/loot_table/{path}.json').is_file())
                for predicate in conditions.get('items',[]):
                    for id in predicate['items']:
                        ns,path=id.split(':');self.assertTrue((guide.RES/f'assets/{ns}/models/item/{path}.json').is_file(),id)
            for group in node['requirements']:
                self.assertTrue(group);self.assertTrue(set(group)<=node['criteria'].keys())
        self.assertEqual(nodes['arrival']['criteria'],{'begin':{'trigger':'minecraft:tick'}})
        self.assertEqual(len(nodes['guide/landmarks_all']['requirements']),8)
        self.assertTrue(all(len(group)==2 for group in nodes['guide/landmarks_all']['requirements']))

    def test_current_names_and_all_biome_material_sources(self):
        _,copy,_=guide.build()
        for locale,index in [('zh_cn',0),('en_us',1)]:
            names=json.loads((guide.RES/f'assets/guogaology/lang/{locale}.json').read_text(encoding='utf8'))
            for theme in guide.THEMES:
                self.assertIn(names['structure.guogaology.'+theme],copy[f'advancement.guogaology.guide.landmark_{theme}.title'][index])
        self.assertTrue({'fffz_trace','fos_trace','lho_hydra_psi','lho_hydra_z'}<=set(guide.RAW))

    def test_no_vanilla_progress_or_recipe_unlock_overlay(self):
        self.assertFalse((guide.RES/'data/minecraft/advancement').exists())
        nodes,_,_=guide.build()
        for node in nodes.values():
            self.assertNotIn('rewards',node)
            self.assertFalse(node['display']['announce_to_chat'])

if __name__=='__main__':unittest.main()
