"""Iron-density and mother-rock/biome scope regressions for Ordinal Crystal ore."""
from copy import deepcopy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from zipfile import ZipFile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from generate_ordinal_crystal_ore import (ROOT, BIOMES, INNER_BIOMES, UNDERWORLD_BIOMES,
    BATCHES, HOSTS, FEATURES, ORE_IDS, add_biome_features, generate, ore_config, placed_config, owned_resources)


class OrdinalCrystalOreTests(unittest.TestCase):
    def test_exact_three_iron_batches_and_native_feature(self):
        expected = {'upper': (90, 'trapezoid', {'absolute': 80}, {'absolute': 384}, 9),
                    'middle': (10, 'trapezoid', {'absolute': -24}, {'absolute': 56}, 9),
                    'small': (10, 'uniform', {'above_bottom': 0}, {'absolute': 72}, 4)}
        self.assertEqual(BATCHES, expected)
        for size in (9, 4):
            config = ore_config(size)
            self.assertEqual(config['type'], 'minecraft:ore')
            self.assertEqual(config['config']['size'], size)
            self.assertEqual(config['config']['discard_chance_on_air_exposure'], 0)

    def test_matches_installed_vanilla_iron_resources_when_available(self):
        jar = Path('C:/Users/sigmoid/.gradle/caches/fabric-loom/26.2/minecraft-client.jar')
        if not jar.exists():
            self.skipTest('Local vanilla benchmark JAR is optional outside this workstation')
        with ZipFile(jar) as archive:
            for batch in BATCHES:
                native = json.loads(archive.read(f'data/minecraft/worldgen/placed_feature/ore_iron_{batch}.json'))
                ours = placed_config(batch)
                self.assertEqual(ours['placement'], native['placement'], batch)
            for suffix, size in (('', 9), ('_small', 4)):
                native = json.loads(archive.read(f'data/minecraft/worldgen/configured_feature/ore_iron{suffix}.json'))
                ours = ore_config(size)
                for field in ('type',):
                    self.assertEqual(ours[field], native[field])
                for field in ('size', 'discard_chance_on_air_exposure'):
                    self.assertEqual(ours['config'][field], native['config'][field])

    def test_only_twelve_exact_host_blocks_and_no_minerals_or_bedrock(self):
        hosts = [value for values in HOSTS.values() for value in values]
        expected = {'guogaology:ordinal_stone' + (f'_{i}' if i else '') for i in range(10)}
        expected.update(('guogaology:absence_glass', 'guogaology:rootbound_stone'))
        self.assertEqual(set(hosts), expected)
        targets = ore_config(9)['config']['targets']
        self.assertEqual([target['state']['Name'] for target in targets], ['guogaology:' + ore for ore in ORE_IDS])
        self.assertFalse(any('bedrock' in value or value.endswith('_ore') for value in hosts))

    def test_all_eleven_inner_underworld_biomes_attach_ore_once_at_step_six(self):
        self.assertEqual(len(INNER_BIOMES), 7)
        self.assertEqual(len(UNDERWORLD_BIOMES), 4)
        self.assertEqual(len(set(BIOMES)), 11)
        for name in BIOMES:
            biome = json.loads((ROOT / f'src/main/resources/data/guogaology/worldgen/biome/{name}.json').read_text())
            for feature in FEATURES:
                self.assertEqual(biome['features'][6].count(feature), 1, name)
                self.assertEqual(sum(stage.count(feature) for stage in biome['features']), 1, name)

    def test_biome_update_preserves_other_features_and_is_idempotent(self):
        fixture = {'effects': {'sky_color': 123}, 'features': [[], [], [], [], [], [], ['minecraft:ore_coal'], [], [], ['guogaology:monumental_landmarks']]}
        before = deepcopy(fixture)
        updated = add_biome_features(fixture)
        self.assertEqual(fixture, before)
        self.assertEqual(updated['features'][6][0], 'minecraft:ore_coal')
        self.assertEqual(updated['features'][9], fixture['features'][9])
        self.assertEqual(add_biome_features(updated), updated)

    def test_generated_configs_roundtrip_and_outer_biomes_are_untouched(self):
        scratch = ROOT / 'build/test-ordinal-crystal-ore'
        scratch.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as temp:
            resources = Path(temp)
            folder = resources / 'data/guogaology/worldgen/biome'
            folder.mkdir(parents=True)
            for name in BIOMES:
                (folder / f'{name}.json').write_text(json.dumps({'features': [[] for _ in range(11)]}))
            sentinel = folder / 'bms_mountains.json'
            sentinel.write_text('{"outer":"untouched"}')
            generate(resources)
            self.assertEqual(sentinel.read_text(), '{"outer":"untouched"}')
            self.assertEqual(generate(resources, check=True), [])
            for relative, expected in owned_resources().items():
                committed = ROOT / 'src/main/resources/data/guogaology' / relative
                self.assertEqual(json.loads(committed.read_text()), expected)


if __name__ == '__main__':
    unittest.main()
