"""Keep native sign boards/text in the correct renderer on all four targets."""
from io import BytesIO
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_sign_resources import FAMILIES, MODELS, legacy_sign_texture, prepare, verify_archive

ROOT = Path(__file__).resolve().parents[2]


class SignResourcesTests(unittest.TestCase):
    def fixture(self, root):
        for family in FAMILIES:
            for shape in MODELS:
                relative = f'assets/guogaology/models/block/{family}_{shape}.json'
                destination = root / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(ROOT / 'src/main/resources' / relative, destination)
            relative = f'assets/guogaology/textures/block/{family}_sign.png'
            destination = root / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / 'src/main/resources' / relative, destination)

    def test_legacy_uv_faces_preserve_source_pixels_without_scaling(self):
        source = Image.new('RGBA', (32, 32))
        for y in range(32):
            for x in range(32):
                source.putpixel((x, y), (x, y, 127, 255))
        result = legacy_sign_texture(source)
        self.assertEqual(result.size, (64, 32))
        # Native old entity UV positions versus native modern template face pixels.
        for destination, origin in (
                ((2, 2), (0, 2)), ((25, 13), (23, 13)),  # front
                ((28, 2), (0, 16)), ((51, 13), (23, 27)),  # back
                ((0, 2), (24, 16)), ((26, 2), (24, 2)),  # edges
                ((2, 0), (0, 0)), ((26, 0), (0, 29)), ((49, 1), (23, 28)),  # caps
                ((2, 16), (28, 0)), ((6, 16), (28, 16)),  # post front/back
                ((0, 16), (30, 16)), ((4, 16), (30, 0)),  # post edges
                ((4, 14), (28, 31))):  # post bottom
            with self.subTest(destination=destination):
                self.assertEqual(result.getpixel(destination), source.getpixel(origin))
        self.assertEqual(sum(pixel[3] != 0 for pixel in result.getdata()), 840)
        self.assertLessEqual(set(result.getdata()), set(source.getdata()) | {(0, 0, 0, 0)})

    def test_only_old_targets_gain_entity_atlas_and_particle_only_models(self):
        (ROOT / 'build').mkdir(exist_ok=True)
        for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
            with self.subTest(target=target), tempfile.TemporaryDirectory(dir=ROOT / 'build') as folder:
                root = Path(folder)
                self.fixture(root)
                before = {p.relative_to(root): p.read_bytes() for p in root.rglob('*') if p.is_file()}
                prepare(root, target)
                first = {p.relative_to(root): p.read_bytes() for p in root.rglob('*') if p.is_file()}
                prepare(root, target)
                self.assertEqual(first, {p.relative_to(root): p.read_bytes() for p in root.rglob('*') if p.is_file()})
                class Archive:
                    def read(self, name):
                        return (root / name).read_bytes()
                verify_archive(Archive(), target)
                if target in ('26.2', '26.3'):
                    self.assertEqual(before, first)
                else:
                    self.assertEqual(len(first) - len(before), 2)
                    for family in FAMILIES:
                        for shape in MODELS:
                            model = json.loads((root / f'assets/guogaology/models/block/{family}_{shape}.json').read_text('utf8'))
                            self.assertEqual(model, {'textures': {'particle': f'guogaology:block/{family}_planks'}})

    def test_legacy_packaging_rejects_missing_models_and_changed_atlas_pixels(self):
        with tempfile.TemporaryDirectory(dir=ROOT / 'build') as folder:
            root = Path(folder)
            self.fixture(root)
            prepare(root, '1.21.11')
            class Archive:
                def read(self, name):
                    return (root / name).read_bytes()
            model = root / 'assets/guogaology/models/block/laver_wall_sign.json'
            model.write_text('{"parent":"minecraft:block/template_wall_sign"}', encoding='utf8')
            with self.assertRaisesRegex(AssertionError, 'native sign model'):
                verify_archive(Archive(), '1.21.11')
            model.write_text('{"textures":{"particle":"guogaology:block/laver_planks"}}', encoding='utf8')
            texture = root / 'assets/guogaology/textures/entity/signs/laver.png'
            with Image.open(texture) as image:
                changed = image.convert('RGBA')
            changed.putpixel((2, 2), (1, 2, 3, 4))
            changed.save(texture)
            with self.assertRaisesRegex(AssertionError, 'native sign atlas'):
                verify_archive(Archive(), '1.21.11')


if __name__ == '__main__':
    unittest.main()
