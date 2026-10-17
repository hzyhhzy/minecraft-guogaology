"""The26.3 tag overlay is explicit, complete and preserves imported/native tags."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from prepare_263_motion_tags import classification,prepare
from audit_block_pixels import registry_inventory

ROOT=Path(__file__).resolve().parents[2]


class MotionTags263(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        (ROOT / 'build').mkdir(exist_ok=True)

    def test_catalog_covers_actual_registrations_and_physical_exceptions(self):
        data=classification()
        groups=[set(data[key]) for key in ('motion_no_leaves','leaf_blocks','legacy_non_motion')]
        self.assertEqual([len(group) for group in groups],[193,2,31])
        self.assertEqual(set.union(*groups),set(registry_inventory()))
        self.assertEqual(groups[1],{'dread_leaves','y_leaves'})
        for name in ('ordinal_stone','ordinal_stone_9','set_glass','set_membrane','cloud_pearl','guogao_drop',
                     'guogao_lamp_shaper','dread_sign','laver_sign','giant_laver','christmas_leaves'):
            self.assertIn(name,groups[0])
        for name in ('matrix_sheet','hydra_sprig','chair','nuke_chair','snake_head','graham','fruit_portal',
                     'lho_trace','fffz_trace','fos_trace','dread_wall_sign','laver_wall_sign'):
            self.assertIn(name,groups[2])
        self.assertEqual(set(data['native_inherited_exceptions']),{'dread_pressure_plate'})

    def test_last_overlay_preserves_donor_additions_and_is_idempotent(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'build') as directory:
            root=Path(directory)
            path=root/'data/minecraft/tags/block/blocks_motion_no_leaves.json'
            path.parent.mkdir(parents=True)
            original={'replace':False,'values':['minecraft:stone','guogaology:ordinal_bricks']}
            path.write_text(json.dumps(original),encoding='utf-8')
            prepare(root);first=path.read_bytes();prepare(root)
            self.assertEqual(first,path.read_bytes())
            result=json.loads(first)
            self.assertFalse(result['replace'])
            self.assertIn('minecraft:stone',result['values'])
            self.assertEqual(result['values'].count('guogaology:ordinal_bricks'),1)
            self.assertEqual(len(result['values']),194)
            data=classification()
            for key in ('leaf_blocks','legacy_non_motion'):
                self.assertFalse({'guogaology:'+name for name in data[key]}&set(result['values']))


if __name__=='__main__':
    unittest.main()
