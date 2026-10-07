"""Validate actual committed lamp pixels, state schemas and imported source mappings."""
from pathlib import Path
import json
import sys
import unittest
from PIL import Image
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from generate_lamp_variants import ROOT, ASSET, COLORS, INK
from import_outer_content import walk


class LampVariants(unittest.TestCase):
    def test_every_actual_pixel_outside_glyph_is_the_shared_substrate(self):
        checked=0
        for color in range(6):
            base=Image.open(ASSET/f'textures/block/sequence_light_{color}.png').convert('RGBA')
            seen=set()
            for digit in range(33):
                image=Image.open(ASSET/f'textures/block/christmas_digit_{color}_{digit}.png').convert('RGBA')
                self.assertEqual(image.size,(32,32));seen.add(image.tobytes())
                glyph=0
                for pixel,substrate in zip(image.getdata(),base.getdata()):
                    if pixel==INK:glyph+=1
                    else:self.assertEqual(pixel,substrate,(color,digit,checked));checked+=1
                self.assertGreater(glyph,40)
            self.assertEqual(len(seen),33,'all glyphs remain distinct')
        self.assertGreater(checked,130000)

    def test_distinct_registry_and_item_state_entrypoints(self):
        for color,name in enumerate(COLORS):
            normal=json.loads((ASSET/f'blockstates/{name}_light.json').read_text('utf8'))
            self.assertEqual(normal,{'variants':{'':{'model':f'guogaology:block/sequence_light_{color}'}}})
            numeric=json.loads((ASSET/f'blockstates/{name}_sequence_light.json').read_text('utf8'))
            self.assertEqual(set(numeric['variants']),{f'digit={n}' for n in range(33)})
            item=json.loads((ASSET/f'models/item/{name}_light.json').read_text('utf8'))
            self.assertNotIn('overrides',item)
            numbers=json.loads((ASSET/f'models/item/{name}_sequence_light.json').read_text('utf8'))
            self.assertEqual(len(numbers['overrides']),33)
            self.assertEqual(numbers['parent'],f'guogaology:block/christmas_digit_{color}_0')
            self.assertEqual(walk({'id':'googology:christmas_light','properties':{'color':str(color)}}),{'id':f'guogaology:{name}_light'})

    def test_generator_never_reintroduces_blank_number_state(self):
        for folder in ('src/main/java','ports/common/main'):
            block=(ROOT/f'{folder}/dev/guogaology/block/ChristmasDigitBlock.java').read_text('utf8')
            self.assertNotIn('BLANK',block);self.assertIn('MAX_DIGIT=32',block)
            plain=(ROOT/f'{folder}/dev/guogaology/block/ChristmasLightBlock.java').read_text('utf8')
            self.assertNotIn('ChristmasDigitBlock',plain);self.assertNotIn('StatefulDecorBlock',plain)
        source=(ROOT/'tools/import_outer_content.py').read_text('utf8')
        self.assertNotIn("'digit','33'",source);self.assertNotIn("props['digit']='33'",source)


if __name__=='__main__':unittest.main()
