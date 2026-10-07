"""Portable appearance naming, actual expression mapping, and narrow copy updates."""
import json
from pathlib import Path
import shutil
import sys
import unittest
import uuid
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from update_variant_copy import COPY, generate


class VariantNames(unittest.TestCase):
    def test_copy_is_current_and_preserves_requested_names(self):
        generate(check=True)
        self.assertEqual(COPY['item.guogaology.lantern_emotion.0'][0], '馃槹（张嘴）')
        self.assertEqual(COPY['item.guogaology.lantern_emotion.1'][0], '馃槬（闭嘴）')
        for locale in range(2):
            self.assertNotEqual(COPY['item.guogaology.lantern_emotion.0'][locale], COPY['item.guogaology.lantern_emotion.1'][locale])
            self.assertNotEqual(COPY['item.guogaology.tape_ink.0'][locale], COPY['item.guogaology.tape_ink.1'][locale])

    def test_expression_mapping_uses_real_models_and_mouth_pixels(self):
        assets = ROOT / 'src/main/resources/assets/guogaology'
        for name in ['guogao_lantern', *[color+'_guogao_lantern' for color in ('cyan', 'rose', 'lime', 'violet', 'scarlet')]]:
            variants = json.loads((assets / f'blockstates/{name}.json').read_text())['variants']
            mouth_areas = []
            for emotion in (0, 1):
                model_name = variants[f'emotion={emotion},part=27']['model'].split(':')[1]
                model = json.loads((assets / f'models/{model_name}.json').read_text())
                image_name = model['textures']['face'].split(':')[1]
                with Image.open(assets / f'textures/{image_name}.png') as original:
                    image = original.convert('RGBA')
                    mouth_areas.append(sum(max(image.getpixel((x, y))[:3]) < 120
                        for x in range(image.width//3, image.width*2//3)
                        for y in range(int(image.height*.70), int(image.height*.89))))
                for part in range(27):
                    self.assertEqual(variants[f'emotion={emotion},part={part}']['model'],
                                     'guogaology:'+model_name+f'_part_{part}')
            self.assertGreater(mouth_areas[0], mouth_areas[1]*2,
                               name+': emotion0 is the filled open mouth; emotion1 is the thin closed mouth')

    def test_both_native_item_implementations_cover_all_portable_classes(self):
        blocks = ROOT / 'src/main/java/dev/guogaology/block'
        classes = {path.stem for path in blocks.glob('*.java') if 'extends StatefulDecorBlock' in path.read_text(encoding='utf-8')}
        for name in ['src/main/java/dev/guogaology/block/VariantDecorItem.java', 'ports/common/main/dev/guogaology/block/VariantDecorItem.java']:
            text = (ROOT / name).read_text(encoding='utf-8')
            for kind in classes:
                self.assertIn('instanceof '+kind, text, kind+' is missing visible variant information')
            self.assertIn('"item.guogaology.lantern_emotion."+variant(stack,"emotion",0,1)', text)
            self.assertIn('variant(stack,"part",27,27)', text)
            self.assertIn('property(stack,"ink","false")', text)

    def test_copy_updater_preserves_unrelated_fields_and_is_idempotent(self):
        root = (ROOT / 'build' / ('variant-copy-check-'+uuid.uuid4().hex)).resolve()
        self.assertTrue(root.is_relative_to(ROOT.resolve()))
        root.mkdir(parents=True)
        self.addCleanup(shutil.rmtree, root)
        paths = ['tools/copy_catalog.json', *[f'src/main/resources/assets/guogaology/lang/{locale}.json' for locale in ('zh_cn', 'en_us')]]
        for name in paths:
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            value = json.loads((ROOT / name).read_text(encoding='utf-8'))
            value['unrelated-future-field'] = ['unchanged', {'nested': 43}]
            if name.startswith('tools/'):
                for translations in value['translations'].values():
                    translations['item.guogaology.lantern_emotion.0'] = 'outdated expression'
            else:
                value['item.guogaology.lantern_emotion.0'] = 'outdated expression'
            path.write_text(json.dumps(value, ensure_ascii=False), encoding='utf-8')
        generate(root)
        for name in paths:
            self.assertEqual(json.loads((root / name).read_text(encoding='utf-8'))['unrelated-future-field'], ['unchanged', {'nested': 43}])
        before = {name: (root / name).read_bytes() for name in paths}
        generate(root)
        self.assertEqual(before, {name: (root / name).read_bytes() for name in paths})


if __name__ == '__main__':
    unittest.main()
