"""Guide graph/resource contracts; actual native predicates are covered in hidden fo262."""
import importlib.util
import json
from pathlib import Path
import sys
import unittest

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('guide057',ROOT/'tools/generate_guide_advancements.py')
guide=importlib.util.module_from_spec(spec);spec.loader.exec_module(guide)

class GuideChecks(unittest.TestCase):
    def test_generated_tree_and_translations_are_current(self):
        guide.generate(check=True)

    def test_reachable_graph_and_real_item_loot_references(self):
        nodes,_,_=guide.build();self.assertEqual(len(nodes),76)
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
        self.assertEqual(nodes['arrival']['display']['icon']['id'],'guogaology:brand_emblem')
        self.assertEqual(nodes['guide/enter_outer']['display']['icon']['id'],'guogaology:guide_emblem')
        self.assertEqual((guide.RES/'assets/guogaology/textures/item/brand_emblem.png').read_bytes(),(guide.RES/'assets/guogaology/icon.png').read_bytes())
        for realm,dimension in [('outer','outer'),('inner','guogaology'),('underworld','guogao')]:
            self.assertEqual(nodes['guide/enter_'+realm]['criteria'],guide.travel('guogaology:'+dimension))
        self.assertEqual(nodes['guide/enter_outer']['parent'],'guogaology:arrival')
        for retired in guide.RETIRED_NODES:self.assertNotIn(retired,nodes)
        self.assertEqual(nodes['guide/portal_return']['criteria'],{'done':{'trigger':'minecraft:impossible'}})
        self.assertEqual(nodes['guide/number_tools']['parent'],'guogaology:guide/enter_inner')
        self.assertEqual(nodes['guide/compute_setup']['parent'],'guogaology:guide/material_astra')
        self.assertEqual(len(nodes['guide/landmarks_all']['requirements']),8)
        self.assertTrue(all(len(group)==1 for group in nodes['guide/landmarks_all']['requirements']))
        self.assertEqual(nodes['guide/enter_inner']['parent'],'guogaology:guide/first_omega')
        for goal in ('shards','crystal','ordinal1','ordinal2','ordinal3'):
            parent=nodes['guide/'+goal]['parent']
            while parent!='guogaology:guide/enter_inner':parent=nodes[parent.removeprefix('guogaology:')]['parent']

    def test_current_names_and_all_biome_material_sources(self):
        _,copy,_=guide.build()
        for locale,index in [('zh_cn',0),('en_us',1)]:
            names=json.loads((guide.RES/f'assets/guogaology/lang/{locale}.json').read_text(encoding='utf8'))
            for theme in guide.THEMES:
                self.assertIn(names['structure.guogaology.'+theme],copy[f'advancement.guogaology.guide.landmark_{theme}.title'][index])
        from guide_biomes import THEME_GUIDES
        nodes,_,_=guide.build()
        for theme,core in zip(guide.THEMES,guide.REGIONAL):
            root='guide/enter_underworld' if theme=='guogao' else 'guide/biome_'+theme
            material='guide/material_'+theme
            self.assertEqual(nodes[material]['parent'],'guogaology:'+root)
            self.assertEqual(nodes['guide/landmark_'+theme]['parent'],'guogaology:'+root)
            self.assertEqual(nodes['guide/landmark_'+theme]['criteria'],{'done':{'trigger':'minecraft:impossible'}})
            self.assertEqual(nodes['guide/landmark_'+theme]['display']['icon']['id'],'guogaology:landmark_emblem_'+theme)
            for grade in range(1,4):
                name='guide/core_'+theme+'_'+str(grade)
                self.assertEqual(nodes[name]['parent'],'guogaology:'+(material if grade==1 else name[:-1]+str(grade-1)))
                self.assertEqual(nodes[name]['criteria'],guide.inventory([core+('_lv'+str(grade) if grade>1 else '')]))
                self.assertNotIn('获得',copy['advancement.guogaology.'+name.replace('/','.')+'.title'][0])
                for description in copy['advancement.guogaology.'+name.replace('/','.')+'.description']:
                    self.assertNotIn(' · Lv1',description,'Do not append the base item’s grade label to a higher-grade description')
        self.assertEqual(THEME_GUIDES['absence'][2],[['ordinal_crystal'],['fffz_trace','fos_trace','lho_hydra_psi','lho_hydra_z']])
        self.assertIn('5×5×5',THEME_GUIDES['absence'][4][0])

    def test_small_landmark_models_are_distinct_and_use_real_assets(self):
        models=guide.landmark_models();self.assertEqual(len(models),8)
        self.assertEqual(len({json.dumps(model,sort_keys=True) for model in models.values()}),8)
        for name,model in models.items():
            self.assertTrue(5<=len(model['elements'])<=24,name)
            self.assertEqual(json.loads((guide.RES/f'assets/guogaology/models/item/{name}.json').read_text()),model)
            for texture in model['textures'].values():
                ns,path=texture.split(':');self.assertIn(ns,('guogaology','minecraft'))
                if ns=='guogaology':self.assertTrue((guide.RES/f'assets/{ns}/textures/{path}.png').is_file(),texture)

    def test_scenery_icons_and_order_do_not_depend_on_resource_iteration(self):
        from collections import OrderedDict
        nodes,_,_=guide.build();locations=guide.positions(nodes)
        self.assertEqual(locations,guide.positions(OrderedDict(reversed(list(nodes.items())))))
        self.assertEqual(len(locations),76)
        # Distinct siblings must have room for a26-pixel native advancement frame.
        values=list(locations.values())
        for i,(x,y) in enumerate(values):
            for xx,yy in values[i+1:]:self.assertTrue(abs(x-xx)>=1 or abs(y-yy)>=1)
        previous=-1
        for theme in guide.THEMES:
            root='guogaology:guide/'+('enter_underworld' if theme=='guogao' else 'biome_'+theme)
            self.assertGreater(locations[root][1],previous);previous=locations[root][1]
            icon=nodes[root.removeprefix('guogaology:')]['display']['icon']['id']
            self.assertEqual(icon,'guogaology:'+guide.BIOME_ICONS[theme])
            self.assertNotIn(icon.removeprefix('guogaology:'),guide.REGIONAL+('ordinal_crystal',))
            self.assertTrue((guide.RES/f'assets/guogaology/models/item/{guide.BIOME_ICONS[theme]}.json').is_file())
        for id,node in nodes.items():
            if 'parent' in node:self.assertGreater(locations['guogaology:'+id][0],locations[node['parent']][0])

    def test_no_vanilla_progress_or_recipe_unlock_overlay(self):
        self.assertFalse((guide.RES/'data/minecraft/advancement').exists())
        nodes,_,_=guide.build()
        for node in nodes.values():
            self.assertNotIn('rewards',node)
            self.assertFalse(node['display']['announce_to_chat'])

if __name__=='__main__':unittest.main()
