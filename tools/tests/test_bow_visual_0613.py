"""Geometry regressions for the16px mineral bow draw sprites."""
import json
from pathlib import Path
import sys
import unittest

from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from generate_inventory_art import bow_icon,METALS


class BowVisual0613Tests(unittest.TestCase):
    def test_limbs_and_arrow_remain_symmetric_about_shooting_axis(self):
        for tier in range(4):
            for stage in (-1,0,1,2):
                with self.subTest(tier=tier,stage=stage):
                    art=bow_icon(tier,stage)
                    self.assertEqual(art.size,(16,16))
                    self.assertEqual(art.tobytes(),art.transpose(Image.Transpose.TRANSPOSE).tobytes())

    def test_arrow_retracts_along_axis_without_sideways_drift(self):
        shaft={(0xf2,0xe6,0xcb,255),(0xa5,0x94,0x73,255)}
        for tier in range(4):
            masks=[]
            for stage in range(3):
                art=bow_icon(tier,stage)
                masks.append({(x,y) for y in range(16) for x in range(16)
                              if art.getpixel((x,y)) in shaft})
            for previous,current in zip(masks,masks[1:]):
                self.assertEqual({(x+1,y+1) for x,y in previous},current)

    def test_fletching_is_whole_and_tracks_nock(self):
        feather=(0xd0,0xd9,0xe5,255)
        for tier in range(4):
            for stage in range(3):
                art=bow_icon(tier,stage)
                nock=9+stage
                for k in (1,2):
                    self.assertEqual(art.getpixel((nock+k,nock+k+1)),feather)
                    self.assertEqual(art.getpixel((nock+k+1,nock+k)),feather)
                # Full draw reaches14, not the16px clip boundary.
                self.assertLess(nock+3,16)

    def test_all_three_draw_frames_are_distinct(self):
        for tier in range(4):
            self.assertEqual(len({bow_icon(tier,s).tobytes() for s in (-1,0,1,2)}),4)

    def test_committed_bow_assets_match_generator(self):
        for tier,metal in enumerate(METALS):
            for stage in (-1,0,1,2):
                suffix='' if stage<0 else f'_pulling_{stage}'
                file=ROOT/'src/main/resources/assets/guogaology/textures/item'/f'{metal}_bow{suffix}.png'
                with Image.open(file) as asset:
                    self.assertEqual(asset.convert('RGBA').tobytes(),bow_icon(tier,stage).tobytes())

    def test_model_transforms_and_draw_thresholds_stay_native(self):
        models=ROOT/'src/main/resources/assets/guogaology/models/item'
        for metal in METALS:
            main=json.loads((models/f'{metal}_bow.json').read_text('utf8'))
            self.assertEqual(main['parent'],'minecraft:item/bow')
            self.assertNotIn('display',main)
            self.assertEqual([v['predicate'].get('pull',0) for v in main['overrides']],[0,.65,.9])
            for stage in range(3):
                frame=json.loads((models/'bows'/f'{metal}_bow_pulling_{stage}.json').read_text('utf8'))
                self.assertEqual(frame['parent'],'minecraft:item/bow')
                self.assertNotIn('display',frame)


if __name__=='__main__':
    unittest.main()
