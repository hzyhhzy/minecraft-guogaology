"""Keep horizontal placement and 0.6.8 world/item appearance compatible across ports."""
import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from generate_decor_orientation import ASSETS, BLOCKS, ROOT, blockstate, generate
from package_multiversion import verify_decor_orientation
from prepare_outer_legacy import legacy
from prepare_outer_resources import prepare
from prepare_port_resources import convert_items
from prepare_port_sources import adapt

BASELINE = json.loads((Path(__file__).parent / 'fixtures/decor_orientation_068.json').read_text('utf8'))


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


class DirectoryArchive:
    def __init__(self, root):
        self.root = root

    def read(self, path):
        return (self.root / path).read_bytes()


class DecorOrientationTests(unittest.TestCase):
    def copy_assets(self, root):
        assets = root / 'assets/guogaology'
        for name in BLOCKS:
            for relative in (f'blockstates/{name}.json', f'models/block/{name}.json', f'models/item/{name}.json', f'items/{name}.json'):
                source = ASSETS / relative
                if source.exists():
                    path = assets / relative
                    path.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(source, path)
        return assets

    def test_old_saved_facings_and_machine_default_keep_the_same_models(self):
        self.assertEqual(generate(check=True), [])
        for name in BLOCKS:
            old = BASELINE['oldBlockstates'][name]['variants']
            new = json.loads((ASSETS / f'blockstates/{name}.json').read_text('utf8'))['variants']
            self.assertEqual(set(new), {'facing=' + side for side in ('north', 'east', 'south', 'west')})
            if name in ('server_rack', 'office_monitor'):
                self.assertEqual(new['facing=north'], old[''], 'Old property-free saves must retain unrotated appearance')
                self.assertEqual(new['facing=north'].get('y', 0), 0)
            else:
                self.assertEqual(new, old, 'Existing saved chair/snake facing values must not change model selection')

    def test_models_items_and_textures_are_unchanged_from_068(self):
        for key in ('canonicalModelSha256', 'canonicalItemSelectorSha256'):
            for relative, expected in BASELINE[key].items():
                with self.subTest(resource=relative):
                    self.assertEqual(digest(json.loads((ASSETS / relative).read_text('utf8'))), expected)
        for relative, expected in BASELINE['textureSha256'].items():
            with self.subTest(texture=relative):
                self.assertEqual(hashlib.sha256((ASSETS / relative).read_bytes()).hexdigest(), expected)

    def test_model_yaw_matches_all_four_horizontal_normals(self):
        directions = {'north': (0, -1), 'east': (1, 0), 'south': (0, 1), 'west': (-1, 0)}
        for name in BLOCKS:
            variants = json.loads((ASSETS / f'blockstates/{name}.json').read_text('utf8'))['variants']
            for side, normal in directions.items():
                variant = variants['facing=' + side]
                x, z = directions['north']
                for _ in range(variant.get('y', 0) // 90):
                    x, z = -z, x
                self.assertEqual((x, z), normal, (name, side))
                self.assertEqual(variant['model'], 'guogaology:block/' + name)

    def test_generator_only_changes_stale_states_and_check_is_read_only(self):
        with tempfile.TemporaryDirectory(dir=ROOT / 'build') as directory:
            assets = self.copy_assets(Path(directory))
            before = {p.relative_to(assets): p.read_bytes() for p in assets.rglob('*.json')}
            for name in ('server_rack', 'office_monitor'):
                (assets / f'blockstates/{name}.json').write_text(json.dumps(BASELINE['oldBlockstates'][name]), encoding='utf8')
            stale = {p: p.read_bytes() for p in assets.rglob('*.json')}
            with self.assertRaisesRegex(SystemExit, 'Stale horizontal furniture states'):
                generate(True, assets)
            self.assertEqual(stale, {p: p.read_bytes() for p in assets.rglob('*.json')})
            changed = generate(False, assets)
            self.assertEqual({p.name for p in changed}, {'server_rack.json', 'office_monitor.json'})
            self.assertFalse(generate(True, assets))
            for relative, data in before.items():
                if str(relative).replace('\\', '/') not in ('blockstates/server_rack.json', 'blockstates/office_monitor.json'):
                    self.assertEqual((assets / relative).read_bytes(), data)

    def test_actual_item_conversion_and_final_donor_import_on_four_targets(self):
        for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
            with self.subTest(target=target), tempfile.TemporaryDirectory(dir=ROOT / 'build') as directory:
                root = Path(directory)
                self.copy_assets(root)
                if target != '1.21.1':
                    convert_items(root)
                prepare(target, root)
                verify_decor_orientation(DirectoryArchive(root), target)

    def test_final_archive_guard_rejects_lost_facings_rotation_and_changed_item(self):
        for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
            with tempfile.TemporaryDirectory(dir=ROOT / 'build') as directory:
                root = Path(directory)
                assets = self.copy_assets(root)
                if target != '1.21.1':
                    convert_items(root)
                archive = DirectoryArchive(root)
                verify_decor_orientation(archive, target)
                for name in BLOCKS:
                    path = assets / f'blockstates/{name}.json'
                    original = path.read_bytes()
                    bad = blockstate(name)
                    bad['variants']['facing=north']['y'] = 90
                    path.write_text(json.dumps(bad), encoding='utf8')
                    with self.subTest(target=target, block=name), self.assertRaisesRegex(AssertionError, 'furniture state'):
                        verify_decor_orientation(archive, target)
                    path.write_bytes(original)
                path = assets / 'models/item/server_rack.json'
                path.write_text(json.dumps({'parent': 'guogaology:block/office_monitor'}), encoding='utf8')
                with self.assertRaisesRegex(AssertionError, 'furniture item model'):
                    verify_decor_orientation(archive, target)

    def test_api_adapters_preserve_placement_rotation_mirror_and_existing_shapes(self):
        # These APIs have matching native signatures in every supported Mojang
        # port; the actual gameplay/codec checks remain in the native QA suite.
        for name in ('ChairBlock.java', 'SnakeHeadBlock.java'):
            common = (ROOT / 'ports/common/main/dev/guogaology/outer/block' / name).read_text('utf8')
            override = (ROOT / 'ports/26.3/src/main/java/dev/guogaology/outer/block' / name).read_text('utf8')
            self.assertEqual(common, override, 'An explicit 26.3 override must not drop the common fix')
            if name == 'ChairBlock.java':
                constants = common[common.index('   private static final VoxelShape SEAT'):common.index('   public ChairBlock')]
                self.assertEqual(hashlib.sha256(constants.encode()).hexdigest(), BASELINE['chairCollisionConstantsSha256'])
            legacy_source = legacy(common, name)
            for source in (common, legacy_source):
                self.assertIn('context.getHorizontalDirection().getOpposite()', source)
                self.assertIn('public BlockState rotate(BlockState state, Rotation rotation)', source)
                self.assertIn('public BlockState mirror(BlockState state, Mirror mirror)', source)
                self.assertIn('rotation.rotate(state.getValue(', source)
                self.assertIn('mirror.getRotation(state.getValue(', source)
                self.assertIn('Direction.NORTH', source)
        nuke = (ROOT / 'ports/common/main/dev/guogaology/outer/block/NukeChairBlock.java').read_text('utf8')
        self.assertIn('extends ChairBlock', nuke)
        self.assertNotIn('getStateForPlacement', nuke)
        common = (ROOT / 'ports/common/main/dev/guogaology/block/HorizontalDecorBlock.java').read_text('utf8')
        for target in ('1.21.11', '26.2', '26.3'):
            port = adapt(common, target, 'HorizontalDecorBlock.java')
            self.assertEqual(port, common)
        shared = (ROOT / 'ports/shared-sources.txt').read_text('utf8')
        self.assertNotIn('HorizontalDecorBlock.java', shared, 'Mapping-specific classes must not enter the pure shared-source set')


if __name__ == '__main__':
    unittest.main()
