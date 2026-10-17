"""26.3 feature imports must use the native block-state codec, including ores."""
import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_port_resources import convert_263_misc, convert_263_value
from package_multiversion import verify_crystal_ore_policy

ROOT = Path(__file__).resolve().parents[2]


class ModernFeatureResources(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        (ROOT / 'build').mkdir(exist_ok=True)

    def test_actual_ordinal_ore_targets_survive_feature_schema_conversion(self):
        source_path = ROOT / 'src/main/resources/data/guogaology/worldgen/configured_feature/ore_ordinal_crystal.json'
        source = json.loads(source_path.read_text(encoding='utf-8'))
        snapshot = copy.deepcopy(source)
        with tempfile.TemporaryDirectory(dir=ROOT / 'build') as directory:
            root = Path(directory)
            old = root / 'data/guogaology/worldgen/configured_feature/ore_ordinal_crystal.json'
            old.parent.mkdir(parents=True)
            old.write_text(json.dumps(source), encoding='utf-8')
            convert_263_misc(root)
            output = json.loads((root / 'data/guogaology/worldgen/feature/ore_ordinal_crystal.json').read_text())
            self.assertFalse(old.exists(), '26.3 uses the feature registry directory')
        self.assertEqual(source, snapshot, 'authoring data for the other targets stays unchanged')
        self.assertEqual(output['type'], 'minecraft:ore')
        self.assertNotIn('config', output)
        self.assertEqual(output['size'], 9)
        self.assertEqual(output['discard_chance_on_air_exposure'], 0.0)
        expected = ['ordinal_crystal_ore', 'empty_crystal_ore', 'dread_crystal_ore']
        self.assertEqual([entry['state'] for entry in output['targets']], ['guogaology:' + name for name in expected])
        self.assertEqual([entry['target'] for entry in output['targets']],
                         [entry['target'] for entry in source['config']['targets']], 'replacement tags and ore behavior stay intact')

    def test_nested_property_states_use_native_id_properties_object(self):
        # The local 26.3 vanilla oak feature uses this exact id/properties shape.
        source = {'type': 'minecraft:tree', 'config': {
            'trunk_provider': {'type': 'minecraft:simple_state_provider',
                               'state': {'Name': 'minecraft:oak_log', 'Properties': {'axis': 'x'}}},
            'decorators': [{'states': [{'Name': 'minecraft:oak_leaves', 'Properties': {'persistent': 'true'}},
                                       {'Name': 'minecraft:air'}]}]}}
        snapshot = copy.deepcopy(source)
        with tempfile.TemporaryDirectory(dir=ROOT / 'build') as directory:
            root = Path(directory)
            old = root / 'data/guogaology/worldgen/configured_feature/tree.json'
            old.parent.mkdir(parents=True)
            old.write_text(json.dumps(source), encoding='utf-8')
            convert_263_misc(root)
            output = json.loads((root / 'data/guogaology/worldgen/feature/tree.json').read_text())
        self.assertEqual(output['trunk_provider']['state'], {'id': 'minecraft:oak_log', 'properties': {'axis': 'x'}})
        self.assertEqual(output['decorators'][0]['states'], [
            {'id': 'minecraft:oak_leaves', 'properties': {'persistent': 'true'}}, 'minecraft:air'])
        self.assertEqual(source, snapshot)

    def test_already_modern_states_and_ordinary_strings_are_unchanged(self):
        source = {'state': {'id': 'minecraft:spruce_log', 'properties': {'axis': 'z'}},
                  'targets': ['minecraft:stone'], 'size': 9}
        self.assertEqual(convert_263_value(source), source)

    def test_release_policy_rejects_legacy_states_even_if_converter_regresses(self):
        source = (ROOT / 'src/main/resources/data/guogaology/worldgen/configured_feature/ore_ordinal_crystal.json').read_bytes()
        class Archive:
            def read(self, name):
                self_name = 'data/guogaology/worldgen/feature/ore_ordinal_crystal.json'
                if name != self_name:
                    raise AssertionError('Legacy states must fail before the other policy checks')
                feature = json.loads(source)
                feature.update(feature.pop('config'))
                return json.dumps(feature).encode()
        # Independence matters: a shared broken converter must not make a bad
        # resource compare equal to its equally broken expected value and pass.
        with patch('prepare_port_resources.convert_263_value', lambda value: value):
            with self.assertRaisesRegex(AssertionError, 'Legacy block-state codec'):
                verify_crystal_ore_policy(Archive(), '26.3')


if __name__ == '__main__':
    unittest.main()
